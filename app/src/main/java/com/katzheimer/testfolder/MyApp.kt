package com.katzheimer.testfolder

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log


class MyApp : Application(), ActivityLifecycleCallbacks {
    private val activitiesBeforeLogin = listOf("LoginActivity", "SplashActivity", "FindIdActivity", "FindPwActivity", "RegisterActivity")
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private lateinit var musicServiceIntent: Intent
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Log.d("APPLICATION", "Application created")
        registerActivityLifecycleCallbacks(this)

        // Initialize your MediaPlayer
//        mediaPlayer =
//            MediaPlayer.create(this, R.raw.paper_flip) // replace with your actual resource
//        mediaPlayer!!.isLooping = true // optional, if you want the music to loop
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        val activityName: String = activity.javaClass.simpleName
//        Log.d("APPLICATION", "Activity name: ${activityName}")
//        Log.d("APPLICATION", "activityReferences: ${activityReferences}")
        if (++activityReferences >= 1 && !isActivityChangingConfigurations) {
            if (activityName !in activitiesBeforeLogin) {
                    // App enters foreground
                val isMusicOn = sharedPreferences.getBoolean("music_on", true)
                if (isMusicOn) {
                    musicServiceIntent = Intent(this, MusicService::class.java)
                    startService(musicServiceIntent)
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        Log.d("APPLICATION", "activityReferences: ${activityReferences}")
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            val isMusicOn = sharedPreferences.getBoolean("music_on", true)
            if (isMusicOn) {
                musicServiceIntent = Intent(this, MusicService::class.java)
                stopService(musicServiceIntent)
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onTerminate() {
        super.onTerminate()
    }
}
