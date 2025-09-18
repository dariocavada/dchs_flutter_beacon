package com.cavadalab.dchs_flutter_beacon

import android.Manifest
import android.app.Activity
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import io.flutter.plugin.common.MethodChannel

class FlutterBeaconBroadcast(private val context: Context, iBeaconLayout: BeaconParser) {
    companion object {
        private val TAG = FlutterBeaconBroadcast::class.java.simpleName
        const val REQUEST_CODE_BLUETOOTH_ADVERTISE = 1236
    }

    private val beaconTransmitter: BeaconTransmitter = BeaconTransmitter(context.applicationContext, iBeaconLayout)
    private var pendingResult: MethodChannel.Result? = null
    private var beacon: Beacon? = null
    private var beaconMap: Map<String, Any?>? = null

    fun isBroadcasting(result: MethodChannel.Result) {
        result.success(beaconTransmitter.isStarted)
    }

    fun stopBroadcast(result: MethodChannel.Result) {
        beaconTransmitter.stopAdvertising()
        result.success(true)
    }

    fun startBroadcast(arguments: Any?, result: MethodChannel.Result) {
        if (arguments !is Map<*, *>) {
            result.error("Broadcast", "Invalid parameter", null)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val map = arguments as Map<String, Any?>
        val beacon = FlutterBeaconUtils.beaconFromMap(map)

        // Salviamo il beacon e la mappa
        this.beacon = beacon
        this.beaconMap = map
        pendingResult = result

        // Verifichiamo e richiediamo il permesso se necessario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasBluetoothAdvertisePermission()) {
                requestBluetoothAdvertisePermission()
                return
            }
        }

        // Se il permesso è già stato concesso o non necessario, avviamo l'advertising
        startAdvertising(beacon, map)
    }

    private fun hasBluetoothAdvertisePermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothAdvertisePermission() {
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE),
                REQUEST_CODE_BLUETOOTH_ADVERTISE
            )
        }
    }

    private fun startAdvertising(beacon: Beacon?, map: Map<String, Any?>?) {
        if (beacon == null || map == null) {
            pendingResult?.error("Broadcast", "Missing beacon data", null)
            pendingResult = null
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val advertisingMode = map["advertisingMode"]
            if (advertisingMode is Int) {
                beaconTransmitter.advertiseMode = advertisingMode
            }
            val advertisingTxPowerLevel = map["advertisingTxPowerLevel"]
            if (advertisingTxPowerLevel is Int) {
                beaconTransmitter.advertiseTxPowerLevel = advertisingTxPowerLevel
            }
            beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.d(TAG, "Start broadcasting = $beacon")
                    pendingResult?.success(true)
                    pendingResult = null
                }

                override fun onStartFailure(errorCode: Int) {
                    val error = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                        else -> "FEATURE_UNSUPPORTED"
                    }
                    Log.e(TAG, error)
                    pendingResult?.error("Broadcast", error, null)
                    pendingResult = null
                }
            })
        } else {
            Log.e(TAG, "FEATURE_UNSUPPORTED")
            pendingResult?.error("Broadcast", "FEATURE_UNSUPPORTED", null)
            pendingResult = null
        }
    }

    fun startAdvertisingAfterPermissionGranted() {
        startAdvertising(beacon, beaconMap)
    }

    fun onAdvertisingPermissionDenied() {
        pendingResult?.error("Broadcast", "BLUETOOTH_ADVERTISE permission denied", null)
        pendingResult = null
    }
}
