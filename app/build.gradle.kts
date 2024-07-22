import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    kotlin("kapt") // kapt 플러그인 추가
}

// To use values in the file local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.katzheimer.testfolder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.katzheimer.testfolder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For using API_KEY
        buildConfigField("String", "API_KEY", "\"${localProperties["API_KEY"]}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Firebase BoM 따로
    implementation(platform(libs.firebase.bom))

    // Firebase libraries만 따로 정리
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config.ktx)
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Play services library for Google Sign-In 추가
    implementation(libs.play.services.auth)

    // AndroidX libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation("androidx.exifinterface:exifinterface:1.3.6") // ExifInterface 추가

    // Additional libraries
    implementation(libs.android.gif.drawable)
    implementation(libs.firebase.storage.ktx)
    implementation("com.github.bumptech.glide:glide:4.12.0") // Glide 추가
    kapt("com.github.bumptech.glide:compiler:4.12.0") // Glide annotation processor

    // Test libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // For tokenizing corpus
    implementation(libs.smile.nlp)
    implementation(libs.smile.core)
    implementation(libs.opennlp.tools)

    // For Using OpenAI API
    implementation(libs.openai.client)
    implementation(libs.ktor.client.android)
    implementation(libs.okhttp)

    implementation("androidx.work:work-runtime-ktx:2.7.1")
}
