package com.example.foregroundservice2023.services

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.foregroundservice2023.R
import com.example.foregroundservice2023.CountDownActivity
import com.example.foregroundservice2023.receivers.NotificationReceiver

class CountdownTimerService : Service(), NotificationReceiver.OnHidingListener {
    companion object {
        const val CHANNEL_ID = "Foreground_Notifications"
        const val NOTIFICATION_ID = 1
        const val COUNTDOWN_TIMER_ACTION = "COUNTDOWN_TIMER_ACTION"

        // Service Actions
        const val START = "START"
        const val PAUSE = "PAUSE"
        const val RESET = "RESET"
        const val STOP = "STOP"
        const val GET_STATUS = "GET_STATUS"
        const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
        const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"
        const val HIDING_COUNTDOWN_NOTIFICATION_ACTION = "HIDING_COUNTDOWN_NOTIFICATION_ACTION"
        const val COUNTDOWN_TIMER_VALUE = "COUNTDOWN_TIMER_VALUE"
        const val COUNTDOWN_TIMER_TICK = "COUNTDOWN_TIMER_TICK"
        const val COUNTDOWN_TIMER_STOP = "COUNTDOWN_TIMER_STOP"
    }
    enum class STATUS{
        FOREGROUND,
        BACKGROUND,
        NONE
    }

    private lateinit var countDownTimer: CountDownTimer
    private var countDownInterval: Long = 1
    private var currentTime: Long = 150
    private var isCountDownTimerRunning = false
    private var notificationTitle = "A01 - T1 Parking"
    private var status = STATUS.NONE
    private lateinit var hidingReceiver: NotificationReceiver

    // Getting access to the NotificationManager
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        /** Register notification hiding receiver  **/
        hidingReceiver = NotificationReceiver()
        hidingReceiver?.setOnHidingClickListener(this)

        val intentFilter = IntentFilter(HIDING_COUNTDOWN_NOTIFICATION_ACTION)
        ContextCompat.registerReceiver(baseContext, hidingReceiver, intentFilter!!, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(hidingReceiver)
        Log.d("LOGDD", "onDestroy has been called")
        val countDownIntent = Intent()
        countDownIntent.action = COUNTDOWN_TIMER_STOP
        sendBroadcast(countDownIntent)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LOGDD", "onStartCommand")
        createNotificationChannel()
        /** Get notification manager **/
        getNotificationManager()

        when (intent?.getStringExtra(COUNTDOWN_TIMER_ACTION)) {
            START -> startCountDownTimer()
            MOVE_TO_FOREGROUND -> moveToForeground()
            MOVE_TO_BACKGROUND -> moveToBackground()
            STOP -> stopCountDownTimer()
        }
        return START_STICKY
    }

    /**
     * Stop service
     */
    private fun stopCountDownTimer(){
        Log.d("LOGDD", "stopCountDownTimer")
        if (isCountDownTimerRunning){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            notificationManager.cancel(NOTIFICATION_ID)
            countDownTimer.cancel()
        }
    }
    /**
     * Handling for moving to background of service
     */
    private fun moveToBackground() {
        if (isCountDownTimerRunning) {
            status = STATUS.BACKGROUND
            stopCountDownTimer()
            countDownTimer = object : CountDownTimer(currentTime * 1000, countDownInterval * 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    currentTime = (millisUntilFinished / 1000)
//                    updateNotification()
                    val countDownIntent = Intent()
                    countDownIntent.action = COUNTDOWN_TIMER_TICK

                    countDownIntent.putExtra(COUNTDOWN_TIMER_VALUE, currentTime)
                    sendBroadcast(countDownIntent)
                    /** Send broadcast **/
                }

                override fun onFinish() {
                }
            }
            updateNotification()
            countDownTimer.start()
            Log.d("LOGDD", "moveToBackground")
        }
    }

    /**
     * Handling for moving to foreground of service
     */
    private fun moveToForeground() {
        if (isCountDownTimerRunning) {
            status = STATUS.FOREGROUND
            countDownTimer.cancel()
            countDownTimer = object : CountDownTimer(currentTime * 1000, countDownInterval * 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    currentTime = (millisUntilFinished / 1000)
                    updateNotification()
                    Log.d("LOGDD", "tick")
                }

                override fun onFinish() {
                }
            }
            startForeground(NOTIFICATION_ID, buildNotification())

            countDownTimer.start()
            Log.d("LOGDD", "moveToForeground")
        }
    }


    /**
     * Start count down timer on background
     */
    private fun startCountDownTimer() {
        status = STATUS.BACKGROUND
        countDownTimer = object : CountDownTimer(currentTime * 1000, countDownInterval * 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = (millisUntilFinished / 1000)
//                updateNotification()
                /** Send broadcast **/
                val countDownIntent = Intent()
                countDownIntent.action = COUNTDOWN_TIMER_TICK

                countDownIntent.putExtra(COUNTDOWN_TIMER_VALUE, currentTime)
                sendBroadcast(countDownIntent)
            }

            override fun onFinish() {
            }
        }
//        startForeground(NOTIFICATION_ID, buildNotification())
        updateNotification()
        countDownTimer.start()
        isCountDownTimerRunning = true
    }

    /**
     * build notification
     */
    private fun buildNotification(): Notification {
        /** Notification content intent **/
        val intent = Intent(this, CountDownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent? = if (status == STATUS.FOREGROUND){
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }else{
            null
        }

        var description = if (status == STATUS.FOREGROUND){
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
        }else{
            "Countdown timer is running"
        }

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(description)
            .setColorized(status == STATUS.FOREGROUND)
            .setColor(Color.parseColor("#BEAEE2"))
            .setSmallIcon(R.drawable.ic_clock)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (status == STATUS.FOREGROUND){
            val hidingIntent = Intent()
            hidingIntent.action = HIDING_COUNTDOWN_NOTIFICATION_ACTION
            hidingIntent.putExtra("data", "Nothing to see here, move along.")
            val hidingPendingIntent = PendingIntent.getBroadcast(this, 0, hidingIntent, 0)
            builder.addAction(R.drawable.ic_clock, "Hide", hidingPendingIntent)
        }
        return builder.build()
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

    /**
     * Get nofification manager
     */
    private fun getNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
    }

    /**
     * This function uses the notificationManager to update the existing notification with the new notification
     * */
    private fun updateNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification()
        )
    }

    /**
     * On hiding click listener function
     */
    override fun onHidingClick() {
        val serviceIntent = Intent(this, CountdownTimerService::class.java)
        stopService(serviceIntent)
        stopCountDownTimer()
        isCountDownTimerRunning = false
    }
}