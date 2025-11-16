package com.tharusha.trakova

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SmsListenerService : Service() {

    private val CHANNEL_ID = "trakova_channel_01"

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        isRunning = true
        println("🔹 SmsListenerService started and running in foreground")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        println("🔹 SmsListenerService destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val sender = intent?.getStringExtra("sender")
        val msg = intent?.getStringExtra("msg")

        if (sender != null && msg != null) {
            handleIncomingMessage(sender, msg)
        }

        return START_STICKY
    }

    private fun handleIncomingMessage(sender: String, msg: String) {
        if (msg.trim() == "/where") {
            sendCurrentLocation(sender)
        }
    }

    private fun sendCurrentLocation(phoneNumber: String) {
        // Check location permission before requesting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            sendSms(phoneNumber, "Location permission denied!")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val reply = if (location != null) {
                    "Current location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Location unavailable"
                }

                sendSms(phoneNumber, reply)
            }
            .addOnFailureListener {
                sendSms(phoneNumber, "Failed to get location")
            }
    }

    fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For API 31 (Android 12) and above
                getSystemService(SmsManager::class.java)
            } else {
                // For devices below API 31 (using the deprecated getDefault() method)
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            println("✅ SMS sent to $phoneNumber" )

        } catch (e: Exception) {
            println("⚠️ Failed to send SMS to $phoneNumber: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trakova Service")
            .setContentText("Listening for SMS commands...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Trakova Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}