package com.duifenyi.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.duifenyi.app.data.model.ManualCheckInAction
import com.duifenyi.app.data.model.ManualCheckInState

@Composable
fun ManualCheckInDialog(
    state: ManualCheckInState,
    onAction: (ManualCheckInAction) -> Unit
) {
    var digits by remember(state.requestId) { mutableStateOf("") }
    var selection by remember(state.requestId, state.gridVersion) { mutableStateOf(emptyList<Int>()) }
    val code = if (state.isHanzi) selection.joinToString("") { state.grid[it].h } else digits
    val canSubmit = !state.isBusy && if (state.isHanzi) selection.size == 4 else digits.length == 4

    AlertDialog(
        // 不允许外部点击误跳过活动；提交过程中也不能重复操作。
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(if (state.isHanzi) "汉字签到" else "数字码签到") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(state.courseName, style = MaterialTheme.typography.titleSmall)
                Text("请使用老师提供的签到内容。建议优先在微信官方页面完成签到。")
                Text(state.message, style = MaterialTheme.typography.bodySmall)
                if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                if (state.isHanzi) {
                    Text("按顺序选择 4 个字：${code.ifEmpty { "尚未选择" }}")
                    state.grid.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { columnIndex, option ->
                                val index = rowIndex * 3 + columnIndex
                                val order = selection.indexOf(index)
                                OutlinedButton(
                                    onClick = {
                                        selection = if (index in selection) selection - index
                                        else selection + index
                                    },
                                    enabled = !state.isBusy && (order >= 0 || selection.size < 4),
                                    modifier = Modifier.weight(1f).heightIn(min = 82.dp),
                                    contentPadding = PaddingValues(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (order >= 0) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(option.h, fontSize = 24.sp)
                                        Text(option.p, fontSize = 12.sp)
                                        if (order >= 0) Text("第 ${order + 1} 个", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                    Row {
                        TextButton(
                            onClick = { selection = selection.dropLast(1) },
                            enabled = !state.isBusy && selection.isNotEmpty()
                        ) { Text("撤销") }
                        TextButton(
                            onClick = { selection = emptyList() },
                            enabled = !state.isBusy && selection.isNotEmpty()
                        ) { Text("清空") }
                        TextButton(
                            onClick = { onAction(ManualCheckInAction.RefreshGrid) },
                            enabled = !state.isBusy
                        ) { Text(if (state.grid.isEmpty()) "重试加载" else "刷新") }
                    }
                } else {
                    OutlinedTextField(
                        value = digits,
                        onValueChange = { value ->
                            if (value.length <= 4 && value.all { it in '0'..'9' }) digits = value
                        },
                        label = { Text("4 位数字码") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !state.isBusy,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ManualCheckInAction.Submit(code)) },
                enabled = canSubmit
            ) { Text("提交签到") }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(ManualCheckInAction.Skip) },
                enabled = !state.isBusy
            ) { Text("跳过本次") }
        }
    )
}
