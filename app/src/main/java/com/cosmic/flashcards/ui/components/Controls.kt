package com.cosmic.flashcards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic.flashcards.ui.theme.GlassBorder
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.Rose
import com.cosmic.flashcards.ui.theme.Space950
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextPrimary
import com.cosmic.flashcards.ui.theme.TextSecondary

/** Filled gradient button — the primary action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                else
                    Brush.horizontalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.08f)))
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Space950,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextMuted,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** Subtle bordered button for secondary actions. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.06f),
            contentColor = tint,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        Text(text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Rose.copy(alpha = 0.12f),
            contentColor = Rose,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier.border(1.dp, Rose.copy(alpha = 0.35f), shape),
    ) {
        Text(text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

/** Themed text field matching the web app's `.field` styling. */
@Composable
fun CosmicField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Column(modifier) {
        if (label != null) {
            SectionLabel(label, Modifier.padding(bottom = 6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = TextMuted, fontSize = 14.sp) } },
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Space950.copy(alpha = 0.55f),
                unfocusedContainerColor = Space950.copy(alpha = 0.45f),
                errorContainerColor = Space950.copy(alpha = 0.45f),
                focusedBorderColor = NeonCyan.copy(alpha = 0.55f),
                unfocusedBorderColor = GlassBorder,
                errorBorderColor = Rose.copy(alpha = 0.6f),
                cursorColor = NeonCyan,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier else Modifier.heightIn(min = 120.dp)),
        )
        if (supportingText != null) {
            Text(
                supportingText,
                fontSize = 11.sp,
                color = if (isError) Rose else TextMuted,
                modifier = Modifier.padding(top = 5.dp, start = 2.dp),
            )
        }
    }
}

/** Simple labelled dropdown, used for deck and tag pickers. */
@Composable
fun <T> CosmicDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T?) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    includeNone: Boolean = true,
    noneLabel: String = "— None —",
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Column(modifier) {
        SectionLabel(label, Modifier.padding(bottom = 6.dp))
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Space950.copy(alpha = 0.45f))
                    .border(1.dp, GlassBorder, shape)
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selected == null) noneLabel else optionLabel(selected),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text("▾", color = TextSecondary)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(com.cosmic.flashcards.ui.theme.Space800),
            ) {
                if (includeNone) {
                    DropdownMenuItem(
                        text = { Text(noneLabel, color = TextSecondary) },
                        onClick = { onSelect(null); expanded = false },
                    )
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), color = TextPrimary) },
                        onClick = { onSelect(option); expanded = false },
                    )
                }
            }
        }
    }
}

/** Row of segmented choices — used for scope and state pickers. */
@Composable
fun SegmentedRow(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = NeonPurple,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, label) ->
            val active = key == selectedKey
            Chip(
                text = label,
                color = if (active) accent else TextSecondary,
                fill = if (active) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                border = if (active) accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f),
                onClick = { onSelect(key) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}
