package com.genspark.privacyfirstai.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genspark.privacyfirstai.ai.RuntimeBadgeTone

@Composable
fun RuntimeBadge(
    text: String,
    tone: RuntimeBadgeTone,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when (tone) {
        RuntimeBadgeTone.Positive -> colorScheme.primaryContainer
        RuntimeBadgeTone.Neutral -> colorScheme.tertiaryContainer
        RuntimeBadgeTone.Warning -> colorScheme.secondaryContainer
        RuntimeBadgeTone.Negative -> colorScheme.errorContainer
    }
    val contentColor = when (tone) {
        RuntimeBadgeTone.Positive -> colorScheme.onPrimaryContainer
        RuntimeBadgeTone.Neutral -> colorScheme.onTertiaryContainer
        RuntimeBadgeTone.Warning -> colorScheme.onSecondaryContainer
        RuntimeBadgeTone.Negative -> colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
