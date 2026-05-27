package com.cavadalab.dchs_flutter_beacon

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.BeaconTransmitter
import java.lang.ref.WeakReference

class FlutterPlatform(context: Context, activity: Activity?) {
    private val context = context.applicationContext
    private var activityWeakReference = WeakReference(activity)

    private val activity: Activity?
        get() = activityWeakReference.get()

    fun setActivity(activity: Activity?) {
        activityWeakReference = WeakReference(activity)
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun openBluetoothSettings(): Boolean {
        val act = activity ?: return false
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        act.startActivityForResult(intent, DchsFlutterBeaconPlugin.REQUEST_CODE_BLUETOOTH)
        return true
    }

    fun requiredAuthorizationPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            else -> emptyArray()
        }
    }

    fun requestAuthorization(): Boolean {
        val permissions = requiredAuthorizationPermissions()
        if (permissions.isEmpty()) {
            return true
        }

        val act = activity ?: return false
        ActivityCompat.requestPermissions(
            act,
            permissions,
            DchsFlutterBeaconPlugin.REQUEST_CODE_LOCATION
        )
        return true
    }

    fun checkLocationServicesPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+: BLUETOOTH_SCAN is required for BLE scanning;
                // ACCESS_FINE_LOCATION is also needed unless neverForLocation is set.
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6–11: ACCESS_FINE_LOCATION is required for BLE scanning on Android 10+.
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }
    }

    fun checkLocationServicesIfEnabled(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                locationManager?.isLocationEnabled ?: false
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val mode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                )
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
            else -> true
        }
    }

    @SuppressLint("MissingPermission")
    fun checkBluetoothIfEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw RuntimeException("No Bluetooth service")
        val adapter = bluetoothManager.adapter
        return adapter?.isEnabled ?: false
    }

    fun isBroadcastSupported(): Boolean {
        return BeaconTransmitter.checkTransmissionSupported(context) == BeaconTransmitter.SUPPORTED
    }

    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        val act = activity ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(act, permission)
    }
}
