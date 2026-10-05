package com.example.weldcalc.model

enum class Family(val label: String) { F1("F1"), F2("F2"), F2BIS("F2bis") }
enum class Process(val label: String) { NORMAL_D5("Continuous / Ø5"), NORMAL_D6("Continuous / Ø6"), PULSE_D8("Pulsation / Ø8") }
data class Sheet(val thickness: Double, val family: Family)
data class NormalRow(val ref: Double, val t2a: Int, val t2b: Int, val t3a: Int, val t3b: Int, val hold: Int, val force: Int, val currents: List<Double>)
data class PulseRow(val ref: Double, val pulses3: Int, val hot3: Int, val pulses2: Int, val hot2: Int, val hold: Int, val force: Int, val strongest: List<Double>, val currents: List<List<Double>> )
data class Result(val reference: Double, val currentKa: Double, val forceDaN: Int, val weldTime: String, val holdTime: Int, val warnings: List<String>, val source: String)
