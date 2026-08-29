import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Redundant with AGP 9.0+ built-in Kotlin support
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keys/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.sg.taskspace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sg.taskspace"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.3.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val backupSecretKey = keystoreProperties.getProperty("backupSecretKey")
            ?: project.findProperty("BACKUP_SECRET_KEY")?.toString() ?: throw IllegalStateException("BACKUP_SECRET_KEY not found")
        buildConfigField("String", "BACKUP_SECRET_KEY", "\"$backupSecretKey\"")
    }

    signingConfigs {
        create("release") {
            val keyFile = rootProject.file("keys/android-sign-keys-new.jks").takeIf { it.exists() }
                ?: rootProject.file(keystoreProperties.getProperty("storeFile") ?: "keys/release.jks")

            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = System.getenv("STORE_PASSWORD")
                    ?: keystoreProperties.getProperty("storePassword")
                    ?: project.findProperty("STORE_PASSWORD")?.toString()
                    ?: ""
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProperties.getProperty("keyAlias")
                    ?: project.findProperty("KEY_ALIAS")?.toString()
                    ?: ""
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProperties.getProperty("keyPassword")
                    ?: project.findProperty("KEY_PASSWORD")?.toString()
                    ?: storePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildToolsVersion = "36.0.0"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.material3)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.7")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore & Security
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.code.gson:gson:2.13.2")
}
