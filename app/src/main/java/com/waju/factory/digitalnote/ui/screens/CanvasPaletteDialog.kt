package com.waju.factory.digitalnote.ui.screens

import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.waju.factory.digitalnote.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun PaletteEditorDialog(
    palette: List<Color>,
    selectedColorIndex: Int,
    editingColorIndex: Int,
    editingColor: Color,
    strokeWidth: Float,
    onDismiss: () -> Unit,
    onSelectColor: (Int) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onPaletteColorChanged: (Color) -> Unit
) {
    val hsv = remember(editingColor) {
        FloatArray(3).apply {
            AndroidColor.colorToHSV(editingColor.toArgb(), this)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("パレット") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "線の太さ: ${strokeWidth.toInt()} px",
                    color = TextSecondary
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 2f..24f
                )

                HorizontalDivider()

                Text("編集中の色", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    palette.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(if (editingColorIndex == index) 34.dp else 28.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selectedColorIndex == index || editingColorIndex == index) 2.dp else 0.dp,
                                    color = if (editingColorIndex == index) MaterialTheme.colorScheme.primary else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(index) }
                        )
                    }
                }

                HueRingPicker(
                    hue = hsv[0],
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    onHueChange = { hue ->
                        val safeSaturation = if (hsv[1] < 0.08f) 0.85f else hsv[1]
                        val safeValue = if (hsv[2] < 0.08f) 0.9f else hsv[2]
                        onPaletteColorChanged(colorFromHsv(hue, safeSaturation, safeValue))
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(editingColor, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                )

                ColorChannelSlider(
                    label = "彩度",
                    value = hsv[1] * 100f,
                    onValueChange = { value ->
                        onPaletteColorChanged(colorFromHsv(hsv[0], value / 100f, hsv[2]))
                    }
                )
                ColorChannelSlider(
                    label = "明るさ",
                    value = hsv[2] * 100f,
                    onValueChange = { value ->
                        onPaletteColorChanged(colorFromHsv(hsv[0], hsv[1], value / 100f))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun HueRingPicker(
    hue: Float,
    modifier: Modifier = Modifier,
    onHueChange: (Float) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val centerFillColor = MaterialTheme.colorScheme.surface
    val hueColors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )

    Canvas(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        onHueChange(Offset(event.x, event.y).toHue(size))
                    }
                }
                true
            }
    ) {
        val strokeWidth = min(size.width, size.height) * 0.16f
        val radius = (min(size.width, size.height) / 2f) - strokeWidth
        drawCircle(
            brush = Brush.sweepGradient(hueColors),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = centerFillColor,
            radius = radius - strokeWidth * 0.65f
        )

        val angle = Math.toRadians(hue.toDouble())
        val handleCenter = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius
        )
        drawCircle(color = Color.White, radius = strokeWidth * 0.22f, center = handleCenter)
        drawCircle(
            color = colorFromHsv(hue, 1f, 1f),
            radius = strokeWidth * 0.14f,
            center = handleCenter
        )
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label: ${value.toInt()}",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f
        )
    }
}

private fun Offset.toHue(size: IntSize): Float {
    if (size.width == 0 || size.height == 0) return 0f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val angle = Math.toDegrees(atan2(y - centerY, x - centerX).toDouble()).toFloat()
    return if (angle < 0f) angle + 360f else angle
}

private fun colorFromHsv(hue: Float, saturation: Float, value: Float): Color {
    return Color(
        AndroidColor.HSVToColor(
            floatArrayOf(
                hue,
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f)
            )
        )
    )
}

