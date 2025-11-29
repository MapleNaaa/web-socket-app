package com.ahmedonibiyo.websocketapp


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ahmedonibiyo.websocketapp.model.AppVisibility
import com.ahmedonibiyo.websocketapp.model.ConnectionState
import com.ahmedonibiyo.websocketapp.model.NotificationHelper
import com.ahmedonibiyo.websocketapp.model.WebSocketViewModel
import com.ahmedonibiyo.websocketapp.ui.theme.WebSocketAppTheme

enum class AppScreen {
    CHAT,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // 这里可以根据 isGranted 做一些提示（可选）
            // if (!isGranted) { ... }
        }
    private val viewModel: WebSocketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        requestPostNotificationPermissionIfNeeded()

        // 如果配置了自动连接，启动时就开服务
        if (viewModel.autoConnect.value) {
            startWebSocketService()
        }

        setContent {
            val themeMode by viewModel.themeMode

            WebSocketAppTheme(themeMode = themeMode) {
                WebSocketApp(
                    viewModel = viewModel,
                    onStartService = { startWebSocketService() },
                    onStopService = { stopWebSocketService() }
                )
            }
        }
    }


    override fun onResume() {
        super.onResume()
        viewModel.setAppInForeground(true)
        AppVisibility.isForeground = true
    }

    override fun onPause() {
        super.onPause()
        viewModel.setAppInForeground(false)
        AppVisibility.isForeground = false
    }


    private fun requestPostNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun startWebSocketService() {
        val intent = Intent(this, WebSocketService::class.java).apply {
            putExtra(WebSocketService.EXTRA_HOST, viewModel.host.value)
            putExtra(WebSocketService.EXTRA_PORT, viewModel.port.value)
            putExtra(WebSocketService.EXTRA_USER_NAME, viewModel.userName.value)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopWebSocketService() {
        val intent = Intent(this, WebSocketService::class.java)
        stopService(intent)
    }

}

@Composable
fun WebSocketApp(
    viewModel: WebSocketViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

    when (currentScreen) {
        AppScreen.CHAT -> ChatScreen(
            viewModel = viewModel,
            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
        )

        AppScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onBackToChat = { currentScreen = AppScreen.CHAT },
            onStartService = onStartService,
            onStopService = onStopService
        )
    }
}

