buildscript {
    dependencies {
        classpath(libs.google.services)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-RC2" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//    }
//}
