package com.irvingwu.huiniaotable.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.irvingwu.huiniaotable.data.AppState
import com.irvingwu.huiniaotable.data.KbParser
import com.irvingwu.huiniaotable.data.TimetableFile
import com.irvingwu.huiniaotable.t
import com.irvingwu.huiniaotable.ui.icons.AppIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebImportScreen(state: AppState, onBack: () -> Unit, onSave: suspend (KbParser.Parsed, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var address by rememberSaveable { mutableStateOf(state.jwxtBaseUrl) }
    var site by rememberSaveable { mutableStateOf<String?>(null) }
    var host by remember { mutableStateOf("") }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<KbParser.Parsed?>(null) }
    fun read(parse: () -> KbParser.Parsed) {
        busy = true
        error = null
        scope.launch {
            try { preview = withContext(Dispatchers.IO) { parse() } }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error = failure.localizedMessage ?: t("无法读取课表", "Could not read timetable") }
            finally { busy = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) read {
            context.contentResolver.openInputStream(uri)?.use { TimetableFile.parse(it) }
                ?: error(t("文件无法打开", "Could not open file"))
        }
    }
    fun back() {
        when {
            busy -> Unit
            preview != null -> preview = null
            browser?.canGoBack() == true -> browser?.goBack()
            site != null -> { site = null; error = null }
            else -> onBack()
        }
    }
    BackHandler { back() }
    Scaffold(topBar = {
        TopAppBar(title = if (site == null) t("导入课表", "Import timetable") else t("教务网页", "School portal"),
            subtitle = if (site != null) host else "",
            navigationIcon = { IconButton(onClick = ::back) { Icon(AppIcons.ArrowBack, t("返回", "Back")) } })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            error?.let { Text(it, color = MiuixTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            val target = site
            if (target == null) Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SmallTitle(text = t("学校教务系统", "School portal"))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(value = address, onValueChange = { address = it }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), label = t("学校网址", "School URL"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                        Text(t("登录后打开学期课表，再读取当前页面。", "Sign in and open the semester timetable before reading the page."),
                            style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        TextButton(text = t("打开教务网页", "Open school portal"), colors = ButtonDefaults.textButtonColorsPrimary(), enabled = address.isNotBlank() && !busy,
                            onClick = {
                                val input = address.trim()
                                val uri = Uri.parse(if ("://" in input) input else "https://$input")
                                if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) {
                                    error = t("请输入学校的 HTTPS 网址；旧版 HTTP 系统可用文件导入。", "Enter an HTTPS URL, or import a file from an HTTP-only portal.")
                                } else { error = null; host = uri.host.orEmpty(); site = uri.toString() }
                            }, modifier = Modifier.fillMaxWidth())
                    }
                }
                SmallTitle(text = t("本地文件", "Local file"))
                Card(Modifier.fillMaxWidth()) {
                    ArrowPreference(title = t("选择课表文件", "Choose a timetable file"), summary = "HTML / XLSX",
                        startAction = { PreferenceIcon(AppIcons.FileUpload) },
                        enabled = !busy, onClick = { picker.launch(arrayOf("text/html", "application/xhtml+xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) })
                }
            } else {
                AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                host = url?.let { Uri.parse(it).host }.orEmpty(); loading = true; error = null
                            }
                            override fun onPageFinished(view: WebView, url: String?) { loading = false }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                if (request.isForMainFrame) { error = failure.description.toString(); loading = false }
                            }
                        }
                        browser = this
                        loadUrl(target)
                    }
                }, onRelease = { browser = null; it.stopLoading(); it.destroy() })
                TextButton(text = t("读取当前课表", "Read timetable"), colors = ButtonDefaults.textButtonColorsPrimary(), enabled = !loading && !busy && browser != null,
                    modifier = Modifier.fillMaxWidth().padding(12.dp), onClick = {
                        busy = true
                        browser?.evaluateJavascript(CAPTURE_TABLES) { raw ->
                            read {
                                val html = JSONTokener(raw).nextValue() as? String
                                require(!html.isNullOrEmpty()) { t("未找到可读取的课表，请打开学期课表或导入导出文件。", "Open the semester timetable or import an exported file.") }
                                KbParser.parse(html)
                            }
                        }
                    })
            }
        }
    }
    preview?.let { parsed ->
        val schedule = parsed.schedule
        WindowDialog(show = true, title = t("确认导入", "Import timetable?"), onDismissRequest = { if (!busy) preview = null }) {
            val names = schedule.courses.map { it.name }.distinct()
            Text(t("${names.size} 门课程 · 至第 ${schedule.lastTeachingWeek} 周", "${names.size} courses · through week ${schedule.lastTeachingWeek}"))
            Text(names.take(3).joinToString("、"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(vertical = 8.dp))
            if (state.schedule != null) Text(t("确认后替换本机课表。", "This replaces the timetable on this device."))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(text = t("取消", "Cancel"), enabled = !busy, onClick = { preview = null }, modifier = Modifier.weight(1f))
                TextButton(text = t("导入", "Import"), colors = ButtonDefaults.textButtonColorsPrimary(), enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try { onSave(parsed, site?.let { Uri.parse(it).buildUpon().clearQuery().fragment(null).build().toString() }.orEmpty()); onBack() }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (failure: Exception) { error = failure.localizedMessage; preview = null }
                        finally { busy = false }
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

private const val CAPTURE_TABLES = """
(function() {
  const parts = [];
  function collect(doc) {
    for (const el of doc.querySelectorAll('table,select')) {
      if (el.parentElement?.closest('table') && el.tagName === 'TABLE') continue;
      if (el.tagName === 'SELECT') {
        const copy = el.cloneNode(true);
        Array.from(copy.options).forEach((o, i) => o.toggleAttribute('selected', el.options[i].selected));
        parts.unshift(copy.outerHTML);
      } else parts.push(el.outerHTML);
    }
    for (const frame of doc.querySelectorAll('iframe,frame')) {
      try { if (frame.contentDocument) collect(frame.contentDocument); } catch (_) {}
    }
  }
  collect(document);
  return parts.join('');
})()
"""
