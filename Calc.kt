package com.example.drugcalc

import java.util.Locale
import kotlin.math.sqrt

/** تبدیل ارقام فارسی/عربی به انگلیسی و تبدیل به عدد */
fun String.num(): Double? = this.map {
    when (it) {
        in '۰'..'۹' -> '0' + (it - '۰')
        in '٠'..'٩' -> '0' + (it - '٠')
        '٫', '،', ',' -> '.'
        else -> it
    }
}.joinToString("").trim().toDoubleOrNull()

fun Double.f(n: Int = 2): String =
    String.format(Locale.US, "%.${n}f", this).trimEnd('0').trimEnd('.')

object Calc {
    // ---- دوز بر اساس وزن ----
    data class DoseResult(
        val single: Double, val daily: Double, val volumeMl: Double?,
        val warnings: List<String>
    )

    fun weightDose(
        weight: Double, mgPerKg: Double, freq: Double,
        concMgPerMl: Double?, maxSingle: Double?, maxDaily: Double?
    ): DoseResult {
        val w = mutableListOf<String>()
        var single = weight * mgPerKg
        if (maxSingle != null && single > maxSingle) {
            single = maxSingle; w += "دوز تکی به سقف مجاز محدود شد."
        }
        var daily = single * freq
        if (maxDaily != null && daily > maxDaily) {
            w += "دوز روزانه از سقف مجاز (${maxDaily.f()} mg) بیشتر است!"
        }
        return DoseResult(single, daily, concMgPerMl?.takeIf { it > 0 }?.let { single / it }, w)
    }

    // ---- BSA / BMI / IBW ----
    fun bsaMosteller(hCm: Double, wKg: Double) = sqrt(hCm * wKg / 3600.0)
    fun bsaDuBois(hCm: Double, wKg: Double) = 0.007184 * Math.pow(wKg, 0.425) * Math.pow(hCm, 0.725)
    fun bmi(hCm: Double, wKg: Double) = wKg / ((hCm / 100) * (hCm / 100))
    fun ibw(hCm: Double, male: Boolean): Double {
        val inchesOver60 = (hCm / 2.54 - 60).coerceAtLeast(0.0)
        return (if (male) 50.0 else 45.5) + 2.3 * inchesOver60
    }
    fun adjBw(ibw: Double, actual: Double) = ibw + 0.4 * (actual - ibw)

    // ---- کلیرانس کراتینین (Cockcroft-Gault) ----
    fun crCl(age: Double, weight: Double, scr: Double, male: Boolean) =
        ((140 - age) * weight) / (72 * scr) * (if (male) 1.0 else 0.85)

    // ---- انفوزیون ----
    fun mlPerHour(volumeMl: Double, hours: Double) = volumeMl / hours
    fun dropsPerMin(volumeMl: Double, minutes: Double, dropFactor: Double) = volumeMl * dropFactor / minutes

    /** mcg/kg/min -> mL/h */
    fun titrationMlH(mcgKgMin: Double, weight: Double, concMcgPerMl: Double) =
        mcgKgMin * weight * 60 / concMcgPerMl

    /** mL/h -> mcg/kg/min */
    fun mcgKgMinFromRate(mlH: Double, weight: Double, concMcgPerMl: Double) =
        mlH * concMcgPerMl / (weight * 60)

    // ---- رقیق‌سازی C1V1 = C2V2 ----
    fun dilutionStockVolume(c1: Double, c2: Double, v2: Double) = c2 * v2 / c1

    // ---- قرص / شربت ----
    fun tablets(desiredMg: Double, tabMg: Double) = desiredMg / tabMg
    fun liquidMl(desiredMg: Double, mgPerMl: Double) = desiredMg / mgPerMl
    fun daysSupply(totalUnits: Double, unitsPerDose: Double, freq: Double) = totalUnits / (unitsPerDose * freq)

    // ---- اطفال ----
    fun young(adultDose: Double, ageYears: Double) = adultDose * ageYears / (ageYears + 12)
    fun clark(adultDose: Double, weightKg: Double) = adultDose * (weightKg * 2.2046) / 150.0

    // ---- غلظت ----
    fun percentToMgPerMl(percent: Double) = percent * 10
    fun ratioToMgPerMl(ratio: Double) = 1000.0 / ratio   // 1:ratio  (w/v)

    // ---- تبدیل واحد ----
    val massUnits = mapOf("kg" to 1e6, "g" to 1e3, "mg" to 1.0, "mcg" to 1e-3, "ng" to 1e-6) // نسبت به mg
    fun convertMass(v: Double, from: String, to: String) = v * massUnits[from]!! / massUnits[to]!!
    fun kgToLb(kg: Double) = kg * 2.20462
    fun lbToKg(lb: Double) = lb / 2.20462
    fun cToF(c: Double) = c * 9 / 5 + 32
    fun fToC(f: Double) = (f - 32) * 5 / 9
}
