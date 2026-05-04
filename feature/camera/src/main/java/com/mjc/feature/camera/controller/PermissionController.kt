package com.mjc.feature.camera.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 权限控制器，处理相机和音频权限
 */
class PermissionController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val REQUEST_CODE_CAMERA = 1001

        // 所需权限列表
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Initial)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * 检查所有必要权限是否已授予
     */
    fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }.also { hasPermissions ->
            _permissionState.value = if (hasPermissions) {
                PermissionState.Granted(REQUIRED_PERMISSIONS.toList())
            } else {
                PermissionState.Denied(REQUIRED_PERMISSIONS.toList())
            }
        }
    }

    /**
     * 获取需要请求的权限列表
     */
    fun getPermissionsToRequest(): Array<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    /**
     * 请求权限（使用Activity Result API）
     */
    fun requestPermissions(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        permissionsToRequest: Array<String>
    ) {
        if (permissionsToRequest.isEmpty()) {
            _permissionState.value = PermissionState.Granted(REQUIRED_PERMISSIONS.toList())
            return
        }
        _permissionState.value = PermissionState.Requesting(permissionsToRequest.toList())
        permissionLauncher.launch(permissionsToRequest)
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(permissionsResult: Map<String, Boolean>) {
        val grantedPermissions = permissionsResult.filter { it.value }.keys.toList()
        val deniedPermissions = permissionsResult.filter { !it.value }.keys.toList()

        when {
            deniedPermissions.isEmpty() -> {
                _permissionState.value = PermissionState.Granted(grantedPermissions)
            }
            deniedPermissions.any { permission ->
                !activityShouldShowRequestPermissionRationale(permission)
            } -> {
                _permissionState.value = PermissionState.PermanentlyDenied(deniedPermissions)
            }
            else -> {
                _permissionState.value = PermissionState.Denied(deniedPermissions)
            }
        }
    }

    /**
     * 检查是否应该显示权限请求理由
     */
    private fun activityShouldShowRequestPermissionRationale(permission: String): Boolean {
        val activity = context as? ComponentActivity ?: return false
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 检查是否所有权限都被永久拒绝
     */
    fun arePermissionsPermanentlyDenied(): Boolean {
        return permissionState.value is PermissionState.PermanentlyDenied
    }

    /**
     * 打开应用设置页面以手动授予权限
     */
    fun openAppSettings(activity: ComponentActivity) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", activity.packageName, null)
        )
        activity.startActivity(intent)
    }

    /**
     * 权限状态密封类
     */
    sealed class PermissionState {
        object Initial : PermissionState()
        data class Requesting(val permissions: List<String>) : PermissionState()
        data class Granted(val permissions: List<String>) : PermissionState()
        data class Denied(val permissions: List<String>) : PermissionState()
        data class PermanentlyDenied(val permissions: List<String>) : PermissionState()
    }
}