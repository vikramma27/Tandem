package app.tandem.duo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.tandem.duo.data.local.NudgePingEntity
import app.tandem.duo.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kinetic emblem: renders a single glowing terracotta ring in Solo/Unpaired state,
 * and interlocks the second champagne-gold ring as soon as the user pairs with a second person.
 */
@Composable
fun DuoKineticOrbitEmblem(
    isPaired: Boolean,
    harmonyProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "duo_orbit")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val animatedHarmony by animateFloatAsState(
        targetValue = harmonyProgress.coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "harmony"
    )

    Canvas(modifier = modifier.size(54.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.28f
        val offsetShift = if (isPaired) size.minDimension * 0.13f else 0f
        val rad = Math.toRadians(phase.toDouble())
        val wobbleX = (cos(rad) * 2.2f).toFloat()
        val wobbleY = (sin(rad) * 2.2f).toFloat()

        // Primary User Ring (Terracotta)
        drawCircle(
            color = TerracottaCoral,
            radius = radius,
            center = Offset(center.x - offsetShift + wobbleX, center.y + wobbleY),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
        // Second User Ring (Champagne Gold — visible when paired, dashed/dim when unpaired)
        if (isPaired) {
            drawCircle(
                color = ChampagneGold,
                radius = radius,
                center = Offset(center.x + offsetShift - wobbleX, center.y - wobbleY),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        drawArc(
            color = SageMint.copy(alpha = 0.55f),
            startAngle = -90f,
            sweepAngle = 360f * animatedHarmony,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LiveAttentionNudgeBanner(
    nudge: NudgePingEntity,
    senderLabel: String,
    onQuickReply: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nudge_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(nudge.urgency.pulseSpeedMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        TerracottaCoral.copy(alpha = glowAlpha),
                        CharcoalSurface
                    )
                )
            )
            .border(1.5.dp, TerracottaCoral.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TerracottaCoral)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE PING • ${senderLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneGold
                )
            }
            Text(
                text = nudge.urgency.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = IvoryText.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = nudge.message,
            style = MaterialTheme.typography.titleMedium,
            color = IvoryText
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("On it", "15 mins", "Done").forEach { reply ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .clickable { onQuickReply(reply) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = reply,
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryText
                    )
                }
            }
        }
    }
}
