package com.cavadalab.dchs_flutter_beacon

import android.os.Build
import android.util.Log

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.RemoteException
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class DchsFlutterBeaconPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler,
    PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {

    companion object {
        const val REQUEST_CODE_LOCATION = 1234
        const val REQUEST_CODE_BLUETOOTH = 5678
    }

    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var activityPluginBinding: ActivityPluginBinding? = null

    private var activity: Activity? = null
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var eventChannelMonitoring: EventChannel
    private lateinit var eventChannelBluetoothState: EventChannel
    private lateinit var eventChannelAuthorizationStatus: EventChannel

    private var beaconManager: BeaconManager? = null
    private var beaconScanner: FlutterBeaconScanner? = null
    private var beaconBroadcast: FlutterBeaconBroadcast? = null
    private var platform: FlutterPlatform? = null

    public var flutterResult: MethodChannel.Result? = null
    private var flutterResultBluetooth: MethodChannel.Result? = null
    private var eventSinkLocationAuthorizationStatus: EventChannel.EventSink? = null

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = binding
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = null
    }

    // ActivityAware methods
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activityPluginBinding = binding
        this.activity = binding.activity

        setupChannels(flutterPluginBinding!!.binaryMessenger, activity!!)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        stopBeaconSession()
        teardownChannels()
        clearActivityScopedReferences()
        this.activity = null
    }

    private fun setupChannels(messenger: BinaryMessenger, activity: Activity) {
        activityPluginBinding?.addActivityResultListener(this)
        activityPluginBinding?.addRequestPermissionsResultListener(this)

        beaconManager = BeaconManager.getInstanceForApplication(activity.applicationContext)

        val iBeaconLayout = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        if (!beaconManager!!.beaconParsers.contains(iBeaconLayout)) {
            beaconManager!!.beaconParsers.clear()
            beaconManager!!.beaconParsers.add(iBeaconLayout)
        }

        platform = FlutterPlatform(activity)
        beaconScanner = FlutterBeaconScanner(this, activity)
        beaconBroadcast = FlutterBeaconBroadcast(activity, iBeaconLayout)

        channel = MethodChannel(messenger, "flutter_beacon")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(messenger, "flutter_beacon_event")
        eventChannel.setStreamHandler(beaconScanner!!.rangingStreamHandler)

        eventChannelMonitoring = EventChannel(messenger, "flutter_beacon_event_monitoring")
        eventChannelMonitoring.setStreamHandler(beaconScanner!!.monitoringStreamHandler)

        eventChannelBluetoothState = EventChannel(messenger, "flutter_bluetooth_state_changed")
        eventChannelBluetoothState.setStreamHandler(FlutterBluetoothStateReceiver(activity))

        eventChannelAuthorizationStatus = EventChannel(messenger, "flutter_authorization_status_changed")
        eventChannelAuthorizationStatus.setStreamHandler(locationAuthorizationStatusStreamHandler)
    }

    private fun teardownChannels() {
        activityPluginBinding?.removeActivityResultListener(this)
        activityPluginBinding?.removeRequestPermissionsResultListener(this)

        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        eventChannelMonitoring.setStreamHandler(null)
        eventChannelBluetoothState.setStreamHandler(null)
        eventChannelAuthorizationStatus.setStreamHandler(null)

        activityPluginBinding = null
    }

    private fun stopBeaconSession() {
        val manager = beaconManager ?: return
        val scanner = beaconScanner ?: return

        scanner.stopRanging()
        manager.removeAllRangeNotifiers()
        scanner.stopMonitoring()
        manager.removeAllMonitorNotifiers()

        if (manager.isBound(scanner.beaconConsumer)) {
            manager.unbind(scanner.beaconConsumer)
        }
    }

    private fun clearActivityScopedReferences() {
        platform = null
        beaconBroadcast = null
        beaconScanner = null
        beaconManager = null
        flutterResult = null
        flutterResultBluetooth = null
        eventSinkLocationAuthorizationStatus = null
    }

    private fun bindBeaconConsumer(result: MethodChannel.Result? = null): Boolean {
        val manager = beaconManager ?: return false
        val scanner = beaconScanner ?: return false

        if (manager.isBound(scanner.beaconConsumer)) {
            return false
        }

        if (result != null) {
            flutterResult = result
        }
        manager.bind(scanner.beaconConsumer)
        return true
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                if (bindBeaconConsumer(result)) {
                    return
                }
                result.success(true)
            }
            "initializeAndCheck" -> {
                initializeAndCheck(result)
            }
            "setScanPeriod" -> {
                val scanPeriod = call.argument<Int>("scanPeriod") ?: 1100
                beaconManager!!.foregroundScanPeriod = scanPeriod.toLong()
                try {
                    beaconManager!!.updateScanPeriods()
                    result.success(true)
                } catch (e: RemoteException) {
                    result.success(false)
                }
            }
            "setBetweenScanPeriod" -> {
                val betweenScanPeriod = call.argument<Int>("betweenScanPeriod") ?: 0
                beaconManager!!.foregroundBetweenScanPeriod = betweenScanPeriod.toLong()
                try {
                    beaconManager!!.updateScanPeriods()
                    result.success(true)
                } catch (e: RemoteException) {
                    result.success(false)
                }
            }
            "setBackgroundScanPeriod" -> {
                val scanPeriod = call.argument<Int>("scanPeriod") ?: 1100
                beaconManager!!.backgroundScanPeriod = scanPeriod.toLong()
                try {
                    beaconManager!!.updateScanPeriods()
                    result.success(true)
                } catch (e: RemoteException) {
                    result.success(false)
                }
            }
            "setBackgroundBetweenScanPeriod" -> {
                val betweenScanPeriod = call.argument<Int>("betweenScanPeriod") ?: 0
                beaconManager!!.backgroundBetweenScanPeriod = betweenScanPeriod.toLong()
                try {
                    beaconManager!!.updateScanPeriods()
                    result.success(true)
                } catch (e: RemoteException) {
                    result.success(false)
                }
            }
            "setUseTrackingCache" -> {
                val enabled = call.argument<Boolean>("enable") ?: false
                BeaconManager.setUseTrackingCache(enabled)
                result.success(true)
            }
            "setMaxTrackingAge" -> {
                val maxTrackingAge = call.argument<Int>("maxTrackingAge") ?: 10000
                beaconManager!!.setMaxTrackingAge(maxTrackingAge)
                result.success(true)
            }
            "setLocationAuthorizationTypeDefault" -> {
                // Android does not have the concept of "requestWhenInUse" and "requestAlways" like iOS does,
                // so this method does nothing.
                result.success(true)
            }
            "authorizationStatus" -> {
                val status = if (platform!!.checkLocationServicesPermission()) "ALLOWED" else "NOT_DETERMINED"
                result.success(status)
            }
            "checkLocationServicesIfEnabled" -> {
                result.success(platform!!.checkLocationServicesIfEnabled())
            }
            "bluetoothState" -> {
                try {
                    val flag = platform!!.checkBluetoothIfEnabled()
                    val status = if (flag) "STATE_ON" else "STATE_OFF"
                    result.success(status)
                } catch (ignored: RuntimeException) {
                    result.success("STATE_UNSUPPORTED")
                }
            }
            "requestAuthorization" -> {
                if (!platform!!.checkLocationServicesPermission()) {
                    this.flutterResult = result
                    platform!!.requestAuthorization()
                    return
                }

                // Ensure an ALLOWED status is posted back.
                eventSinkLocationAuthorizationStatus?.success("ALLOWED")
                result.success(true)
            }
            "openBluetoothSettings" -> {
                if (!platform!!.checkBluetoothIfEnabled()) {
                    this.flutterResultBluetooth = result
                    platform!!.openBluetoothSettings()
                    return
                }
                result.success(true)
            }
            "openLocationSettings" -> {
                platform!!.openLocationSettings()
                result.success(true)
            }
            "openApplicationSettings" -> {
                result.notImplemented()
            }
            "close" -> {
                stopBeaconSession()
                result.success(true)
            }
            "startBroadcast" -> {
                beaconBroadcast!!.startBroadcast(call.arguments, result)
            }
            "stopBroadcast" -> {
                beaconBroadcast!!.stopBroadcast(result)
            }
            "isBroadcasting" -> {
                beaconBroadcast!!.isBroadcasting(result)
            }
            "isBroadcastSupported" -> {
                result.success(platform!!.isBroadcastSupported())
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun initializeAndCheck(result: MethodChannel.Result?) {
        if (platform!!.checkLocationServicesPermission()
            && platform!!.checkBluetoothIfEnabled()
            && platform!!.checkLocationServicesIfEnabled()
        ) {
            result?.success(true)
            return
        }

        flutterResult = result
        when {
            !platform!!.checkBluetoothIfEnabled() -> {
                platform!!.openBluetoothSettings()
            }
            !platform!!.checkLocationServicesPermission() -> {
                platform!!.requestAuthorization()
            }
            !platform!!.checkLocationServicesIfEnabled() -> {
                platform!!.openLocationSettings()
            }
            else -> {
                if (bindBeaconConsumer()) {
                    return
                }
                result?.success(true)
            }
        }
    }

    private val locationAuthorizationStatusStreamHandler = object : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            eventSinkLocationAuthorizationStatus = events
        }

        override fun onCancel(arguments: Any?) {
            eventSinkLocationAuthorizationStatus = null
        }
    }

    // region Activity Callbacks

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == FlutterBeaconBroadcast.REQUEST_CODE_BLUETOOTH_ADVERTISE) {
            val advertisingAllowed = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (advertisingAllowed) {
                beaconBroadcast?.startAdvertisingAfterPermissionGranted()
            } else {
                beaconBroadcast?.onAdvertisingPermissionDenied()
            }
            return true
        }

        if (requestCode != REQUEST_CODE_LOCATION) {
            return false
        }

        val deniedPermissions = permissions
            .zip(grantResults.asIterable())
            .filter { (_, grantResult) -> grantResult != PackageManager.PERMISSION_GRANTED }
            .map { (permission, _) -> permission }

        val locationServiceAllowed = platform?.checkLocationServicesPermission() == true
        val status = when {
            permissions.isEmpty() || grantResults.isEmpty() -> "NOT_DETERMINED"
            locationServiceAllowed -> "ALLOWED"
            deniedPermissions.any { permission ->
                platform?.shouldShowRequestPermissionRationale(permission) == true
            } -> "NOT_DETERMINED"
            else -> "DENIED"
        }

        eventSinkLocationAuthorizationStatus?.success(status)

        flutterResult?.let {
            if (locationServiceAllowed) {
                it.success(true)
            } else {
                it.error("Beacon", "location services not allowed", null)
            }
            flutterResult = null
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE_BLUETOOTH) {
            return false
        }

        val bluetoothEnabled = resultCode == Activity.RESULT_OK

        if (bluetoothEnabled) {
            if (!platform!!.checkLocationServicesPermission()) {
                if (flutterResult == null) {
                    flutterResult = flutterResultBluetooth
                    flutterResultBluetooth = null
                }
                platform!!.requestAuthorization()
            } else {
                flutterResultBluetooth?.success(true)
                flutterResultBluetooth = null

                flutterResult?.success(true)
                flutterResult = null
            }
        } else {
            flutterResultBluetooth?.error("Beacon", "bluetooth disabled", null)
            flutterResultBluetooth = null

            flutterResult?.error("Beacon", "bluetooth disabled", null)
            flutterResult = null
        }

        return true
    }

    // endregion

    fun getBeaconManager(): BeaconManager? {
        return beaconManager
    }

    fun getActivity(): Activity? {
        return activity
    }
}
