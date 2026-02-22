import java.io.File
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

private data class FirebaseConfigMeta(
    val projectId: String,
    val mobileSdkAppId: String
)

private fun quoteForBuildConfig(value: String): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

private fun extractJsonString(rawJson: String, key: String): String {
    val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
    return regex.find(rawJson)?.groupValues?.get(1).orEmpty()
}

private fun readFirebaseConfigMeta(file: File): FirebaseConfigMeta {
    if (!file.exists()) return FirebaseConfigMeta(projectId = "", mobileSdkAppId = "")
    val raw = file.readText()
    return FirebaseConfigMeta(
        projectId = extractJsonString(raw, "project_id"),
        mobileSdkAppId = extractJsonString(raw, "mobilesdk_app_id")
    )
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = file.readBytes()
    return digest.digest(bytes).joinToString("") { "%02x".format(it) }
}

android {
    namespace = "com.islami.Aha"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.islami.Aha"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        val forceDebugAppCheck = providers
            .gradleProperty("FORCE_APPCHECK_DEBUG")
            .orNull
            ?.toBooleanStrictOrNull()
            ?: false
        buildConfigField("boolean", "FORCE_APPCHECK_DEBUG", forceDebugAppCheck.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "environment"
    val devFirebaseConfigMeta = readFirebaseConfigMeta(file("src/dev/google-services.json"))
    val prodFirebaseConfigMeta = readFirebaseConfigMeta(file("src/prod/google-services.json"))
    productFlavors {
        create("dev") {
            dimension = "environment"
            versionNameSuffix = "-dev"
            buildConfigField("String", "APP_ENV", "\"dev\"")
            // Dev defaults to DEBUG so emulator login works when App Check enforcement is active.
            buildConfigField("String", "APP_CHECK_MODE", "\"DEBUG\"")
            buildConfigField(
                "String",
                "FIREBASE_PROJECT_ID",
                quoteForBuildConfig(devFirebaseConfigMeta.projectId)
            )
            buildConfigField(
                "String",
                "FIREBASE_MOBILE_SDK_APP_ID",
                quoteForBuildConfig(devFirebaseConfigMeta.mobileSdkAppId)
            )
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "APP_ENV", "\"prod\"")
            buildConfigField("String", "APP_CHECK_MODE", "\"PLAY_INTEGRITY\"")
            buildConfigField(
                "String",
                "FIREBASE_PROJECT_ID",
                quoteForBuildConfig(prodFirebaseConfigMeta.projectId)
            )
            buildConfigField(
                "String",
                "FIREBASE_MOBILE_SDK_APP_ID",
                quoteForBuildConfig(prodFirebaseConfigMeta.mobileSdkAppId)
            )
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.1")
    implementation(libs.firebase.crashlytics)
    ksp("com.google.dagger:hilt-compiler:2.59.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("validateFirebaseConfigSeparation") {
    group = "verification"
    description = "Validate dev/prod google-services.json presence. Identical files are allowed unless STRICT_FIREBASE_CONFIG=true."

    doLast {
        val strictByProperty = providers
            .gradleProperty("STRICT_FIREBASE_CONFIG")
            .orNull
            ?.toBooleanStrictOrNull()
            ?: false
        val strictValidation = strictByProperty

        fun enforceOrWarn(message: String) {
            if (strictValidation) {
                throw GradleException(message)
            } else {
                logger.warn("Firebase config validation (non-strict): $message")
            }
        }

        val devFile = file("src/dev/google-services.json")
        val prodFile = file("src/prod/google-services.json")

        if (!devFile.exists()) {
            enforceOrWarn("Missing Firebase config: app/src/dev/google-services.json")
            return@doLast
        }
        if (!prodFile.exists()) {
            enforceOrWarn("Missing Firebase config: app/src/prod/google-services.json")
            return@doLast
        }

        val devHash = sha256(devFile)
        val prodHash = sha256(prodFile)
        if (devHash == prodHash) {
            enforceOrWarn(
                "Firebase config dev/prod are identical. " +
                    "Use separate files for app/src/dev/google-services.json and app/src/prod/google-services.json."
            )
        }

        val devMeta = readFirebaseConfigMeta(devFile)
        val prodMeta = readFirebaseConfigMeta(prodFile)
        if (devMeta.projectId.isNotBlank() && devMeta.projectId == prodMeta.projectId) {
            logger.warn(
                "Dev and prod Firebase project_id are the same (${devMeta.projectId}). " +
                    "This is allowed but not recommended for strict environment isolation."
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("validateFirebaseConfigSeparation")
}
