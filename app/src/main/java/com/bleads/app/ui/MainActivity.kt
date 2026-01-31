package com.bleads.app.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bleads.app.databinding.ActivityMainBinding
import com.bleads.app.service.BLEScanningService
import com.bleads.app.util.PreferencesHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesHelper = PreferencesHelper(this)

        // Check if user is logged in
        val user = preferencesHelper.getUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupViews(user)
        startBLEScanningService()
    }

    private fun setupViews(user: com.bleads.app.data.User) {
        binding.tvUserName.text = "Welcome, ${user.name}"
        binding.tvUserPhone.text = user.phone
        binding.tvStatus.text = "Scanning for beacons..."

        binding.btnLogout.setOnClickListener {
            preferencesHelper.clearUser()
            stopBLEScanningService()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun startBLEScanningService() {
        // Check Bluetooth
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            binding.tvStatus.text = "Bluetooth is disabled"
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // Check permissions
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            binding.tvStatus.text = "Permissions required"
            Toast.makeText(this, "Please grant required permissions", Toast.LENGTH_LONG).show()
            return
        }

        val serviceIntent = Intent(this, BLEScanningService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopBLEScanningService() {
        val serviceIntent = Intent(this, BLEScanningService::class.java)
        stopService(serviceIntent)
    }
}
