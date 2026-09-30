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

    @Test fun loadedGuidesAreFifthAndSixthAndHaveDistinctRequiredChannels() {
        assertEquals(Guide.TRACK, Guide.entries[4]); assertEquals(Guide.DYNO, Guide.entries[5])
        assertTrue("0D" in Guide.TRACK.required); assertFalse("0D" in Guide.DYNO.required)
    }
    @Test fun trackRequiresMovementButDoesNotAbortForMovement() {
        val tracker = GuideTracker(Guide.TRACK, 0)
        val loaded = readings(rpm = 4000.0, speed = 80.0) + ("04" to 85.0)
        val result = tracker.accept(loaded, 1000)
        assertTrue(result.qualifying); assertFalse(result.terminal)
        assertFalse(tracker.accept(loaded + ("0D" to 0.0), 2000).qualifying)
    }
    @Test fun dynoSupportsZeroOrUnavailableWheelSpeed() {
        for(data in listOf(readings(speed = 0.0), readings() - "0D")) {
            val result = GuideTracker(Guide.DYNO, 0).accept(data + ("04" to 85.0), 1000)
            assertTrue(result.qualifying); assertFalse(result.terminal)
        }
    }
    @Test fun loadedCaptureNeverDeclaresAnAutomaticPass() {
        for(guide in listOf(Guide.TRACK, Guide.DYNO)) {
            val tracker = GuideTracker(guide, 0)
            val data = readings(rpm = 4000.0, speed = 80.0) + ("04" to 85.0)
            for(second in 0..590 step 5) {
                val result = tracker.accept(data, second * 1000L)
                assertFalse(result.complete); assertFalse(result.terminal)
            }
            val end = tracker.accept(data, 600000)
            assertTrue(end.terminal); assertFalse(end.complete); assertTrue(end.text.contains("No pass/fail"))
        }
    }
    @Test fun loadedMissingChannelsAndTemperatureStillStopCapture() {
        for(guide in listOf(Guide.TRACK, Guide.DYNO)) {
            val tracker = GuideTracker(guide, 0)
            val missing = readings() - "04"
            tracker.accept(missing, 1000); tracker.accept(missing, 2000)
            assertTrue(tracker.accept(missing, 3000).terminal)
            assertTrue(GuideTracker(guide, 0).accept(readings(coolant = 111.0), 1000).terminal)
        }
    }
    @Test fun loadedCoverageExcludesCooldownAndLongGaps() {
        val tracker = GuideTracker(Guide.DYNO, 0)
        val loaded = readings() + ("04" to 85.0)
        tracker.accept(loaded, 0)
        assertEquals(5, tracker.accept(loaded, 5000).seconds)
        assertFalse(tracker.accept(readings(), 10000).qualifying)
        assertEquals(5, tracker.accept(loaded, 15000).seconds)
        assertEquals(5, tracker.accept(loaded, 40000).seconds)
    }
}
