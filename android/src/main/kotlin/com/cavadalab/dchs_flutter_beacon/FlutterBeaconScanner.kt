package com.cavadalab.dchs_flutter_beacon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.plugin.common.EventChannel
import org.altbeacon.beacon.*
import java.lang.ref.WeakReference

class FlutterBeaconScanner(private val plugin: DchsFlutterBeaconPlugin, private val context: Context) {

    companion object {
        private val TAG = FlutterBeaconScanner::class.java.simpleName
    }

    private val contextRef = WeakReference(context)
    private val handler = Handler(Looper.getMainLooper())

    private var eventSinkRanging: EventChannel.EventSink? = null
    private var eventSinkMonitoring: EventChannel.EventSink? = null
    private var regionRanging: MutableList<Region>? = null
    private var regionMonitoring: MutableList<Region>? = null

    val rangingStreamHandler = object : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            Log.d("FLUTTER-BEACON - RANGING", "Start ranging = $arguments")
            startRanging(arguments, events)
        }

        override fun onCancel(arguments: Any?) {
            Log.d("FLUTTER-BEACON - RANGING", "Stop ranging = $arguments")
            stopRanging()
        }
    }

    private fun startRanging(arguments: Any?, eventSink: EventChannel.EventSink?) {
        if (arguments is List<*>) {
            regionRanging = mutableListOf()
            arguments.forEach { obj ->
                if (obj is Map<*, *>) {
                    val region = FlutterBeaconUtils.regionFromMap(obj as Map<String, Any>)
                    if (region != null) {
                        regionRanging?.add(region)
                    }
                }
            }
        } else {
            eventSink?.error("FLUTTER-BEACON - Beacon", "Invalid region for ranging", null)
            return
        }
        eventSinkRanging = eventSink
        val beaconManager = plugin.getBeaconManager()
        if (beaconManager != null && !beaconManager.isBound(beaconConsumer)) {
            beaconManager.bind(beaconConsumer)
        } else {
            startRanging()
        }
    }

    fun startRanging() {
        if (regionRanging.isNullOrEmpty()) {
            Log.e("FLUTTER-BEACON - RANGING", "Region ranging is null or empty. Ranging not started.")
            return
        }
        try {
            val beaconManager = plugin.getBeaconManager()
            beaconManager?.apply {
                removeAllRangeNotifiers()
                addRangeNotifier(rangeNotifier)
                regionRanging?.forEach { region ->
                    startRangingBeaconsInRegion(region)
                }
            }
        } catch (e: RemoteException) {
            eventSinkRanging?.error("FLUTTER-BEACON - Beacon", e.localizedMessage, null)
        }
    }

    fun stopRanging() {
        Log.d("FLUTTER-BEACON - RANGING", "stopRanging() called")
        if (!regionRanging.isNullOrEmpty()) {
            try {
                val beaconManager = plugin.getBeaconManager()
                regionRanging?.forEach { region ->
                    beaconManager?.stopRangingBeaconsInRegion(region)
                }
                beaconManager?.removeRangeNotifier(rangeNotifier)
            } catch (ignored: RemoteException) {
            }
        }
        eventSinkRanging = null
    }

    private val rangeNotifier = RangeNotifier { beacons, region ->
        if (eventSinkRanging != null) {
            val map = mutableMapOf<String, Any?>()
            map["region"] = FlutterBeaconUtils.regionToMap(region)
            map["beacons"] = FlutterBeaconUtils.beaconsToArray(ArrayList(beacons))
            handler.post {
                if (eventSinkRanging != null) {
                    eventSinkRanging?.success(map)
                } else {
                    Log.e("FLUTTER-BEACON - RANGING", "eventSinkRanging is null inside handler")
                }
            }
        } else {
            Log.e("FLUTTER-BEACON - RANGING", "eventSinkRanging is null before handler")
        }
    }
    
    val monitoringStreamHandler = object : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            startMonitoring(arguments, events)
        }

        override fun onCancel(arguments: Any?) {
            stopMonitoring()
        }
    }

    private fun startMonitoring(arguments: Any?, eventSink: EventChannel.EventSink?) {
        Log.d(TAG, "FLUTTER-BEACON - START MONITORING = $arguments")
        if (arguments is List<*>) {
            regionMonitoring = mutableListOf()
            arguments.forEach { obj ->
                if (obj is Map<*, *>) {
                    val region = FlutterBeaconUtils.regionFromMap(obj as Map<String, Any>)
                    if (region != null) {
                        regionMonitoring?.add(region)
                    }
                }
            }
        } else {
            eventSink?.error("Beacon", "Invalid region for monitoring", null)
            return
        }
        eventSinkMonitoring = eventSink
        val beaconManager = plugin.getBeaconManager()
        if (beaconManager != null && !beaconManager.isBound(beaconConsumer)) {
            beaconManager.bind(beaconConsumer)
        } else {
            startMonitoring()
        }
    }

    fun startMonitoring() {
        if (regionMonitoring.isNullOrEmpty()) {
            Log.e("MONITORING", "Region monitoring is null or empty. Monitoring not started.")
            return
        }
        try {
            val beaconManager = plugin.getBeaconManager()
            beaconManager?.apply {
                removeAllMonitorNotifiers()
                addMonitorNotifier(monitorNotifier)
                regionMonitoring?.forEach { region ->
                    startMonitoringBeaconsInRegion(region)
                }
            }
        } catch (e: RemoteException) {
            eventSinkMonitoring?.error("Beacon", e.localizedMessage, null)
        }
    }

    fun stopMonitoring() {
        if (!regionMonitoring.isNullOrEmpty()) {
            try {
                val beaconManager = plugin.getBeaconManager()
                regionMonitoring?.forEach { region ->
                    beaconManager?.stopMonitoringBeaconsInRegion(region)
                }
                beaconManager?.removeMonitorNotifier(monitorNotifier)
            } catch (ignored: RemoteException) {
            }
        }
        eventSinkMonitoring = null
    }

    private val monitorNotifier = object : MonitorNotifier {
        override fun didEnterRegion(region: Region) {
            eventSinkMonitoring?.let { sink ->
                val map = mutableMapOf<String, Any?>()
                map["event"] = "didEnterRegion"
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post {
                    sink.success(map)
                }
            }
        }

        override fun didExitRegion(region: Region) {
            eventSinkMonitoring?.let { sink ->
                val map = mutableMapOf<String, Any?>()
                map["event"] = "didExitRegion"
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post {
                    sink.success(map)
                }
            }
        }

        override fun didDetermineStateForRegion(state: Int, region: Region) {
            eventSinkMonitoring?.let { sink ->
                val map = mutableMapOf<String, Any?>()
                map["event"] = "didDetermineStateForRegion"
                map["state"] = FlutterBeaconUtils.parseState(state)
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post {
                    sink.success(map)
                }
            }
        }
    }

    val beaconConsumer = object : BeaconConsumer {
        override fun onBeaconServiceConnect() {
            
            if (plugin.flutterResult != null) {
                plugin.flutterResult?.success(true)
                plugin.flutterResult = null
            } else {
                startRanging()
                startMonitoring()
            }
        }

        override fun getApplicationContext(): Context {
            return contextRef.get()?.applicationContext!!
        }

        override fun unbindService(connection: ServiceConnection) {
            val ctx = contextRef.get()
            if (ctx is Activity) {
                ctx.unbindService(connection)
            } else {
                ctx?.unbindService(connection)
            }
        }

        override fun bindService(intent: Intent, connection: ServiceConnection, mode: Int): Boolean {
            val ctx = contextRef.get()
            return if (ctx is Activity) {
                ctx.bindService(intent, connection, mode)
            } else {
                ctx?.bindService(intent, connection, mode) ?: false
            }
        }
    }
}
