package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.irvingwu.huiniaotable.ui.compat.Button
import com.irvingwu.huiniaotable.ui.compat.Card
import com.irvingwu.huiniaotable.ui.compat.CardDefaults
import com.irvingwu.huiniaotable.ui.compat.CircularProgressIndicator
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.IconButton
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import com.irvingwu.huiniaotable.ui.compat.Scaffold
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.data.AppState

/**
 * 身份鉴权页面：学号与密码身份验证。
 * 符合 Miuix / HyperOS 设计规范：规范化系统用语、应用图标资产、
 * 动态色彩层级、无障碍辅助标签及交互过渡。
 */
@Composable
fun LoginScreen(
    state: AppState,
    onLogin: (String, String) -> Unit,
    onOpenWebImport: () -> Unit = {},
) {
    var jwxtUrl by rememberSaveable { mutableStateOf(state.jwxtBaseUrl) }
    var studentId by rememberSaveable { mutableStateOf(state.studentId) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val submit = {
        if (studentId.isNotBlank() && password.isNotEmpty() && !state.loggingIn) {
            com.irvingwu.huiniaotable.data.TimetableRepository.setJwxtBaseUrl(jwxtUrl.trim()); onLogin(studentId.trim(), password)
        }
    }

    Scaffold(
        containerColor = AppTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // 应用图标容器
            Surface(
                shape = CircleShape,
                color = AppTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 1.dp,
                modifier = Modifier.size(100.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = t("应用图标", "App icon"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = t("登录灰鸟课表", "Sign in to Huiniao"),
                style = AppTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("优雅、高效的高校课表工具", "An elegant, efficient timetable for university"),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            // 鉴权表单容器
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colorScheme.surfaceContainerLow,
                ),
                shape = AppTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                                        OutlinedTextField(
                        value = jwxtUrl,
                        onValueChange = { jwxtUrl = it },
                        label = { Text(t("教务系统网址 (URL)", "Academic system URL")) },
                        placeholder = { Text("https://jwxt.XXXX.edu.cn/") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Language,
                                contentDescription = t("教务网址", "Academic system URL"),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.medium,
                    )

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text(t("统一身份认证学号", "Student ID")) },
                        placeholder = { Text(t("请输入学号", "Enter your student ID")) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Person,
                                contentDescription = t("学号标识", "Student ID field"),
                            )
                        },
                        trailingIcon = {
                            if (studentId.isNotEmpty()) {
                                IconButton(onClick = { studentId = "" }) {
                                    Icon(
                                        imageVector = AppIcons.Clear,
                                        contentDescription = t("清除学号输入", "Clear student ID"),
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(t("认证密码", "Password")) },
                        placeholder = { Text(t("请输入教务系统登录密码", "Enter your academic system password")) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Lock,
                                contentDescription = t("密码标识", "Password field"),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (passwordVisible) {
                                        t("隐藏密码明文", "Hide password")
                                    } else {
                                        t("显示密码明文", "Show password")
                                    },
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 错误信息提示横幅
                    AnimatedVisibility(
                        visible = state.loginError != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        state.loginError?.let { err ->
                            Surface(
                                shape = AppTheme.shapes.small,
                                color = AppTheme.colorScheme.errorContainer,
                                contentColor = AppTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = AppIcons.ErrorOutline,
                                        contentDescription = t("认证错误提示", "Authentication error"),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = err,
                                        style = AppTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = submit,
                        enabled = studentId.isNotBlank() && password.isNotEmpty() && !state.loggingIn,
                        shape = AppTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (state.loggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(t("系统鉴权中…", "Signing in…"), style = AppTheme.typography.labelLarge)
                        } else {
                            Icon(
                                imageVector = AppIcons.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(t("验证并登入", "Verify and sign in"), style = AppTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            top.yukonga.miuix.kmp.basic.TextButton(
                text = t("用网页登录导入课表", "Sign in on the web page instead"),
                onClick = onOpenWebImport,
                modifier = Modifier.fillMaxWidth(),
            )

            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

            // 隐私安全与数据合规说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colorScheme.surfaceContainerLowest,
                ),
                shape = AppTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Security,
                            contentDescription = t("数据安全规范", "Data security"),
                            tint = AppTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = t("凭据安全与合规说明", "Credential security and compliance"),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = t(
                            "• 本地隔离持久化：身份凭据仅通过 Android KeyStore 硬件级安全密钥库（AES-GCM-256）加密持久化于设备私有沙盒。\n" +
                                "• 官方端点通信：认证凭据仅在客户端向您配置的教务系统网址发起 HTTPS 鉴权请求时调用，无任何第三方云端遥测。\n" +
                                "• 只读查询规范：程序仅实现教学课表数据只读同步协议，不包含选课、退课、成绩提交等变更操作权限。",
                            "• Local isolation: credentials are encrypted with Android KeyStore (AES-GCM-256) in the app's private storage.\n" +
                                "• Direct connection: credentials are only sent to the academic system URL you configure over HTTPS. No third-party telemetry.\n" +
                                "• Read-only access: the app only reads timetable data and cannot enroll, drop, or submit grades.",
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
