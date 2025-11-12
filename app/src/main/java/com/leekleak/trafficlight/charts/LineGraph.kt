package com.leekleak.trafficlight.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.util.px
import java.lang.Float.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LineGraph(
    maximum: Long,
    data: Pair<Long, Long>,
) {
    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val backgroundColor = GraphTheme.backgroundColor
    val cornerRadius = CornerRadius(32.dp.px)

    data class RectDrawData (
        val rect: Rect,
        val color: Color
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val wifiMaxWidth = size.width - if (data.second != 0L) 24.dp.toPx() else 0f
        val cellularMaxWidth = size.width - if (data.first != 0L) 24.dp.toPx() else 0f
        val wifiSizeX =
            min(
                max((data.first.toDouble() / maximum.toDouble() * size.width).toFloat(), cornerRadius.x),
                wifiMaxWidth
            )
        val cellularSizeX =
            min(
            max((data.second.toDouble() / maximum.toDouble() * size.width).toFloat(), cornerRadius.x),
            cellularMaxWidth
            )

        val padding = 6.dp.toPx()

        val wifiRect = Rect(
            Offset(padding, padding),
            Size(wifiSizeX - padding * 2, size.height - padding * 2)
        )

        val cellularRect = Rect(
            Offset(size.width - cellularSizeX + padding, padding),
            Size(cellularSizeX - padding * 2, size.height- padding * 2)
        )

        val backgroundRect = Rect(
            Offset(0f, 0f),
            Size(size.width, size.height)
        )

        val rects = mutableListOf(RectDrawData(backgroundRect, backgroundColor))
        if (data.first != 0L) rects.add(RectDrawData(wifiRect, primaryColor))
        if (data.second != 0L) rects.add(RectDrawData(cellularRect, secondaryColor))

        for (data in rects) {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = data.rect,
                        cornerRadius = cornerRadius
                    )
                )
            }
            drawPath(path, data.color)
        }
    }
}


