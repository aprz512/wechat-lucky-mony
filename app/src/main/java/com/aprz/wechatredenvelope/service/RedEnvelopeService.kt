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
import com.aprz.wechatredenvelope.App
import com.aprz.wechatredenvelope.Controller
import com.aprz.wechatredenvelope.MainActivity
import com.aprz.wechatredenvelope.R
import com.aprz.wechatredenvelope.utils.shortToast


class RedEnvelopeService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "money"
        private const val NOTIFICATION_ID = 10000

        const val LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI"
        const val LUCKY_MONEY_NOT_HOOK_RECEIVE_UI =
            "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI"
        const val LUCKY_MONEY_DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"

        const val SECRET_CODE = "@!_a5right_$@"
        const val OPEN_TEXT = "开"
    }

    private var windowNode: AccessibilityNodeInfo? = null
    private var chatListViewId: String? = null
    private var redEnvelopeViewId: String? = null
    private var openRedEnvelopeViewId: String? = null
    private var showInitTips = true

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

        Log.e("aprz", "onAccessibilityEvent type = ${event}")

        prepareViewIds(event)
        checkViewIds()

        clickNotOpenedRedEnvelope()
        clickOpenRedEnvelopeButton(event)
        quitLuckyMoneyDetailUI(event)
    }

    private fun checkViewIds() {
        if (chatListViewId != null
            && redEnvelopeViewId != null
            && openRedEnvelopeViewId != null
            && showInitTips
        ) {
            shortToast("环境初始化完毕")
            showInitTips = false
        }
    }

    private fun prepareViewIds(event: AccessibilityEvent) {
        if (chatListViewId == null || redEnvelopeViewId == null) {
            if (LAUNCHER_UI == event.className) {
                setRedEnvelopeViewId()
                setChatListViewId()
            }
        }

        if (openRedEnvelopeViewId == null) {
            if (LUCKY_MONEY_NOT_HOOK_RECEIVE_UI == event.className) {
                setOpenRedEnvelopeViewId()
            }
        }
    }

    private fun setOpenRedEnvelopeViewId() {
        val openRedEnvelopeViewId = findViewIdInWindowNode {
            it.contentDescription == OPEN_TEXT
                    && it.viewIdResourceName?.startsWith("com.tencent.mm:id/", false) == true
        }
        if (openRedEnvelopeViewId.isNullOrEmpty()) {
            Log.e("aprz", "can not find openRedEnvelopeViewId")
        } else {
            this.openRedEnvelopeViewId = openRedEnvelopeViewId
        }
    }

    private fun setRedEnvelopeViewId() {
        val secretCodeViewId = findViewIdInWindowNode {
            Log.e("aprz", "setRedEnvelopeViewId -> ${it.viewIdResourceName} - ${it.text}")
            it.text == SECRET_CODE
                    && it.viewIdResourceName?.startsWith("com.tencent.mm:id/", false) == true
        }
        if (secretCodeViewId.isNullOrEmpty()) {
            Log.e("aprz", "can not find secretCodeViewId")
        } else {
            this.redEnvelopeViewId = secretCodeViewId
        }

        if (redEnvelopeViewId?.isNotEmpty() == true) {
            return
        }

        val redEnvelopeViewId = findViewIdInWindowNode {
            it.text == "微信红包" && it.viewIdResourceName?.startsWith("com.tencent.mm:id/", false) == true
        }
        if (redEnvelopeViewId.isNullOrEmpty()) {
            Log.e("aprz", "can not find redEnvelopeViewId")
        } else {
            this.redEnvelopeViewId = redEnvelopeViewId
        }

        if (redEnvelopeViewId.isNullOrEmpty()) {
            return
        }

//        val childView =
//            windowNode!!.findAccessibilityNodeInfosByViewId(redEnvelopeViewId)[0]
//        childView.parent
    }

    private fun setChatListViewId() {
        if (redEnvelopeViewId.isNullOrEmpty()) {
            return
        }
        val chatListViewId = findViewIdInWindowNode {
            it.className == "androidx.recyclerview.widget.RecyclerView"
                    && it.viewIdResourceName?.startsWith("com.tencent.mm:id/", false) == true
        }
        if (chatListViewId.isNullOrEmpty()) {
            Log.e("aprz", "can not find chatListViewId")
        } else {
            this.chatListViewId = chatListViewId
        }
    }

    private fun quitLuckyMoneyDetailUI(event: AccessibilityEvent) {
        if (LUCKY_MONEY_DETAIL_UI == event.className) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }

    private fun clickOpenRedEnvelopeButton(event: AccessibilityEvent) {
        windowNode ?: return

        if (this.openRedEnvelopeViewId.isNullOrEmpty()) {
            return
        }

        if (LUCKY_MONEY_NOT_HOOK_RECEIVE_UI != event.className) {
            return
        }

        val openViewNodeInfos =
            windowNode?.findAccessibilityNodeInfosByViewId(openRedEnvelopeViewId!!)

        if (openViewNodeInfos.isNullOrEmpty()) {
            return
        }

        performViewClick(openViewNodeInfos[0])
    }

    private fun findViewIdInWindowNode(filter: (AccessibilityNodeInfo) -> Boolean): String? {
        windowNode ?: return null
        return findViewInAccessibilityNodeInfo(filter, windowNode!!)
    }

    private fun findViewInAccessibilityNodeInfo(
        filter: (AccessibilityNodeInfo) -> Boolean,
        nodeInfo: AccessibilityNodeInfo
    ): String? {
        val find = filter(nodeInfo)
        if (!find) {
            val childCount = nodeInfo.childCount
            for (i in 0 until childCount) {
                val child = nodeInfo.getChild(i)
                if (child != null) {
                    val result = findViewInAccessibilityNodeInfo(filter, child)
                    if (result?.isNotEmpty() == true) {
                        return result
                    }
                }
            }
        } else {
            return nodeInfo.viewIdResourceName
        }
        return null
    }

    /**
     * 找到所有没有打开的红包
     */
    private fun clickNotOpenedRedEnvelope() {
        windowNode ?: return

        if (chatListViewId.isNullOrEmpty()) {
            return
        }
        val chatList = windowNode!!.findAccessibilityNodeInfosByViewId(chatListViewId!!)
        if (chatList.isNullOrEmpty()) {
//            Log.e("aprz", "chatList is null")
            return
        }

        if (redEnvelopeViewId.isNullOrEmpty()) {
            return
        }
        val redEnvelops =
            windowNode?.findAccessibilityNodeInfosByViewId(redEnvelopeViewId!!)
        if (redEnvelops.isNullOrEmpty()) {
            Log.e("aprz", "redEnvelops is null")
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
            Log.e("aprz", "isRedEnvelop text = ${child.text}")
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
            Log.e("aprz", "isRedEnvelopOpened text = ${child.text}")
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
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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