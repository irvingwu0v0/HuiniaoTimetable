package com.irvingwu.huiniaotable.ui

import com.irvingwu.huiniaotable.ui.icons.AppIcons

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.irvingwu.huiniaotable.ui.compat.LinearProgressIndicator
import com.irvingwu.huiniaotable.ui.compat.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONTokener
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.SchoolFamily
import com.irvingwu.huiniaotable.t

/**
 * 网页导入入口：在 WebView 里正常登录教务系统（验证码 / SSO / RSA 全部交给网页处理），
 * 打开课表页面后把整页 HTML 交给 [com.irvingwu.huiniaotable.data.KbParser] 解析。
 *
 * 这样既不需要在本地保存账号密码，也不依赖某一所学校的登录协议。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebImportScreen(
    state: AppState,
    onBack: () -> Unit,
    errorMessage: String? = null,
    onImported: (html: String, baseUrl: String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(state.jwxtBaseUrl) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var detected by remember { mutableStateOf<SchoolFamily?>(null) }
    var autoJumped by remember { mutableStateOf(false) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onBack()
    }

    fun load(target: String) {
        val normalized = target.trim().let { if (it.startsWith("http")) it else "https://$it" }
        if (normalized.isBlank() || normalized == "https://") return
        url = normalized
        val view = webView
        if (view == null) {
            loadedUrl = normalized
        } else {
            view.loadUrl(normalized)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = t("网页登录导入", "Sign in with web page"),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, t("返回", "Back"))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webView?.evaluateJavascript(AUTO_OPEN_TIMETABLE_JS, null) },
                        enabled = webView != null && !loading,
                    ) {
                        Icon(AppIcons.EventNote, t("跳到课表", "Go to timetable"))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(t("教务系统网址", "Academic system URL")) },
                    placeholder = { Text("https://jwxt.example.edu.cn/jsxsd/") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { load(url) }),
                )
                Button(onClick = { load(url) }) {
                    Text(t("打开", "Open"))
                }
            }

            Text(
                text = buildString {
                    append(
                        t(
                            "登录完成后请停留在「课表」页面，再点下面的导入按钮。",
                            "After signing in, stay on the timetable page, then tap Import.",
                        ),
                    )
                    detected?.let { append(t(" 已识别：${familyLabel(it)}", " Detected: ${familyLabel(it)}")) }
                },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            errorMessage?.let { err ->
                Surface(
                    color = MiuixTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = err,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
            ) {
                val target = loadedUrl
                if (target == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(AppIcons.Public, null)
                        Text(
                            text = t("输入学校教务系统网址后打开", "Enter your school's URL to begin"),
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        loading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        loading = false
                                        if (view == null) return
                                        view.evaluateJavascript(DETECT_SCHOOL_JS) { raw ->
                                            val family = familyOf(decodeJavascriptString(raw))
                                            detected = family?.takeIf { it != SchoolFamily.Generic }
                                            // 登录成功、但当前还不在课表页时，自动跳到课表入口。
                                            if (!autoJumped && family != null) {
                                                view.postDelayed({
                                                    view.evaluateJavascript(AUTO_JUMP_JS) { result ->
                                                        val status = decodeJavascriptString(result)
                                                        if (status == "jsxsd" || status == "jwglxt") {
                                                            autoJumped = true
                                                        }
                                                    }
                                                }, 1200)
                                            }
                                        }
                                    }
                                }
                                webView = this
                                loadUrl(target)
                            }
                        },

                    )
                }
            }

            Button(
                onClick = {
                    val view = webView ?: return@Button
                    view.evaluateJavascript("document.documentElement.outerHTML") { raw ->
                        val html = decodeJavascriptString(raw)
                        if (html.isNullOrBlank()) {
                            onImported("", url)
                        } else {
                            onImported(html, view.url ?: url)
                        }
                    }
                },
                enabled = webView != null && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
            ) {
                Text(t("导入当前页面课表", "Import timetable from this page"))
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 按页面特征判断教务系统族，用于显示与自动跳转。
 * 强智：/jsxsd/ 路径、LoginToXkLdap 表单或 kbcontent 课表格子；
 * 正方：jwglxt 路径、SafeCodeImg 验证码或 kbtable 课表；
 * 青果：kingosoft 特征或 /xsd/ 路径。
 */
private const val DETECT_SCHOOL_JS = """
(function () {
  var href = location.href || '';
  var html = (document.documentElement && document.documentElement.outerHTML) || '';
  var all = href + html;
  if (/kbcontent|jsxsd|LoginToXkLdap/i.test(all)) return 'qiangzhi';
  if (/kingosoft|\/xsd\//i.test(all)) return 'kingosoft';
  if (/jwglxt|SafeCodeImg|kbtable|zhengfang/i.test(all)) return 'zhengfang';
  return 'unknown';
})()
"""

private fun familyOf(key: String?): SchoolFamily? = when (key) {
    "qiangzhi" -> SchoolFamily.Qiangzhi
    "zhengfang" -> SchoolFamily.Zhengfang
    "kingosoft" -> SchoolFamily.Kingosoft
    else -> null
}

private fun familyLabel(family: SchoolFamily): String = when (family) {
    SchoolFamily.Qiangzhi -> t("强智教务", "Qiangzhi")
    SchoolFamily.Zhengfang -> t("正方教务", "Zhengfang")
    SchoolFamily.Kingosoft -> t("青果教务", "Kingosoft")
    SchoolFamily.Generic -> t("通用教务", "Generic")
}

/**
 * 登录后自动前往课表页：
 * 1. 已经在课表页（页面里能看到课表格子）就直接返回，不打断用户；
 * 2. 强智 / 正方的课表入口路径固定，直接跳转比模拟点击可靠；
 * 3. 其他情况交给「跳到课表」按钮的按文字查找逻辑。
 */
private const val AUTO_JUMP_JS = """
(function () {
  if (document.querySelector('table#timetable, table#kbtable, table#Table1, table#table1, div.kbcontent, div.kbcontent1')) return 'already';
  if (document.querySelector('input[type=password], form#loginForm, #SafeCodeImg')) return 'login';
  var href = location.href || '';
  if (/\/jsxsd\//i.test(href)) {
    location.href = href.replace(/\/jsxsd\/.*$/, '/jsxsd/xskb/xskb_list.do');
    return 'jsxsd';
  }
  if (/\/jwglxt\//i.test(href)) {
    location.href = href.replace(/\/jwglxt\/.*$/, '/jwglxt/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151');
    return 'jwglxt';
  }
  return 'none';
})()
"""

/**
 * 不区分学校：在页面里按文字找「课表」相关入口并点击。
 * 强智 / 正方 / 青果 的课表入口文案都是「课表」「课程表」「学生课表」之类，
 * 比写死 URL 路径可靠得多。
 */
private const val AUTO_OPEN_TIMETABLE_JS = """
(function () {
  if (document.querySelector('input[type=password], form#loginForm, #SafeCodeImg')) return 'login';
  var pattern = /课表|课程表|我的课表|学生课表|班级课表|学期课表/;
  var nodes = Array.from(document.querySelectorAll('a,button,li,span,div'));
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    var text = (node.textContent || '').trim();
    if (!text || text.length > 16 || !pattern.test(text)) continue;
    if (node.tagName === 'A' || node.tagName === 'BUTTON') { node.click(); return 'clicked'; }
    var link = node.closest('a') || node.querySelector('a');
    if (link) { link.click(); return 'clicked-link'; }
  }
  // 兜底：强智系课表页路径固定为 <上下文>/xskb/xskb_list.do
  if (/\/jsxsd\//i.test(location.href)) {
    location.href = location.href.replace(/\/jsxsd\/.*$/, '/jsxsd/xskb/xskb_list.do');
    return 'fallback-jsxsd';
  }
  return 'not-found';
})()
"""

/** evaluateJavascript 返回的是 JSON 编码后的字符串字面量，这里还原成真实 HTML。 */
private fun decodeJavascriptString(raw: String?): String? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return runCatching {
        val value = JSONTokener(raw).nextValue()
        if (value is String) value else null
    }.getOrNull()
}
