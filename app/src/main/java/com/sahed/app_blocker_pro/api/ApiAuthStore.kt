package com.sahed.app_blocker_pro.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/** API grants live in the main process, alongside the permission activity and API service. */
object ApiAuthStore {
    private const val PREFS = "AppPreferences"
    private const val KEY_ENABLED = "apiEnabled"
    private const val KEY_GRANTS = "apiAuthorizedPackages"
    private const val KEY_CERTIFICATES = "apiAuthorizedSigningCertificates"
    private const val KEY_REVISION = "apiAuthorizationRevision"

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Long>>() {}.type
    private val certificatesType = object : TypeToken<Map<String, List<String>>>() {}.type

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun isApiEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    @Synchronized
    fun setApiEnabled(context: Context, enabled: Boolean) {
        if (enabled == isApiEnabled(context)) return
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_REVISION, UUID.randomUUID().toString()).apply()
    }

    /** Changes even when access is revoked and then restored during an unlock check. */
    @Synchronized
    fun revision(context: Context): String = prefs(context).getString(KEY_REVISION, "").orEmpty()

    private fun storedGrants(context: Context): Map<String, Long> = try {
        gson.fromJson(prefs(context).getString(KEY_GRANTS, "{}"), mapType) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }

    private fun storedCertificates(context: Context): Map<String, List<String>> = try {
        gson.fromJson(prefs(context).getString(KEY_CERTIFICATES, "{}"), certificatesType) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }

    /** Package -> approval time. Legacy grants without signing certificates require approval again. */
    @Synchronized
    fun grants(context: Context): Map<String, Long> {
        val certificates = storedCertificates(context)
        return storedGrants(context).filterKeys { packageName ->
            try {
                val expected = certificates[packageName].orEmpty()
                expected.isNotEmpty() && expected == ApiPackageIdentity.signingCertificates(context, packageName)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun grantedPackages(context: Context): Set<String> = grants(context).keys

    @Synchronized
    fun isAnyGranted(context: Context, packageNames: Array<String>?): Boolean {
        if (packageNames == null || !isApiEnabled(context)) return false
        val granted = grantedPackages(context)
        return packageNames.any { it in granted }
    }

    @Synchronized
    fun grant(context: Context, packageName: String, expectedCertificates: List<String>) {
        val signatures = ApiPackageIdentity.signingCertificates(context, packageName)
        require(signatures.isNotEmpty() && signatures == expectedCertificates)
        val updated = storedGrants(context) + (packageName to System.currentTimeMillis())
        val certificates = storedCertificates(context) + (packageName to signatures)
        prefs(context).edit().putString(KEY_GRANTS, gson.toJson(updated))
            .putString(KEY_CERTIFICATES, gson.toJson(certificates))
            .putString(KEY_REVISION, UUID.randomUUID().toString()).apply()
    }

    @Synchronized
    fun revoke(context: Context, packageName: String) {
        prefs(context).edit().putString(KEY_GRANTS, gson.toJson(storedGrants(context) - packageName))
            .putString(KEY_CERTIFICATES, gson.toJson(storedCertificates(context) - packageName))
            .putString(KEY_REVISION, UUID.randomUUID().toString()).apply()
    }
}
