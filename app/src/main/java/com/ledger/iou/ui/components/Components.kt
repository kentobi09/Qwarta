package com.ledger.iou.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.ui.theme.ColorAccentNegative
import com.ledger.iou.ui.theme.ColorAccentNegativeMuted
import com.ledger.iou.ui.theme.ColorAccentNeutral
import com.ledger.iou.ui.theme.ColorAccentNeutralMuted
import com.ledger.iou.ui.theme.ColorAccentPositive
import com.ledger.iou.ui.theme.ColorAccentPositiveMuted
import com.ledger.iou.ui.theme.ColorBackground
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.ColorTextTertiary
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.theme.TABULAR_NUMERALS_SETTINGS

@Composable
fun HairlineCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    backgroundColor: Color = ColorSurfaceCard,
    borderColor: Color = ColorSurfaceBorder,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableHairlineCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    cornerRadius: Dp = 10.dp,
    backgroundColor: Color = ColorSurfaceCard,
    borderColor: Color = ColorSurfaceBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        content()
    }
}

@Composable
fun AmountText(
    cents: Long,
    isMasked: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = ColorTextPrimary,
    useMonospace: Boolean = false,
    currencySymbol: String = "₱"
) {
    val formatted = LedgerRepository.formatCents(cents, mask = isMasked, currencySymbol = currencySymbol)
    Text(
        text = formatted,
        modifier = modifier,
        style = TextStyle(
            fontFamily = if (useMonospace) FontFamily.Monospace else FontFamily.SansSerif,
            fontWeight = fontWeight,
            fontSize = fontSize,
            fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
            color = color
        )
    )
}

@Composable
fun MicroCapsLabel(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = ColorTextSecondary,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .then(
                if (backgroundColor != Color.Transparent) Modifier.background(
                    backgroundColor,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .then(
                if (borderColor != Color.Transparent) Modifier.border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MicroCapsStyle.copy(color = textColor),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusPill(
    isOverdue: Boolean,
    isSettled: Boolean,
    relativeDueText: String?,
    modifier: Modifier = Modifier
) {
    when {
        isSettled -> {
            MicroCapsLabel(
                text = "SETTLED",
                textColor = ColorAccentPositive,
                backgroundColor = ColorAccentPositiveMuted,
                borderColor = ColorAccentPositive.copy(alpha = 0.3f),
                modifier = modifier
            )
        }
        isOverdue -> {
            MicroCapsLabel(
                text = relativeDueText ?: "OVERDUE",
                textColor = ColorAccentNegative,
                backgroundColor = ColorAccentNegativeMuted,
                borderColor = ColorAccentNegative.copy(alpha = 0.3f),
                modifier = modifier
            )
        }
        relativeDueText != null -> {
            MicroCapsLabel(
                text = relativeDueText,
                textColor = ColorAccentNeutral,
                backgroundColor = ColorAccentNeutralMuted,
                borderColor = ColorSurfaceBorder,
                modifier = modifier
            )
        }
        else -> {
            MicroCapsLabel(
                text = "OPEN",
                textColor = ColorTextSecondary,
                backgroundColor = ColorSurfaceCardElevated,
                borderColor = ColorSurfaceBorder,
                modifier = modifier
            )
        }
    }
}

@Composable
fun DebtorCard(
    personWithTx: PersonWithTransactions,
    isPrivacyMasked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = personWithTx.isOverdue()
    val isSettled = personWithTx.isSettled
    val nextDue = personWithTx.nextUpcomingDueDate()
    val relativeDue = nextDue?.let { LedgerRepository.formatRelativeDue(it) }

    ClickableHairlineCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Initials Avatar + Name & Last activity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Editorial minimal circle monogram
                val initials = personWithTx.person.name
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                    .ifEmpty { "?" }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ColorSurfaceCardElevated, CircleShape)
                        .border(1.dp, ColorSurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ColorTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = personWithTx.person.name,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = ColorTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Updated ${LedgerRepository.formatDate(personWithTx.latestActivityEpoch)}",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right: Amount + Status Pill
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val amountColor = when {
                    isSettled -> ColorAccentPositive
                    isOverdue -> ColorAccentNegative
                    else -> ColorAccentNeutral
                }

                AmountText(
                    cents = personWithTx.balanceCents,
                    isMasked = isPrivacyMasked,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    useMonospace = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                StatusPill(
                    isOverdue = isOverdue,
                    isSettled = isSettled,
                    relativeDueText = relativeDue
                )
            }
        }
    }
}

@Composable
fun SegmentedFilterBar(
    selectedTab: Int,
    tabs: List<Pair<String, Int>>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorSurfaceCard, RoundedCornerShape(8.dp))
            .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, (label, count) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) ColorSurfaceCardElevated else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            1.dp,
                            ColorSurfaceBorderHover,
                            RoundedCornerShape(6.dp)
                        ) else Modifier
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MicroCapsStyle.copy(
                            color = if (isSelected) ColorTextPrimary else ColorTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) ColorSurfaceBorderHover else ColorSurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                                    color = if (isSelected) ColorTextPrimary else ColorTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = ColorSurfaceBorder
) {
    Divider(
        modifier = modifier,
        thickness = 1.dp,
        color = color
    )
}
