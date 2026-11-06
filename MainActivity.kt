package com.example.drugcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Teal, background = Paper, surface = Paper)) {
                    App()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val tabs = listOf<Pair<String, @Composable () -> Unit>>(
        "دوز بر وزن" to { DoseScreen() },
        "BSA / BMI" to { BodyScreen() },
        "کلیرانس کراتینین" to { RenalScreen() },
        "انفوزیون" to { InfusionScreen() },
        "تیتراسیون" to { TitrationScreen() },
        "رقیق‌سازی" to { DilutionScreen() },
        "قرص / شربت" to { TabletScreen() },
        "اطفال" to { PedsScreen() },
        "غلظت" to { ConcScreen() },
        "تبدیل واحد" to { UnitScreen() },
    )
    var sel by remember { mutableStateOf(0) }
    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("محاسبات دارویی") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Teal, titleContentColor = androidx.compose.ui.graphics.Color.White)
                )
                ScrollableTabRow(selectedTabIndex = sel, edgePadding = 8.dp, containerColor = Paper) {
                    tabs.forEachIndexed { i, t -> Tab(selected = sel == i, onClick = { sel = i }, text = { Text(t.first, fontSize = 13.sp) }) }
                }
            }
        },
        bottomBar = {
            Text("ابزار کمکی است؛ نتایج را قبل از استفاده بالینی با منابع معتبر و داروساز/پزشک تطبیق دهید.",
                fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
    ) { pad -> Box(Modifier.padding(pad)) { tabs[sel].second() } }
}
