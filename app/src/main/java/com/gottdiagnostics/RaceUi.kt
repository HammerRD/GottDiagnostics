package com.gottdiagnostics

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal val RaceLime = Color(0xFFD8FC60)
internal val RaceCyan = Color(0xFF76DFEB)
internal val RaceOrange = Color(0xFFFFBB82)
internal val RaceMuted = Color(0xFFA3ADB9)
internal val RacePanel = Color(0xFF171D24)
internal val RaceLine = Color(0xFF303A45)
internal val RaceRed = Color(0xFFFF9390)
private val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

@Composable internal fun RaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = RaceLime, onPrimary = Color(0xFF11170A), secondary = RaceCyan,
            background = Color(0xFF090D12), surface = RacePanel, surfaceVariant = Color(0xFF202933),
            onSurface = Color(0xFFF3F6F8), onBackground = Color(0xFFF3F6F8), onSurfaceVariant = RaceMuted,
            outline = Color(0xFF727E8B), error = RaceRed),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = Condensed, fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
            headlineSmall = TextStyle(fontFamily = Condensed, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontFamily = Condensed, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.sp)
        ),
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable internal fun RaceEyebrow(text: String, color: Color = RaceLime) {
    Text(text, color = color, style = MaterialTheme.typography.labelSmall)
}

@Composable internal fun RaceHeading(kicker: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RaceEyebrow(kicker)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if(subtitle != null) Text(subtitle, color = RaceMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable internal fun RaceBadge(text: String, color: Color = RaceLime) {
    Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(5.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.35f))) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = color,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp))
    }
}

@Composable internal fun RaceHeader(state: State) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(30.dp, 28.dp)) {
            for(i in 0..1) {
                val x = i * size.width * 0.46f
                val stripe = Path().apply { moveTo(x, size.height); lineTo(x + size.width * 0.29f, 0f); lineTo(x + size.width * 0.53f, 0f); lineTo(x + size.width * 0.24f, size.height); close() }
                drawPath(stripe, RaceLime)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("GOTT", style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic, letterSpacing = 2.sp))
            RaceEyebrow("DRIFT / TRACK / DIAGNOSTICS", RaceMuted)
        }
        RaceBadge(if(state.monitoring) "REC" else if(state.connected) "LINKED" else "OFFLINE", if(state.connected) RaceLime else RaceMuted)
    }
}

@Composable internal fun RaceNavigation(tab: Int, select: (Int) -> Unit) {
    Surface(color = Color(0xFF11171E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, RaceLine)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 5.dp)) {
            listOf("Link", "Guides", "Live", "Codes", "Garage", "AI").forEachIndexed { index, label ->
                val active = index == tab
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(active) RaceLime.copy(alpha = 0.12f) else Color.Transparent)
                    .semantics { selected = active }.clickable(role = Role.Tab) { select(index) }.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    NavigationGlyph(index, if(active) RaceLime else RaceMuted)
                    Text(label, fontSize = 11.sp, fontWeight = if(active) FontWeight.Bold else FontWeight.Normal, color = if(active) RaceLime else RaceMuted)
                }
            }
        }
    }
}

@Composable private fun NavigationGlyph(index: Int, color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val u = size.width / 20f
        fun line(x: Float, y: Float, xx: Float, yy: Float) = drawLine(color, Offset(x*u,y*u), Offset(xx*u,yy*u), 1.6f*u, StrokeCap.Round)
        when(index) {
            0 -> { line(8f,3f,13f,7f); line(13f,7f,6f,13f); line(6f,13f,13f,17f); line(13f,17f,13f,7f); line(8f,3f,8f,18f); line(5f,5f,15f,15f) }
            1 -> { line(4f,18f,4f,3f); val flag = Path().apply { moveTo(4*u,3*u); lineTo(17*u,3*u); lineTo(14*u,7*u); lineTo(17*u,11*u); lineTo(4*u,11*u) }; drawPath(flag,color,style=Stroke(1.6f*u)) }
            2 -> { drawArc(color,150f,240f,false,Offset(2*u,2*u),Size(16*u,16*u),style=Stroke(1.6f*u)); line(10f,10f,15f,6f); drawCircle(color,2*u,Offset(10*u,10*u)) }
            3 -> { line(7f,3f,13f,3f); line(10f,3f,10f,6f); val engine = Path().apply { moveTo(5*u,6*u); lineTo(14*u,6*u); lineTo(17*u,9*u); lineTo(17*u,15*u); lineTo(5*u,15*u); lineTo(3*u,12*u); lineTo(3*u,9*u); close() }; drawPath(engine,color,style=Stroke(1.6f*u)); line(7f,10f,12f,10f) }
            4 -> { val garage = Path().apply { moveTo(2*u,8*u); lineTo(10*u,2*u); lineTo(18*u,8*u); lineTo(18*u,18*u); lineTo(2*u,18*u); close() }; drawPath(garage,color,style=Stroke(1.6f*u)); line(6f,10f,14f,10f); line(6f,14f,14f,14f) }
            else -> { val star = Path().apply { moveTo(10*u,1*u); lineTo(12*u,7*u); lineTo(18*u,10*u); lineTo(12*u,12*u); lineTo(10*u,19*u); lineTo(7*u,12*u); lineTo(1*u,10*u); lineTo(7*u,7*u); close() }; drawPath(star,color,style=Stroke(1.6f*u)) }
        }
    }
}

internal fun vehicleAccent(id: String) = when(id) { "350z-2006" -> RaceCyan; "370z-2009" -> RaceOrange; else -> RaceLime }

@Composable internal fun VehicleHero(vehicle: Vehicle, selected: Boolean = true, compact: Boolean = false, onClick: (() -> Unit)? = null, enabled: Boolean = true) {
    val accent = vehicleAccent(vehicle.id)
    val click = if(onClick != null) Modifier.clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected } else Modifier
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).then(click),
        color = RacePanel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if(selected) accent.copy(alpha = 0.7f) else RaceLine)) {
        Box(Modifier.background(Brush.linearGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent)))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    RaceEyebrow(if(compact) "${vehicle.title.take(4)} / NISSAN" else "SELECTED / NISSAN", accent)
                    if(selected) RaceBadge(if(compact) "SELECTED" else "6MT • 93 OCT", accent)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vehicle.title.removePrefix(vehicle.title.take(5)), style = MaterialTheme.typography.headlineLarge.copy(fontSize = if(compact) 30.sp else 40.sp))
                        RaceEyebrow(when(vehicle.id) { "350z-2006" -> "VQ35DE REV-UP"; "370z-2009" -> "VQ37VHR / UPREV"; else -> "VQ35DE / MODIFIED" }, RaceMuted)
                    }
                    if(compact) CoupeArt(accent, Modifier.width(104.dp).height(54.dp))
                }
                if(!compact) {
                    CoupeArt(accent, Modifier.fillMaxWidth().height(92.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RaceEyebrow("${vehicle.title.take(4)} MODEL", RaceMuted)
                        RaceEyebrow("REAR-WHEEL DRIVE", RaceMuted)
                    }
                }
            }
        }
    }
}

/** Stylized coupe illustration, not a photograph or a claim about a car's body modifications. */
@Composable private fun CoupeArt(accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        val x = size.width / 320f; val y = size.height / 100f
        for(i in 0..5) drawLine(accent.copy(alpha=0.08f), Offset((i*65f-60f)*x,100*y),Offset((i*65f+30f)*x,0f), x)
        drawLine(accent.copy(alpha=0.25f), Offset(6*x,84*y),Offset(314*x,84*y),x)
        val body = Path().apply {
            moveTo(13*x,68*y); lineTo(19*x,48*y); lineTo(54*x,44*y); lineTo(103*x,18*y)
            cubicTo(116*x,10*y,154*x,10*y,169*x,16*y); lineTo(211*x,43*y); lineTo(275*x,51*y)
            lineTo(299*x,61*y); lineTo(307*x,73*y); lineTo(292*x,77*y); lineTo(25*x,77*y); close()
        }
        drawPath(body, Color(0xFF273540)); drawPath(body, accent, style=Stroke(1.5f*x))
        val glass = Path().apply { moveTo(74*x,43*y); lineTo(109*x,23*y); lineTo(155*x,22*y); lineTo(190*x,43*y); close() }
        drawPath(glass, Color(0xFF0B1118)); drawPath(glass, accent.copy(alpha=.4f), style=Stroke(x))
        drawLine(accent.copy(alpha=.5f),Offset(135*x,24*y),Offset(145*x,43*y),x)
        drawLine(accent.copy(alpha=.6f),Offset(102*x,59*y),Offset(212*x,59*y),x)
        drawLine(Color(0xFFF1F5D7),Offset(278*x,54*y),Offset(296*x,60*y),2*x)
        drawLine(RaceRed,Offset(19*x,52*y),Offset(29*x,52*y),2*x)
        for(cx in listOf(66f,251f)) {
            drawCircle(Color(0xFF090D12),17*x,Offset(cx*x,73*y))
            drawCircle(Color(0xFF768A9B),11*x,Offset(cx*x,73*y),style=Stroke(2*x))
            drawCircle(accent,3*x,Offset(cx*x,73*y))
        }
    }
}

@Composable internal fun RacePanel(title: String, text: String, accent: Color = RaceCyan) {
    Surface(color = accent.copy(alpha = 0.06f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            RaceEyebrow(title, accent)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable internal fun GuideOption(guide: Guide, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected },
        color = if(selected) RaceLime.copy(alpha=.08f) else RacePanel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if(selected) RaceLime else RaceLine)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("0${guide.ordinal+1}", fontFamily = FontFamily.Monospace, fontSize = 23.sp, color = if(selected) RaceLime else RaceMuted)
            Column(Modifier.weight(1f)) { Text(guide.title, style=MaterialTheme.typography.titleMedium); RaceEyebrow(if(guide == Guide.CRUISE) "NORMAL ROAD LOAD" else "STATIONARY / NEUTRAL", RaceMuted) }
            Text(if(selected) "●" else "○", color = if(selected) RaceLime else RaceMuted)
        }
    }
}

@Composable internal fun Tachometer(state: State) {
    val rpm = state.values["0C"]
    Surface(shape=RoundedCornerShape(18.dp), color=RacePanel, border=BorderStroke(1.dp,RaceLine)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RaceEyebrow("ENGINE SPEED", RaceMuted)
                RaceBadge(if(rpm == null) "NO DATA" else if(state.monitoring) "RECORDING" else "LAST READING", if(state.monitoring) RaceLime else RaceMuted)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(rpm?.let { String.format(Locale.US,"%.0f",it) } ?: "—", style=MaterialTheme.typography.headlineLarge.copy(fontSize=64.sp,lineHeight=68.sp), color=if(rpm != null) RaceLime else RaceMuted)
                Text("RPM", Modifier.padding(bottom=10.dp), color=RaceMuted, fontFamily=FontFamily.Monospace)
            }
            Canvas(Modifier.fillMaxWidth().height(25.dp)) {
                val fraction = ((rpm ?: 0.0)/9000).coerceIn(0.0,1.0)
                val step = size.width / 36f
                for(i in 0..35) {
                    val bar = Path().apply { moveTo(i*step,size.height); lineTo(i*step+step*.3f,0f); lineTo((i+1)*step-step*.2f,0f); lineTo((i+1)*step-step*.5f,size.height); close() }
                    drawPath(bar,if(rpm != null && i < fraction*36) RaceLime else RaceLine)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { listOf("0","3","6","9 ×1000").forEach { RaceEyebrow(it,RaceMuted) } }
            Text("Display scale only — not an engine limit or RPM target.", style=MaterialTheme.typography.bodySmall,color=RaceMuted)
        }
    }
}

@Composable internal fun MetricTile(pid: Pid, value: Double?, modifier: Modifier = Modifier) {
    val accent = when(pid.code) { "05" -> if(value != null && value > 110) RaceRed else RaceOrange; "42" -> RaceLime; else -> RaceCyan }
    Surface(modifier=modifier,shape=RoundedCornerShape(12.dp),color=RacePanel,border=BorderStroke(1.dp,RaceLine)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            RaceEyebrow(pid.name.uppercase(Locale.US),RaceMuted)
            Text(value?.let { String.format(Locale.US, if(pid.code == "0D") "%.0f" else "%.1f",it) } ?: "—",
                style=MaterialTheme.typography.headlineLarge.copy(fontSize=32.sp,lineHeight=36.sp),color=if(value == null) RaceMuted else accent)
            Text(pid.unit,style=MaterialTheme.typography.labelSmall,color=accent)
        }
    }
}
