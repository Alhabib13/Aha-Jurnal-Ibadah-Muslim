import java.io.File
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

private data class FirebaseConfigMeta(val projectId: String, val mobileSdkAppId: String)

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

android {
    namespace = "com.islami.Aha"
    compileSdk = 36
    val firebaseConfigMeta = readFirebaseConfigMeta(file("google-services.json"))

    defaultConfig {
        applicationId = "com.islami.Aha"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        val forceDebugAppCheck = providers
            .gradleProperty("FORCE_APPCHECK_DEBUG")
            .orNull
            ?.toBooleanStrictOrNull()
            ?: false
        buildConfigField("String", "APP_ENV", "\"prod\"")
        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            quoteForBuildConfig(firebaseConfigMeta.projectId)
        )
        buildConfigField(
            "String",
            "FIREBASE_MOBILE_SDK_APP_ID",
            quoteForBuildConfig(firebaseConfigMeta.mobileSdkAppId)
        )
        buildConfigField("boolean", "FORCE_APPCHECK_DEBUG", forceDebugAppCheck.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "APP_CHECK_MODE", "\"NONE\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            buildConfigField("String", "APP_CHECK_MODE", "\"PLAY_INTEGRITY\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

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

tasks.register("validateFirebaseConfigPresence") {
    group = "verification"
    description = "Validate the single Firebase configuration file required by this app."

    doLast {
        val firebaseFile = file("google-services.json")
        if (!firebaseFile.exists()) {
            throw GradleException("Missing Firebase config: app/google-services.json")
        }
        val meta = readFirebaseConfigMeta(firebaseFile)
        if (meta.projectId.isBlank() || meta.mobileSdkAppId.isBlank()) {
            throw GradleException(
                "Invalid Firebase config: app/google-services.json must contain project_id and mobilesdk_app_id."
            )
        }
    }
}

tasks.register<Copy>("syncLegalDocsToAssets") {
    group = "build setup"
    description = "Sync legal HTML files from docs/ into app assets."
    from(rootProject.file("docs"))
    include("privacy-policy.html", "terms-of-service.html")
    into(layout.projectDirectory.dir("src/main/assets"))
}

tasks.named("preBuild").configure {
    dependsOn("validateFirebaseConfigPresence")
    dependsOn("syncLegalDocsToAssets")
}
