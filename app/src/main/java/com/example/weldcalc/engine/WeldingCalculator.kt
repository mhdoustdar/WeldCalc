package com.example.weldcalc.engine

import com.example.weldcalc.data.StandardData
import com.example.weldcalc.model.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class WeldingCalculator {
    fun referenceFor(sheets: List<Sheet>): Double? {
        if (sheets.size == 2) return StandardData.retain(min(sheets[0].thickness, sheets[1].thickness))
        if (sheets.size == 3) return StandardData.retain(sheets.map { it.thickness }.average())
        return null
    }

    fun calculate(
        sheets: List<Sheet>, family: Family, process: Process, coatedFaces: Int,
        hz: Int ,
        coatingOver10um: Boolean = false, counterElectrode: Boolean = false
    ): Result {
        val warnings = mutableListOf<String>()
        require(sheets.size in 2..3) { "این نسخه محاسبات پارامتر را برای ۲ یا ۳ ورق انجام می‌دهد." }
        require(sheets.all { it.thickness in 0.55..3.0 }) { "محدوده ضخامت استاندارد: 0.55 تا 3.00 mm." }
        require(coatedFaces in 0..6) { "تعداد سطوح پوشش‌دار باید 0 تا 6 باشد." }
        if (coatedFaces == 6) warnings += "استاندارد در §4.1 مونتاژهای ۶ پوشش را ممنوع اعلام می‌کند؛ نتیجه برای استفاده تولیدی مجاز نیست."
        val ref = referenceFor(sheets) ?: error("ضخامت مرجع قابل تعیین نیست.")
        if (sheets.size == 3) warnings += "برای ۳ ورق، E بر اساس میانگین ضخامت ورق‌ها تعیین و سپس طبق §4.2 به ضخامت نگه‌داشته‌شده تبدیل شد."
        if (counterElectrode && sheets.any { it.thickness > 1.3 }) warnings += "در حالت counter-electrode، Emax ≤ 1.3 mm در شکل/جدول §4.1 محدودیت دارد."
        if (family != Family.F1) warnings += "برای F2/F2bis برنامه جوش اختصاصی لازم است و بهینه‌سازی holding time ممنوع است."
        if (sheets.any { it.family != family }) warnings += "خانواده پارامتر انتخابی با خانواده حداقل یکی از ورق‌ها متفاوت است؛ تطبیق مهندسی خانواده باید تأیید شود."
        if (hz !in listOf(50,60)) error("فرکانس فقط 50 یا 60 Hz است.")

        val result = when (process) {
            Process.NORMAL_D5 -> normal5(sheets, family, ref, coatedFaces, hz, coatingOver10um, warnings)
            Process.NORMAL_D6 -> normal6(sheets, family, ref, coatedFaces, hz, coatingOver10um, counterElectrode, warnings)
            Process.PULSE_D8 -> pulse(sheets, family, ref, coatedFaces, hz, warnings)
        }
        return result
    }

    private fun normal5(sheets: List<Sheet>, family: Family, ref: Double, coated: Int, hz: Int, over10: Boolean, warnings: MutableList<String>): Result {
        if (family != Family.F1) {
            warnings += "جدول 4 فقط پارامتر F1 را ارائه می‌کند و 0.9/1.0 برای F2/F2bis صراحتاً نامعتبر است."
            if (ref >= .9) error("برای Ø5 و خانواده F2/F2bis در E=0.9 یا 1.0 طبق Table 4 پارامتر مجاز وجود ندارد.")
        }
        require(coated in listOf(2,4,6)) { "برای Ø5، Table 4 فقط 2، 4 یا 6 سطح پوشش را جدول‌بندی کرده است." }
        val row = StandardData.d5F1.firstOrNull { kotlin.math.abs(it.ref-ref)<1e-9 } ?: error("Table 4 فقط تا E=1.0 را پوشش می‌دهد.")
        var t = if (sheets.size == 2) if (sheets[1].thickness < 1.47) row.t2a else row.t2b else if (sheets[2].thickness < 1.47) row.t3a else row.t3b
        var i = row.currents[listOf(2,4,6).indexOf(coated)]
        if (over10) { i += .5; t += 2; warnings += "قاعده coating >10 µm اعمال شد: +0.5 kA و +2 cycles؛ مبنای این قاعده §5.2.2 است و برای تولید باید دامنه کاربرد آن تأیید شود." }
        if (hz == 60) t = ceil(t * 1.2).toInt()
        if (hz == 60) warnings += "60 Hz: زمان جوش 20% افزایش یافت؛ جریان بدون تغییر (§5.5)."
        return Result(row.ref,i,row.force,"$t cycles",row.hold,warnings,"E34.03.180.G §5.1 Table 4 — Ø5 Class N")
    }

    private fun normal6(sheets: List<Sheet>, family: Family, ref: Double, coated: Int, hz: Int, over10: Boolean, counter: Boolean, warnings: MutableList<String>): Result {
        val rows = when(family) { Family.F1 -> StandardData.d6F1; Family.F2 -> StandardData.d6F2; Family.F2BIS -> StandardData.d6F2bis }
        val row = rows.firstOrNull { kotlin.math.abs(it.ref-ref)<1e-9 } ?: error("این ضخامت مرجع در Table 5/6/7 موجود نیست.")
        var t = if (sheets.size == 2) if (sheets[1].thickness <= 1.5) row.t2a else row.t2b else if (sheets[2].thickness <= 1.95) row.t3a else row.t3b
        var i = row.currents.getOrElse(coated) { error("پوشش خارج از محدوده جدول است.") }
        if (over10) { i += .5; t += 2; warnings += "قاعده coating >10 µm اعمال شد: +0.5 kA و +2 cycles (§5.2.2)." }
        if (counter && family == Family.F2BIS) { i += 1.0; warnings += "F2bis روی counter electrode: +1000 A طبق Table 7." }
        if (hz == 60) { t = ceil(t*1.2).toInt(); warnings += "60 Hz: زمان جوش 20% افزایش یافت؛ جریان بدون تغییر (§5.5)." }
        return Result(row.ref,i,row.force,"$t cycles",row.hold,warnings,"E34.03.180.G §5.2.1 Table ${when(family){Family.F1->5;Family.F2->6;Family.F2BIS->7}} — Ø6 Class N")
    }

    private fun pulse(sheets: List<Sheet>, family: Family, ref: Double, coated: Int, hz: Int, warnings: MutableList<String>): Result {
        require(coated <= 5) { "۶ پوشش در استاندارد ممنوع است." }
        val rows = when(family){Family.F1->StandardData.pF1;Family.F2->StandardData.pF2;Family.F2BIS->StandardData.pF2bis}
        val row = rows.firstOrNull { kotlin.math.abs(it.ref-ref)<1e-9 } ?: error("این ضخامت مرجع در Table 8/9/10 موجود نیست.")
        val strongest = sheets.maxOf { it.thickness }
        val finest = StandardData.retain(sheets.minOf { it.thickness }) ?: error("ضخامت نازک‌ترین ورق خارج از جدول است.")
        val sIndex = row.strongest.indexOfFirst { strongest <= it + 1e-9 }
        if (sIndex < 0) error("ضخامت قوی‌ترین ورق برای این ردیف جدول pulsation موجود نیست.")
        val fIndex = finestColumn(finest)
        val current = row.currents[sIndex].getOrNull(fIndex) ?: error("ترکیب ضخامت نازک‌ترین/قوی‌ترین ورق در جدول موجود نیست.")
        val isThree = sheets.size == 3
        val pulses = if (isThree) row.pulses3 else row.pulses2
        val hotBase = if (isThree) row.hot3 else row.hot2
        val hot = if (hz == 50) hotBase else ceil(hotBase * 1.2).toInt()
        if (hz == 60) warnings += "60 Hz: زمان جوش 20% افزایش یافت؛ جریان بدون تغییر (§5.5)."
        val pulsesText = "${pulses}(${hot}+1)"
        return Result(row.ref,current,row.force,pulsesText,row.hold,warnings,"E34.03.180.G §5.3 Table ${when(family){Family.F1->8;Family.F2->9;Family.F2BIS->10}} — Ø8 pulsation")
    }

    private fun finestColumn(r: Double): Int = listOf(0.0,.6,.7,.8,.9,1.0,1.2,1.5,2.0,2.5,3.0).indexOfFirst { kotlin.math.abs(it-r)<1e-6 }.also { if(it<0) error("ضخامت نازک‌ترین ورق پس از §4.2 در ستون‌های جدول pulsation نیست.") }
}
