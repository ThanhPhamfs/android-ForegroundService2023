package com.example.foregroundservice2023.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View

open class NotificationReceiver : BroadcastReceiver() {
    private var onHidingClickListener: OnHidingListener? = null


    override fun onReceive(context: Context?, intent: Intent?) {
        onHidingClickListener?.onHidingClick()
    }

    interface OnHidingListener{
        fun onHidingClick()
    }

    fun setOnHidingClickListener(onHidingClickListener: OnHidingListener){
        this.onHidingClickListener = onHidingClickListener
    }
}