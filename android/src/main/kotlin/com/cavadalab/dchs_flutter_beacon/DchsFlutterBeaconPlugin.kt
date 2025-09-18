package com.cavadalab.dchs_flutter_beacon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log

import android.app.Activity
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
    private var applicationContext: Context? = null
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
        this.applicationContext = binding.applicationContext
        setupChannels(binding.binaryMessenger, null)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = null
    }

    // ActivityAware methods
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activityPluginBinding = binding
        this.activity = binding.activity
        // Re-setup channels with activity context
        setupChannels(flutterPluginBinding!!.binaryMessenger, activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        teardownChannels()
        this.activity = null
        // Re-setup channels with only application context for background
        setupChannels(flutterPluginBinding!!.binaryMessenger, null)
    }

    private fun setupChannels(messenger: BinaryMessenger, activity: Activity?) {
        activityPluginBinding?.addActivityResultListener(this)
        activityPluginBinding?.addRequestPermissionsResultListener(this)

        val ctx = activity ?: applicationContext
        if (ctx == null) return

        beaconManager = BeaconManager.getInstanceForApplication(ctx)
        val iBeaconLayout = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        if (!beaconManager!!.beaconParsers.contains(iBeaconLayout)) {
            beaconManager!!.beaconParsers.clear()
            beaconManager!!.beaconParsers.add(iBeaconLayout)
        }

        // Use activity if available, otherwise use application context for background
        platform = if (activity != null) FlutterPlatform(activity) else FlutterPlatform(ctx)
        beaconScanner = FlutterBeaconScanner(this, ctx)
        beaconBroadcast = FlutterBeaconBroadcast(ctx, iBeaconLayout)

        channel = MethodChannel(messenger, "flutter_beacon")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(messenger, "flutter_beacon_event")
        eventChannel.setStreamHandler(beaconScanner!!.rangingStreamHandler)

        eventChannelMonitoring = EventChannel(messenger, "flutter_beacon_event_monitoring")
        eventChannelMonitoring.setStreamHandler(beaconScanner!!.monitoringStreamHandler)

        eventChannelBluetoothState = EventChannel(messenger, "flutter_bluetooth_state_changed")
        eventChannelBluetoothState.setStreamHandler(FlutterBluetoothStateReceiver(ctx))

        eventChannelAuthorizationStatus = EventChannel(messenger, "flutter_authorization_status_changed")
        eventChannelAuthorizationStatus.setStreamHandler(locationAuthorizationStatusStreamHandler)
    }

    private fun setupForegroundService(context: Context) {
        Log.d("DchsFlutterBeaconPlugin", "setupForegroundService iniziato")
        
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("DchsFlutterBeaconPlugin", "setupForegroundService iniziato beacon-ref-notification-id")
            Notification.Builder(context, "beacon-ref-notification-id")
        } else {
            Notification.Builder(context)
        }
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Beacon services")

        val intent = Intent(context, activity?.javaClass ?: return)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "beacon-ref-notification-id",
                "My Notification Name",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "My Notification Channel Description"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channel.id)
            Log.d("DchsFlutterBeaconPlugin", "NotificationChannel creato")
        }

        val notification = builder.build()
        BeaconManager.getInstanceForApplication(context).enableForegroundServiceScanning(notification, 456)
        Log.d("DchsFlutterBeaconPlugin", "Foreground service scanning abilitato")
    }

    private fun teardownChannels() {
        activityPluginBinding?.removeActivityResultListener(this)
        activityPluginBinding?.removeRequestPermissionsResultListener(this)

        platform = null
        beaconBroadcast = null

        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        eventChannelMonitoring.setStreamHandler(null)
        eventChannelBluetoothState.setStreamHandler(null)
        eventChannelAuthorizationStatus.setStreamHandler(null)

        activityPluginBinding = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                if (beaconManager != null && !beaconManager!!.isBound(beaconScanner!!.beaconConsumer)) {
                    this.flutterResult = result
                    beaconManager!!.bind(beaconScanner!!.beaconConsumer)
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
                if (beaconManager != null) {
                    beaconScanner!!.stopRanging()
                    beaconManager!!.removeAllRangeNotifiers()
                    beaconScanner!!.stopMonitoring()
                    beaconManager!!.removeAllMonitorNotifiers()
                    if (beaconManager!!.isBound(beaconScanner!!.beaconConsumer)) {
                        beaconManager!!.unbind(beaconScanner!!.beaconConsumer)
                    }
                }
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
                if (beaconManager != null && !beaconManager!!.isBound(beaconScanner!!.beaconConsumer)) {
                    beaconManager!!.bind(beaconScanner!!.beaconConsumer)
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
        if (requestCode != REQUEST_CODE_LOCATION) {
            return false
        }

        var locationServiceAllowed = false
        if (permissions.isNotEmpty() && grantResults.isNotEmpty()) {
            val permission = permissions[0]
            if (!platform!!.shouldShowRequestPermissionRationale(permission)) {
                val grantResult = grantResults[0]
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    locationServiceAllowed = true
                }
                val status = if (locationServiceAllowed) "ALLOWED" else "DENIED"
                eventSinkLocationAuthorizationStatus?.success(status)
            } else {
                eventSinkLocationAuthorizationStatus?.success("NOT_DETERMINED")
            }
        } else {
            eventSinkLocationAuthorizationStatus?.success("NOT_DETERMINED")
        }

        flutterResult?.let {
            if (locationServiceAllowed) {
                it.success(true)
            } else {
                it.error("Beacon", "location services not allowed", null)
            }
            flutterResult = null
        }

        return locationServiceAllowed
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val bluetoothEnabled = requestCode == REQUEST_CODE_BLUETOOTH && resultCode == Activity.RESULT_OK

        if (bluetoothEnabled) {
            if (!platform!!.checkLocationServicesPermission()) {
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

        return bluetoothEnabled
    }

    // endregion

    fun getBeaconManager(): BeaconManager? {
        return beaconManager
    }

    fun getActivity(): Activity? {
        return activity
    }
}
