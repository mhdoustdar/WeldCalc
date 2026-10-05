package com.example.weldcalc.engine

import com.example.weldcalc.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeldingCalculatorTest {
    private val c = WeldingCalculator()
    @Test fun referenceMapping(){
        assertEquals(.6,c.referenceFor(listOf(Sheet(.64,Family.F1),Sheet(.80,Family.F1)))!!,.0001)
        assertEquals(.9,c.referenceFor(listOf(Sheet(.86,Family.F1),Sheet(.94,Family.F1),Sheet(.95,Family.F1)))!!,.0001)
    }
    @Test fun table5Example(){
        val r=c.calculate(listOf(Sheet(.8,Family.F1),Sheet(1.0,Family.F1)),Family.F1,Process.NORMAL_D6,2,50)
        assertEquals(10.0,r.currentKa,.0001); assertEquals(230,r.forceDaN); assertEquals("8 cycles",r.weldTime)
    }
    @Test fun sixtyHzKeepsCurrent(){
        val r=c.calculate(listOf(Sheet(.8,Family.F1),Sheet(1.0,Family.F1)),Family.F1,Process.NORMAL_D6,2,60)
        assertEquals(10.0,r.currentKa,.0001); assertEquals("10 cycles",r.weldTime)
    }
    @Test fun pulseF2Example(){
        val r=c.calculate(listOf(Sheet(1.5,Family.F2),Sheet(2.0,Family.F2)),Family.F2,Process.PULSE_D8,0,50)
        assertEquals(11.7,r.currentKa,.0001); assertEquals(500,r.forceDaN); assertEquals("4(5+1)",r.weldTime)
    }
    @Test fun sixCoatingIsFlagged(){
        val r=c.calculate(listOf(Sheet(.8,Family.F1),Sheet(1.0,Family.F1)),Family.F1,Process.NORMAL_D6,6,50)
        assertTrue(r.warnings.any { it.contains("۶ پوشش") })
    }
}
