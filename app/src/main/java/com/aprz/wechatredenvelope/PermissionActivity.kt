package com.aprz.wechatredenvelope

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aprz.wechatredenvelope.databinding.ActivityPermissionBinding
import com.aprz.wechatredenvelope.databinding.ItemPermissionBinding
import com.aprz.wechatredenvelope.utils.shortToast
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.lang.ref.WeakReference

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvPermissionCheckList.layoutManager = LinearLayoutManager(this)

        val permissionDataList = buildData()
        binding.rvPermissionCheckList.adapter = PermissionAdapter(permissionDataList, WeakReference(this))
    }

    private fun buildData(): List<PermissionData> {
        val hasNotificationPermission = XXPermissions.isGranted(
            App.instance,
            Permission.POST_NOTIFICATIONS
        )

        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager.addAccessibilityStateChangeListener {

        }
        accessibilityManager.isEnabled

        return listOf(
            PermissionData(getString(R.string.notification_permission), hasNotificationPermission) {
                XXPermissions.with(it).permission(Permission.POST_NOTIFICATIONS)
                    .request { _, allGranted ->
                        if (!allGranted) {
                            shortToast(getString(R.string.please_grant_notification_permission))
                        }
                    }
            },
            PermissionData(getString(R.string.accessibility_permission), accessibilityManager.isEnabled) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        )
    }

    override fun onResume() {
        super.onResume()
        // just rebuild data
        val permissionDataList = buildData()
        binding.rvPermissionCheckList.adapter = PermissionAdapter(permissionDataList, WeakReference(this))
    }

}

data class PermissionData(
    val textDescription: String,
    val granted: Boolean,
    val requestPermissionAction: (activity: AppCompatActivity) -> Unit
)

class PermissionViewHolder(
    private val binding: ItemPermissionBinding,
    private val hostRef: WeakReference<AppCompatActivity>
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindView(data: PermissionData) {
        binding.tvDesc.text = data.textDescription
        binding.scSwitch.isChecked = data.granted
        binding.scSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (data.granted) {
                shortToast(R.string.permission_granted)
                binding.scSwitch.isChecked = true
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                val activity = hostRef.get() ?: return@setOnCheckedChangeListener
                data.requestPermissionAction(activity)
            }
        }
    }
}

class PermissionAdapter(
    private val buttonData: List<PermissionData>,
    private val hostRef: WeakReference<AppCompatActivity>
) :
    RecyclerView.Adapter<PermissionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val buttonBinding =
            ItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PermissionViewHolder(buttonBinding, hostRef)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        holder.bindView(buttonData[position])
    }

    override fun getItemCount(): Int {
        return buttonData.size
    }

}