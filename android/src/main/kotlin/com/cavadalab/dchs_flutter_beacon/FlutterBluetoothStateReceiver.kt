package com.cavadalab.dchs_flutter_beacon

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.flutter.plugin.common.EventChannel

class FlutterBluetoothStateReceiver(private val context: Context) : BroadcastReceiver(), EventChannel.StreamHandler {

    private var eventSink: EventChannel.EventSink? = null
    private var isRegistered = false

    override fun onReceive(context: Context, intent: Intent) {
        if (eventSink == null) return
        val action = intent.action

        if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            sendState(state)
        }
    }

    private fun sendState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> eventSink?.success("STATE_OFF")
            BluetoothAdapter.STATE_TURNING_OFF -> eventSink?.success("STATE_TURNING_OFF")
            BluetoothAdapter.STATE_ON -> eventSink?.success("STATE_ON")
            BluetoothAdapter.STATE_TURNING_ON -> eventSink?.success("STATE_TURNING_ON")
            else -> eventSink?.error("BLUETOOTH_STATE", "invalid bluetooth adapter state", null)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        var state = BluetoothAdapter.STATE_OFF

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter != null) {
            state = adapter.state
        }

        eventSink = events
        sendState(state)

        if (!isRegistered) {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(this, filter)
            isRegistered = true
        }
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        if (isRegistered) {
            runCatching { context.unregisterReceiver(this) }
            isRegistered = false
        }
    }
}
