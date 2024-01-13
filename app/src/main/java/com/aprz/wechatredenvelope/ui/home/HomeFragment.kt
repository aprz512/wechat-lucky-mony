package com.aprz.wechatredenvelope.ui.home

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aprz.wechatredenvelope.App
import com.aprz.wechatredenvelope.Controller
import com.aprz.wechatredenvelope.MainActivity
import com.aprz.wechatredenvelope.PermissionActivity
import com.aprz.wechatredenvelope.R
import com.aprz.wechatredenvelope.databinding.FragmentHomeBinding
import com.aprz.wechatredenvelope.service.RedEnvelopeService
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        initView()

        initListeners()

        showNotification()

        return root
    }

    private fun initView() {
        val hasNotificationPermission = XXPermissions.isGranted(App.instance,
            Permission.POST_NOTIFICATIONS)

        if (hasNotificationPermission) {
            binding.clContainer.isVisible = true
            binding.btnGoToPermission.isVisible = false
        } else {
            binding.clContainer.isVisible = false
            binding.btnGoToPermission.isVisible = true
        }

    }


    private fun initListeners() {
        binding.btnGoToPermission.setOnClickListener {
            startActivity(Intent(activity, PermissionActivity::class.java))
        }
        binding.scWechatSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.lavScan.resumeAnimation()
                binding.scWechatSwitch.setText(R.string.close_wechat_red_envelope_monitor)
            } else {
                binding.lavScan.pauseAnimation()
                binding.scWechatSwitch.setText(R.string.open_wechat_red_envelope_monitor)
            }
            Controller.enable = isChecked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNotification() {
        val manager = requireActivity().getSystemService(AccessibilityService.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RedEnvelopeService.CHANNEL_ID,
                "抢红包服务运行状态",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
            Notification.Builder(App.instance, RedEnvelopeService.CHANNEL_ID)
        } else {
            Notification.Builder(App.instance)
        }
        val notificationIntent = Intent(requireActivity(), MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            requireActivity(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
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

        manager.notify(1000, notification)

//        startForeground(RedEnvelopeService.NOTIFICATION_ID, notification)

    }
}