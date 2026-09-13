package com.zr.financetracker.rieaz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zr.financetracker.rieaz.ui.CurrencyHelper

@Composable
fun DonutChart(
    data: List<Pair<String, Double>>,
    totalAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(170.dp)) {
            val width = size.width
            val height = size.height
            val strokeWidth = 32.dp.toPx()

            if (totalAmount <= 0.0 || data.isEmpty()) {
                // Empty state donut circle
                drawArc(
                    color = defaultColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                var currentAngle = -90f
                data.forEach { (cat, amount) ->
                    val sweepAngle = (amount / totalAmount).toFloat() * 360f * animatedProgress.value
                    drawArc(
                        color = getCategoryColor(cat),
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentAngle += (amount / totalAmount).toFloat() * 360f
                }
            }
        }

        // Inner aggregate labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Total Spend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format("%s%,.2f", currencySymbol, CurrencyHelper.convertFromBDT(totalAmount, currencySymbol)),
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SpendingTrendLineGraph(
    points: List<Double>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingLeft = 40.dp.toPx()
                val paddingRight = 10.dp.toPx()
                val paddingTop = 15.dp.toPx()
                val paddingBottom = 20.dp.toPx()

                val chartWidth = canvasWidth - paddingLeft - paddingRight
                val chartHeight = canvasHeight - paddingTop - paddingBottom

                val maxVal = points.maxOrNull()?.coerceAtLeast(1000.0) ?: 1000.0

                // 1. Draw horizontal dotted/dashed grid lines & Y-axis labels
                val gridLinesCount = 4
                for (i in 0 until gridLinesCount) {
                    val y = paddingTop + chartHeight - (i.toFloat() / (gridLinesCount - 1)) * chartHeight
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(canvasWidth - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.isNotEmpty()) {
                    val stepX = chartWidth / (points.size - 1)
                    val coordinates = points.mapIndexed { index, value ->
                        val x = paddingLeft + index * stepX
                        val ratio = if (maxVal > 0.0) value / maxVal else 0.0
                        val y = paddingTop + chartHeight - (ratio * chartHeight * animatedProgress.value).toFloat()
                        Offset(x, y)
                    }

                    // 2. Draw standard gradient area under path
                    val fillPath = Path().apply {
                        if (coordinates.isNotEmpty()) {
                            moveTo(paddingLeft, paddingTop + chartHeight)
                            lineTo(coordinates.first().x, coordinates.first().y)
                            for (i in 1 until coordinates.size) {
                                val p0 = coordinates[i - 1]
                                val p1 = coordinates[i]
                                // Cubic bezier control helper connection
                                val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                            }
                            lineTo(canvasWidth - paddingRight, paddingTop + chartHeight)
                            close()
                        }
                    }

                    // Brush gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.25f),
                                lineColor.copy(alpha = 0.01f)
                            ),
                            start = Offset(0f, paddingTop),
                            end = Offset(0f, paddingTop + chartHeight)
                        )
                    )

                    // 3. Draw smooth trend Bezier stroke
                    val strokePath = Path().apply {
                        if (coordinates.isNotEmpty()) {
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 1 until coordinates.size) {
                                val p0 = coordinates[i - 1]
                                val p1 = coordinates[i]
                                val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                            }
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 4. Highlight vertices
                    coordinates.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val days = listOf("1", "5", "10", "15", "20", "25", "30")
            days.forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    color = labelColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Category color finder
fun getCategoryColor(cat: String): Color {
    return when (cat.lowercase().trim()) {
        "food" -> Color(0xFFF97316)
        "rent" -> Color(0xFF6366F1)
        "transport" -> Color(0xFFEC4899)
        "health" -> Color(0xFFF43F5E)
        "education" -> Color(0xFF8B5CF6)
        "entertainment" -> Color(0xFFEAB308)
        "utility" -> Color(0xFF14B8A6)
        "shopping" -> Color(0xFF10B981)
        "income" -> Color(0xFF22C55E)
        else -> Color(0xFF64748B)
    }
}
