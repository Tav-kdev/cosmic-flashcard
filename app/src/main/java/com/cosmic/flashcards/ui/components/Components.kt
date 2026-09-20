package com.cosmic.flashcards.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic.flashcards.ui.theme.Accent
import com.cosmic.flashcards.ui.theme.CosmicBackground
import com.cosmic.flashcards.ui.theme.GlassBorder
import com.cosmic.flashcards.ui.theme.GlassFill
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextSecondary
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * The drifting starfield behind everything.
 *
 * Stars are generated once into a fixed list and drawn directly to the canvas,
 * then translated by a single long-running animation — cheap enough to leave
 * running behind every screen.
 */
@Composable
fun StarField(modifier: Modifier = Modifier, starCount: Int = 110) {
    val stars = remember {
        val rng = Random(7)
        List(starCount) {
            Triple(
                rng.nextFloat(),                    // x, 0..1
                rng.nextFloat(),                    // y, 0..1
                0.6f + rng.nextFloat() * 1.1f,      // radius in dp
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )

    Box(
        modifier = modifier.drawBehind {
            val h = size.height
            for ((fx, fy, r) in stars) {
                // Wrap vertically so the field scrolls forever without a seam.
                val y = ((fy - drift) % 1f + 1f) % 1f
                val alpha = 0.25f + (r - 0.6f) / 1.1f * 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r.dp.toPx(),
                    center = Offset(fx * size.width, y * h),
                )
            }
        }
    )
}

/** Page scaffold: gradient background + starfield, with content on top. */
@Composable
fun CosmicBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        StarField(Modifier.fillMaxSize())
        content()
    }
}

/** The translucent bordered card used throughout the app. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier
        .clip(shape)
        .background(GlassFill)
        .border(1.dp, borderColor, shape)
    Box(if (onClick != null) base.clickable(onClick = onClick) else base) {
        content()
    }
}

/** Small uppercase section heading. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = modifier,
    )
}

/** Rounded pill used for tags, states and counts. */
@Composable
fun Chip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Color = color.copy(alpha = 0.12f),
    border: Color = color.copy(alpha = 0.28f),
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    val base = modifier
        .clip(shape)
        .background(fill)
        .border(1.dp, border, shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** The gradient square that fronts every deck. */
@Composable
fun DeckTile(emoji: String, accent: Accent, size: Int = 48) {
    val shape = RoundedCornerShape((size / 4).dp)
    Box(
        Modifier
            .size(size.dp)
            .clip(shape)
            .background(accent.tile)
            .border(1.dp, accent.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size / 2.4f).sp)
    }
}

/** Thin gradient progress bar. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Int = 8,
    brush: Brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)),
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(shape)
                .background(brush)
        )
    }
}

/** Centred empty state with an emoji, a headline and optional actions. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    // RowScope so callers can use Modifier.weight on their buttons.
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 40.sp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (actions != null) {
                Row(
                    Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) { actions() }
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeonCyan)
    }
}

/** Dim caption text. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = TextMuted) {
    Text(
        text,
        style = LocalTextStyle.current.copy(fontSize = 11.sp),
        color = color,
        modifier = modifier,
    )
}

/** Absolute difference helper used by a couple of layout calculations. */
fun Float.absDiff(other: Float) = (this - other).absoluteValue
