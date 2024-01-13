package com.aprz.wechatredenvelope.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.aprz.wechatredenvelope.App
import com.aprz.wechatredenvelope.Controller
import com.aprz.wechatredenvelope.MainActivity
import com.aprz.wechatredenvelope.R
import com.aprz.wechatredenvelope.utils.shortToast


class RedEnvelopeService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "money"
        private const val NOTIFICATION_ID = 10000

        const val CHAT_LIST_ID = "com.tencent.mm:id/bme"
        const val RED_ENVELOPE_VIEW_ID = "com.tencent.mm:id/bhx"

        const val RED_ENVELOPE_OPEN_BUTTON_VIEW_ID1 = "com.tencent.mm:id/it9"
        const val RED_ENVELOPE_OPEN_BUTTON_VIEW_ID2 = "com.tencent.mm:id/it8"
        const val RED_ENVELOPE_CLOSE_VIEW_ID = "com.tencent.mm:id/it7"
        const val LUCKY_MONEY_NOT_HOOK_RECEIVE_UI =
            "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI"
        const val LUCKY_MONEY_DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"
    }

    private var windowNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        shortToast("无障碍服务启动成功")
        showNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        windowNode = rootInActiveWindow
        if (windowNode == null || !Controller.enable) {
            return
        }

        clickNotOpenedRedEnvelope()
        clickOpenRedEnvelopeButton()
        quitLuckyMoneyDetailUI(event)
    }

    private fun quitLuckyMoneyDetailUI(event: AccessibilityEvent) {
        if (LUCKY_MONEY_DETAIL_UI == event.className) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }

    private fun clickOpenRedEnvelopeButton() {
        windowNode ?: return
        // not in chat list
        val openButton1 =
            windowNode?.findAccessibilityNodeInfosByViewId(RED_ENVELOPE_OPEN_BUTTON_VIEW_ID1)
        if (openButton1.isNullOrEmpty()) {
            return
        }
        val openButton2 =
            windowNode?.findAccessibilityNodeInfosByViewId(RED_ENVELOPE_OPEN_BUTTON_VIEW_ID2)
        if (openButton2.isNullOrEmpty()) {
            return
        }
        performViewClick(openButton1[0])
        performViewClick(openButton2[0])
    }

    /**
     * 找到所有没有打开的红包
     */
    private fun clickNotOpenedRedEnvelope() {
        windowNode ?: return
        // not in chat list
        val chatList = windowNode?.findAccessibilityNodeInfosByViewId(CHAT_LIST_ID)
        if (chatList.isNullOrEmpty()) {
            return
        }
        val redEnvelops =
            windowNode?.findAccessibilityNodeInfosByViewId(RED_ENVELOPE_VIEW_ID)
        if (redEnvelops.isNullOrEmpty()) {
            return
        }
        val first = redEnvelops.filter {
            val isRedEnvelop = isRedEnvelop(it)
            val opened = isRedEnvelopOpened(it)
            Log.e("status", "isRedEnvelop = ${isRedEnvelop}, opened = $opened")
            isRedEnvelop && !opened
        }
        if (first.isEmpty()) {
            return
        }
        performViewClick(first[0])
    }

    private fun performViewClick(nodeInfo: AccessibilityNodeInfo) {
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun isRedEnvelop(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean {
        val childCount = accessibilityNodeInfo.childCount
        for (i in 0 until childCount) {
            val child = accessibilityNodeInfo.getChild(i)
            if (child?.text.toString() == "微信红包") {
                return true
            }
        }
        return false
    }

    private fun isRedEnvelopOpened(accessibilityNodeInfo: AccessibilityNodeInfo): Boolean {
        val childCount = accessibilityNodeInfo.childCount
        for (i in 0 until childCount) {
            val child = accessibilityNodeInfo.getChild(i)
            if (child?.text.toString() == "已领取" || child?.text.toString() == "已被领完") {
                return true
            }
        }
        return false
    }


    /**
     * (required) This method is called when the system wants to interrupt the feedback
     * your service is providing, usually in response to a user action such as moving
     * focus to a different control.
     * This method may be called many times over the lifecycle of your service.
     */
    override fun onInterrupt() {
        shortToast("抢红包辅助服务已断开")
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun showNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "抢红包服务运行状态",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
            Notification.Builder(App.instance, CHANNEL_ID)
        } else {
            Notification.Builder(App.instance)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = builder.setContentIntent(pendingIntent)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.ic_launcher
                )
            ) // set the large icon in the drop down list.
            .setContentTitle(getString(R.string.app_name)) // set the caption in the drop down list.
            .setSmallIcon(R.mipmap.ic_launcher_round) // set the small icon in state.
            .setContentText(getString(R.string.content_notification)) // set context content.
            .setWhen(System.currentTimeMillis()) // set the time for the notification to occur.
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

}