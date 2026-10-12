package com.duifenyi.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duifenyi.app.ui.theme.*

/**
 * 签到延迟设置 — 输入框在左，说明文字在右
 */
@Composable
fun DelayInput(
    delay: String,
    onDelayChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = delay,
            onValueChange = { value ->
                if (value.isEmpty() || value.all { it.isDigit() }) {
                    onDelayChange(value)
                }
            },
            label = { Text("签到延迟（秒）") },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                unfocusedBorderColor = BorderColor,
                focusedBorderColor = Primary
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "检测到签到后等待N秒再签\n0 = 立即签到",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
