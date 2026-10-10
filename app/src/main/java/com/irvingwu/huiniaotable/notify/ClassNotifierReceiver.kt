package com.irvingwu.huiniaotable.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 上课实时通知的调度入口：
 *  - 内部闹钟（[ClassNotifier.ACTION_REFRESH]）到点后重算状态；
 *  - 开机、改时间、换时区、跨天、应用升级后重建整条提醒链。
 */
class ClassNotifierReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ClassNotifier.ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                try {
                    ClassNotifier.refresh(context.applicationContext)
                } catch (_: Throwable) {}
            }
        }
    }
}
