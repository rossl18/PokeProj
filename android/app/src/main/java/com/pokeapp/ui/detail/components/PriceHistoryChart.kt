package com.pokeapp.ui.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.pokeapp.domain.model.PricePoint
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val labelFormatter = DateTimeFormatter.ofPattern("M/d")

private fun formatLabel(isoTimestamp: String): String = try {
    OffsetDateTime.parse(isoTimestamp).format(labelFormatter)
} catch (e: DateTimeParseException) {
    isoTimestamp
}

@Composable
fun PriceHistoryChart(history: List<PricePoint>, modifier: Modifier = Modifier) {
    val pointsWithPrice = remember(history) { history.filter { it.marketPrice != null } }

    if (pointsWithPrice.size < 2) {
        Box(modifier = modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("Not enough history yet for a chart.")
        }
        return
    }

    // X = point index (keeps axis math simple/precise); a parallel map from
    // index -> formatted timestamp label drives the bottom axis text.
    val labels = remember(pointsWithPrice) {
        pointsWithPrice.mapIndexed { index, point -> index.toFloat() to formatLabel(point.fetchedAt) }.toMap()
    }
    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ -> labels[x.toFloat()] ?: "" }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(pointsWithPrice) {
        modelProducer.runTransaction {
            lineModel { series(pointsWithPrice.map { it.marketPrice!! }) }
        }
    }

    // Auto-ranging otherwise puts the min/max points exactly on the chart's
    // top/bottom edge, making the line look like it's clipping off the chart.
    val rangeProvider = remember {
        object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val span = maxY - minY
                val padding = if (span > 0.0) span * 0.1 else (if (minY == 0.0) 1.0 else kotlin.math.abs(minY) * 0.1)
                return minY - padding
            }

            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
                val span = maxY - minY
                val padding = if (span > 0.0) span * 0.1 else (if (maxY == 0.0) 1.0 else kotlin.math.abs(maxY) * 0.1)
                return maxY + padding
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(rangeProvider = rangeProvider),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(220.dp),
    )
}
