package com.nbunone.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** 도넛 차트 + 범례. values 합이 0이면 빈 상태 표시 */
@Composable
fun DonutChart(
    entries: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    size: Int = 160
) {
    val total = entries.sumOf { it.second.toDouble() }.toFloat()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size.dp)) {
                val stroke = 28.dp.toPx()
                val d = min(this.size.width, this.size.height) - stroke
                val topLeft = Offset((this.size.width - d) / 2, (this.size.height - d) / 2)
                if (total <= 0f) {
                    drawArc(Color(0xFFE2E8F0), 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
                } else {
                    var start = -90f
                    entries.forEachIndexed { i, (_, v) ->
                        val sweep = v / total * 360f
                        drawArc(
                            ChartColors[i % ChartColors.size], start, sweep, false,
                            topLeft, Size(d, d), style = Stroke(stroke)
                        )
                        start += sweep
                    }
                }
            }
            if (total > 0f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${total.trim()}h", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("총 활동", fontSize = 11.sp, color = Slate)
                }
            } else {
                Text("기록 없음", fontSize = 12.sp, color = Slate)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEachIndexed { i, (label, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(ChartColors[i % ChartColors.size], CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    val pct = if (total > 0f) (v / total * 100).toInt() else 0
                    Text("$label ${v.trim()}h ($pct%)", fontSize = 13.sp)
                }
            }
        }
    }
}

/** 가로 막대 (0..max 스케일) */
@Composable
fun BarRow(label: String, value: Float, max: Float, color: Color, valueText: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text(valueText ?: value.trim(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
        ) {
            val frac = if (max > 0f) (value / max).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

fun Float.trim(): String =
    if (this == this.toLong().toFloat()) this.toLong().toString() else "%.1f".format(this)
