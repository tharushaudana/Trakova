package com.tharusha.trakova

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var btnToggleService: Button
    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestPermissions()

        serviceRunning = SmsListenerService.isRunning;

        btnToggleService = findViewById(R.id.btnToggleService)

        updateToggleServiceButton()

        btnToggleService.setOnClickListener {
            if (!notificationsEnabled()) {
                Toast.makeText(this, "Please enable notifications to run the service", Toast.LENGTH_LONG).show()
                requestEnableNotifications()
                return@setOnClickListener
            }

            if (serviceRunning) {
                stopService(Intent(this, SmsListenerService::class.java))
            } else {
                val intent = Intent(this, SmsListenerService::class.java)
                ContextCompat.startForegroundService(this, intent)
            }
            serviceRunning = !serviceRunning

            updateToggleServiceButton()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun notificationsEnabled(): Boolean {
        val mgr = NotificationManagerCompat.from(this)
        return mgr.areNotificationsEnabled()
    }

    private fun requestEnableNotifications() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", packageName)
        }
        startActivity(intent)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    private fun updateToggleServiceButton() {
        if (serviceRunning) {
            btnToggleService.text = "Disable Service"
        } else {
            btnToggleService.text = "Enable Service"
        }
    }
}