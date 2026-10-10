package com.irvingwu.huiniaotable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.irvingwu.huiniaotable.data.TimetableRepository
import com.irvingwu.huiniaotable.notify.ClassNotifier
import com.irvingwu.huiniaotable.ui.HuiniaoApp
import com.irvingwu.huiniaotable.widget.TimetableWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TimetableRepository.init(applicationContext)
        setContent {
            HuiniaoApp()
        }
    }

    override fun onStart() {
        super.onStart()
        // 回到前台时顺手刷新桌面控件与上课通知，保证与 App 内课表一致。
        TimetableWidgetProvider.updateAll(this)
        ClassNotifier.refresh(this)
    }
}
