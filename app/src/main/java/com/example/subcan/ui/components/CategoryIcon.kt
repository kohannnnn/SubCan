package com.example.subcan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.subcan.data.model.SubscriptionCategory
import com.example.subcan.ui.theme.SubCanTheme

@Composable
fun CategoryIcon(category: SubscriptionCategory, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(category.color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.label,
            tint = category.color,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
private fun CategoryIconPreview() {
    SubCanTheme {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubscriptionCategory.entries.forEach { cat ->
                CategoryIcon(category = cat, size = 48)
            }
        }
    }
}
