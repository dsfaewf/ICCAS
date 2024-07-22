package com.katzheimer.testfolder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.katzheimer.testfolder.LoginActivity
import com.katzheimer.testfolder.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMessagingService"
        private const val CHANNEL_ID = "fcm_default_channel"  // Updated to use default_notification_channel_id
    }

    private var FCMToken = ""

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Token: $token")

        // Save the token in SharedPreferences
        val pref = getSharedPreferences("firebaseToken", Context.MODE_PRIVATE)
        pref.edit().putString("firebaseToken", token).apply()

        // Send the token to your server
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "From: ${message.from}")

        // Log the message data and notification body
        Log.d(TAG, "Message data: ${message.data}")
        Log.d(TAG, "Message notification body: ${message.notification?.body}")

        // Check if the message contains data payload
        if (message.data.isNotEmpty()) {
            scheduleJob()
        } else {
            handleNow()
        }

        // Check if the message contains notification payload
        message.notification?.let { notification ->
            setNotification(notification)
        }
    }

    private fun scheduleJob() {
        val work = OneTimeWorkRequest.Builder(MyWorker::class.java).build()
        WorkManager.getInstance(applicationContext).enqueue(work)
    }

    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "Sending token to server: $token")
        // Implement your logic to send the token to your server
    }

    // FirebaseMessagingService 내부의 setNotification 함수에 추가된 코드입니다.
    private fun setNotification(notification: RemoteMessage.Notification) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel if running on Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_ID
            val channelName = "FCM Notifications"
            val channelDescription = "Channel for FCM notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }

        // Unique ID for the notification
        val uniId = (System.currentTimeMillis() / 7).toInt()

        // Intent to launch when the notification is tapped
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // PendingIntent for the notification
        val pendingIntent = PendingIntent.getActivity(
            this, uniId, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val title = notification.title ?: "Default Title"
        val body = notification.body ?: "Default Body"

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Notify the notificationManager
        notificationManager.notify(uniId, notificationBuilder.build())
    }

    fun getFirebaseToken(): String {
        // Get the Firebase token asynchronously
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d(TAG, "Firebase token: $it")
            FCMToken = it
        }
        return FCMToken
    }

    internal class MyWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {
        override fun doWork(): Result {
            // Implement your background work here
            return Result.success()
        }
    }
}
