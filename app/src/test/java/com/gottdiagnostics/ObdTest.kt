package com.gottdiagnostics
import org.junit.Assert.*
import org.junit.Test
class ObdTest {
    @Test fun decodesPidUnits() {
        assertEquals(1726.0, Obd.value("0C", "41 0C 1A F8\r>" )!!, 0.01)
        assertEquals(90.0, Obd.value("05", "SEARCHING...\r41 05 82\r>" )!!, 0.01)
        assertEquals(12.5, Obd.value("42", "41 42 30 D4")!!, 0.01)
        assertEquals(-25.0, Obd.value("06", "41 06 60")!!, 0.01)
    }
    @Test fun missingAndTruncatedAreNotZero() {
        assertNull(Obd.value("0C", "NO DATA\r>"))
        assertNull(Obd.value("0C", "41 0C 1A"))
        assertNull(Obd.value("05", "41 0D 00"))
    }
    @Test fun decodesDtcFamiliesAndIgnoresPadding() {
        assertEquals(listOf("P0133", "C0123", "U0123"), Obd.dtcs("43 01 33 41 23 C1 23 00 00", 0x43))
        assertEquals(listOf("P0300"), Obd.dtcs("47 03 00 00 00\r>", 0x47))
        assertTrue(Obd.dtcs("43 00 00 00 00", 0x43).isEmpty())
    }
    @Test fun decodesCanCountAndMultiframe() {
        assertEquals(listOf("P0133", "P0300"), Obd.dtcs("43 02 01 33 03 00", 0x43))
        assertEquals(listOf("P0133", "P0300", "P0420", "P0171"), Obd.dtcs("00A\r0: 43 04 01 33 03 00\r1: 04 20 01 71\r>", 0x43))
    }
    @Test fun flagsThresholdsOnlyWhenDataExists() {
        assertTrue(Obd.anomalies(emptyMap()).isEmpty())
        assertEquals(3, Obd.anomalies(mapOf("05" to 115.0, "42" to 10.0, "07" to -30.0)).size)
        assertTrue(Obd.anomalies(mapOf("05" to 95.0, "42" to 14.0, "07" to 5.0)).isEmpty())
    }
}
