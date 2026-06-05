package com.duifenyi.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duifenyi.app.data.model.LogLevel
import com.duifenyi.app.ui.component.*
import com.duifenyi.app.ui.theme.*
import com.duifenyi.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToLocationConfig: () -> Unit,
    onLogout: () -> Unit
) {
    val courses by viewModel.repository.courses.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val logs by viewModel.repository.logs.collectAsState()
    val isMonitoring by viewModel.repository.isMonitoring.collectAsState()
    val delaySeconds by viewModel.delaySeconds.collectAsState()
    val selectedLocationName by viewModel.repository.selectedLocationName.collectAsState()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val locationNames = remember { viewModel.getAllLocationNames() }

    // DEBUG: 观察课程列表变化
    LaunchedEffect(courses.size) {
        Log.d("HomeScreen", "courses updated: size=${courses.size}, items=${courses.map { it.courseName }}")
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "对分易签到助手",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimary
                        )
                        if (isMonitoring) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = SuccessGreen
                            ) {
                                Text(
                                    text = "监控中",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary
                ),
                actions = {
                    IconButton(onClick = { scope.launch { viewModel.repository.loadCourses() } }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新课程",
                            tint = OnPrimary.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "退出登录",
                            tint = OnPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // 课程选择 + 延迟 + 控制按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CourseSelector(
                        courses = courses,
                        selectedCourse = selectedCourse,
                        onCourseSelected = { viewModel.selectCourse(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DelayInput(
                        delay = delaySeconds,
                        onDelayChange = { viewModel.setDelay(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 开始/停止按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedCourse != null) {
                                    viewModel.startMonitoring()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = OnPrimary
                            ),
                            enabled = !isMonitoring && selectedCourse != null && courses.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "开始监听",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.stopMonitoring() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = OnPrimary
                            ),
                            enabled = isMonitoring,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "停止",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 地址控制卡片
            val coord = remember(selectedLocationName) {
                if (selectedLocationName.isNotEmpty()) viewModel.getCoordinateForCourse(selectedLocationName)
                else Pair("", "")
            }

            LocationCard(
                locationNames = locationNames,
                selectedName = selectedLocationName,
                lng = coord.first,
                lat = coord.second,
                note = "",
                onLocationSelected = { viewModel.selectLocation(it) },
                onConfigureClick = onNavigateToLocationConfig,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 日志面板
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 300.dp, max = 500.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 日志标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "运行日志",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        TextButton(onClick = { viewModel.repository.clearLogs() }) {
                            Text(
                                text = "清空",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    LogView(
                        logs = logs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
