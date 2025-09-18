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


class FlutterPlatform(context: Context) {
    private val contextWeakReference = WeakReference(context)

    private val context: Context?
        get() = contextWeakReference.get()

    private val activity: Activity?
        get() = context as? Activity

    fun openLocationSettings() {
        val ctx = context ?: return
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (ctx is Activity) ctx.startActivity(intent) else ctx.startActivity(intent)
    }

    fun openBluetoothSettings() {
        val ctx = context ?: return
        if (ctx is Activity) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ctx.startActivityForResult(intent, DchsFlutterBeaconPlugin.REQUEST_CODE_BLUETOOTH)
        }
    }

    fun requestAuthorization() {
        val ctx = context
        if (ctx is Activity) {
            ActivityCompat.requestPermissions(
                ctx,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                DchsFlutterBeaconPlugin.REQUEST_CODE_LOCATION
            )
        }
    }

    fun checkLocationServicesPermission(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkLocationServicesIfEnabled(): Boolean {
        val ctx = context ?: return false
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                locationManager?.isLocationEnabled ?: false
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val mode = Settings.Secure.getInt(
                    ctx.contentResolver,
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
        val ctx = context ?: throw RuntimeException("Context is null")
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw RuntimeException("No Bluetooth service")
        val adapter = bluetoothManager.adapter
        return adapter?.isEnabled ?: false
    }

    fun isBroadcastSupported(): Boolean {
        val ctx = context ?: return false
        return BeaconTransmitter.checkTransmissionSupported(ctx) == BeaconTransmitter.SUPPORTED
    }

    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        val ctx = context
        return if (ctx is Activity) ActivityCompat.shouldShowRequestPermissionRationale(ctx, permission) else false
    }
}
