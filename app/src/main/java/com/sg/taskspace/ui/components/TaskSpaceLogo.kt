package com.sg.taskspace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.PrimaryIndigo

/**
 * High-definition, resolution-independent vector logo for TaskSpace.
 * Accurately represents the stacked task-card papers, checkmark, and branding.
 */
@Composable
fun TaskSpaceLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    cornerRadius: Dp = size * 0.22f,
    showShadow: Boolean = true
) {
    val shadowElevation = if (showShadow) (size.value * 0.08f).dp else 0.dp

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showShadow) {
                    Modifier.shadow(
                        elevation = shadowElevation,
                        shape = RoundedCornerShape(cornerRadius),
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()

            // 1. Warm Cream Base Background
            drawRect(
                color = Color(0xFFFCE6C9),
                size = androidx.compose.ui.geometry.Size(w, h)
            )

            // 2. Cyan Paper Layer
            val cyanPath = Path().apply {
                moveTo(-w * 0.05f, h * 0.14f)
                lineTo(w * 0.88f, -h * 0.02f)
                lineTo(w * 0.96f, h * 0.44f)
                lineTo(-w * 0.05f, h * 0.58f)
                close()
            }
            drawPath(path = cyanPath, color = Color(0xFFAFF0FC))

            // 3. Coral Rose Paper Layer
            val coralPath = Path().apply {
                moveTo(w * 0.08f, h * 0.24f)
                lineTo(w * 1.05f, h * 0.42f)
                lineTo(w * 1.05f, h * 0.78f)
                lineTo(-w * 0.05f, h * 0.65f)
                close()
            }
            drawPath(path = coralPath, color = Color(0xFFFF949C))

            // 4. Dark Obsidian Task Card Layer
            val obsPath = Path().apply {
                moveTo(-w * 0.05f, h * 0.72f)
                lineTo(w * 0.94f, h * 0.52f)
                lineTo(w * 1.05f, h * 0.80f)
                lineTo(w * 1.05f, h * 1.05f)
                lineTo(-w * 0.05f, h * 1.05f)
                close()
            }
            drawPath(path = obsPath, color = Color(0xFF080C14))

            // 5. Crisp White Checkmark
            val strokeWidth = (w * 0.045f).coerceAtLeast(2.5f)
            val chkPath = Path().apply {
                moveTo(w * 0.07f, h * 0.84f)
                lineTo(w * 0.16f, h * 0.93f)
                lineTo(w * 0.35f, h * 0.76f)
            }
            drawPath(
                path = chkPath,
                color = Color.White,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 6. Vector glyph representation of "Task" & "Space" lines
            val lineStroke = (w * 0.028f).coerceAtLeast(1.5f)
            // Task line indicator
            drawLine(
                color = Color.White,
                start = Offset(w * 0.52f, h * 0.74f),
                end = Offset(w * 0.88f, h * 0.74f),
                strokeWidth = lineStroke * 1.4f,
                cap = StrokeCap.Round
            )
            // Space line indicator
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(w * 0.54f, h * 0.85f),
                end = Offset(w * 0.82f, h * 0.85f),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Full branded header banner with TaskSpace logo, title, and metadata.
 */
@Composable
fun TaskSpaceBrandHeader(
    modifier: Modifier = Modifier,
    appVersion: String = "1.0.0",
    subtitle: String = "Offline Productivity & Focus Engine"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaskSpaceLogoIcon(size = 60.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TaskSpace",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = PrimaryIndigo.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v$appVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
