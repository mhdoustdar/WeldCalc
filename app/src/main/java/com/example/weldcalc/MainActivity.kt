package com.example.weldcalc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LocalLayoutDirection
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.weldcalc.data.StandardData
import com.example.weldcalc.engine.WeldingCalculator
import com.example.weldcalc.model.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WeldCalcApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeldCalcApp() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("WeldCalc Industrial") }, actions = {
            IconButton(onClick = { tab = 3 }) { Icon(Icons.Default.Info, "استاندارد") }
        }) }, bottomBar = {
            NavigationBar { listOf("محاسبه","پیچ/مهره","برآمدگی","استاندارد").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab==i, onClick={tab=i}, icon={Icon(if(i==0) Icons.Default.Calculate else if(i==3) Icons.Default.Info else Icons.Default.History,label)}, label={Text(label)})
            } }
        }) { pad ->
            CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when(tab) { 0 -> SpotScreen(); 1 -> FastenerScreen(); 2 -> EmbossScreen(); else -> StandardScreen() }
                }
            }
        }
    }
}

@Composable
fun SpotScreen() {
    val calc = remember { WeldingCalculator() }
    var sheetCount by remember { mutableIntStateOf(2) }
    var t1 by remember { mutableStateOf("0.8") }
    var t2 by remember { mutableStateOf("0.8") }
    var t3 by remember { mutableStateOf("0.8") }
    var t4 by remember { mutableStateOf("0.8") }
    var family by remember { mutableStateOf(Family.F1) }
    var process by remember { mutableStateOf(Process.NORMAL_D6) }
    var coated by remember { mutableIntStateOf(0) }
    var hz by remember { mutableIntStateOf(50) }
    var over10 by remember { mutableStateOf(false) }
    var counter by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("محاسبه پارامتر جوش مقاومتی", style=MaterialTheme.typography.headlineSmall)
        Text("مبنای محاسبات: PSA PEUGEOT-CITROËN E34.03.180.G", style=MaterialTheme.typography.bodyMedium)
        SegmentedRow(listOf("۲ ورق","۳ ورق","۴ ورق"), sheetCount-2) { sheetCount=it+2; result=null }
        SheetField("ورق ۱ (mm)",t1){t1=it}
        SheetField("ورق ۲ (mm)",t2){t2=it}
        if(sheetCount>=3) SheetField("ورق ۳ (mm)",t3){t3=it}
        if(sheetCount==4) {
            Text("ورق چهارم برای بررسی مونتاژ ۴ ضخامت",style=MaterialTheme.typography.labelLarge)
            SheetField("ورق ۴ (mm)",t4){t4=it}
            Text("استاندارد §5.4 پارامتر عددی مستقلی برای ۴ ضخامت ارائه نمی‌کند؛ مرجع E = نازک‌ترینِ دو ورق ضخیم‌تر است و feasibility باید تأیید شود.",style=MaterialTheme.typography.bodySmall)
        }
        EnumSelector("خانواده پارامتر", Family.values().toList(), family, {it.label}) { family=it }
        EnumSelector("فرآیند", Process.values().toList(), process, {it.label}) { process=it }
        EnumSelector("فرکانس", listOf(50,60), hz, {"$it Hz"}) { hz=it }
        IntSelector("تعداد سطوح پوشش‌دار ≤10µm", (0..6).toList(), coated) { coated=it }
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Checkbox(over10,{over10=it}); Text("پوشش > 10 µm")
        }
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Checkbox(counter,{counter=it}); Text("جوش روی counter electrode")
        }
        Button(onClick={
            error=null
            try {
                if (sheetCount == 4) {
                    val ts = listOf(parseDecimal(t1),parseDecimal(t2),parseDecimal(t3),parseDecimal(t4))
                    val e = ts.sortedDescending().take(2).minOrNull()!!
                    error = "§5.4: برای ۴ ضخامت پارامتر عددی جدول‌بندی نشده است. ضخامت مرجع طبق استاندارد = ${fmt(StandardData.retain(e) ?: e)} mm؛ feasibility و پارامتر باید با Responsible welding methods تأیید شود."
                    result=null
                } else {
                    val ts = listOf(t1,t2).let { if(sheetCount==3) it+ t3 else it }.map { parseDecimal(it) }
                    result=calc.calculate(ts.map { Sheet(it, family) },family,process,coated,hz,over10,counter)
                }
            } catch(e: Exception) { result=null; error=e.message ?: "خطای نامشخص" }
        }, modifier=Modifier.fillMaxWidth()) { Text("محاسبه پارامتر") }
        error?.let { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) { Text(it,Modifier.padding(16.dp)) } }
        result?.let { ResultCard(it) }
        if(sheetCount==2 && process==Process.NORMAL_D5) Text("نکته: Table 4 برای Ø5 فقط تعداد پوشش ۲/۴/۶ را جدول‌بندی کرده است.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable fun ResultCard(r: Result) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text("نتیجه",style=MaterialTheme.typography.titleLarge)
        Param("ضخامت مرجع E", "${fmt(r.reference)} mm")
        Param("جریان", "${fmt(r.currentKa)} kA")
        Param("نیرو", "${r.forceDaN} daN")
        Param("زمان جوش", r.weldTime)
        Param("Holding", "${r.holdTime} cycles")
        Text("منبع: ${r.source}",style=MaterialTheme.typography.bodySmall)
        r.warnings.forEach { Text("⚠ $it",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={}) { Text("ثبت در تاریخچه") }; OutlinedButton(onClick={}) { Icon(Icons.Default.Share,"اشتراک"); Text(" اشتراک") } }
    }}
}
@Composable fun Param(a:String,b:String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b,style=MaterialTheme.typography.titleMedium)}}

@Composable fun FastenerScreen(){
    var screw by remember { mutableStateOf(StandardData.screws.first()) }
    var annular by remember { mutableStateOf(false) }
    var nut by remember { mutableStateOf(StandardData.nuts.first()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("پیچ و مهره — §5.7",style=MaterialTheme.typography.headlineSmall)
        Text("پیچ STNG 9691205699",style=MaterialTheme.typography.titleMedium)
        EnumSelector("قطر پیچ / بازه",StandardData.screws,screw,{"Ø${it.diameter} — ${it.range} mm"}){screw=it}
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(annular,{annular=it});Text("برآمدگی حلقوی (Annular)")}
        val sf=if(annular)screw.annForce else screw.threeForce; val st=if(annular)screw.annTime else screw.threeTime; val si=if(annular)screw.annCurrent else screw.threeCurrent; val sh=if(annular)screw.annHold else screw.threeHold
        Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("پارامتر",style=MaterialTheme.typography.titleLarge);Param("Force","$sf daN");Param("Current","${fmt(si)} kA");Param("Weld time","$st cycles");Param("Hold","$sh cycles")}}
        Text("مهره استاندارد",style=MaterialTheme.typography.titleMedium)
        EnumSelector("سایز مهره",StandardData.nuts,nut,{it.name}){nut=it}
        Card{Column(Modifier.padding(16.dp)){Param("Force","${nut.force} daN");Param("Current","${nut.currentA} A");Param("Weld time","${nut.time} cycles");Param("Hold","${nut.hold} cycles")}}
        Text("برای 22MnB5، HR و DP≥780 استاندارد درخواست مشاوره سرویس مرجع/Assembly Technical را ذکر می‌کند.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable fun EmbossScreen(){
    var e by remember { mutableStateOf(StandardData.emboss[0]) }
    var coated by remember { mutableStateOf(true) }
    var multiplier by remember { mutableStateOf(1.0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Welding of embossment — §5.7.4",style=MaterialTheme.typography.headlineSmall)
        EnumSelector("تعداد embossment",StandardData.emboss,e,{"${it.count} عدد"}){e=it}
        EnumSelector("ضریب تعداد",listOf(1.0,1.5,2.0,3.0,4.0),multiplier,{"×${it}"}){multiplier=it}
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(coated,{coated=it});Text("ورق پوشش‌دار")}
        Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Param("Force","${(e.force*multiplier).toInt()} daN");Param("Current","${e.currentA(coated,multiplier)} A");Param("Weld time","${e.time} cycles");Param("Hold","${e.hold} cycles")}}
        Text("برای embossment مثلثی: Force = 20 daN/mm، Current = 750 A/mm و Time = 8–10 cycles؛ این بخش برآورد مرحله مطالعه است.",style=MaterialTheme.typography.bodySmall)
    }
}
private fun StandardData.Emboss.currentA(coated:Boolean,m:Double)=((if(coated)coatedA else bareA)*m).toInt()

@Composable fun StandardScreen(){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("مبنای مهندسی و محدودیت‌ها",style=MaterialTheme.typography.headlineSmall)
        Text("E34.03.180.G — نسخه فایل: OR 01/03/2001 / E 10/02/2015",style=MaterialTheme.typography.bodyMedium)
        Bullet("§4.1: برای ۲ ورق E از نازک‌ترین ورق و برای ۳ ورق E از میانگین ورق‌ها تعیین می‌شود؛ برای برخی آرایش‌های counter-electrode محدودیت Emax≤1.3 mm وجود دارد.")
        Bullet("§4.2: ضخامت واقعی به بازه‌های محاسباتی 0.6 تا 3.0 mm نگاشت می‌شود.")
        Bullet("§5: مقادیر جریان/نیرو/زمان توصیه‌های طراحی تجهیزات هستند، نه مقادیر مطلق تولید.")
        Bullet("§5.5: در 60 Hz زمان جوش 20% افزایش و جریان بدون تغییر است.")
        Bullet("§5.6: برای اسمبلی دارای حداقل یک ورق F2/F2bis برنامه جوش اختصاصی لازم است.")
        Bullet("§5.7: پیچ، مهره و embossment جداول مستقل دارند.")
        Bullet("۶ پوشش ممنوع است؛ آرایش‌های خارج از محدوده نیازمند تست weldability/تأیید مرجع مربوطه هستند.")
        HorizontalDivider()
        Text("نسخه نرم‌افزار: 1.0.0-industrial",style=MaterialTheme.typography.labelLarge)
        Text("این اپ عمداً نتیجه خارج از جدول را حدس نمی‌زند؛ در موارد فاقد پارامتر، خطا/هشدار مهندسی می‌دهد.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable fun Bullet(s:String){Text("• $s",style=MaterialTheme.typography.bodyMedium)}

@Composable fun SheetField(label:String,value:String,onChange:(String)->Unit){OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())}
@Composable fun <T> EnumSelector(label:String,items:List<T>,selected:T,text:(T)->String,onSelected:(T)->Unit){var open by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()){Text("$label: ${text(selected)}")};DropdownMenu(open,{open=false}){items.forEach{item->DropdownMenuItem(text={Text(text(item))},onClick={onSelected(item);open=false})}}}}
@Composable fun IntSelector(label:String,items:List<Int>,selected:Int,onSelected:(Int)->Unit){EnumSelector(label,items,selected,{it.toString()},onSelected)}
@Composable fun SegmentedRow(items:List<String>,selected:Int,onSelect:(Int)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){items.forEachIndexed{i,s->if(i==selected)Button(onClick={onSelect(i)},Modifier.weight(1f)){Text(s)}else OutlinedButton(onClick={onSelect(i)},Modifier.weight(1f)){Text(s)}}}}
private fun parseDecimal(s:String):Double=s.trim().replace(',','.').replace('٫','.').replace(Regex("[۰-۹]")){(it.value[0].code-1776).toString()}.toDouble()
private fun fmt(v:Double)=String.format(Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
