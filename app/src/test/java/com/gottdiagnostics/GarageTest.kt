package com.gottdiagnostics

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GarageTest {
    @Test fun customProfilesSurviveSerializationWithoutNissanAssumptions() {
        val car = Garage.create("2018", "Ford", "Mustang GT", "Blue", "V8; manual; 93 AKI; stock tune")
        val restored = Garage.decode(Garage.encode(listOf(car))).single()
        assertEquals(car, restored)
        assertEquals("Ford", restored.make)
        assertEquals("Blue", restored.paint)
        assertTrue(restored.profile("Misfire when warm").contains("Misfire when warm"))
        assertFalse(restored.profile("").contains("VQ35"))
    }
    @Test fun identicalModelsHaveIndependentIdentities() {
        val first = Garage.create("2018", "Ford", "Mustang", "", "")
        val second = Garage.create("2018", "Ford", "Mustang", "", "")
        assertNotEquals(first.id, second.id)
        assertEquals(2, Garage.decode(Garage.encode(listOf(first, second))).size)
        assertFalse(sessionMatchesVehicle(sequenceOf(event("vehicle_id", first.id), event("vehicle", first.profile(""))), second))
    }
    @Test fun logsMatchStableIdEvenAfterProfileChanges() {
        val car = Garage.create("2018", "Ford", "Mustang", "", "")
        assertTrue(sessionMatchesVehicle(sequenceOf(event("vehicle_id", car.id), event("vehicle", "old snapshot")), car))
    }
    @Test fun legacyNissanLogsAreMatchedButNeverGivenToNewCars() {
        val nissan = Vehicles.all.first()
        val header = event("vehicle", Vehicles.profile(nissan.id, "old notes"))
        assertTrue(sessionMatchesVehicle(sequenceOf(header), nissan))
        assertFalse(sessionMatchesVehicle(sequenceOf(header), Vehicles.all.last()))
        assertFalse(sessionMatchesVehicle(sequenceOf(header), Garage.create("2003", "Nissan", "350Z DE", "", "")))
    }
    @Test fun malformedStorageAndDuplicateEntriesAreHandled() {
        assertTrue(Garage.decode("bad json").isEmpty())
        assertTrue(Garage.decode("[{}]").isEmpty())
        val car = Garage.create("2020", "Toyota", "Supra", "", "")
        assertEquals(listOf(car), Garage.decode(Garage.encode(listOf(car, car))))
    }
    @Test fun rejectsMissingIdentityAndBadYear() {
        for(args in listOf(listOf("", "Ford", "Mustang"), listOf("1000", "Ford", "Mustang"), listOf("2020", "", "Mustang"))) {
            assertTrue(runCatching { Garage.create(args[0], args[1], args[2], "", "") }.isFailure)
        }
    }
    private fun event(type: String, value: String) = JSONObject().put("type", type).put("data", value).toString()
}
