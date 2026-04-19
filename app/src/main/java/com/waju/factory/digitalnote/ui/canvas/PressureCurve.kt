package com.waju.factory.digitalnote.ui.canvas

import kotlin.math.pow

fun applyPressureCurve(baseWidth: Float, pressure: Float, sensitivity: Float): Float {
    val clampedPressure = pressure.coerceIn(0.1f, 1.6f)
    val normalized = (clampedPressure / 1.6f).coerceIn(0f, 1f)
    val curved = normalized.pow(2f)
    val gain = (0.45f + sensitivity.coerceIn(0.3f, 1.2f) * curved)
    return (baseWidth * gain).coerceIn(1f, 40f)
}

