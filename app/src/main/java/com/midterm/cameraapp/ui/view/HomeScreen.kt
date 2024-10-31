package com.midterm.cameraapp.ui.view

import CameraGrid
import GalleryScreen
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.midterm.cameraapp.R
import com.midterm.cameraapp.data.GalleryImage
import com.midterm.cameraapp.data.ImageGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Thêm enum class cho các tùy chọn hẹn giờ
enum class TimerOption(val seconds: Int) {
    OFF(0),
    THREE(3),
    FIVE(5),
    TEN(10)
}

@Composable
fun CameraScreen(
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCapture by remember {
        mutableStateOf(
            ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
        )
    }
    var isCameraReady by remember { mutableStateOf(true) }
    var cameraProvider: ProcessCameraProvider? = null
    var isFlashEnabled by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(AspectRatio.RATIO_4_3) }
    var isGridEnabled by remember { mutableStateOf(false) }

    var showGallery by remember { mutableStateOf(false) }
    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageGallery = remember { ImageGallery() }
    var selectedTimerSeconds by remember { mutableStateOf(0) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }

    // Load images when the screen is created
    LaunchedEffect(Unit) {
        scope.launch {
            images = imageGallery.loadImages(context)
        }
    }

    // Định nghĩa reloadGallery như một suspend function
    val reloadGallery: suspend () -> Unit = {
        images = imageGallery.loadImages(context)
    }

    if (showGallery) {
        GalleryScreen(
            images = images,
            onImageClick = { uri ->
                // Handle image click
            },
            onClose = { showGallery = false },
            onDeleteImage = { image ->
                scope.launch {
                    // Xử lý xóa ảnh
                    context.contentResolver.delete(
                        image.uri,
                        null,
                        null
                    )
                    reloadGallery() // Gọi trong coroutine scope
                }
            },
            onEditImage = { image ->
                // Handle edit
            },
            onReload = reloadGallery // Truyền suspend function
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Camera Preview (layer dưới cùng)
            MainContent(
                lensFacing = lensFacing,
                capturedImageUri = capturedImageUri,
                onCaptureImage = { uri -> capturedImageUri = uri },
                imageCapture = imageCapture,
                onCameraReady = { isCameraReady = true },
                cameraProvider = cameraProvider,
                aspectRatio = aspectRatio,
                isGridEnabled = isGridEnabled,
                modifier = Modifier.fillMaxSize(),
                isFlashEnabled = isFlashEnabled
            )

            // 3. UI Controls (layer trên cùng)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Controls
                UITopBar(
                    isFlashEnabled = isFlashEnabled,
                    onFlashToggle = { isFlashEnabled = !isFlashEnabled },
                    onAspectRatioSelected = { ratio ->
                        val newAspectRatio = when (ratio) {
                            "16:9" -> AspectRatio.RATIO_16_9
                            "4:3" -> AspectRatio.RATIO_4_3
                            "1:1" -> 0
                            else -> AspectRatio.RATIO_16_9
                        }
                        Log.d("AspectRatioChange", "Selected ratio: $ratio, value: $newAspectRatio")
                        aspectRatio = newAspectRatio
                        imageCapture = updateImageCaptureConfig(newAspectRatio, imageCapture)
                    },
                    isGridEnabled = isGridEnabled,
                    onGridToggle = { isGridEnabled = !isGridEnabled },
                    modifier = Modifier.align(Alignment.TopCenter),
                    onTimerSelected = { seconds ->
                        selectedTimerSeconds = seconds
                        Log.d("Timer", "Selected timer: $seconds seconds")
                    }
                )

                // Bottom Controls
                BottomBar(
                    onSwitchCamera = {
                        isCameraReady = false
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }

                        // Đảm bảo camera được chuẩn bị kỹ càng
                        Handler(Looper.getMainLooper()).postDelayed({
                            isCameraReady = true
                        }, 500)
                    },
                    imageCapture = imageCapture,
                    onCaptureImage = { uri ->
                        capturedImageUri = uri
                    },
                    isCameraReady = isCameraReady,
                    isFlashEnabled = isFlashEnabled,
                    onFlashToggle = { isFlashEnabled = !isFlashEnabled },
                    cameraProvider = cameraProvider,
                    latestImage = images.firstOrNull(),
                    onGalleryClick = { showGallery = true },
                    onImageSaved = {
                        scope.launch {
                            reloadGallery()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    timerSeconds = selectedTimerSeconds,
                    onCountdownUpdate = { seconds ->
                        countdownSeconds = seconds
                    }
                )
            }
        }
    }
}
@Composable
fun MainContent(
    lensFacing: Int,
    capturedImageUri: Uri?,
    onCaptureImage: (Uri) -> Unit,
    imageCapture: ImageCapture,
    onCameraReady: () -> Unit,
    cameraProvider: ProcessCameraProvider?,
    aspectRatio: Int,
    isGridEnabled: Boolean,
    modifier: Modifier = Modifier,
    isFlashEnabled: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(
                when (aspectRatio) {
                    AspectRatio.RATIO_16_9 -> 9f / 16f
                    AspectRatio.RATIO_4_3 -> 3f / 4f
                    0 -> 1f
                    else -> 3f / 4f
                }
            )
            .clip(RoundedCornerShape(20.dp))
    ) {
        if (capturedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(capturedImageUri),
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lensFacing = lensFacing,
                imageCapture = imageCapture,
                onCameraReady = onCameraReady,
                cameraProvider = cameraProvider,
                aspectRatio = aspectRatio,
                isFlashEnabled = isFlashEnabled
            )
            // Show Grid
            if (isGridEnabled) {
                CameraGrid(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int,
    imageCapture: ImageCapture,
    onCameraReady: () -> Unit,
    cameraProvider: ProcessCameraProvider?,
    aspectRatio: Int,
    isFlashEnabled: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                bindPreview(
                    cameraProvider,
                    previewView,
                    lifecycleOwner,
                    lensFacing,
                    imageCapture,
                    aspectRatio,
                    isFlashEnabled
                )
                onCameraReady()
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun bindPreview(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    lensFacing: Int,
    imageCapture: ImageCapture,
    aspectRatio: Int,
    isFlashEnabled: Boolean
) {
    val previewBuilder = Preview.Builder()

    when (aspectRatio) {
        AspectRatio.RATIO_16_9 -> {
            previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            Log.d("Preview", "Setting preview ratio to 16:9")
        }
        AspectRatio.RATIO_4_3 -> {
            previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            Log.d("Preview", "Setting preview ratio to 4:3")
        }
        0 -> { // 1:1
            previewBuilder.setTargetResolution(Size(1080, 1080))
            Log.d("Preview", "Setting preview ratio to 1:1")
        }
    }

    val preview = previewBuilder.build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }


    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    // Cập nhật cấu hình flash cho imageCapture
    imageCapture.flashMode = when (isFlashEnabled) {
        true -> ImageCapture.FLASH_MODE_ON
        false -> ImageCapture.FLASH_MODE_OFF
    }

    try {
        // Unbind mọi phiên camera trước khi bind phiên mới
        cameraProvider.unbindAll()

        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        // Kiểm tra xem thiết bị có hỗ trợ flash không
        val hasFlash = camera.cameraInfo.hasFlashUnit()
        Log.d("CameraFlash", "Device has flash unit: $hasFlash")

        if (hasFlash) {
            camera.cameraControl.enableTorch(isFlashEnabled)
        }

        // Kiểm tra trạng thái của camera sau khi bind
        val cameraInfo = camera.cameraInfo
        cameraInfo.cameraState.observe(lifecycleOwner) { state ->
            when (state.type) {
                CameraState.Type.OPEN -> {
                    Log.d("CameraState", "Camera đã mở thành công")
                }

                CameraState.Type.CLOSED -> {
                    Log.d("CameraState", "Camera đã đóng")
                }

                CameraState.Type.PENDING_OPEN -> {
                    Log.d("CameraState", "Camera đang chờ mở")
                }

                CameraState.Type.CLOSING -> {
                    Log.d("CameraState", "Camera đang đóng")
                }

                else -> {
                    Log.d("CameraState", "Trạng thái camera không xác định: ${state.type}")
                }
            }

            if (state.error != null) {
                Log.e("CameraStateError", "Camera gặp lỗi: ${state.error}")
            }
        }

    } catch (exc: Exception) {
        Log.e("CameraBindError", "Lỗi khi bind camera", exc)
    }
}
@Composable
fun BottomBar(
    onSwitchCamera: () -> Unit,
    imageCapture: ImageCapture,
    onCaptureImage: (Uri?) -> Unit,
    isCameraReady: Boolean,
    isFlashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    cameraProvider: ProcessCameraProvider?,
    latestImage: GalleryImage?,
    onGalleryClick: () -> Unit,
    onImageSaved: () -> Unit,
    modifier: Modifier = Modifier,
    timerSeconds: Int = 0,
    onCountdownUpdate: (Int?) -> Unit
) {
    val context = LocalContext.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val scope = rememberCoroutineScope()

    var isShotTaken by remember { mutableStateOf(false) }
    var tempPhotoFile: File? by remember { mutableStateOf(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isShotTaken) {
                // Nút Accept - Lưu ảnh vào thư viện thiết bị
                IconButton(onClick = {
                    tempPhotoFile?.let { file ->
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        }

                        scope.launch {
                            try {
                                // Lưu ảnh vào MediaStore
                                val uri = context.contentResolver.insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )
                                uri?.let {
                                    context.contentResolver.openOutputStream(uri).use { outputStream ->
                                        file.inputStream().use { inputStream ->
                                            inputStream.copyTo(outputStream!!)
                                        }
                                    }
                                    onCaptureImage(uri)
                                }

                                // Reload gallery ngay sau khi lưu ảnh
                                onImageSaved()

                                // Reset states
                                isShotTaken = false
                                tempPhotoFile = null
                                onCaptureImage(null)
                            } catch (e: Exception) {
                                Log.e("SaveImage", "Error saving image", e)
                            }
                        }
                    }
                }, modifier = Modifier.size(60.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = "Accept",
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFFCFCFCF)
                    )
                }
            } else {
                // Gallery Button with latest image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onGalleryClick)
                ) {
                    if (latestImage != null) {
                        Image(
                            painter = rememberAsyncImagePainter(latestImage.uri),
                            contentDescription = "Gallery",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.gallery),
                            contentDescription = "Gallery",
                            tint = Color(0xFFCFCFCF),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

            }

            IconButton(
                onClick = {
                    if (isCameraReady) {
                        scope.launch {
                            if (timerSeconds > 0) {
                                // Đếm ngược
                                for (i in timerSeconds downTo 1) {
                                    Log.d("Timer", "Countdown: $i seconds")
                                    delay(1000) // Đợi 1 giây
                                }
                            }

                            // Chụp ảnh sau khi đếm ngược xong
                            tempPhotoFile = withContext(Dispatchers.IO) {
                                File.createTempFile("IMG_", ".jpg", context.cacheDir)
                            }
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempPhotoFile!!).build()

                            imageCapture.takePicture(
                                outputOptions,
                                mainExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        Log.d("CameraShot", "Image captured at: ${tempPhotoFile!!.absolutePath}")
                                        isShotTaken = true
                                        onCaptureImage(Uri.fromFile(tempPhotoFile!!))
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraShot", "Image capture failed: ${exception.message}", exception)
                                    }
                                }
                            )
                        }
                    } else {
                        Log.d("CameraShot", "Camera is not ready yet!")
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .size(100.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shot),
                    contentDescription = "Shot",
                    tint = Color(0xFF7A3030),
                    modifier = Modifier.size(100.dp)
                )
            }

            if (isShotTaken) {
                // Nút Remove - Không lưu ảnh và khởi động lại camera preview
                IconButton(onClick = {
                    tempPhotoFile = null
                    onCaptureImage(null) // Đặt lại URI ảnh để khởi động lại preview
                    isShotTaken = false
                }, modifier = Modifier.size(60.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.remove),
                        contentDescription = "Remove",
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFFCFCFCF)
                    )
                }
            } else {
                // Nút Switch Camera
                IconButton(
                    onClick = { onSwitchCamera() },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.switch_camera),
                        contentDescription = "Switch Camera",
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFFCFCFCF)
                    )
                }
            }
        }
    }
}
@Composable
fun AspectRatioSelector(onAspectRatioChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { onAspectRatioChange(AspectRatio.RATIO_4_3) }) {
            Text("4:3")
        }
        Button(onClick = { onAspectRatioChange(AspectRatio.RATIO_16_9) }) {
            Text("16:9")
        }
        Button(onClick = { onAspectRatioChange(1) }) {
            Text("1:1")
        }
    }
}

@Composable
fun UITopBar(
    isFlashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    onAspectRatioSelected: (String) -> Unit,
    isGridEnabled: Boolean,
    onGridToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onTimerSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var timerExpanded by remember { mutableStateOf(false) }
    var selectedRatio by remember { mutableStateOf("4:3") }
    var selectedTimer by remember { mutableStateOf(TimerOption.OFF) }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Main TopBar content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer Button
            IconButton(
                onClick = { timerExpanded = !timerExpanded },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTimer != TimerOption.OFF) Color(0xFF2196F3) else Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.timer), // Thêm icon timer vào resources
                    contentDescription = "Timer",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Flash Toggle Button
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFlashEnabled) Color(0xFF2196F3) else Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = if (isFlashEnabled) R.drawable.flash else R.drawable.no_flash),
                    contentDescription = "Flash",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Aspect Ratio Selector Button
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.photo),
                    contentDescription = "Aspect Ratio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Grid Toggle Button
            IconButton(
                onClick = onGridToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGridEnabled) Color(0xFF2196F3) else Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = if (isGridEnabled) R.drawable.grid else R.drawable.grid_off),
                    contentDescription = "Grid",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Timer Selector Menu
        if (timerExpanded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .width(280.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2A2A2A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimerOption.values().forEach { option ->
                        TimerOptionItem(
                            seconds = option.seconds,
                            isSelected = selectedTimer == option,
                            onClick = {
                                selectedTimer = option
                                onTimerSelected(option.seconds)
                                timerExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Aspect Ratio Selector Menu (Floating)
        if (expanded) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp) // Điều chỉnh khoảng cách từ top
                    .width(280.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2A2A2A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AspectRatioItem("16:9", selectedRatio) {
                        onAspectRatioSelected("16:9")
                        selectedRatio = "16:9"
                        expanded = false
                    }
                    AspectRatioItem("4:3", selectedRatio) {
                        onAspectRatioSelected("4:3")
                        selectedRatio = "4:3"
                        expanded = false
                    }
                    AspectRatioItem("1:1", selectedRatio) {
                        onAspectRatioSelected("1:1")
                        selectedRatio = "1:1"
                        expanded = false
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioItem(
    ratio: String,
    selectedRatio: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (ratio == selectedRatio) Color(0xFF2196F3) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ratio,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (ratio == selectedRatio) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TimerOptionItem(
    seconds: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2196F3) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (seconds == 0) "OFF" else "${seconds}s",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Hàm cập nhật ImageCapture khi thay đổi tỷ lệ
fun updateImageCaptureConfig(newAspectRatio: Int, currentImageCapture: ImageCapture): ImageCapture {
    val builder = ImageCapture.Builder()

    when (newAspectRatio) {
        AspectRatio.RATIO_16_9 -> {
            builder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            Log.d("ImageCapture", "Setting ratio to 16:9")
        }
        AspectRatio.RATIO_4_3 -> {
            builder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            Log.d("ImageCapture", "Setting ratio to 4:3")
        }
        0 -> { // 1:1
            builder.setTargetResolution(Size(1080, 1080))
            Log.d("ImageCapture", "Setting ratio to 1:1")
        }
    }

    return builder.build()
}
