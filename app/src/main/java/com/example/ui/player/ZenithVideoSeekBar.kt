package com.example.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentYellow
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ZenithVideoSeekBar(
    currentTimeSec: Long,
    durationSec: Long,
    bufferedSec: Long = 0L,
    isScrubbing: Boolean,
    scrubPositionSec: Long,
    seekbarStyle: String = "Modern",
    accentColor: Color,
    introRangeSec: LongRange? = null,
    outroRangeSec: LongRange? = null,
    enabled: Boolean = true,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val safeDuration = durationSec.coerceAtLeast(1L)
    val displayedSec = if (isScrubbing) scrubPositionSec.coerceIn(0L, safeDuration)
    else currentTimeSec.coerceIn(0L, safeDuration)

    val progressFraction = if (durationSec > 0L) {
        (displayedSec.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val bufferedFraction = if (durationSec > 0L) {
        (bufferedSec.coerceIn(0L, safeDuration).toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val baseTrackHeightDp = when (seekbarStyle) {
        "Minimal" -> 2.5.dp
        "Thick" -> 8.dp
        "Neon" -> 4.5.dp
        "Wave" -> 3.5.dp
        else -> 4.dp
    }
    val activeTrackHeightDp by animateDpAsState(
        targetValue = if (isScrubbing) baseTrackHeightDp + 2.5.dp else baseTrackHeightDp,
        animationSpec = tween(150),
        label = "seek_track_h"
    )

    val baseThumbRadiusDp = when (seekbarStyle) {
        "Minimal" -> 5.dp
        "Thick" -> 9.dp
        "Neon" -> 8.dp
        "Wave" -> 7.dp
        else -> 7.dp
    }
    val activeThumbRadiusDp by animateDpAsState(
        targetValue = if (isScrubbing) baseThumbRadiusDp + 3.dp else baseThumbRadiusDp,
        animationSpec = tween(150),
        label = "seek_thumb_r"
    )

    val haloAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 0.34f else if (seekbarStyle == "Neon") 0.22f else 0f,
        animationSpec = tween(150),
        label = "seek_halo"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave_seek")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    var canvasWidthPx by remember { mutableFloatStateOf(1f) }
    val currentDurationState by rememberUpdatedState(safeDuration)
    val currentEnabledState by rememberUpdatedState(enabled && durationSec > 0L)
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubChange by rememberUpdatedState(onScrubChange)
    val currentOnScrubFinished by rememberUpdatedState(onScrubFinished)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val horizontalPadPx = with(density) { 10.dp.toPx() }

        // Floating scrub time tooltip above thumb
        if (isScrubbing && durationSec > 0L) {
            val deltaSec = scrubPositionSec - currentTimeSec
            val deltaPrefix = if (deltaSec >= 0) "+" else "-"
            val absDelta = kotlin.math.abs(deltaSec)
            val chapterTag = when {
                introRangeSec != null && scrubPositionSec in introRangeSec -> " • OP"
                outroRangeSec != null && scrubPositionSec in outroRangeSec -> " • ED"
                else -> ""
            }
            val tooltipWidthPx = with(density) { 116.dp.toPx() }
            val thumbCenterX = horizontalPadPx + (maxWidthPx - 2 * horizontalPadPx).coerceAtLeast(1f) * progressFraction
            val clampedOffsetX = (thumbCenterX - tooltipWidthPx / 2f)
                .coerceIn(0f, (maxWidthPx - tooltipWidthPx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedOffsetX.roundToInt(), with(density) { (-26).dp.roundToPx() }) }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE610141D))
                    .border(0.8.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(scrubPositionSec),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "($deltaPrefix${formatTime(absDelta)})$chapterTag",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!currentEnabledState) return@awaitEachGesture
                        down.consume()

                        fun xToSeconds(x: Float): Long {
                            val usable = (canvasWidthPx - 2 * horizontalPadPx).coerceAtLeast(1f)
                            val frac = ((x - horizontalPadPx) / usable).coerceIn(0f, 1f)
                            return (frac * currentDurationState).toLong().coerceIn(0L, currentDurationState)
                        }

                        var lastSec = xToSeconds(down.position.x)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnScrubStart(lastSec)

                        try {
                            drag(down.id) { change ->
                                change.consume()
                                val newSec = xToSeconds(change.position.x)
                                if (newSec != lastSec) {
                                    lastSec = newSec
                                    currentOnScrubChange(newSec)
                                }
                            }
                        } finally {
                            currentOnScrubFinished(lastSec)
                        }
                    }
                }
        ) {
            val trackH = activeTrackHeightDp.toPx()
            val thumbR = activeThumbRadiusDp.toPx()
            val startX = horizontalPadPx
            val endX = (size.width - horizontalPadPx).coerceAtLeast(startX + 1f)
            val trackWidth = endX - startX
            val centerY = size.height / 2f
            val activeX = startX + trackWidth * progressFraction
            val bufferedX = startX + trackWidth * bufferedFraction
            val corner = CornerRadius(trackH / 2f, trackH / 2f)

            // 1. Inactive background track
            drawRoundRect(
                color = Color(0x45FFFFFF),
                topLeft = Offset(startX, centerY - trackH / 2f),
                size = Size(trackWidth, trackH),
                cornerRadius = corner
            )

            // 2. Buffered progress track
            if (bufferedX > startX) {
                drawRoundRect(
                    color = Color(0x78FFFFFF),
                    topLeft = Offset(startX, centerY - trackH / 2f),
                    size = Size((bufferedX - startX).coerceAtMost(trackWidth), trackH),
                    cornerRadius = corner
                )
            }

            // 3. OP / ED chapter highlight segments
            if (durationSec > 0L) {
                fun drawChapterSegment(range: LongRange) {
                    val segStartFrac = (range.first.toFloat() / safeDuration).coerceIn(0f, 1f)
                    val segEndFrac = (range.last.toFloat() / safeDuration).coerceIn(0f, 1f)
                    if (segEndFrac > segStartFrac) {
                        val sx = startX + trackWidth * segStartFrac
                        val sw = trackWidth * (segEndFrac - segStartFrac)
                        drawRoundRect(
                            color = AccentYellow.copy(alpha = 0.6f),
                            topLeft = Offset(sx, centerY - trackH / 2f),
                            size = Size(sw, trackH),
                            cornerRadius = corner
                        )
                    }
                }
                if (introRangeSec != null) drawChapterSegment(introRangeSec)
                if (outroRangeSec != null) drawChapterSegment(outroRangeSec)
            }

            // 4. Active played track (by style)
            val playedWidth = (activeX - startX).coerceAtLeast(0f)
            if (playedWidth > 0f) {
                when (seekbarStyle) {
                    "Wave" -> {
                        val path = Path()
                        val amp = (trackH * 0.9f).coerceAtLeast(3.dp.toPx())
                        val wavelength = 24.dp.toPx().coerceAtLeast(12f)
                        path.moveTo(startX, centerY)
                        var x = startX
                        while (x <= activeX) {
                            val angle = ((x - startX) / wavelength) * (2f * PI.toFloat()) - wavePhase
                            val y = centerY + sin(angle) * amp
                            path.lineTo(x, y)
                            x += 3f
                        }
                        drawPath(
                            path = path,
                            color = accentColor,
                            style = Stroke(width = trackH, cap = StrokeCap.Round)
                        )
                    }

                    "Neon" -> {
                        // Outer neon bloom
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.35f),
                            topLeft = Offset(startX, centerY - trackH * 1.15f),
                            size = Size(playedWidth, trackH * 2.3f),
                            cornerRadius = CornerRadius(trackH, trackH)
                        )
                        // Bright inner neon core
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(startX, centerY - trackH / 2f),
                            size = Size(playedWidth, trackH),
                            cornerRadius = corner
                        )
                    }

                    else -> {
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(startX, centerY - trackH / 2f),
                            size = Size(playedWidth, trackH),
                            cornerRadius = corner
                        )
                    }
                }
            }

            // 5. Thumb + Halo
            val thumbCenter = Offset(activeX.coerceIn(startX, endX), centerY)
            if (haloAlpha > 0f) {
                drawCircle(
                    color = accentColor.copy(alpha = haloAlpha),
                    radius = thumbR * 1.9f,
                    center = thumbCenter
                )
            }
            drawCircle(
                color = accentColor,
                radius = thumbR,
                center = thumbCenter
            )
            if (seekbarStyle == "Thick" || seekbarStyle == "Neon" || isScrubbing) {
                drawCircle(
                    color = Color.White,
                    radius = (thumbR * 0.38f).coerceAtLeast(2f),
                    center = thumbCenter
                )
            }
        }
    }
}
