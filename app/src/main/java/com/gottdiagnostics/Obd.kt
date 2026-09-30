package com.gottdiagnostics

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

data class Pid(val code: String, val name: String, val unit: String)
object Obd {
    val pids = listOf(
        Pid("0C", "Engine RPM", "rpm"), Pid("0D", "Speed", "km/h"), Pid("05", "Coolant", "°C"),
        Pid("04", "Engine load", "%"), Pid("11", "Throttle", "%"), Pid("0F", "Intake air", "°C"),
        Pid("06", "Short fuel trim bank 1", "%"), Pid("07", "Long fuel trim bank 1", "%"),
        Pid("08", "Short fuel trim bank 2", "%"), Pid("09", "Long fuel trim bank 2", "%"),
        Pid("03", "Fuel system 1 status", "bitmask"), Pid("10", "Mass airflow", "g/s"),
        Pid("0B", "Manifold absolute pressure", "kPa"), Pid("0E", "Ignition advance", "°"),
        Pid("1F", "Engine runtime", "s"), Pid("42", "ECU voltage", "V"),
        Pid("44", "Commanded equivalence ratio", "ratio")
    )
    fun supported(raw: String, base: Int): Set<String>? {
        val rows = bytes(raw).filter { it.size >= 6 && it[0] == 0x41 && it[1] == base }
        if (rows.isEmpty()) return null
        return buildSet {
            for (row in rows) {
                val bitmap = row.drop(2).take(4).fold(0L) { acc, byte -> (acc shl 8) or byte.toLong() }
                for (bit in 0..31) if (bitmap and (1L shl (31 - bit)) != 0L) add("%02X".format(base + bit + 1))
            }
        }
    }
    fun bytes(raw: String): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        var assembled: MutableList<Int>? = null
        for (line in raw.replace(">", "").split('\r', '\n')) {
            val trimmed = line.trim()
            val numbered = Regex("^([0-9A-Fa-f]+):\\s*(.*)$").matchEntire(trimmed)
            val clean = (numbered?.groupValues?.get(2) ?: trimmed).replace(" ", "")
            if (clean.length < 2 || clean.length % 2 != 0 || !clean.matches(Regex("[0-9A-Fa-f]+"))) continue
            val values = clean.chunked(2).map { it.toInt(16) }
            if (numbered != null) {
                if (numbered.groupValues[1].toInt(16) == 0) {
                    assembled?.let { result.add(it.toList()) }
                    assembled = values.toMutableList()
                } else assembled?.addAll(values)
            } else {
                assembled?.let { result.add(it.toList()) }; assembled = null
                result.add(values)
            }
        }
        assembled?.let { result.add(it.toList()) }
        return result
    }
    fun value(code: String, raw: String): Double? {
        val pid = code.toInt(16)
        val data = bytes(raw).firstOrNull { it.size >= 3 && it[0] == 0x41 && it[1] == pid }?.drop(2) ?: return null
        val a = data[0].toDouble()
        return when(code) {
            "0C" -> data.getOrNull(1)?.let { (a * 256 + it) / 4 }
            "42" -> data.getOrNull(1)?.let { (a * 256 + it) / 1000 }
            "05", "0F" -> a - 40
            "04", "11" -> a * 100 / 255
            "06", "07", "08", "09" -> (a - 128) * 100 / 128
            "0D", "0B", "03" -> a
            "0E" -> a / 2 - 64
            "10" -> data.getOrNull(1)?.let { (a * 256 + it) / 100 }
            "1F" -> data.getOrNull(1)?.let { a * 256 + it }
            "44" -> data.getOrNull(1)?.let { (a * 256 + it) * 2 / 65536 }
            else -> null
        }
    }
    fun dtcs(raw: String, response: Int): List<String> = bytes(raw).filter { it.firstOrNull() == response }.flatMap { row ->
        // CAN responses include a DTC count; legacy protocols return pairs directly.
        val payload = row.drop(1)
        val pairs = if (payload.size % 2 == 1) payload.drop(1).take(payload[0] * 2) else payload
        pairs.chunked(2).mapNotNull { pair ->
            if (pair.size != 2 || pair.all { it == 0 }) null else {
                val a = pair[0]; val b = pair[1]
                "${"PCBU"[a shr 6]}${(a shr 4) and 3}${(a and 15).toString(16)}${b.toString(16).padStart(2, '0')}".uppercase()
            }
        }
    }.distinct()
    fun anomalies(values: Map<String, Double>): List<String> = buildList {
        values["05"]?.let { if(it > 110) add("High coolant temperature: %.1f °C".format(it)) }
        values["42"]?.let { if(it < 11.8) add("Low ECU voltage: %.2f V".format(it)); if(it > 15.2) add("High ECU voltage: %.2f V".format(it)) }
        for (pid in listOf("06", "07", "08", "09")) values[pid]?.let { if(kotlin.math.abs(it) > 20) add("${pids.first { it.code == pid }.name} exceeds ±20%%: %.1f%%".format(it)) }
    }
}

class ObdConnection {
    @Volatile private var socket: BluetoothSocket? = null
    private val mutex = Mutex()
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        close()
        val s = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        socket = s
        try { s.connect() } catch(e: Exception) { close(); throw e }
    }
    suspend fun command(command: String, timeout: Long = 6000): String = mutex.withLock {
        val s = socket ?: throw IOException("Adapter disconnected")
        s.outputStream.write((command + "\r").toByteArray(Charsets.US_ASCII)); s.outputStream.flush()
        val deadline = android.os.SystemClock.elapsedRealtime() + timeout
        val result = StringBuilder()
        while(android.os.SystemClock.elapsedRealtime() < deadline) {
            if(s.inputStream.available() > 0) {
                val b = s.inputStream.read()
                if(b < 0) throw IOException("Adapter closed connection")
                if(b == '>'.code) return@withLock result.toString()
                result.append(b.toChar())
                if(result.length > 32768) throw IOException("Adapter response too large")
            } else delay(20)
        }
        close()
        throw IOException("Adapter response timed out; reconnect to resynchronize")
    }
    fun close() { try { socket?.close() } catch (_: IOException) {} finally { socket = null } }
}
