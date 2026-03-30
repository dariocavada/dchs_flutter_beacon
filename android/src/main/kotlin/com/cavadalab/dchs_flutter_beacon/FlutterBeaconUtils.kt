package com.cavadalab.dchs_flutter_beacon

import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
import java.util.*

object FlutterBeaconUtils {

    fun parseState(state: Int): String {
        return when (state) {
            MonitorNotifier.INSIDE -> "INSIDE"
            MonitorNotifier.OUTSIDE -> "OUTSIDE"
            else -> "UNKNOWN"
        }
    }

    fun beaconsToArray(beacons: List<Beacon>?): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        beacons?.forEach { beacon ->
            val map = beaconToMap(beacon)
            list.add(map)
        }
        return list
    }

    private fun beaconToMap(beacon: Beacon): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["proximityUUID"] = beacon.id1.toString().uppercase(Locale.getDefault())
        map["major"] = beacon.id2.toInt()
        map["minor"] = beacon.id3.toInt()
        map["rssi"] = beacon.rssi
        map["txPower"] = beacon.txPower
        map["accuracy"] = String.format(Locale.US, "%.2f", beacon.distance)
        map["macAddress"] = beacon.bluetoothAddress ?: ""
        return map
    }

    fun regionToMap(region: Region): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["identifier"] = region.uniqueId
        region.id1?.let {
            map["proximityUUID"] = it.toString()
        }
        region.id2?.let {
            map["major"] = it.toInt()
        }
        region.id3?.let {
            map["minor"] = it.toInt()
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    fun regionFromMap(map: Map<*, *>): Region? {
        return try {
            var identifier = ""
            val identifiers = mutableListOf<Identifier>()

            val objectIdentifier = map["identifier"]
            if (objectIdentifier is String) {
                identifier = objectIdentifier
            }

            val proximityUUID = map["proximityUUID"]
            if (proximityUUID is String) {
                identifiers.add(Identifier.parse(proximityUUID))
            }

            val major = map["major"]
            if (major is Int) {
                identifiers.add(Identifier.fromInt(major))
            }
            val minor = map["minor"]
            if (minor is Int) {
                identifiers.add(Identifier.fromInt(minor))
            }

            Region(identifier, identifiers)
        } catch (e: RuntimeException) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun beaconFromMap(map: Map<*, *>): Beacon {
        val builder = Beacon.Builder()

        val proximityUUID = map["proximityUUID"]
        if (proximityUUID is String) {
            builder.setId1(proximityUUID)
        }
        val major = map["major"]
        if (major is Int) {
            builder.setId2(major.toString())
        }
        val minor = map["minor"]
        if (minor is Int) {
            builder.setId3(minor.toString())
        }

        val txPower = map["txPower"]
        if (txPower is Int) {
            builder.setTxPower(txPower)
        } else {
            builder.setTxPower(-59)
        }

        builder.setDataFields(listOf(0L))
        builder.setManufacturer(0x004C) // Apple Inc.

        //builder.manufacturer = 0x004C // Apple Inc.

        return builder.build()
    }
}
