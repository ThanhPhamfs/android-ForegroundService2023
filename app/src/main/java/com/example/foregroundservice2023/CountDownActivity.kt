package com.example.foregroundservice2023

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.foregroundservice2023.databinding.ActivityCountDownBinding
import com.example.foregroundservice2023.services.CountdownTimerService


class CountDownActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCountDownBinding
    private lateinit var countDownValueReceiver: BroadcastReceiver
    private lateinit var countDownTimerStopReceiver: BroadcastReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountDownBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonStartService.setOnClickListener{
            startService()
        }

        binding.buttonStopService.setOnClickListener{
            stopService()
        }

    }

    override fun onStart() {
        super.onStart()
        /** Move to background for countdown timer service **/
        val serviceIntent = Intent(this, CountdownTimerService::class.java)
        serviceIntent.putExtra(CountdownTimerService.COUNTDOWN_TIMER_ACTION, CountdownTimerService.MOVE_TO_BACKGROUND)
        startService(serviceIntent)

        /** Register countdown value receiver **/
        val countDownValueFilter = IntentFilter()
        countDownValueFilter.addAction(CountdownTimerService.COUNTDOWN_TIMER_TICK)
        countDownValueReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val currentTime = intent?.getLongExtra(CountdownTimerService.COUNTDOWN_TIMER_VALUE, 0)!!
                binding.tvCountDownTimerValue.text = (if (currentTime.toInt() == 0) {
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
            }
        }
        registerReceiver(countDownValueReceiver, countDownValueFilter)

        /** Register countdown timer stop **/
        val countDownStopFilter = IntentFilter()
        countDownStopFilter.addAction(CountdownTimerService.COUNTDOWN_TIMER_STOP)
        countDownTimerStopReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                binding.tvCountDownTimerValue.text = "Stop"
            }
        }
        registerReceiver(countDownTimerStopReceiver, countDownStopFilter)
    }

    override fun onPause() {
        super.onPause()
        /** Move to foreground for countdown timer service **/
        val serviceIntent = Intent(this, CountdownTimerService::class.java)
        serviceIntent.putExtra(CountdownTimerService.COUNTDOWN_TIMER_ACTION, CountdownTimerService.MOVE_TO_FOREGROUND)
        startService(serviceIntent)

        /** Unregister countdown timer receivers **/
        unregisterReceiver(countDownValueReceiver)
        unregisterReceiver(countDownTimerStopReceiver)
    }
    /**
     * Start service
     */
    private fun startService() {
        val serviceIntent = Intent(this, CountdownTimerService::class.java)
        serviceIntent.putExtra(CountdownTimerService.COUNTDOWN_TIMER_ACTION, CountdownTimerService.START)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("LOGDD", "startService")
//            ContextCompat.startForegroundService(this, intent)
            startService(serviceIntent)
        }
    }

    /**
     * Stop service
     */
    private fun stopService() {
        val serviceIntent = Intent(this, CountdownTimerService::class.java)
        serviceIntent.putExtra(CountdownTimerService.COUNTDOWN_TIMER_ACTION, CountdownTimerService.STOP)
        startService(serviceIntent)
        if (checkService(CountdownTimerService.javaClass)){
            stopService(serviceIntent)
        }
    }
    /**
     * Check service
     */
    private fun checkService(serviceClass: Class<*>):Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name.contains(service.service.className)) {
                return true
            }
        }
        return false
    }
}