package com.duifenyi.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duifenyi.app.data.model.LoginState
import com.duifenyi.app.ui.theme.*
import com.duifenyi.app.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val loginState by viewModel.repository.loginState.collectAsState()
    val showPasteDialog by viewModel.showPasteDialog.collectAsState()
    var manualCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // ═══ 稳定引用 onLoginSuccess ═══
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)

    // ═══ 登录状态监听 → 立即跳转 + Toast 提示 ═══
    LaunchedEffect(Unit) {
        snapshotFlow { loginState }
            .collect { state ->
                when (state) {
                    LoginState.LoggedIn -> {
                        currentOnLoginSuccess()
                        Toast.makeText(context, "✅ 登录成功", Toast.LENGTH_SHORT).show()
                    }
                    is LoginState.Error -> {
                        Toast.makeText(context, "❌ ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
    }

    // ═══ 剪贴板监听 ═══
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    DisposableEffect(Unit) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            try {
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString() ?: ""
                    val code = viewModel.repository.extractWeChatCode(text)
                    if (code != null) {
                        viewModel.loginWithCode(code)
                        Toast.makeText(context, "检测到微信授权码，正在登录...", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) { }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose { clipboardManager.removePrimaryClipChangedListener(listener) }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ 顶部标题 ═══
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Primary,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "对分易签到助手",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持数字码 · 二维码 · 定位签到",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══ 微信登录卡片 ═══
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = WeChatGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "微",
                                style = MaterialTheme.typography.headlineMedium,
                                color = OnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "微信扫码登录",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "使用微信授权登录对分易平台\n自动完成认证，无需输入密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ═══ 微信登录按钮 ═══
                    Button(
                        onClick = {
                            val url = viewModel.getWeChatAuthUrl()
                            // 复制URL到剪贴板
                            val clip = ClipData.newPlainText("auth_url", url)
                            clipboardManager.setPrimaryClip(clip)

                            // 方式1: 分享到微信（最可靠）
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                    setPackage("com.tencent.mm")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(shareIntent)
                                Toast.makeText(context, "请发送给「文件传输助手」后点击链接授权", Toast.LENGTH_LONG).show()
                            } catch (e1: Exception) {
                                // 方式2: 微信未安装 → 浏览器打开
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(context, "已打开浏览器，请在微信中完成授权", Toast.LENGTH_LONG).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "已复制链接，请手动在微信中打开", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WeChatGreen,
                            contentColor = OnPrimary
                        ),
                        enabled = loginState != LoginState.LoggingIn,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (loginState == LoginState.LoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (loginState == LoginState.LoggingIn) "登录中..." else "微信登录",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ═══ 手动粘贴链接按钮 ═══
                    OutlinedButton(
                        onClick = {
                            viewModel.togglePasteDialog()
                            try {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    manualCode = text
                                }
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "手动粘贴授权链接",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══ 手动输入对话框 ═══
            if (showPasteDialog) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "粘贴微信授权链接",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "粘贴微信授权链接到这里...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = BorderColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.togglePasteDialog()
                                    manualCode = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("取消")
                            }

                            Button(
                                onClick = {
                                    val code = viewModel.repository.extractWeChatCode(manualCode)
                                    if (code != null) {
                                        viewModel.loginWithCode(code)
                                        viewModel.togglePasteDialog()
                                        manualCode = ""
                                    } else {
                                        Toast.makeText(context, "未检测到有效的授权码", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = manualCode.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("确认登录")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══ 使用说明 ═══
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. 点击「微信登录」，选择分享到微信\n" +
                                "2. 发送给「文件传输助手」\n" +
                                "3. 在微信中点击链接完成授权\n" +
                                "4. 授权后复制浏览器地址栏中的链接\n" +
                                "5. 返回本应用，自动检测并登录\n\n" +
                                "若无法自动检测，请使用手动粘贴",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
