package com.cavadalab.dchs_flutter_beacon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.altbeacon.beacon.MonitorNotifier

internal class FlutterBeaconUtilsTest {
    @Test
    fun parseState_returnsExpectedLabels() {
        assertEquals("INSIDE", FlutterBeaconUtils.parseState(MonitorNotifier.INSIDE))
        assertEquals("OUTSIDE", FlutterBeaconUtils.parseState(MonitorNotifier.OUTSIDE))
        assertEquals("UNKNOWN", FlutterBeaconUtils.parseState(Int.MIN_VALUE))
    }

    @Test
    fun regionFromMap_buildsRegionWithIdentifiers() {
        val region = FlutterBeaconUtils.regionFromMap(
            mapOf(
                "identifier" to "region-1",
                "proximityUUID" to "74278BDA-B644-4520-8F0C-720EAF059935",
                "major" to 10,
                "minor" to 20
            )
        )

        assertNotNull(region)
        assertEquals("region-1", region.uniqueId)
        assertEquals("74278bda-b644-4520-8f0c-720eaf059935", region.id1.toString())
        assertEquals(10, region.id2?.toInt())
        assertEquals(20, region.id3?.toInt())
    }

    @Test
    fun regionFromMap_returnsNullForInvalidUuid() {
        val region = FlutterBeaconUtils.regionFromMap(
            mapOf(
                "identifier" to "broken-region",
                "proximityUUID" to "not-a-uuid"
            )
        )

        assertNull(region)
    }
}
