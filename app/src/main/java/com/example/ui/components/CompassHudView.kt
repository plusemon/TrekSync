package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeamMember
import com.example.model.UserLocation
import com.example.ui.theme.TrekAlpineCyan
import com.example.ui.theme.TrekDarkBackground
import com.example.ui.theme.TrekDarkSurface
import com.example.ui.theme.TrekSignalGreen
import com.example.ui.theme.TrekSosCrimson
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassHudView(
    headingDegrees: Float,
    currentLocation: UserLocation,
    teamMembers: List<TeamMember>,
    modifier: Modifier = Modifier
) {
    val cardinal = when (headingDegrees.toInt()) {
        in 0..22, in 338..360 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        else -> "NW"
    }

    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(TrekDarkSurface.copy(alpha = 0.9f))
            .testTag("compass_hud_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 4.dp.toPx()

            // Outer Compass Dial Ring
            drawCircle(
                color = TrekSignalGreen.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Compass Ticks and Cardinal markers rotated opposite to heading
            rotate(degrees = -headingDegrees, pivot = center) {
                for (i in 0 until 360 step 30) {
                    val angleRad = Math.toRadians(i.toDouble())
                    val tickLength = if (i % 90 == 0) 8.dp.toPx() else 4.dp.toPx()
                    val start = Offset(
                        x = center.x + (radius - tickLength) * sin(angleRad).toFloat(),
                        y = center.y - (radius - tickLength) * cos(angleRad).toFloat()
                    )
                    val end = Offset(
                        x = center.x + radius * sin(angleRad).toFloat(),
                        y = center.y - radius * cos(angleRad).toFloat()
                    )
                    val tickColor = if (i == 0) TrekSosCrimson else TrekSignalGreen.copy(alpha = 0.6f)
                    drawLine(
                        color = tickColor,
                        start = start,
                        end = end,
                        strokeWidth = if (i == 0) 2.5.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // North Arrow
                val northPath = Path().apply {
                    moveTo(center.x, center.y - radius + 2.dp.toPx())
                    lineTo(center.x - 5.dp.toPx(), center.y - radius + 14.dp.toPx())
                    lineTo(center.x + 5.dp.toPx(), center.y - radius + 14.dp.toPx())
                    close()
                }
                drawPath(path = northPath, color = TrekSosCrimson)
            }

            // Teammate direction blips on radar ring
            for (member in teamMembers) {
                if (member.id != "USER_LEADER") {
                    val bearingToMember = calculateBearing(
                        currentLocation.latitude, currentLocation.longitude,
                        member.location.latitude, member.location.longitude
                    )
                    val relativeAngle = bearingToMember - headingDegrees
                    val rad = Math.toRadians(relativeAngle.toDouble())

                    val blipPos = Offset(
                        x = center.x + (radius - 12.dp.toPx()) * sin(rad).toFloat(),
                        y = center.y - (radius - 12.dp.toPx()) * cos(rad).toFloat()
                    )

                    val blipColor = Color(android.graphics.Color.parseColor(member.colorHex))
                    drawCircle(color = blipColor, radius = 3.5.dp.toPx(), center = blipPos)
                }
            }
        }

        // Center Digital Azimuth & Cardinal display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${headingDegrees.toInt()}°",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = cardinal,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TrekSignalGreen,
                fontSize = 10.sp
            )
        }
    }
}

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    var theta = Math.toDegrees(atan2(y, x))
    if (theta < 0) theta += 360.0
    return theta
}
