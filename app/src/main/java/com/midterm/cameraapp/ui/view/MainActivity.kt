package com.midterm.cameraapp.ui.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.midterm.cameraapp.ui.theme.CameraAppTheme

class MainActivity : ComponentActivity() {
    // Sử dụng ActivityResultContracts để yêu cầu quyền camera
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Nếu quyền được cấp, khởi chạy ứng dụng
                startApp()
                // Nếu quyền bị từ chối, hiển thị thông báo
                Toast.makeText(
                    this,
                    "Camera permission is required to use this feature.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kiểm tra quyền camera trước khi hiển thị giao diện
        checkCameraPermission()
    }

    // Hàm kiểm tra và yêu cầu quyền truy cập camera
    private fun checkCameraPermission() {
        val cameraPermission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(
                this,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Nếu quyền đã được cấp, khởi chạy ứng dụng
            startApp()
        } else {
            // Yêu cầu quyền camera nếu chưa được cấp
            requestPermissionLauncher.launch(cameraPermission)
        }
    }

    // Hàm khởi chạy ứng dụng sau khi quyền được cấp
    private fun startApp() {
        setContent {
            CameraAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen()  // Gọi CameraScreen tại đây
                }
            }
        }
    }
}
