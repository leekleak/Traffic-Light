package com.leekleak.trafficlight.charts

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch


@Composable
fun BarGraph(
    modifier: Modifier = Modifier,
    data: List<BarData>,
) {
    BarGraphImpl(
        modifier = modifier,
        xAxisData = data.map { it.x },
        yAxisData = data.map { Pair(it.y1, it.y2) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BarGraphImpl(
    modifier: Modifier,
    xAxisData: List<String>,
    yAxisData: List<Pair<Double, Double>>,
) {
    val scope = rememberCoroutineScope()
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
    val vibrationEffectStrong = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
    val vibrationEffectWeak = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onSecondaryColor = GraphTheme.onSecondaryColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius

    val shapeWifi = GraphTheme.wifiShape()
    val shapeCellular = GraphTheme.cellularShape()
    val iconWifi = GraphTheme.wifiIcon()
    val iconCellular = GraphTheme.cellularIcon()

    val wifiLegendStrength = remember { mutableIntStateOf(5) }
    val cellularLegendStrength = remember { mutableIntStateOf(5) }

    val wifiLegendOffset = animateFloatAsState(
        if (wifiLegendStrength.intValue > 0) 0f else 120.dp.px,
        tween(250, easing = EaseIn)
    )
    val cellularLegendOffset = animateFloatAsState(
        if (cellularLegendStrength.intValue > 0) 0f else 120.dp.px,
        tween(250, easing = EaseIn)
    )

    val wifiAnimation = remember { Animatable(0f) }
    val cellularAnimation = remember { Animatable(0f) }

    var wifiOffset: Offset = Offset.Zero
    var cellularOffset: Offset = Offset.Zero
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(true) {
                detectTapGestures { offset ->
                    scope.launch {
                        if (
                            (offset - wifiOffset).x in (0f..32.dp.toPx()) &&
                            (offset - wifiOffset).y in (0f..32.dp.toPx()) &&
                            wifiLegendStrength.intValue != 0
                        ) {
                            wifiAnimation.animateTo(
                                targetValue = if (wifiAnimation.targetValue == 15f) 0f else 15f,
                                animationSpec = tween(150)
                            )
                            wifiLegendStrength.intValue -= 1
                            if (wifiLegendStrength.intValue == 0) {
                                vibrator.vibrate(vibrationEffectStrong)
                            }
                            else {
                                vibrator.vibrate(vibrationEffectWeak)
                            }
                        }
                        if (
                            (offset - cellularOffset).x in (0f..32.dp.toPx()) &&
                            (offset - cellularOffset).y in (0f..32.dp.toPx()) &&
                            cellularLegendStrength.intValue != 0
                        ) {
                            cellularAnimation.animateTo(
                                targetValue = if (cellularAnimation.targetValue == 15f) 0f else 15f,
                                animationSpec = tween(150)
                            )
                            cellularLegendStrength.intValue -= 1
                            if (cellularLegendStrength.intValue == 0) {
                                vibrator.vibrate(vibrationEffectStrong)
                            }
                            else {
                                vibrator.vibrate(vibrationEffectWeak)
                            }
                        }
                    }
                }
            }
    ) {
        val barGraphHelper = BarGraphHelper(
            scope = this,
            yAxisData = yAxisData,
            xAxisData = xAxisData
        )

        wifiOffset = barGraphHelper.metrics.wifiIconOffset
        cellularOffset = barGraphHelper.metrics.cellularIconOffset

        barGraphHelper.drawGrid(gridColor)

        barGraphHelper.drawLegend(
            barGraphHelper.metrics.wifiIconOffset.copy(
                y = barGraphHelper.metrics.wifiIconOffset.y + wifiLegendOffset.value
            ),
            primaryColor,
            shapeWifi,
            iconWifi,
            onPrimaryColor,
            wifiAnimation.value,
        )

        barGraphHelper.drawLegend(
            barGraphHelper.metrics.cellularIconOffset.copy(
                y = barGraphHelper.metrics.cellularIconOffset.y + cellularLegendOffset.value
            ),
            secondaryColor,
            shapeCellular,
            iconCellular,
            onSecondaryColor,
            cellularAnimation.value,
        )

        barGraphHelper.drawTextLabelsOverXAndYAxis(gridColor)
        barGraphHelper.drawBars(cornerRadius, primaryColor, secondaryColor)
    }

}

