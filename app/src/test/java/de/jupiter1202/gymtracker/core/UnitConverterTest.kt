package de.jupiter1202.gymtracker.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test
    fun kgToLbs_converts100kg_returns220point462() {
        assertEquals(220.462, UnitConverter.kgToLbs(100.0), 0.001)
    }

    @Test
    fun lbsToKg_converts220point462lbs_returns100kg() {
        assertEquals(100.0, UnitConverter.lbsToKg(220.462), 0.001)
    }

    @Test
    fun roundTrip_kgToLbsToKg_preservesPrecision() {
        val original = 75.0
        assertEquals(original, UnitConverter.lbsToKg(UnitConverter.kgToLbs(original)), 0.001)
    }
}
