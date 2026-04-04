import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun firebaseConfig(key: String): String {
    return (localProperties.getProperty(key) ?: System.getenv(key) ?: "").trim()
}

fun adsConfig(key: String): String {
    return (localProperties.getProperty(key) ?: System.getenv(key) ?: "").trim()
}

fun gradleStringLiteral(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val backupSecret =
    (localProperties.getProperty("backupSecret")
        ?: System.getenv("BACKUP_SECRET")
        ?: "trivia-royale-local-backup-v1")
        .trim()
val sampleAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val defaultAdMobAppId = "YOUR_ADMOB_APP_ID"
val defaultInterstitialAdUnitId = "YOUR_INTERSTITIAL_AD_UNIT_ID"
val defaultRewardedAdUnitId = "YOUR_REWARDED_AD_UNIT_ID"
val configuredAdMobAppId = adsConfig("admobAppId").ifBlank { defaultAdMobAppId }
val configuredInterstitialAdUnitId = adsConfig("admobInterstitialAdUnitId").ifBlank { defaultInterstitialAdUnitId }
val configuredRewardedAdUnitId = adsConfig("admobRewardedAdUnitId").ifBlank { defaultRewardedAdUnitId }

android {
    namespace = "com.triviaroyale"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.triviaroyale"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "FIREBASE_API_KEY", gradleStringLiteral(firebaseConfig("firebaseApiKey")))
        buildConfigField("String", "FIREBASE_APP_ID", gradleStringLiteral(firebaseConfig("firebaseAppId")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", gradleStringLiteral(firebaseConfig("firebaseProjectId")))
        buildConfigField("String", "FIREBASE_SENDER_ID", gradleStringLiteral(firebaseConfig("firebaseSenderId")))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", gradleStringLiteral(firebaseConfig("firebaseStorageBucket")))
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", gradleStringLiteral(firebaseConfig("firebaseWebClientId")))
        buildConfigField("String", "BACKUP_SECRET", gradleStringLiteral(backupSecret))

        buildConfigField("String", "ADMOB_APP_ID", gradleStringLiteral(configuredAdMobAppId))
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", gradleStringLiteral(configuredInterstitialAdUnitId))
        buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", gradleStringLiteral(configuredRewardedAdUnitId))
        manifestPlaceholders["admobAppId"] = configuredAdMobAppId.ifBlank { sampleAdMobAppId }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-dev"

            // Use Google's official test Ad Unit IDs for debug builds.
            // Real IDs are used in release builds via defaultConfig.
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            manifestPlaceholders["admobAppId"] = sampleAdMobAppId
            resValue("string", "app_name", "Trivia Royale Dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
