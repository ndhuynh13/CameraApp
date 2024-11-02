package com.midterm.cameraapp.ui.view

import CameraGrid
import GalleryScreen
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import com.midterm.cameraapp.data.MediaGallery
import com.midterm.cameraapp.data.VideoItem
import com.midterm.cameraapp.ui.view.CameraConstants.FILENAME_FORMAT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Thêm enum class cho các tùy chọn hẹn giờ
enum class TimerOption(val seconds: Int) {
    OFF(0),
    THREE(3),
    FIVE(5),
    TEN(10)
}
// Thêm vào đầu file, sau các import
enum class CameraMode {
    PHOTO,
    VIDEO
}

// Hoặc tạo một object để chứa các constants
object CameraConstants {
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
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

    var cameraMode by remember { mutableStateOf<CameraMode>(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var recordingTime by remember { mutableStateOf(0L) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var lastSavedVideoUri by remember { mutableStateOf<Uri?>(null) }


    var pendingRecording by remember { mutableStateOf(false) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    val mediaGallery = remember { MediaGallery() }


    // Khởi tạo CameraProvider
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()
    }

    // Khởi tạo videoCapture dựa trên cameraMode
    LaunchedEffect(cameraMode) {
        cameraProvider?.unbindAll()
        delay(200) // Đợi để đảm bảo camera giải phóng xong
        if (cameraMode == CameraMode.VIDEO) {
            try {
                cameraProvider?.unbindAll()
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HIGHEST,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                        )
                    )
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                Log.d("CameraBinding", "VideoCapture initialized")

                // Bind camera sau khi khởi tạo VideoCapture
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView!!.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                delay(100) // Đợi một chút trước khi bind

                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                Log.d("CameraBinding", "Camera bound with video capture")
            } catch (e: Exception) {
                Log.e("CameraBinding", "Error setting up video capture: ${e.message}", e)
            }
        }
    }

    // Tách riêng phần xử lý recording
    LaunchedEffect(pendingRecording) {
        if (pendingRecording && videoCapture != null) {
            try {
                recording = startRecording(
                    videoCapture = videoCapture,
                    context = context,
                    onVideoSaved = { uri ->
                        lastSavedVideoUri = uri
                        isRecording = false
                        recording = null
                        pendingRecording = false
                    },
                    onError = { error ->
                        isRecording = false
                        recording = null
                        pendingRecording = false
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("Recording", "Failed to start recording: ${e.message}")
                pendingRecording = false
            }
        }
    }

    // Load images when the screen is created
    LaunchedEffect(Unit) {
        scope.launch {
            images = imageGallery.loadImages(context)
            videos = mediaGallery.loadVideos(context)
        }
    }

    // Định nghĩa reloadGallery như một suspend function
    val reloadGallery: suspend () -> Unit = {
        images = imageGallery.loadImages(context)
        videos = mediaGallery.loadVideos(context)
    }

    // Truyền videos vào GalleryScreen
    if (showGallery) {
        GalleryScreen(
            images = images,
            videos = videos,
            onImageClick = { uri ->
                // Handle image click
            },
            onClose = { showGallery = false },
            onDeleteImage = { image ->
                scope.launch {
                    context.contentResolver.delete(
                        image.uri,
                        null,
                        null
                    )
                    reloadGallery()
                }
            },
            onDeleteVideo = { video ->
                scope.launch {
                    context.contentResolver.delete(
                        video.uri,
                        null,
                        null
                    )
                    reloadGallery()
                }
            },
            onEditImage = { image ->
                // Handle edit
            },
            onReload = reloadGallery,
            onReloadVideos = reloadGallery,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this // Lưu reference đến previewView
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

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
                isFlashEnabled = isFlashEnabled,
                videoCapture = videoCapture
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
                    },
                    cameraMode = cameraMode,
                    onModeChange = { mode -> cameraMode = mode },
                    recordingTime = recordingTime
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
                    },
                    cameraMode = cameraMode,
                    isRecording = isRecording,
                    onRecordingStart = {
                        if (!isRecording && videoCapture != null) {
                            try {
                                isRecording = true
                                recording = startRecording(
                                    videoCapture = videoCapture,
                                    context = context,
                                    onVideoSaved = { uri ->
                                        lastSavedVideoUri = uri
                                        Log.d("VideoSaved", "Video saved to: $uri")
                                        Toast.makeText(context, "Video đã được lưu", Toast.LENGTH_SHORT).show()
                                        isRecording = false
                                    },
                                    onError = { error ->
                                        Log.e("RecordingError", "Error recording video: $error")
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        isRecording = false
                                        recording = null
                                    }
                                )

                                if (recording == null) {
                                    isRecording = false
                                }
                            } catch (e: Exception) {
                                Log.e("RecordingError", "Error starting recording: ${e.message}", e)
                                Toast.makeText(context, "Lỗi khi bắt đầu quay video", Toast.LENGTH_SHORT).show()
                                isRecording = false
                                recording = null
                            }
                        }
                    },
                    onRecordingStop = {
                        if (isRecording && recording != null) {
                            try {
                                Log.d("RecordingStop", "Stopping recording")
                                recording?.stop()
                                recording = null
                                isRecording = false
                            } catch (e: Exception) {
                                Log.e("RecordingError", "Error stopping recording: ${e.message}", e)
                                Toast.makeText(context, "Lỗi khi dừng quay video", Toast.LENGTH_SHORT).show()
                                recording = null
                                isRecording = false
                            }
                        }
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
    videoCapture: VideoCapture<Recorder>?
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
                isFlashEnabled = isFlashEnabled,
                videoCapture = videoCapture
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
    isFlashEnabled: Boolean,
    videoCapture: VideoCapture<Recorder>?
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
                    isFlashEnabled,
                    videoCapture
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
    isFlashEnabled: Boolean,
    videoCapture: VideoCapture<Recorder>?
) {
    try {
        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()


        // Bind use cases dựa trên mode
        val camera = if (videoCapture != null) {
            Log.d("CameraBinding", "Binding with video capture")
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        } else {
            Log.d("CameraBinding", "Binding without video capture")
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        }

        // Cập nhật cấu hình flash cho imageCapture
        imageCapture.flashMode = when (isFlashEnabled) {
            true -> ImageCapture.FLASH_MODE_ON
            false -> ImageCapture.FLASH_MODE_OFF
        }

        // Kiểm tra xem thiết bị có hỗ trợ flash không
        val hasFlash = camera.cameraInfo.hasFlashUnit()
        if (hasFlash) {
            camera.cameraControl.enableTorch(isFlashEnabled)
        }

        // Theo dõi trạng thái camera
        camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
            Log.d("CameraState", "Camera state: ${state.type}")
            if (state.error != null) {
                Log.e("CameraError", "Camera error: ${state.error}")
            }
        }

    } catch (exc: Exception) {
        Log.e("CameraBindError", "Lỗi khi bind camera: ${exc.message}", exc)
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
    onCountdownUpdate: (Int?) -> Unit,
    cameraMode: CameraMode,
    isRecording: Boolean,
    onRecordingStart: () -> Unit,
    onRecordingStop: () -> Unit,
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
                    when (cameraMode) {
                        CameraMode.PHOTO -> {
                            // Existing photo capture logic
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
                        }
                        CameraMode.VIDEO -> {
                            if (isRecording) {
                                Log.d("VideoRecording", "Stopping recording...")
                                onRecordingStop()
                            } else {
                                Log.d("VideoRecording", "Starting recording...")
                                onRecordingStart()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .size(100.dp)
            ) {
                Icon(
                    painter = when {
                        cameraMode == CameraMode.PHOTO -> painterResource(id = R.drawable.shot)
                        isRecording -> painterResource(id = R.drawable.stop)
                        else -> painterResource(id = R.drawable.play)
                    },
                    contentDescription = if (cameraMode == CameraMode.PHOTO) "Shot" else "Record",
                    tint = if (isRecording) Color.Red else Color(0xFF7A3030),
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
fun UITopBar(
    isFlashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    onAspectRatioSelected: (String) -> Unit,
    isGridEnabled: Boolean,
    onGridToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onTimerSelected: (Int) -> Unit,
    cameraMode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    recordingTime: Long = 0L
) {
    var expanded by remember { mutableStateOf(false) }
    var timerExpanded by remember { mutableStateOf(false) }
    var selectedRatio by remember { mutableStateOf("4:3") }
    var selectedTimer by remember { mutableStateOf(TimerOption.OFF) }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Hiển thị thời gian quay video nếu đang recording
        if (cameraMode == CameraMode.VIDEO && recordingTime > 0) {
            Text(
                text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Mode Switch
            IconButton(
                onClick = {
                    onModeChange(if (cameraMode == CameraMode.PHOTO) CameraMode.VIDEO else CameraMode.PHOTO)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (cameraMode == CameraMode.VIDEO) Color(0xFF2196F3) else Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (cameraMode == CameraMode.PHOTO)
                            R.drawable.photo_camera
                        else
                            R.drawable.video_camera
                    ),
                    contentDescription = "Switch Camera Mode",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

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

private fun startRecording(
    videoCapture: VideoCapture<Recorder>?,
    context: Context,
    onVideoSaved: (Uri) -> Unit,
    onError: (String) -> Unit
): Recording? {
    if (videoCapture == null) {
        onError("VideoCapture chưa được khởi tạo")
        return null
    }

    try {
        Log.d("VideoCapture", "Bắt đầu quay video...")

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        return videoCapture.output
            .prepareRecording(context, mediaStoreOutput)
            .apply {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("VideoCapture", "Đang quay video...")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            recordEvent.outputResults.outputUri?.let { uri ->
                                onVideoSaved(uri)
                            }
                        } else {
                            onError("Lỗi quay video: ${recordEvent.error}")
                        }
                    }
                }
            }
    } catch (e: Exception) {
        val msg = "Không thể bắt đầu quay video: ${e.message}"
        Log.e("VideoCapture", msg, e)
        onError(msg)
        return null
    }
}


