package com.tharusha.trakova

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleService: Button
    private lateinit var btnAddNumber: Button
    private lateinit var txtPhoneNumber: EditText
    private lateinit var listNumbers: ListView

    private var serviceRunning = false
    private lateinit var adapter: ArrayAdapter<String>
    private val numbersList = ArrayList<String>()

    private val PREF_NAME = "trakova_prefs"
    private val KEY_NUMBERS = "authorized_numbers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        requestPermissions()

        // UI References
        btnToggleService = findViewById(R.id.btnToggleService)
        btnAddNumber = findViewById(R.id.btnAddNumber)
        txtPhoneNumber = findViewById(R.id.txtPhoneNumber)
        listNumbers = findViewById(R.id.listNumbers)

        // Load stored numbers
        loadNumbers()

        // List Adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, numbersList)
        listNumbers.adapter = adapter

        // Detect if service running
        serviceRunning = SmsListenerService.isRunning

        updateToggleServiceButton()

        // Toggle Button Click
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

        // Add Number Button
        btnAddNumber.setOnClickListener {
            val number = txtPhoneNumber.text.toString().trim()

            if (number.isEmpty()) {
                Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!numbersList.contains(number)) {
                numbersList.add(number)
                saveNumbers()
                adapter.notifyDataSetChanged()
                txtPhoneNumber.text.clear()
            } else {
                Toast.makeText(this, "Number already added", Toast.LENGTH_SHORT).show()
            }
        }

        // Long press → remove number
        listNumbers.setOnItemLongClickListener { _, _, position, _ ->
            val num = numbersList[position]

            Toast.makeText(this, "Removed: $num", Toast.LENGTH_SHORT).show()

            numbersList.removeAt(position)
            saveNumbers()
            adapter.notifyDataSetChanged()

            true
        }
    }

    // Load numbers from SharedPreferences
    private fun loadNumbers() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_NUMBERS, emptySet()) ?: emptySet()
        numbersList.clear()
        numbersList.addAll(stored)
    }

    // Save numbers to SharedPreferences
    private fun saveNumbers() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_NUMBERS, numbersList.toSet()).apply()
    }

    private fun notificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
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

    // Changes button color + text according to service state
    private fun updateToggleServiceButton() {
        if (serviceRunning) {
            btnToggleService.text = "Disable Service"
            btnToggleService.setBackgroundResource(R.drawable.rounded_toggle_button_enabled)
        } else {
            btnToggleService.text = "Enable Service"
            btnToggleService.setBackgroundResource(R.drawable.rounded_toggle_button_disabled)
        }
    }
}
