package com.example.drugcalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Teal = Color(0xFF0F5C5C)
val Paper = Color(0xFFF6F8F7)
val Warn = Color(0xFFB3261E)

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content
    )
}

@Composable
fun Num(label: String, state: MutableState<String>, hint: String? = null) {
    OutlinedTextField(
        value = state.value, onValueChange = { state.value = it },
        label = { Text(label) }, supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Choice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, s ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(s) })
        }
    }
}

@Composable
fun Result(lines: List<Pair<String, String>>, warnings: List<String> = emptyList(), note: String? = null) {
    if (lines.isEmpty() && warnings.isEmpty()) return
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3EFEC))) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, fontSize = 14.sp)
                    Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }
            warnings.forEach { Text("⚠ $it", color = Warn, fontWeight = FontWeight.Medium) }
            note?.let { Text(it, fontSize = 12.sp, color = Color.DarkGray) }
        }
    }
}

// ───────────── صفحه‌ها ─────────────

@Composable
fun DoseScreen() {
    val w = remember { mutableStateOf("") }; val d = remember { mutableStateOf("") }
    val fr = remember { mutableStateOf("") }; val c = remember { mutableStateOf("") }
    val ms = remember { mutableStateOf("") }; val md = remember { mutableStateOf("") }
    Page {
        Num("وزن بیمار (kg)", w); Num("دوز (mg/kg)", d)
        Num("تعداد دفعات در روز", fr)
        Num("غلظت دارو (mg/mL) — اختیاری", c)
        Num("سقف دوز تکی (mg) — اختیاری", ms)
        Num("سقف دوز روزانه (mg) — اختیاری", md)
        val r = if (w.value.num() != null && d.value.num() != null && fr.value.num() != null)
            Calc.weightDose(w.value.num()!!, d.value.num()!!, fr.value.num()!!,
                c.value.num(), ms.value.num(), md.value.num()) else null
        if (r != null) Result(
            buildList {
                add("دوز هر نوبت" to "${r.single.f()} mg")
                add("دوز روزانه" to "${r.daily.f()} mg")
                r.volumeMl?.let { add("حجم هر نوبت" to "${it.f()} mL") }
            }, r.warnings
        )
    }
}

@Composable
fun BodyScreen() {
    val h = remember { mutableStateOf("") }; val w = remember { mutableStateOf("") }
    val sex = remember { mutableStateOf(0) }
    val dose = remember { mutableStateOf("") }
    Page {
        Choice(listOf("مرد", "زن"), sex.value) { sex.value = it }
        Num("قد (cm)", h); Num("وزن (kg)", w)
        Num("دوز بر مترمربع (mg/m²) — اختیاری", dose)
        val hh = h.value.num(); val ww = w.value.num()
        if (hh != null && ww != null && hh > 0 && ww > 0) {
            val bsa = Calc.bsaMosteller(hh, ww)
            val ibw = Calc.ibw(hh, sex.value == 0)
            Result(buildList {
                add("BSA (Mosteller)" to "${bsa.f(2)} m²")
                add("BSA (Du Bois)" to "${Calc.bsaDuBois(hh, ww).f(2)} m²")
                add("BMI" to Calc.bmi(hh, ww).f(1))
                add("وزن ایده‌آل (IBW)" to "${ibw.f(1)} kg")
                add("وزن تعدیل‌شده (AdjBW)" to "${Calc.adjBw(ibw, ww).f(1)} kg")
                dose.value.num()?.let { add("دوز کل" to "${(it * bsa).f()} mg") }
            })
        }
    }
}

@Composable
fun RenalScreen() {
    val age = remember { mutableStateOf("") }; val w = remember { mutableStateOf("") }
    val scr = remember { mutableStateOf("") }; val sex = remember { mutableStateOf(0) }
    Page {
        Choice(listOf("مرد", "زن"), sex.value) { sex.value = it }
        Num("سن (سال)", age); Num("وزن (kg)", w); Num("کراتینین سرم (mg/dL)", scr)
        val a = age.value.num(); val ww = w.value.num(); val s = scr.value.num()
        if (a != null && ww != null && s != null && s > 0) {
            val cl = Calc.crCl(a, ww, s, sex.value == 0)
            val stage = when {
                cl >= 90 -> "طبیعی (≥۹۰)"; cl >= 60 -> "کاهش خفیف (۶۰–۸۹)"
                cl >= 30 -> "کاهش متوسط (۳۰–۵۹)"; cl >= 15 -> "کاهش شدید (۱۵–۲۹)"
                else -> "نارسایی کلیه (<۱۵)"
            }
            Result(listOf("CrCl (Cockcroft-Gault)" to "${cl.f(1)} mL/min", "وضعیت کلیه" to stage),
                note = "برای چاقی شدید از وزن ایده‌آل/تعدیل‌شده استفاده کنید. تنظیم دوز را با مرجع دارویی تطبیق دهید.")
        }
    }
}

@Composable
fun InfusionScreen() {
    val v = remember { mutableStateOf("") }; val t = remember { mutableStateOf("") }
    val df = remember { mutableStateOf("20") }
    Page {
        Num("حجم کل (mL)", v); Num("مدت (ساعت)", t)
        Num("ضریب قطره ست (gtt/mL)", df, "معمولاً ۱۰، ۱۵، ۲۰ یا ۶۰ (میکروست)")
        val vv = v.value.num(); val tt = t.value.num(); val f = df.value.num()
        if (vv != null && tt != null && tt > 0)
            Result(buildList {
                add("سرعت" to "${Calc.mlPerHour(vv, tt).f(1)} mL/h")
                if (f != null) add("قطره در دقیقه" to "${Calc.dropsPerMin(vv, tt * 60, f).f(0)} gtt/min")
            })
    }
}

@Composable
fun TitrationScreen() {
    val mode = remember { mutableStateOf(0) }
    val w = remember { mutableStateOf("") }; val c = remember { mutableStateOf("") }
    val x = remember { mutableStateOf("") }
    Page {
        Choice(listOf("دوز ← سرعت", "سرعت ← دوز"), mode.value) { mode.value = it; x.value = "" }
        Num("وزن (kg)", w)
        Num("غلظت محلول (mcg/mL)", c, "مثلاً ۲۰۰ mg در ۲۵۰ mL = ۸۰۰ mcg/mL")
        if (mode.value == 0) Num("دوز هدف (mcg/kg/min)", x) else Num("سرعت پمپ (mL/h)", x)
        val ww = w.value.num(); val cc = c.value.num(); val xx = x.value.num()
        if (ww != null && cc != null && xx != null && ww > 0 && cc > 0) {
            if (mode.value == 0) Result(listOf("سرعت پمپ" to "${Calc.titrationMlH(xx, ww, cc).f(2)} mL/h"))
            else Result(listOf("دوز" to "${Calc.mcgKgMinFromRate(xx, ww, cc).f(3)} mcg/kg/min"))
        }
    }
}

@Composable
fun DilutionScreen() {
    val c1 = remember { mutableStateOf("") }; val c2 = remember { mutableStateOf("") }
    val v2 = remember { mutableStateOf("") }
    Page {
        Num("غلظت اولیه C1", c1); Num("غلظت نهایی C2 (هم‌واحد)", c2)
        Num("حجم نهایی V2 (mL)", v2)
        val a = c1.value.num(); val b = c2.value.num(); val v = v2.value.num()
        if (a != null && b != null && v != null && a > 0) {
            val s = Calc.dilutionStockVolume(a, b, v)
            Result(listOf("حجم دارو (V1)" to "${s.f(2)} mL", "حجم حلال" to "${(v - s).f(2)} mL"),
                if (b > a) listOf("غلظت نهایی نمی‌تواند از غلظت اولیه بیشتر باشد.") else emptyList())
        }
    }
}

@Composable
fun TabletScreen() {
    val want = remember { mutableStateOf("") }; val tab = remember { mutableStateOf("") }
    val conc = remember { mutableStateOf("") }
    val total = remember { mutableStateOf("") }; val fr = remember { mutableStateOf("") }
    Page {
        Num("دوز مورد نیاز (mg)", want)
        Num("قدرت هر قرص (mg)", tab)
        Num("غلظت شربت/تزریقی (mg/mL)", conc)
        Num("تعداد کل قرص موجود — برای محاسبه مدت مصرف", total)
        Num("دفعات مصرف در روز", fr)
        val d = want.value.num()
        val t = tab.value.num(); val c = conc.value.num()
        val tb = if (d != null && t != null && t > 0) Calc.tablets(d, t) else null
        val ml = if (d != null && c != null && c > 0) Calc.liquidMl(d, c) else null
        val days = if (tb != null && total.value.num() != null && fr.value.num() != null && tb > 0 && fr.value.num()!! > 0)
            Calc.daysSupply(total.value.num()!!, tb, fr.value.num()!!) else null
        Result(buildList {
            tb?.let { add("تعداد قرص" to "${it.f(2)} عدد") }
            ml?.let { add("حجم مایع" to "${it.f(2)} mL") }
            days?.let { add("مدت مصرف" to "${it.f(1)} روز") }
        })
    }
}

@Composable
fun PedsScreen() {
    val adult = remember { mutableStateOf("") }; val age = remember { mutableStateOf("") }
    val w = remember { mutableStateOf("") }
    Page {
        Num("دوز بزرگسال (mg)", adult); Num("سن کودک (سال)", age); Num("وزن کودک (kg)", w)
        val a = adult.value.num()
        Result(buildList {
            val ag = age.value.num(); val ww = w.value.num()
            if (a != null && ag != null) add("قانون Young" to "${Calc.young(a, ag).f(1)} mg")
            if (a != null && ww != null) add("قانون Clark" to "${Calc.clark(a, ww).f(1)} mg")
        }, note = "این فرمول‌ها تقریبی‌اند؛ دوز اطفال را ترجیحاً بر اساس mg/kg و مرجع معتبر تعیین کنید.")
    }
}

@Composable
fun ConcScreen() {
    val pct = remember { mutableStateOf("") }; val ratio = remember { mutableStateOf("") }
    Page {
        Num("درصد (w/v) مثلاً ۰٫۹", pct)
        Num("نسبت ۱:X — مقدار X (مثلاً ۱:۱۰۰۰ → ۱۰۰۰)", ratio)
        Result(buildList {
            pct.value.num()?.let { add("${it.f()}% =" to "${Calc.percentToMgPerMl(it).f(2)} mg/mL") }
            ratio.value.num()?.takeIf { it > 0 }?.let { add("۱:${it.f(0)} =" to "${Calc.ratioToMgPerMl(it).f(3)} mg/mL") }
        })
    }
}

@Composable
fun UnitScreen() {
    val units = Calc.massUnits.keys.toList()
    val v = remember { mutableStateOf("") }
    val from = remember { mutableStateOf(2) }; val to = remember { mutableStateOf(1) }
    val kg = remember { mutableStateOf("") }; val c = remember { mutableStateOf("") }
    Page {
        Text("جرم", fontWeight = FontWeight.Bold)
        Num("مقدار", v)
        Text("از"); Choice(units, from.value) { from.value = it }
        Text("به"); Choice(units, to.value) { to.value = it }
        v.value.num()?.let {
            Result(listOf("نتیجه" to "${Calc.convertMass(it, units[from.value], units[to.value]).f(6)} ${units[to.value]}"))
        }
        HorizontalDivider()
        Num("وزن (kg ← lb)", kg)
        kg.value.num()?.let { Result(listOf("lb" to Calc.kgToLb(it).f(2), "(اگر ورودی lb بود، kg)" to Calc.lbToKg(it).f(2))) }
        Num("دما (°C ← °F)", c)
        c.value.num()?.let { Result(listOf("°F" to Calc.cToF(it).f(1), "(اگر ورودی °F بود، °C)" to Calc.fToC(it).f(1))) }
    }
}
