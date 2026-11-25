package com.irvingwu.huiniaotable.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.irvingwu.huiniaotable.R
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.ui.compat.AppTheme
import com.irvingwu.huiniaotable.ui.compat.Icon
import com.irvingwu.huiniaotable.ui.compat.Scaffold
import com.irvingwu.huiniaotable.ui.compat.Surface
import com.irvingwu.huiniaotable.ui.compat.Text
import com.irvingwu.huiniaotable.ui.icons.AppIcons
import top.yukonga.miuix.kmp.basic.Button

/**
 * 首次启动 / 清空课表后的欢迎页：只保留一条网页登录路径。
 *
 * 登录、验证码、统一身份认证全部在 WebView 里完成，App 不收集也不保存账号密码，
 * 只保留解析后的课表与来源网址。
 */
@Composable
fun WelcomeScreen(onOpenWebImport: () -> Unit) {
    Scaffold(containerColor = AppTheme.colorScheme.surface) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            Surface(
                shape = CircleShape,
                color = AppTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 1.dp,
                modifier = Modifier.size(100.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = t("应用图标", "App icon"),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = t("欢迎使用灰鸟课表", "Welcome to Huiniao"),
                style = AppTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = t(
                    "在网页里登录学校教务系统，登录成功后自动抓取并解析课表。",
                    "Sign in to your academic system in the built-in browser; the timetable is parsed automatically.",
                ),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onOpenWebImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                cornerRadius = 16.dp,
            ) {
                Icon(
                    imageVector = AppIcons.Public,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(t("打开教务系统网页登录", "Open academic system web page"))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = t(
                    "支持验证码、统一身份认证与多校区门户；自动识别强智、正方、青果等常见教务系统。",
                    "Captcha and SSO are handled in the page; Qiangzhi, Zhengfang and Kingosoft are detected automatically.",
                ),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Surface(
                color = AppTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = t("隐私说明", "Privacy"),
                        style = AppTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = t(
                            "• App 不保存学号与密码，登录状态只存在于本次网页会话\n• 课表、作息与假期数据均保存在本机\n• 课表仅在导入或手动更新时读取，不会后台上传",
                            "• No student ID or password is stored; the session lives only in this page\n• Timetable and schedule data stay on this device\n• Data is only read when you import or refresh",
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
