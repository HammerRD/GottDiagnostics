package com.gottdiagnostics

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AnalysisTest {
    private val response = """{"status":"completed","output":[{"type":"reasoning","summary":[]},{"type":"message","content":[{"type":"output_text","text":"Check recorded fuel trims."}]}]}"""
    @Test fun responseTextAndRefusalAreHandled() {
        assertEquals("Check recorded fuel trims.", AnalysisProtocol.answer(response))
        assertEquals("Cannot assess.", AnalysisProtocol.answer("""{"output":[{"type":"message","content":[{"type":"refusal","refusal":"Cannot assess."}]}]}"""))
        assertTrue(AnalysisProtocol.answer(response.replace("completed", "incomplete")).contains("Response incomplete"))
    }
    @Test(expected = IllegalStateException::class) fun emptyResponseIsNotSuccess() { AnalysisProtocol.answer("""{"output":[]}""") }
    @Test fun requestDisablesStorageAndBoundsHistory() {
        val request = AnalysisProtocol.request("gpt-5.4-mini", JSONObject().put("vehicle", "2003"), "Why rough?", List(6) { "Q$it" to "A$it" })
        assertFalse(request.getBoolean("store"))
        assertFalse(request.has("tools")); assertFalse(request.has("api_key"))
        val input = JSONObject(request.getString("input"))
        assertEquals(3, input.getJSONArray("previous_exchanges").length())
        assertTrue(request.getString("instructions").contains("Never produce executable"))
    }
    @Test fun summaryUsesRecordedVehicleAndBoundsDetailsButKeepsStatistics() {
        val lines = mutableListOf("""{"type":"vehicle","data":"2003 with UNKNOWN injectors"}""")
        lines += """{"type":"raw","data":"adapter raw reply excluded from AI"}"""
        for(i in 1..40) lines += """{"type":"sample","time":"t$i","data":{"0C":$i}}"""
        val summary = SessionSummary.create(lines.asSequence(), "session-1.jsonl")
        assertEquals(40, summary.getInt("sample_count"))
        assertEquals(30, summary.getJSONArray("last_30_samples").length())
        assertEquals("2003 with UNKNOWN injectors", summary.getString("vehicle"))
        val rpm = summary.getJSONObject("statistics_all_samples").getJSONObject("0C")
        assertEquals(20.5, rpm.getDouble("mean"), 0.001)
        assertEquals(1.0, rpm.getDouble("min"), 0.001)
        assertFalse(summary.toString().contains("adapter raw reply"))
        assertTrue(summary.getJSONArray("missing_pids").toString().contains("05"))
    }
    @Test fun scanAndGuideOutcomesSurviveWithoutLiveSamples() {
        val summary = SessionSummary.create(sequenceOf("""{"type":"dtcs","data":["Stored: P0300"]}""", """{"type":"guide_end","data":{"complete":false,"result":"Stopped"}}"""), "session")
        assertEquals(0, summary.getInt("sample_count"))
        assertEquals(1, summary.getJSONArray("dtc_scans_last_10").length())
        assertFalse(summary.getJSONArray("guide_events_last_30").getJSONObject(0).getJSONObject("data").getBoolean("complete"))
    }
    @Test fun idleAndNeutralStatisticsStaySeparate() {
        val summary = SessionSummary.create(sequenceOf(
            """{"type":"sample","guide":"IDLE","qualifying":true,"data":{"0C":800}}""",
            """{"type":"sample","guide":"IDLE","qualifying":false,"data":{"0C":1500}}""",
            """{"type":"sample","guide":"NEUTRAL","qualifying":true,"data":{"0C":2200}}"""
        ), "session")
        val groups = summary.getJSONObject("qualifying_statistics_by_guide")
        assertEquals(800.0, groups.getJSONObject("IDLE").getJSONObject("0C").getDouble("mean"), 0.01)
        assertEquals(2200.0, groups.getJSONObject("NEUTRAL").getJSONObject("0C").getDouble("mean"), 0.01)
    }
    @Test fun httpErrorsGiveActionableMessagesWithoutEchoingCredentials() {
        assertTrue(AnalysisProtocol.httpError(401).contains("Replace"))
        assertTrue(AnalysisProtocol.httpError(429).contains("billing"))
        assertTrue(AnalysisProtocol.httpError(503).contains("temporarily"))
    }
    private class FakeConnection(private val status: Int, private val response: String): HttpURLConnection(URL("https://api.openai.com/v1/responses")) {
        val sent = ByteArrayOutputStream()
        var closed = false
        override fun connect() {}
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getOutputStream() = sent
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
    }
    @Test fun transportPostsAuthenticatedJsonWithoutFollowingRedirects() {
        val fake = FakeConnection(200, response)
        val answer = OpenAiClient { fake }.analyze("test-key", AnalysisProtocol.request("model", JSONObject(), "", emptyList()))
        assertEquals("Check recorded fuel trims.", answer)
        assertEquals("POST", fake.requestMethod)
        assertEquals("Bearer test-key", fake.getRequestProperty("Authorization"))
        assertFalse(fake.instanceFollowRedirects)
        assertFalse(fake.sent.toString().contains("test-key"))
        assertTrue(fake.closed)
    }
    @Test fun transportRejectsHttpFailureAndCloses() {
        val fake = FakeConnection(401, "do not expose response credentials")
        try { OpenAiClient { fake }.analyze("key", JSONObject()); fail("Expected failure") }
        catch(e: IllegalStateException) { assertTrue(e.message!!.contains("API key")); assertFalse(e.message!!.contains("credentials")) }
        assertTrue(fake.closed)
    }
}
