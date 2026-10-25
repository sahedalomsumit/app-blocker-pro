package com.sahed.app_blocker_pro.api

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The exported bound service other apps talk to, like Shizuku's service. A client binds this and
 * calls the [ICurboxApi] methods. Every call is checked against the calling app's identity: only
 * apps the user has allowed in [ApiAuthStore] may run commands or read state.
 *
 * This lives in the main process so its DataStore writes and broadcasts behave exactly like an in
 * app change.
 */
class CurboxApiService : Service() {

    private val gson = Gson()
    private val commandMutex = Mutex()

    private val binder = object : ICurboxApi.Stub() {

        override fun apiVersion(): Int = CurboxApiContract.API_VERSION

        override fun isGranted(): Boolean = callerAllowed()

        override fun execute(command: String?, args: Bundle?): String {
            if (!callerAllowed()) return CurboxApiContract.STATUS_DENIED
            if (ApiCommand.fromNameOrNull(command) == null) return CurboxApiContract.STATUS_UNKNOWN_COMMAND
            return try {
                // Capture Binder identity before entering any coroutine.
                val callerPackages = packageManager.getPackagesForUid(Binder.getCallingUid()).orEmpty()
                val requestedPackage = args?.getString("provider_package")
                val callerPackage = callerPackages.firstOrNull {
                    (requestedPackage == null || requestedPackage == it) &&
                        ApiAuthStore.isAnyGranted(applicationContext, arrayOf(it))
                } ?: return CurboxApiContract.STATUS_DENIED
                val revision = ApiAuthStore.revision(applicationContext)
                runBlocking {
                    commandMutex.withLock {
                        if (revision != ApiAuthStore.revision(applicationContext) ||
                            !ApiAuthStore.isAnyGranted(applicationContext, arrayOf(callerPackage))) {
                            CurboxApiContract.STATUS_DENIED
                        } else {
                            val applied = CurboxApiCommands.runCommand(
                                applicationContext, command, args ?: Bundle(), callerPackage
                            )
                            if (applied) CurboxApiContract.STATUS_OK else CurboxApiContract.STATUS_FAILED
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "execute failed for $command", e)
                CurboxApiContract.STATUS_FAILED
            }
        }

        override fun query(state: String?): String? {
            if (!callerAllowed()) return null
            return try {
                val values = runBlocking {
                    CurboxApiCommands.queryState(applicationContext, state)
                } ?: return null
                gson.toJson(values)
            } catch (e: Exception) {
                Log.e(TAG, "query failed for $state", e)
                null
            }
        }

        override fun list(kind: String?): String? {
            if (!callerAllowed()) return null
            return try {
                val data = runBlocking {
                    CurboxApiCommands.list(applicationContext, kind)
                } ?: return null
                gson.toJson(data)
            } catch (e: Exception) {
                Log.e(TAG, "list failed for $kind", e)
                null
            }
        }
    }

    private fun callerAllowed(): Boolean {
        val packages = packageManager.getPackagesForUid(Binder.getCallingUid())
        return ApiAuthStore.isAnyGranted(applicationContext, packages)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TAG = "CurboxApiService"
    }
}
