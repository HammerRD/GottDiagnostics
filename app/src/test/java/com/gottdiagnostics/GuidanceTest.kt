package com.gottdiagnostics

import org.junit.Assert.*
import org.junit.Test

class GuidanceTest {
    private fun readings(rpm: Double = 800.0, speed: Double = 0.0, coolant: Double = 90.0) = mapOf("0C" to rpm, "0D" to speed, "05" to coolant, "0F" to 20.0, "04" to 30.0)
    @Test fun warmIdleNeedsTimeAndSeveralSamples() {
        val tracker = GuideTracker(Guide.IDLE, 0)
        assertFalse(tracker.accept(readings(), 0).complete)
        var result = GuideProgress("")
        for(second in 5..60 step 5) result = tracker.accept(readings(), second * 1000L)
        assertTrue(result.complete); assertTrue(result.terminal); assertEquals(60, result.seconds)
    }
    @Test fun coolantAndMovementAbortStationaryTests() {
        assertTrue(GuideTracker(Guide.IDLE, 0).accept(readings(coolant = 111.0), 1000).terminal)
        assertTrue(GuideTracker(Guide.NEUTRAL, 0).accept(readings(speed = 2.0), 1000).terminal)
        assertFalse(GuideTracker(Guide.CRUISE, 0).accept(readings(rpm = 2000.0, speed = 60.0), 1000).terminal)
    }
    @Test fun missingSpeedIsNotTreatedAsStationary() {
        val tracker = GuideTracker(Guide.IDLE, 0)
        val data = readings() - "0D"
        tracker.accept(data, 1000); tracker.accept(data, 2000)
        val result = tracker.accept(data, 3000)
        assertTrue(result.terminal); assertFalse(result.complete); assertTrue(result.text.contains("unavailable"))
    }
    @Test fun neutralNeverExtendsBeyondFifteenSeconds() {
        val tracker = GuideTracker(Guide.NEUTRAL, 0)
        tracker.accept(readings(rpm = 1800.0), 1000)
        val result = tracker.accept(readings(rpm = 2200.0), 15000)
        assertTrue(result.terminal); assertFalse(result.complete)
        assertEquals(result, tracker.accept(readings(rpm = 2200.0), 20000))
    }
    @Test fun excessiveNeutralRpmAborts() {
        assertTrue(GuideTracker(Guide.NEUTRAL, 0).accept(readings(rpm = 3100.0), 1000).terminal)
    }
    @Test fun coldStartRequiresEngineOffAndTemperatureBaseline() {
        assertTrue(GuideTracker(Guide.COLD, 0).accept(readings(coolant = 20.0), 1000).terminal)
        assertTrue(GuideTracker(Guide.COLD, 0).accept(readings(rpm = 0.0, coolant = 90.0), 1000).terminal)
        val result = GuideTracker(Guide.COLD, 0).accept(readings(rpm = 0.0, coolant = 22.0), 1000)
        assertFalse(result.terminal); assertTrue(result.text.contains("Start engine"))
    }
    @Test fun gapsDoNotCountAsContinuousObservedTime() {
        val tracker = GuideTracker(Guide.IDLE, 0)
        tracker.accept(readings(), 1000)
        assertEquals(0, tracker.accept(readings(), 40000).seconds)
    }
    @Test fun vehicleUnknownsArePreserved() {
        assertTrue(Vehicles.profile("350z-2003", "rough idle").contains("UNKNOWN"))
        assertTrue(Vehicles.profile("370z-2009", "").contains("NOT confirmed"))
        assertTrue(Vehicles.all.all { it.details.contains("manual") && it.details.contains("93 octane") })
    }
}
