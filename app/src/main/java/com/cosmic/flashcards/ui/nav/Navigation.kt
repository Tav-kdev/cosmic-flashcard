package com.cosmic.flashcards.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic.flashcards.ui.theme.GlassFill
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPink
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.Space950
import com.cosmic.flashcards.ui.theme.TextSecondary

/** Every destination in the app. Card/deck/tag editors take an id argument. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val CARDS = "cards"
    const val DECKS = "decks"
    const val TAGS = "tags"
    const val IMPORT = "import"
    const val STUDY_HOME = "study"
    const val STUDY_SESSION = "study/session"
    const val STUDY_DONE = "study/done"
    const val EXAM_SETUP = "study/exam"

    const val CARD_EDIT = "card/edit"
    fun cardEdit(id: Long) = "$CARD_EDIT/$id"

    const val DECK_EDIT = "deck/edit"
    fun deckEdit(id: Long) = "$DECK_EDIT/$id"

    const val TAG_EDIT = "tag/edit"
    fun tagEdit(id: Long) = "$TAG_EDIT/$id"
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val BOTTOM_ITEMS = listOf(
    NavItem(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    NavItem(Routes.DECKS, "Decks", Icons.Outlined.List),
    NavItem(Routes.TAGS, "Tags", Icons.Outlined.Menu),
    NavItem(Routes.IMPORT, "Import", Icons.Outlined.Add),
)

/**
 * The bottom bar, with the elevated Study button in the middle — the same
 * shape as the web app's mobile nav.
 */
@Composable
fun CosmicBottomBar(
    currentRoute: String?,
    dueCount: Int,
    onNavigate: (String) -> Unit,
    onStudy: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(GlassFill)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            )
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab(BOTTOM_ITEMS[0], currentRoute, onNavigate)
            BottomTab(BOTTOM_ITEMS[1], currentRoute, onNavigate)
            StudyButton(dueCount = dueCount, onClick = onStudy)
            BottomTab(BOTTOM_ITEMS[2], currentRoute, onNavigate)
            BottomTab(BOTTOM_ITEMS[3], currentRoute, onNavigate)
        }
    }
}

@Composable
private fun BottomTab(item: NavItem, currentRoute: String?, onNavigate: (String) -> Unit) {
    val active = currentRoute == item.route
    val tint by animateFloatAsState(if (active) 1f else 0f, label = "tab")
    val color = lerpColor(TextSecondary, NeonCyan, tint)

    Column(
        Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigate(item.route) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(24.dp))
        Text(item.label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StudyButton(dueCount: Int, onClick: () -> Unit) {
    Column(
        Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .offset(y = (-14).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple)))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Study",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (dueCount > 0) {
                Box(
                    Modifier
                        .offset(y = (-18).dp, x = 4.dp)
                        .clip(CircleShape)
                        .background(NeonPink)
                        .border(2.dp, Space950, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (dueCount > 99) "99+" else dueCount.toString(),
                        color = Space950,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            "Study",
            color = NeonCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(y = (-10).dp),
        )
    }
}

/** Minimal colour interpolation — avoids pulling in a graphics helper. */
private fun lerpColor(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = from.alpha + (to.alpha - from.alpha) * t,
)

/** Shared top bar: the COSMIC wordmark plus an optional back affordance. */
@Composable
fun CosmicTopBar(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GlassFill)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = TextSecondary, fontSize = 18.sp)
            }
            Box(Modifier.width(12.dp))
        } else {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
                contentAlignment = Alignment.Center,
            ) {
                Text("✦", color = Color.White, fontSize = 15.sp)
            }
            Box(Modifier.width(12.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(
                title ?: "COSMIC",
                color = Color.White,
                fontSize = if (title == null) 16.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (title == null) com.cosmic.flashcards.ui.theme.Display else null,
            )
            Text(
                subtitle ?: "Flashcards",
                color = TextSecondary,
                fontSize = 11.sp,
            )
        }

        trailing?.invoke()
    }
}

/** Height reserved so content can scroll clear of the elevated study button. */
val BottomBarHeight = 88.dp

@Composable
fun BottomSpacer() {
    Box(Modifier.height(BottomBarHeight))
}
