package com.ble.scan.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ble.scan.data.Campaign
import com.ble.scan.data.User
import com.ble.scan.util.FirebaseHelper
import com.ble.scan.util.NotificationHelper
import com.ble.scan.util.PreferencesHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class BLEScanningService : LifecycleService() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val scannedUuids = mutableSetOf<String>()
    private val notificationHelper by lazy { NotificationHelper(this) }
    private val firebaseHelper by lazy { FirebaseHelper() }
    private val preferencesHelper by lazy { PreferencesHelper(this) }
    private var currentUser: User? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            results.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            // Restart scanning after a delay
            lifecycleScope.launch {
                delay(5000)
                if (!isScanning) {
                    startScanning()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BLEScanningService"
        private const val SCAN_PERIOD_MS = 10000L // Scan for 10 seconds
        private const val RESTART_DELAY_MS = 5000L // Wait 5 seconds before restarting
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        currentUser = preferencesHelper.getUser()
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        startForeground(1, notificationHelper.createForegroundServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service started")
        
        if (currentUser == null) {
            currentUser = preferencesHelper.getUser()
        }
        
        if (hasRequiredPermissions()) {
            startScanning()
        } else {
            Log.e(TAG, "Missing required permissions")
            stopSelf()
        }
        
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopScanning()
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startScanning() {
        if (isScanning || bluetoothLeScanner == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Cannot start scanning: isScanning=$isScanning, scanner=$bluetoothLeScanner, enabled=${bluetoothAdapter.isEnabled}")
            return
        }

        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Missing permissions for scanning")
            return
        }

        try {
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            val scanFilters = emptyList<ScanFilter>() // Scan all beacons

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
                    isScanning = true
                    Log.d(TAG, "BLE scanning started")
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
                isScanning = true
                Log.d(TAG, "BLE scanning started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}", e)
        }
    }

    private fun stopScanning() {
        if (!isScanning || bluetoothLeScanner == null) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothLeScanner?.stopScan(scanCallback)
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothLeScanner?.stopScan(scanCallback)
            }
            isScanning = false
            Log.d(TAG, "BLE scanning stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}", e)
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val scanRecord = result.scanRecord
        val rssi = result.rssi

        // Extract UUID from scan record - try multiple methods
        var uuidString: String? = null

        // Method 1: Check service UUIDs (most common for beacons)
        val serviceUuids = scanRecord?.serviceUuids
        if (!serviceUuids.isNullOrEmpty()) {
            uuidString = serviceUuids[0].toString()
        }

        // Method 2: Check manufacturer data (iBeacon format)
        if (uuidString == null) {
            val manufacturerData = scanRecord?.getManufacturerSpecificData(0x004C) // Apple
            if (manufacturerData != null && manufacturerData.size >= 16) {
                // iBeacon format: first 16 bytes contain UUID
                val uuidBytes = manufacturerData.sliceArray(0 until 16)
                uuidString = bytesToUuid(uuidBytes)
            }
        }

        // Method 3: Check service data (Eddystone format)
        if (uuidString == null) {
            val serviceData = scanRecord?.serviceData
            if (!serviceData.isNullOrEmpty()) {
                // Try to extract from service data
                for ((uuid, data) in serviceData) {
                    if (data != null && data.size > 0) {
                        // Use the service UUID itself
                        uuidString = uuid.toString()
                        break
                    }
                }
            }
        }

        if (uuidString == null) {
            return
        }

        val normalizedUuid = uuidString.lowercase().trim()

        // Check if we've already processed this UUID recently (avoid duplicate notifications)
        if (scannedUuids.contains(normalizedUuid)) {
            return
        }

        Log.d(TAG, "Found beacon: $normalizedUuid, RSSI: $rssi, Device: ${device.address}")

        // Check Firebase for campaign
        lifecycleScope.launch {
            val campaign = firebaseHelper.getCampaignByUuid(normalizedUuid)
            
            if (campaign != null && currentUser != null) {
                // Calculate approximate distance from RSSI
                val distance = calculateDistance(rssi)
                
                // Show notification
                val notificationId = System.currentTimeMillis().toInt()
                notificationHelper.showCampaignNotification(campaign, notificationId)
                
                // Log to Firebase
                firebaseHelper.logNotification(
                    currentUser!!,
                    device.name ?: "Unknown Beacon",
                    campaign.name,
                    distance
                )
                
                // Mark as processed
                scannedUuids.add(normalizedUuid)
                
                // Remove from processed list after 5 minutes to allow re-notification
                delay(300000) // 5 minutes
                scannedUuids.remove(normalizedUuid)
                
                Log.d(TAG, "Notification sent for campaign: ${campaign.name}")
            }
        }
    }

    /**
     * Convert byte array to UUID string (iBeacon format)
     */
    private fun bytesToUuid(bytes: ByteArray): String {
        if (bytes.size < 16) return ""
        
        val msb = ((bytes[0].toInt() and 0xFF).toLong() shl 56) or
                ((bytes[1].toInt() and 0xFF).toLong() shl 48) or
                ((bytes[2].toInt() and 0xFF).toLong() shl 40) or
                ((bytes[3].toInt() and 0xFF).toLong() shl 32) or
                ((bytes[4].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[5].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[6].toInt() and 0xFF).toLong() shl 8) or
                (bytes[7].toInt() and 0xFF).toLong()
        
        val lsb = ((bytes[8].toInt() and 0xFF).toLong() shl 56) or
                ((bytes[9].toInt() and 0xFF).toLong() shl 48) or
                ((bytes[10].toInt() and 0xFF).toLong() shl 40) or
                ((bytes[11].toInt() and 0xFF).toLong() shl 32) or
                ((bytes[12].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[13].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[14].toInt() and 0xFF).toLong() shl 8) or
                (bytes[15].toInt() and 0xFF).toLong()
        
        val uuid = java.util.UUID(msb, lsb)
        return uuid.toString()
    }

    /**
     * Calculate approximate distance from RSSI (in meters)
     * This is a rough estimation
     */
    private fun calculateDistance(rssi: Int): Double {
        // Using the formula: distance = 10^((TxPower - RSSI) / (10 * N))
        // Where TxPower is typically -59 dBm for beacons at 1 meter
        // N is path loss exponent (typically 2 for free space)
        val txPower = -59.0
        val n = 2.0
        return Math.pow(10.0, (txPower - rssi) / (10 * n))
    }
}
