import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
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
    namespace = "com.example.testfolder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.testfolder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For using API_KEY
        buildConfigField("String", "API_KEY", "${localProperties["API_KEY"]}")
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

    // Add the dependency for the Realtime Database library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx) //비밀번호 인증
    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
//    implementation("androidx.core:core-ktx:+")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.19")
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
}