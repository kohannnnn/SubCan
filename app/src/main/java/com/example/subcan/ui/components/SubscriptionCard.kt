package com.example.subcan.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.subcan.data.db.Subscription
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun SubscriptionCard(subscription: Subscription, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val daysUntilRenewal = ChronoUnit.DAYS.between(
        java.time.LocalDate.now(),
        subscription.nextBillingDate
    ).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                category = subscription.category,
                size = 48
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subscription.billingCycle.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildRenewalText(daysUntilRenewal, subscription),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysUntilRenewal <= 3) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "¥${"%,d".format(subscription.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/${subscription.billingCycle.label.last()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildRenewalText(daysUntil: Int, subscription: Subscription): String {
    val dateStr = subscription.nextBillingDate.format(
        DateTimeFormatter.ofPattern("M/d")
    )
    if (!subscription.autoRenew) {
        return when {
            daysUntil < 0 -> "利用期間終了"
            daysUntil == 0 -> "本日で利用終了"
            else -> "終了予定: $dateStr"
        }
    }

    return when {
        daysUntil < 0 -> "更新日超過"
        daysUntil == 0 -> "🔔 本日更新"
        daysUntil <= 7 -> "🔔 あと${daysUntil}日 ($dateStr)"
        else -> "次回: $dateStr"
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionCardPreview() {
    SubCanTheme {
        Column {
            PreviewData.sampleSubscriptions.take(3).forEach { sub ->
                SubscriptionCard(subscription = sub, onClick = {})
            }
        }
    }
}
