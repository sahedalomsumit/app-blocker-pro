import java.util.Properties
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.sahed.app_blocker_pro"
    compileSdk = 35

    lint {
        disable.add("NullSafeMutableLiveData")
        abortOnError = false
    }

    defaultConfig {
        applicationId = "com.sahed.app_blocker_pro"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "app_name", "App Blocker Pro")
        resValue("string", "app_short_description", "Fewer distractions. More of the day you actually wanted.")

        buildConfigField("Boolean", "SUPPORTS_UI_HIDER", "false")
        buildConfigField("Boolean", "SUPPORTS_ANTI_UNINSTALL", "false")
        buildConfigField("Boolean", "SUPPORTS_WRITE_SECURE_SETTINGS", "false")
    }


    splits {
        abi {
            isEnable = false

        }
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            val propertiesFile = rootProject.file("key.properties")
            if (propertiesFile.exists()) {
                propertiesFile.inputStream().use(properties::load)
            }
            val configuredStoreFile = properties.getProperty("storeFile", "app/upload-keystore.jks")
            val rootStoreFile = rootProject.file(configuredStoreFile)
            storeFile = if (rootStoreFile.exists()) {
                rootStoreFile
            } else {
                project.file(configuredStoreFile)
            }
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "App Blocker Pro (Debug)")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }
}



dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)


    // QR Scanner & Generator
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Shizuku dependecies
    implementation (libs.api)
    implementation (libs.provider)

    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.mpandroidchart)
    implementation(libs.timerangepicker)

}
androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        tasks.register("installAndGrantAccessibility$variantName") {
            group = "install"
            description = "Installs the app, grants Accessibility permission, and launches it"
            dependsOn("install$variantName")
            
            doLast {
                val adbPath = sdkComponents.adb.get().asFile.absolutePath
                val appId = variant.applicationId.get()
                Thread.sleep(2000)
                // Grant Accessibility Permission
                exec {
                    val baseId = "com.sahed.app_blocker_pro"
                    val services = "$appId/$baseId.services.AppBlockerService"

                    commandLine(adbPath, "shell", "settings", "put", "secure", "enabled_accessibility_services", services)
                }

                // Launch MainActivity
                exec {
                    val baseId = "com.sahed.app_blocker_pro"
                    commandLine(adbPath, "shell", "am", "start", "-n", "$appId/$baseId.ui.activity.FragmentActivity")
                }
            }
        }
    }
}
