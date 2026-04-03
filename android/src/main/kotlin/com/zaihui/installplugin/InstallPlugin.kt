package com.zaihui.installplugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.FileNotFoundException

class InstallPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private var apkFile: File? = null
    private var appId: String? = null
    private var methodChannel: MethodChannel? = null
    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null

    companion object {
        private const val installRequestCode = 1234
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "installApk" -> {
                val filePath = call.argument<String>("filePath")
                val appId = call.argument<String>("appId")
                Log.d("android plugin", "installApk $filePath $appId")
                try {
                    installApk(filePath, appId)
                    result.success("Success")
                } catch (e: Throwable) {
                    result.error(e.javaClass.simpleName, e.message, null)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        setupMethodChannel(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        teardownMethodChannel()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        binding.addActivityResultListener { requestCode, resultCode, intent ->
            Log.d(
                "ActivityResultListener",
                "requestCode=$requestCode, resultCode=$resultCode, intent=$intent"
            )
            if (resultCode == Activity.RESULT_OK && requestCode == installRequestCode) {
                activity?.let {
                    install24(it, apkFile, appId)
                }
                true
            } else {
                false
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activity = null
        activityBinding = null
    }

    private fun setupMethodChannel(context: Context, messenger: BinaryMessenger) {
        methodChannel = MethodChannel(messenger, "install_plugin")
        methodChannel?.setMethodCallHandler(this)
    }

    private fun teardownMethodChannel() {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
    }

    @Throws(FileNotFoundException::class, NullPointerException::class)
    private fun installApk(filePath: String?, appId: String?) {
        val currentActivity = activity ?: throw NullPointerException("Activity is null!")
        val currentContext = currentActivity.applicationContext

        if (filePath.isNullOrEmpty()) throw NullPointerException("filePath is null or empty!")
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException("$filePath does not exist!")

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                if (canRequestPackageInstalls(currentActivity)) {
                    install24(currentContext, file, appId)
                } else {
                    showSettingPackageInstall(currentActivity)
                    this.apkFile = file
                    this.appId = appId
                }
            }
            else -> installBelow24(currentContext, file)
        }
    }

    private fun showSettingPackageInstall(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivityForResult(intent, installRequestCode)
        } else {
            throw RuntimeException("Requires Android O or higher")
        }
    }

    private fun canRequestPackageInstalls(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun installBelow24(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }

    private fun install24(context: Context, file: File?, appId: String?) {
        requireNotNull(context) { "Context cannot be null" }
        requireNotNull(file) { "File cannot be null" }
        requireNotNull(appId) { "App ID cannot be null" }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri = FileProvider.getUriForFile(
                context,
                "$appId.fileProvider.install",
                file
            )
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }
}