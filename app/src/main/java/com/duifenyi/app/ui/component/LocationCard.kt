package com.duifenyi.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duifenyi.app.ui.theme.*

/**
 * 地址/定位控制卡片 — 从已配置地址中选择签到坐标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCard(
    locationNames: List<String>,
    selectedName: String,
    lng: String,
    lat: String,
    note: String,
    onLocationSelected: (String) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "签到地址",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                TextButton(onClick = onConfigureClick) {
                    Text(
                        text = "配置",
                        color = Primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 地址选择下拉框
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedName.ifEmpty {
                        if (locationNames.isEmpty()) "暂无已配置地址" else "请选择签到地址"
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = Primary
                    ),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    locationNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                onLocationSelected(name)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 坐标展示
            if (selectedName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("地址", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(selectedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("经度", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(lng.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("纬度", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(lat.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
                if (note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            } else if (locationNames.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "点击「配置」添加签到地址，未配置时将使用默认坐标",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
