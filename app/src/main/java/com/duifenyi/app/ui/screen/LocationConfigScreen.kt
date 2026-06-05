package com.duifenyi.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duifenyi.app.data.local.PreferencesManager
import com.duifenyi.app.data.model.LocationConfig
import com.duifenyi.app.ui.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationConfigScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val gson = remember { Gson() }

    var locationsMap by remember {
        mutableStateOf(loadLocationsMap(prefs, gson))
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<Pair<String, LocationConfig>?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Pair<String, LocationConfig>?>(null) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "定位地址配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = OnPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加地址"
                )
            }
        }
    ) { padding ->
        if (locationsMap.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无定位配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角按钮添加课程定位配置\n未配置的课程将使用默认坐标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locationsMap.entries.toList()) { (courseName, config) ->
                    LocationConfigItem(
                        courseName = courseName,
                        config = config,
                        onEdit = {
                            editingEntry = Pair(courseName, config)
                        },
                        onDelete = {
                            showDeleteDialog = Pair(courseName, config)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // 添加/编辑对话框
        if (showAddDialog || editingEntry != null) {
            LocationEditDialog(
                initialName = editingEntry?.first ?: "",
                initialLng = editingEntry?.second?.lng ?: "",
                initialLat = editingEntry?.second?.lat ?: "",
                initialNote = editingEntry?.second?.note ?: "",
                isEditing = editingEntry != null,
                onDismiss = {
                    showAddDialog = false
                    editingEntry = null
                },
                onSave = { name, lng, lat, note ->
                    if (name.isBlank()) {
                        Toast.makeText(context, "课程名称不能为空", Toast.LENGTH_SHORT).show()
                        return@LocationEditDialog
                    }
                    val newMap = locationsMap.toMutableMap()
                    newMap[name] = LocationConfig(lng, lat, note)
                    saveLocationsMap(prefs, gson, newMap)
                    locationsMap = newMap
                    showAddDialog = false
                    editingEntry = null
                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 删除确认对话框
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除配置") },
                text = {
                    Text("确定要删除「${showDeleteDialog!!.first}」的定位配置吗？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val courseName = showDeleteDialog!!.first
                            val newMap = locationsMap.toMutableMap()
                            newMap.remove(courseName)
                            saveLocationsMap(prefs, gson, newMap)
                            locationsMap = newMap
                            showDeleteDialog = null
                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun LocationConfigItem(
    courseName: String,
    config: LocationConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "经度: ${config.lng}  纬度: ${config.lat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (config.note.isNotEmpty()) {
                    Text(
                        text = config.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = Primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = DangerRed
                )
            }
        }
    }
}

@Composable
private fun LocationEditDialog(
    initialName: String,
    initialLng: String,
    initialLat: String,
    initialNote: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, lng: String, lat: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var lng by remember { mutableStateOf(initialLng) }
    var lat by remember { mutableStateOf(initialLat) }
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "编辑定位配置" else "添加定位配置")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("地址名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lng,
                        onValueChange = { lng = it },
                        label = { Text("经度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        label = { Text("纬度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注(教室位置等)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, lng, lat, note) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun loadLocationsMap(
    prefs: PreferencesManager,
    gson: Gson
): Map<String, LocationConfig> {
    return try {
        val json = prefs.loadLocations()
        val type = object : TypeToken<Map<String, LocationConfig>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun saveLocationsMap(
    prefs: PreferencesManager,
    gson: Gson,
    map: Map<String, LocationConfig>
) {
    try {
        val json = gson.toJson(map)
        prefs.saveLocations(json)
    } catch (_: Exception) { }
}
