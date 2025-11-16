package com.tharusha.trakova

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val format = bundle.getString("format")
            val pdus = bundle.get("pdus") as Array<*>

            for (pdu in pdus) {
                val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                val sender = sms.originatingAddress
                val message = sms.messageBody

                println("JJJJJJJJJJJJJJJJJJ: $sender")

                if (!SmsListenerService.isRunning) {
                    println("SmsListenerService is not running. Ignoring SMS.")
                    return
                }

                // Filter: Only specific number
                if (sender == "+94773531478") {
                    // Forward to foreground service
                    val serviceIntent = Intent(context, SmsListenerService::class.java)
                    serviceIntent.putExtra("sender", sender)
                    serviceIntent.putExtra("msg", message)
                    context.startService(serviceIntent)
                }
            }
        }
    }
}