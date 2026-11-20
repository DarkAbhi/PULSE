package dev.abhishekan.pulse.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseNavigationRail(
    items: List<PulseNavigationItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    val itemShape = RoundedCornerShape(spacing.cardPadding)
    val shape = RoundedCornerShape(spacing.target)
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    val selectedBounds = bounds[selectedIndex].takeIf { selectedIndex in items.indices }
    val motion = tween<Float>(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing)
    val indicatorTop by animateFloatAsState(selectedBounds?.top ?: 0f, motion, label = "Rail selection position")
    val indicatorHeight by animateFloatAsState(selectedBounds?.height ?: 0f, motion, label = "Rail selection height")

    Box(
        modifier
            .width(96.dp)
            .animateContentSize(tween(PulseTokens.MOTION_MILLIS, easing = FastOutSlowInEasing))
            .shadow(
                PulseTheme.elevation.high,
                shape,
                ambientColor = colors.textPrimary.copy(alpha = 0.08f),
                spotColor = colors.textPrimary.copy(alpha = 0.08f),
            ).background(colors.surface, shape)
            .clip(shape)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.compact, vertical = spacing.medium),
    ) {
        Box(
            Modifier.drawBehind {
                if (selectedBounds != null) {
                    drawRoundRect(
                        colors.primary,
                        topLeft = Offset(0f, indicatorTop),
                        size = Size(size.width, indicatorHeight),
                        cornerRadius = CornerRadius(spacing.cardPadding.toPx()),
                    )
                    drawCircle(
                        colors.primary,
                        radius = 3.dp.toPx(),
                        center = Offset(-8.dp.toPx(), indicatorTop + indicatorHeight / 2),
                    )
                }
            },
        ) {
            Column(
                Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val focused by interaction.collectIsFocusedAsState()
                    val contentColor by animateColorAsState(
                        if (selected) colors.onPrimary else colors.textSecondary,
                        tween(PulseTokens.MOTION_MILLIS),
                        label = "Rail item color",
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.spacious)
                            .onGloballyPositioned { bounds[index] = it.boundsInParent() }
                            .clip(itemShape)
                            .border(
                                PulseTokens.focusWidth,
                                if (focused) colors.outline else Color.Transparent,
                                itemShape,
                            ).background(
                                if (pressed) colors.textPrimary.copy(alpha = 0.08f) else Color.Transparent,
                            ).selectable(
                                selected = selected,
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelected(index) },
                            ).padding(horizontal = spacing.tiny, vertical = spacing.small),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.tiny, Alignment.CenterVertically),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            Box(Modifier.size(spacing.cardPadding), contentAlignment = Alignment.Center) { item.icon() }
                        }
                        BasicText(
                            item.label,
                            style =
                                PulseTheme.typography.label2.copy(
                                    color = contentColor,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                ),
                        )
                    }
                }
            }
        }
    }
}
