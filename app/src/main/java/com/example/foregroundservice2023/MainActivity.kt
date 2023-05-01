package com.example.foregroundservice2023

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.foregroundservice2023.receivers.NotificationReceiver
import com.example.foregroundservice2023.services.CountdownTimerService

class MainActivity : AppCompatActivity(), NotificationReceiver.OnHidingListener {
    companion object {
        const val CHANNEL_ID = "Countdown_Notifications"
        const val COUNTDOWN_NOTIFICATION_TITLE = "Countdown Timer"
        const val HIDING_ACTION = "HIDING_ACTION"
        const val HIDING_NOTIFICATION_ID = 1
    }
    val countDownInterval: Long = 1
    var currentTime: Long = 150
    var countDownTimer: CountDownTimer? = null
    private var hidingReceiver: NotificationReceiver? = null
    private var intentFilter: IntentFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /** Count down timer **/
        countDownTimer = object : CountDownTimer(currentTime * 1000, countDownInterval * 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = (millisUntilFinished / 1000)
                buildNotification()
            }

            override fun onFinish() {
            }
        }
        findViewById<View>(R.id.btn_start)?.setOnClickListener {
            countDownTimer?.start()
        }
    //        findViewById<View>(R.id.btn_start)?.setOnClickListener {
//            var intent = Intent(this, SampleForegroundService::class.java)
//            startService(intent)
//
//            Handler().postDelayed({
//                updateTextStatus()
//            },5000)        }
//        findViewById<View>(R.id.btn_stop)?.setOnClickListener {
//            val intentStop = Intent(this, SampleForegroundService::class.java)
//            intentStop.action = ACTION_STOP
//            startService(intentStop)
//            Handler().postDelayed({
//                updateTextStatus()
//            },100)
//        }
//        updateTextStatus()
    }

    override fun onResume() {
        /** Register notification hiding receiver  **/
        hidingReceiver = NotificationReceiver()
        hidingReceiver?.setOnHidingClickListener(this)

        intentFilter = IntentFilter(HIDING_ACTION)
        ContextCompat.registerReceiver(baseContext, hidingReceiver, intentFilter!!, ContextCompat.RECEIVER_NOT_EXPORTED)
        super.onResume()
    }

    override fun onDestroy() {
        unregisterReceiver(hidingReceiver)
        super.onDestroy()
    }

    /**
     * Build notification
     */
    private fun buildNotification() {
        createNotificationChannel()
        /** Notification content intent **/
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        /** Hiding button intent **/
//        val hidingIntent = Intent(this, NotificationReceiver::class.java).apply {
//            putExtra(HIDING_NOTIFICATION_ID, "0")
//            action = HIDING_ACTION
//        }
        val hidingIntent = Intent()
        hidingIntent.action = HIDING_ACTION
        hidingIntent.putExtra("data", "Nothing to see here, move along.")
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
        else 0
        val hidingPendingIntent = PendingIntent.getBroadcast(this, 0, hidingIntent, 0)

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(COUNTDOWN_NOTIFICATION_TITLE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentText(
                (if (currentTime.toInt() == 0) {
                    "Done"
                } else {
                    val hours = currentTime.div(60).div(60)
                    val minutes = currentTime.div(60)
                    val seconds = currentTime.rem(60)
                    "${"%02d".format(hours)}:${"%02d".format(minutes)}:${
                        "%02d".format(
                            seconds
                        )
                    }"
                }).toString()
            ).setColorized(true).setSmallIcon(R.drawable.ic_clock).setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent).setAutoCancel(true)
            .addAction(R.drawable.ic_clock, "Hide", hidingPendingIntent)
            .setOngoing(true)
            .build()

        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(HIDING_NOTIFICATION_ID, builder)
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "foregroundChannel"
            val descriptionText = "Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateTextStatus() {
        if (isMyServiceRunning(CountdownTimerService::class.java)) {
            findViewById<TextView>(R.id.txt_service_status)?.text = "Service is Running"
        } else {
            findViewById<TextView>(R.id.txt_service_status)?.text = "Service is NOT Running"
        }
    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//            Log.d("LOGGGG: ", "${manager.getRunningServices(Int.MAX_VALUE).size}")
//            for (service in manager.getRunningServices(
//                Int.MAX_VALUE
//            )) {
//                if (serviceClass.name == service.service.className) {
//                    Log.d("LOGGGG: ", "${serviceClass.name} ${service.service.className}")
//                    return true
//                }
//            }
            val services: List<ActivityManager.RunningServiceInfo> =
                manager.getRunningServices(Int.MAX_VALUE)
            Log.d("LOGGGG: ", "${services.size}")
            for (serviceInfo in services) {
                val componentName = serviceInfo.service
                val serviceName = componentName.className

            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /**
     * On hiding button click on countdown timer notification
     *      - Cancel countdown timer
     *      - Clear the countdown notification
     */
    override fun onHidingClick() {
        Log.d("LOGDD", "onHidingClick")
        countDownTimer?.cancel()
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(HIDING_NOTIFICATION_ID)
    }

}