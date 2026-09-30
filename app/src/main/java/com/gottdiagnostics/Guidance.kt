package com.gottdiagnostics

import kotlin.math.abs

data class Vehicle(val id: String, val title: String, val details: String)
object Vehicles {
    val all = listOf(
        Vehicle("350z-2003", "2003 350Z DE", "VQ35DE; manual; 93 octane (rating standard not specified). Upgraded injectors: brand, flow rate, latency and matching calibration UNKNOWN. Aftermarket exhaust. Identify injectors and verify calibration before high-load fueling evaluation."),
        Vehicle("350z-2006", "2006 350Z Rev-Up", "VQ35DE Rev-Up; manual; 93 octane (rating standard not specified). Kinetix intake manifold, full exhaust, high-flow catalytic converters. Tune status UNKNOWN."),
        Vehicle("370z-2009", "2009 370Z", "VQ37VHR; manual; 93 octane (rating standard not specified). Cold-air intake, full exhaust with test pipes. UpRev tune believed to match current modifications, but NOT confirmed. Catalyst-monitor behavior may reflect exhaust changes; do not assume engine failure from catalyst codes alone.")
    )
    fun get(id: String) = all.firstOrNull { it.id == id } ?: all.first()
    fun profile(id: String, notes: String) = get(id).let { "${it.title}\n${it.details}\nOwner notes / symptoms: $notes" }
}

enum class Guide(val title: String, val seconds: Int, val maxSeconds: Int, val required: Set<String>, val instructions: String, val purpose: String) {
    COLD("Cold start", 120, 300, setOf("0C", "0D", "05", "0F"),
        "Park outdoors with ventilation, neutral selected and parking brake on. Let the engine cool for at least 6 hours. With ignition ON and engine OFF, connect and start this recording. Wait for ‘Start engine’ before starting without pressing the accelerator. Leave it idling; do not rev.",
        "Captures the transition from engine off to running and the first two minutes. Coolant and intake temperature should initially be within 10 °C. That check alone cannot prove a full cold soak."),
    IDLE("Warm idle", 60, 600, setOf("0C", "0D", "05"),
        "Park outdoors, select neutral, apply the parking brake and turn off A/C. Allow the engine to warm normally. Start recording and leave the accelerator untouched. Qualifying data requires coolant 75–105 °C and RPM 550–1,200 while stationary.",
        "Examines idle stability and available mixture corrections. One minute of qualifying idle is useful for a baseline; it does not certify engine health."),
    NEUTRAL("Brief neutral RPM", 10, 15, setOf("0C", "0D", "05"),
        "Only with a warm, normally running engine: park outdoors, select neutral and apply the parking brake. Start recording, then gently hold 2,000–2,500 RPM for up to 15 seconds and release the accelerator. Never chase the timer or extend the hold; stop if the app cannot collect enough data. Recording stops 15 seconds after you press Start; release the accelerator when it stops. No redline or throttle blips.",
        "Compares mixture behavior against warm idle. No-load revving cannot reproduce road load, validate full-load fueling, or prove a tune is safe."),
    CRUISE("Steady normal driving", 90, 600, setOf("0C", "0D", "05", "04"),
        "Set up while parked. Secure the phone and keep the app open; a passenger may operate it. Drive only at a legal, comfortable pace, without hard acceleration or lugging. The app looks for moderate load and fairly steady speed. Never change your driving to satisfy the timer. Review results only after parking.",
        "Observes normal road load. Looks for speed above 15 km/h, RPM 1,200–3,500, coolant 75–105 °C and reported load at most 60%. This is not a high-load or full-throttle test.");
    val abort = "Stop the test for an oil-pressure warning, flashing check-engine light, overheating, knocking or severe rough running. For road sessions, pull over safely before using the phone. The app cannot detect every unsafe condition."
}

data class GuideProgress(val text: String, val seconds: Int = 0, val samples: Int = 0, val terminal: Boolean = false, val complete: Boolean = false, val qualifying: Boolean = false)

/** Conservative approximate coverage; sequential OBD samples are not simultaneous measurements. */
class GuideTracker(private val guide: Guide, private val startedMs: Long) {
    private var lastMs = startedMs
    private var qualifyingMs = 0L
    private var qualifyingSamples = 0
    private var previousQualified = false
    private var missingCycles = 0
    private var coldPrepared = false
    private var cruiseSpeed: Double? = null
    private var done: GuideProgress? = null
    fun accept(values: Map<String, Double>, nowMs: Long): GuideProgress {
        done?.let { return it }
        val gap = (nowMs - lastMs).coerceAtLeast(0); lastMs = nowMs
        fun finish(text: String, complete: Boolean = false): GuideProgress = GuideProgress(text, (qualifyingMs / 1000).toInt(), qualifyingSamples, true, complete, complete).also { done = it }
        if ((values["05"] ?: 0.0) > 110) return finish("Stopped: high coolant temperature. Stop testing and assess the vehicle safely.")
        if (guide != Guide.CRUISE && (values["0D"] ?: 0.0) > 0) return finish("Stopped: vehicle movement detected during a stationary test.")
        if (nowMs - startedMs >= guide.maxSeconds * 1000L) return finish("Time limit reached without enough qualifying data. Release the accelerator if raised; review missing readings or test conditions. Do not extend a neutral hold.")
        val missing = guide.required - values.keys
        if (missing.isNotEmpty()) {
            previousQualified = false; missingCycles++
            if (missingCycles >= 3) return finish("Cannot assess this test: required readings unavailable (${missing.joinToString()}). Missing is not zero; use a compatible diagnostic tool rather than repeating the same test.")
            return GuideProgress("Waiting for required PIDs: ${missing.joinToString()}")
        }
        missingCycles = 0
        val rpm = values.getValue("0C"); val coolant = values.getValue("05"); val speed = values.getValue("0D")
        if (guide == Guide.NEUTRAL && rpm > 3000) return finish("Stopped: RPM too high for this brief no-load check. Release the accelerator.")
        if (guide == Guide.COLD && !coldPrepared) {
            if (rpm > 100) return finish("Cold-start capture must begin with the engine OFF. This recording cannot establish a cold-start baseline.")
            if (abs(coolant - values.getValue("0F")) > 10) return finish("Coolant and intake temperatures differ by more than 10 °C. A cold-soak baseline is not established; investigate or wait for a suitable cold start.")
            coldPrepared = true
            return GuideProgress("Start engine now without pressing the accelerator; leave it idling.")
        }
        val qualifies = when (guide) {
            Guide.COLD -> rpm in 400.0..2000.0
            Guide.IDLE -> coolant in 75.0..105.0 && rpm in 550.0..1200.0
            Guide.NEUTRAL -> coolant in 75.0..105.0 && rpm in 2000.0..2500.0
            Guide.CRUISE -> {
                val steady = cruiseSpeed?.let { abs(speed - it) <= 8 } ?: true
                cruiseSpeed = speed
                coolant in 75.0..105.0 && rpm in 1200.0..3500.0 && speed > 15 && values.getValue("04") <= 60 && steady
            }
        }
        if (qualifies) {
            qualifyingSamples++
            if (previousQualified && gap <= 15000) qualifyingMs += gap.coerceAtMost(5000)
        }
        previousQualified = qualifies
        if (qualifyingMs >= guide.seconds * 1000L && qualifyingSamples >= 4) return finish("Recording complete: enough qualifying data for this check, not a clean bill of health. ${if (guide == Guide.NEUTRAL) "Release the accelerator." else "Review after parking."}", true)
        return GuideProgress(if (qualifies) "Capturing qualifying readings…" else "Conditions do not match this test; recording continues. Do not force conditions or extend a neutral hold.", (qualifyingMs / 1000).toInt(), qualifyingSamples, qualifying = qualifies)
    }
}
