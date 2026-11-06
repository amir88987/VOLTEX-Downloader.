package app.videogir

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import okhttp3.Request as OkRequest

// ───────────── رنگ‌ها: سرمه‌ای تیره و آبی ─────────────
private val Bg = Color(0xFF030814)
private val Surface1 = Color(0xFF0A1330)
private val Surface2 = Color(0xFF111C42)
private val Blue = Color(0xFF2F7BFF)
private val BlueDeep = Color(0xFF1A3FD8)
private val Cyan = Color(0xFF5CE1FF)
private val TextMain = Color(0xFFF2F6FF)
private val TextDim = Color(0xFF93A3D1)
private val Line = Color(0xFF1E2D63)
private val Ok = Color(0xFF4ADE80)
private val Err = Color(0xFFFF8A9A)
private val Bad = Color(0xFFFF4D63)

private val BlueGradient get() = Brush.horizontalGradient(listOf(Blue, BlueDeep))
private val DisabledGradient get() = Brush.horizontalGradient(listOf(Surface2, Surface2))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue, onPrimary = Color.White,
            background = Bg, onBackground = TextMain,
            surface = Surface1, onSurface = TextMain,
            surfaceVariant = Surface2, onSurfaceVariant = TextDim,
            error = Err, outline = Line
        ),
        content = content
    )
}

// ───────────── کمکی‌ها ─────────────
private fun String.fa(): String =
    map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

private fun fmtSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / 1048576.0
    val s = if (mb >= 1024) String.format(Locale.US, "%.1f گیگابایت", mb / 1024)
    else String.format(Locale.US, "%.1f مگابایت", mb)
    return s.replace('.', '٫').fa()
}

private fun fmtDuration(sec: Long): String {
    if (sec <= 0) return ""
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    val t = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
    return t.fa()
}

/** بخشی از زمان کل (a تا b) را به عدد ۰ تا ۱ تبدیل می‌کند. */
private fun seg(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)

private fun backOut(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = x - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

private fun outCubic(x: Float): Float {
    val u = 1f - x
    return 1f - u * u * u
}

private class Particle(val x: Float, val y: Float, val r: Float, val speed: Float, val phase: Float)

// ───────────── ریشه برنامه ─────────────
@Composable
fun VoltexApp(vm: MainVm, onDownload: (VideoMeta, Option) -> Unit, showSplashFirst: Boolean) {
    AppTheme {
        var splash by rememberSaveable { mutableStateOf(showSplashFirst) }
        Box(Modifier.fillMaxSize().background(Bg)) {
            Root(vm, onDownload)
            if (splash) SplashScreen { splash = false }
        }
    }
}

// ───────────── انیمیشن شروع ─────────────
@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val letters = "VOLTEX"
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(3600, easing = LinearEasing))
        onFinished()
    }
    val inf = rememberInfiniteTransition(label = "splash")
    val spin by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "spin"
    )
    val drift by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "drift"
    )
    val particles = remember {
        List(48) {
            Particle(
                Random.nextFloat(), Random.nextFloat(),
                1f + Random.nextFloat() * 2.2f, 0.25f + Random.nextFloat() * 0.75f,
                Random.nextFloat() * 6.28f
            )
        }
    }

    val tv = t.value
    val bgIn = seg(tv, 0f, 0.12f)
    val logoP = seg(tv, 0.05f, 0.36f)
    val logoScale = 0.15f + 0.85f * backOut(logoP)
    val logoAlpha = seg(tv, 0.05f, 0.16f)
    val arcAlpha = seg(tv, 0.18f, 0.32f)
    val flashX = seg(tv, 0.30f, 0.42f)
    val flash = 1f - abs(flashX * 2f - 1f)
    val lineP = seg(tv, 0.64f, 0.80f)
    val tagP = seg(tv, 0.80f, 0.90f)
    val sweep = seg(tv, 0.64f, 0.86f) * (letters.length + 2f) - 1f
    val exitP = seg(tv, 0.92f, 1f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF020611))
                .graphicsLayer {
                    alpha = 1f - exitP
                    scaleX = 1f + 0.10f * exitP
                    scaleY = 1f + 0.10f * exitP
                }
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            // پس‌زمینه: شبکه، هاله و ذرات
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.radialGradient(
                        listOf(BlueDeep.copy(alpha = 0.55f * bgIn), Color.Transparent),
                        center = Offset(w / 2f, h * 0.42f), radius = w * 1.1f
                    )
                )
                val step = 38.dp.toPx()
                var x = 0f
                while (x < w) {
                    drawLine(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Blue.copy(alpha = 0.16f * bgIn), Color.Transparent)
                        ),
                        Offset(x, 0f), Offset(x, h), 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < h) {
                    val edge = 1f - abs(y / h - 0.5f) * 2f
                    drawLine(
                        Blue.copy(alpha = 0.14f * bgIn * edge),
                        Offset(0f, y), Offset(w, y), 1f
                    )
                    y += step
                }
                for (p in particles) {
                    val py = ((p.y - drift * p.speed) % 1f + 1f) % 1f
                    val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(drift * 2f * PI.toFloat() * 2f + p.phase))
                    drawCircle(
                        Cyan.copy(alpha = 0.55f * tw * bgIn),
                        radius = p.r.dp.toPx() * 0.7f,
                        center = Offset(p.x * w, py * h)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // لوگو با حلقه‌های انرژی
                Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(176.dp)) {
                        val c = center
                        val base = size.minDimension / 2f
                        // موج‌های انفجاری
                        for (k in 0..2) {
                            val p = seg(tv, 0.10f + 0.07f * k, 0.50f + 0.07f * k)
                            if (p > 0f && p < 1f) {
                                drawCircle(
                                    Cyan.copy(alpha = (1f - p) * 0.55f),
                                    radius = base * (0.9f + 3.2f * outCubic(p)),
                                    center = c, style = Stroke(2.dp.toPx())
                                )
                            }
                        }
                        // هاله
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Blue.copy(alpha = 0.55f * logoAlpha), Color.Transparent),
                                center = c, radius = base * 2.2f
                            ),
                            radius = base * 2.2f, center = c
                        )
                        // حلقه چرخان
                        rotate(spin, c) {
                            drawArc(
                                Brush.sweepGradient(listOf(Color.Transparent, Cyan, Color.White), center = c),
                                startAngle = 0f, sweepAngle = 290f, useCenter = false,
                                topLeft = Offset(c.x - base * 1.04f, c.y - base * 1.04f),
                                size = Size(base * 2.08f, base * 2.08f),
                                alpha = arcAlpha,
                                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        rotate(-spin * 1.6f, c) {
                            drawArc(
                                Brush.sweepGradient(listOf(Color.Transparent, Blue), center = c),
                                startAngle = 0f, sweepAngle = 200f, useCenter = false,
                                topLeft = Offset(c.x - base * 1.2f, c.y - base * 1.2f),
                                size = Size(base * 2.4f, base * 2.4f),
                                alpha = arcAlpha * 0.9f,
                                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Image(
                        painterResource(R.drawable.voltex_logo), null,
                        Modifier.size(150.dp).graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                            alpha = logoAlpha
                        }
                    )
                    // فلاش لحظه فرود
                    Canvas(Modifier.size(176.dp)) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.55f * flash), Color.Transparent),
                                center = center, radius = size.minDimension * 0.75f
                            ),
                            radius = size.minDimension * 0.75f
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
                Row {
                    letters.forEachIndexed { i, ch ->
                        val a = outCubic(seg(tv, 0.38f + 0.045f * i, 0.54f + 0.045f * i))
                        val glow = (1f - abs(sweep - i) / 1.6f).coerceIn(0f, 1f)
                        Text(
                            ch.toString(),
                            style = TextStyle(
                                color = lerp(Color.White, Cyan, glow),
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 3.sp,
                                shadow = Shadow(Blue, Offset.Zero, 6f + 34f * glow)
                            ),
                            modifier = Modifier.graphicsLayer {
                                alpha = a
                                translationY = (1f - a) * (if (i % 2 == 0) -70f else 70f)
                                scaleX = 0.5f + 0.5f * a
                                scaleY = 0.5f + 0.5f * a
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .height(3.dp)
                        .width((190f * lineP).dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan, Blue, Color.Transparent)))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "دانلودر ویدیو یوتیوب",
                    color = TextDim, fontSize = 14.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = tagP
                        translationY = (1f - tagP) * 16f
                    }
                )
            }
        }
    }
}

// ───────────── پس‌زمینه زنده ─────────────
@Composable
private fun AuroraBackground() {
    val inf = rememberInfiniteTransition(label = "aurora")
    val a by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Bg)
        drawRect(
            Brush.radialGradient(
                listOf(Blue.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(w * (0.95f - 0.55f * a), h * (0.02f + 0.10f * b)),
                radius = w * 1.05f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(BlueDeep.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * (0.05f + 0.5f * b), h * (0.98f - 0.12f * a)),
                radius = w * 0.95f
            )
        )
        // شبکه محو در بالای صفحه
        val step = 36.dp.toPx()
        val limit = h * 0.45f
        var x = 0f
        while (x < w) {
            drawLine(
                Brush.verticalGradient(listOf(Blue.copy(alpha = 0.10f), Color.Transparent), 0f, limit),
                Offset(x, 0f), Offset(x, limit), 1f
            )
            x += step
        }
        var y = 0f
        while (y < limit) {
            drawLine(Blue.copy(alpha = 0.10f * (1f - y / limit)), Offset(0f, y), Offset(w, y), 1f)
            y += step
        }
    }
}

// ───────────── صفحه اصلی ─────────────
@Composable
private fun Root(vm: MainVm, onDownload: (VideoMeta, Option) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize()) {
            AuroraBackground()
            Scaffold(containerColor = Color.Transparent) { pad ->
                val dl by DownloadHub.state.collectAsStateWithLifecycle()
                val busy = dl is Dl.Running

                Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Header() }
                        item { InputCard(vm) }
                        when (val s = vm.ui) {
                            Ui.Idle -> item { HintCard() }
                            Ui.Loading -> item { RadarLoader() }
                            is Ui.Error -> item { ErrorCard(s.message) }
                            is Ui.Ready -> {
                                item { Appear(0) { MetaCard(s.data.meta) } }
                                itemsIndexed(s.data.options) { index, opt ->
                                    Appear(index + 1) {
                                        OptionRow(opt, enabled = !busy) { onDownload(s.data.meta, opt) }
                                    }
                                }
                            }
                        }
                    }
                    DownloadBar(dl)
                }
            }
        }
    }
}

@Composable
private fun Appear(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 8) * 70L)
        a.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Box(Modifier.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 48f
        scaleX = 0.96f + 0.04f * a.value
        scaleY = 0.96f + 0.04f * a.value
    }) { content() }
}

@Composable
private fun Header() {
    val inf = rememberInfiniteTransition(label = "header")
    val spin by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "spin"
    )
    val shimmer by inf.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "shimmer"
    )
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val c = center
                val r = size.minDimension / 2f - 2.dp.toPx()
                drawCircle(Blue.copy(alpha = 0.18f), radius = r, center = c, style = Stroke(2.dp.toPx()))
                rotate(spin, c) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, Cyan), center = c),
                        startAngle = 0f, sweepAngle = 240f, useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2f, r * 2f),
                        style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Image(
                painterResource(R.drawable.voltex_logo), null,
                Modifier.size(48.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "VOLTEX",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Cyan, Blue, Color.White),
                        start = Offset(shimmer * 260f, 0f),
                        end = Offset(shimmer * 260f + 260f, 60f)
                    ),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp
                )
            )
            Text("دانلودر ویدیو یوتیوب", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GradientButton(
    text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    val inf = rememberInfiniteTransition(label = "shine")
    val ph by inf.animateFloat(
        initialValue = -0.4f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ph"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) BlueGradient else DisabledGradient)
            .drawWithContent {
                drawContent()
                if (enabled) {
                    val w = size.width
                    val x = ph * w
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                            start = Offset(x - w * 0.22f, 0f),
                            end = Offset(x + w * 0.04f, size.height)
                        )
                    )
                }
            }
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else TextDim, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InputCard(vm: MainVm) {
    val clipboard = LocalClipboardManager.current
    val loading = vm.ui == Ui.Loading
    val inf = rememberInfiniteTransition(label = "inputBorder")
    val ph by inf.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(if (loading) 1100 else 3200, easing = LinearEasing)),
        label = "ph"
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(Line, if (loading) Cyan else Blue, Line),
        start = Offset(ph * 700f, 0f),
        end = Offset(ph * 700f + 700f, 360f)
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1)
            .border(1.5f.dp, borderBrush, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = vm.input,
            onValueChange = { vm.input = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://youtu.be/…", color = TextDim) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.load() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue,
                unfocusedBorderColor = Line,
                cursorColor = Cyan,
                focusedContainerColor = Surface2,
                unfocusedContainerColor = Surface2
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Blue.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable {
                        clipboard.getText()?.text?.let {
                            vm.input = Extractor.findUrl(it) ?: it
                            vm.load()
                        }
                    }
                    .padding(vertical = 14.dp, horizontal = 18.dp)
            ) { Text("چسباندن", color = Cyan, fontWeight = FontWeight.Bold) }
            GradientButton(
                "دریافت کیفیت‌ها",
                enabled = vm.input.isNotBlank() && vm.ui != Ui.Loading,
                modifier = Modifier.weight(1f)
            ) { vm.load() }
        }
    }
}

@Composable
private fun HintCard() {
    val steps = listOf(
        "لینک ویدیو را بچسبان و «دریافت کیفیت‌ها» را بزن.",
        "کیفیت دلخواه را انتخاب کن.",
        "فایل در پوشه Movies/VOLTEX ذخیره می‌شود."
    )
    val inf = rememberInfiniteTransition(label = "hint")
    val pulse by inf.animateFloat(
        initialValue = 0.9f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("چطور کار می‌کند؟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        steps.forEachIndexed { i, t ->
            Appear(i + 1) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                val k = if (i == 0) pulse else 1f
                                scaleX = k
                                scaleY = k
                            }
                            .clip(CircleShape)
                            .background(BlueGradient),
                        contentAlignment = Alignment.Center
                    ) { Text("${i + 1}".fa(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Spacer(Modifier.width(12.dp))
                    Text(t, color = TextDim, fontSize = 14.sp)
                }
            }
        }
        Text(
            "نکته: در برنامه یوتیوب دکمه «اشتراک‌گذاری» را بزن و VOLTEX را انتخاب کن.",
            color = Cyan, fontSize = 13.sp
        )
        Text("برای اتصال به یوتیوب فیلترشکن را روشن کن.", color = TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun RadarLoader() {
    val inf = rememberInfiniteTransition(label = "radar")
    val p by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing)),
        label = "p"
    )
    val spin by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "spin"
    )
    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val c = center
            val maxR = size.minDimension / 2f
            for (k in 0..2) {
                val q = (p + k / 3f) % 1f
                drawCircle(
                    Blue.copy(alpha = (1f - q) * 0.55f),
                    radius = maxR * q, center = c, style = Stroke(2.dp.toPx())
                )
            }
            rotate(spin, c) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, Cyan), center = c),
                    startAngle = 0f, sweepAngle = 300f, useCenter = false,
                    topLeft = Offset(c.x - maxR * 0.62f, c.y - maxR * 0.62f),
                    size = Size(maxR * 1.24f, maxR * 1.24f),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Image(
            painterResource(R.drawable.voltex_logo), null,
            Modifier.size(62.dp).clip(CircleShape)
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(message) {
        for (x in listOf(-16f, 14f, -10f, 6f, 0f)) shake.animateTo(x, tween(60))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shake.value }
            .clip(RoundedCornerShape(20.dp))
            .background(Bad.copy(alpha = 0.10f))
            .border(1.dp, Bad.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) { Text(message, color = Err, fontSize = 14.sp) }
}

@Composable
private fun MetaCard(meta: VideoMeta) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .border(1.dp, Line, RoundedCornerShape(24.dp))
    ) {
        val bmp by produceState<ImageBitmap?>(null, meta.thumb) {
            value = meta.thumb?.let { loadBitmap(it) }
        }
        val imgAlpha by animateFloatAsState(if (bmp != null) 1f else 0f, tween(600), label = "thumb")
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Surface2)) {
            bmp?.let {
                Image(
                    it, null,
                    Modifier.fillMaxSize().graphicsLayer { alpha = imgAlpha },
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                )
            )
            val dur = fmtDuration(meta.durationSec)
            if (dur.isNotEmpty()) {
                Text(
                    dur,
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xAA000000))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(
                meta.title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            if (meta.uploader.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(meta.uploader, color = TextDim, fontSize = 13.sp)
            }
        }
    }
}

private suspend fun loadBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        Http.client.newCall(OkRequest.Builder().url(url).build()).execute().use { r ->
            r.body?.byteStream()?.let { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun OptionRow(opt: Option, enabled: Boolean, onClick: () -> Unit) {
    val height = opt.label.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    val hd = height >= 720
    val shape = RoundedCornerShape(20.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(if (pressed && enabled) Surface2 else Surface1)
            .border(1.dp, if (enabled && hd) Blue.copy(alpha = 0.6f) else Line, shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled && hd) BlueGradient else DisabledGradient)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                opt.label,
                color = if (enabled && hd) Color.White else if (enabled) Cyan else TextDim,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(opt.detail, fontSize = 13.sp, color = TextMain)
            val size = fmtSize(opt.size)
            if (size.isNotEmpty()) Text(size, fontSize = 12.sp, color = TextDim)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (enabled) BlueGradient else DisabledGradient),
            contentAlignment = Alignment.Center
        ) {
            Text("↓", color = if (enabled) Color.White else TextDim, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FancyProgress(progress: Float) {
    val inf = rememberInfiniteTransition(label = "progress")
    val ph by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ph"
    )
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "shown")
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = CornerRadius(size.height / 2f)
        // صفحه راست‌به‌چپ است؛ نوار از راست پر شود
        scale(-1f, 1f, center) {
            drawRoundRect(Surface2, cornerRadius = r)
            if (progress < 0f) {
                val w = size.width * 0.35f
                val x = (size.width + w) * ph - w
                clipRect(0f, 0f, size.width, size.height) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Blue, Cyan), startX = x, endX = x + w),
                        topLeft = Offset(x, 0f), size = Size(w, size.height), cornerRadius = r
                    )
                }
            } else {
                val w = size.width * shown
                if (w > 0f) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(BlueDeep, Blue, Cyan), startX = 0f, endX = w.coerceAtLeast(1f)),
                        size = Size(w, size.height), cornerRadius = r
                    )
                    val band = size.width * 0.22f
                    val gx = (w + band) * ph - band
                    clipRect(0f, 0f, w, size.height) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                                startX = gx, endX = gx + band
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadBar(dl: Dl) {
    // آخرین وضعیت غیر خالی را نگه می‌داریم تا هنگام بسته شدن، محتوا ناگهان نپرد
    val last = remember { arrayOf<Dl>(Dl.Idle) }
    if (dl !is Dl.Idle) last[0] = dl
    AnimatedVisibility(
        visible = dl !is Dl.Idle,
        enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200))
    ) {
        DownloadBarContent(last[0])
    }
}

@Composable
private fun DownloadBarContent(dl: Dl) {
    val ctx = LocalContext.current
    val top = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    when (dl) {
        Dl.Idle -> Unit
        is Dl.Running -> Column(
            Modifier
                .fillMaxWidth()
                .clip(top)
                .background(Surface1)
                .border(1.dp, Blue.copy(alpha = 0.45f), top)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(dl.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            FancyProgress(dl.progress)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pct = if (dl.progress >= 0f) " · ${(dl.progress * 100).toInt()}٪".fa() else ""
                Text(dl.phase + pct, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { ctx.startService(DownloadService.cancelIntent(ctx)) }) {
                    Text("لغو", color = Err, fontWeight = FontWeight.Bold)
                }
            }
        }
        is Dl.Done -> {
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(top)
                    .background(Surface1)
                    .border(1.dp, Ok.copy(alpha = 0.45f), top)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                        .clip(CircleShape)
                        .background(Ok.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("✓", color = Ok, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ذخیره شد", color = Ok, fontWeight = FontWeight.Bold)
                    Text(dl.where, color = TextDim, fontSize = 12.sp)
                }
                TextButton(onClick = { DownloadHub.state.value = Dl.Idle }) { Text("باشه", color = Cyan) }
            }
        }
        is Dl.Failed -> Row(
            Modifier
                .fillMaxWidth()
                .clip(top)
                .background(Surface1)
                .border(1.dp, Bad.copy(alpha = 0.55f), top)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dl.message, color = Err, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { DownloadHub.state.value = Dl.Idle }) { Text("باشه", color = Cyan) }
        }
    }
}
