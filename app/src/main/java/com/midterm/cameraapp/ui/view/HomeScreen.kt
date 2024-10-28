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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.blur
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen(
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
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

    // Load images when the screen is created
    LaunchedEffect(Unit) {
        scope.launch {
            images = imageGallery.loadImages(context)
        }
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
                    // Reload danh sách ảnh
                    images = imageGallery.loadImages(context)
                }
            },
            onEditImage = { image ->
                // Xử lý chỉnh sửa ảnh
                // Có thể navigate đến màn hình edit hoặc mở intent để edit
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF22272E))
        ) {
            Column {
                UITopBar(
                    isFlashEnabled = isFlashEnabled,
                    onFlashToggle = { isFlashEnabled = !isFlashEnabled },
                    onAspectRatioSelected = { ratio ->
                        // Chuyển đổi String thành Int
                        aspectRatio = when (ratio) {
                            "16:9" -> AspectRatio.RATIO_16_9
                            "4:3" -> AspectRatio.RATIO_4_3
                            "1:1" -> 0 // Giá trị cho tỷ lệ 1:1
                            else -> AspectRatio.RATIO_16_9 // Giá trị mặc định
                        }
                    },
                    isGridEnabled = isGridEnabled,
                    onGridToggle = { isGridEnabled = !isGridEnabled }
                )
                MainContent(
                    lensFacing = lensFacing,
                    capturedImageUri = capturedImageUri,
                    onCaptureImage = { uri -> capturedImageUri = uri },
                    imageCapture = imageCapture,
                    onCameraReady = { isCameraReady = true },
                    cameraProvider = cameraProvider,
                    aspectRatio = aspectRatio, // Truyền aspectRatio vào MainContent
                    isGridEnabled = isGridEnabled // Truyền isGridEnabled vào MainContent
                )

                Spacer(modifier = Modifier.weight(1f))
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
                    onGalleryClick = { showGallery = true }
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
    aspectRatio: Int, // Nhận aspectRatio từ CameraScreen
    isGridEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                when (aspectRatio) {
                    0 -> 1f // Tỷ lệ 1:1
                    AspectRatio.RATIO_16_9 -> 9f / 16f // Tỷ lệ 16:9
                    AspectRatio.RATIO_4_3 -> 3f / 4f // Tỷ lệ 4:3
                    else -> 3f / 4f // Mặc định là 4:3 nếu có lỗi
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
                aspectRatio = aspectRatio // Truyền aspectRatio vào CameraPreview
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
    aspectRatio: Int // Nhận aspectRatio
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
                bindPreview(cameraProvider, previewView, lifecycleOwner, lensFacing, imageCapture, aspectRatio)
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
    aspectRatio: Int
) {
    val previewBuilder = Preview.Builder()
    if (aspectRatio == 0) {
        // Thiết lập tỷ lệ 1:1
        previewBuilder.setTargetResolution(Size(1080, 1080)) // Kích thước 1:1
    } else {
        // Thiết lập tỷ lệ mặc định 4:3 hoặc 16:9
        previewBuilder.setTargetAspectRatio(aspectRatio)
    }

    val preview = previewBuilder.build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }


    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    try {
        // Unbind mọi phiên camera trước khi bind phiên mới
        cameraProvider.unbindAll()

        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

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
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val scope = rememberCoroutineScope()

    var isShotTaken by remember { mutableStateOf(false) }
    var tempPhotoFile: File? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF22272E))
            .blur(0.dp),
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
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Đặt thư mục vào thư viện ảnh
                        }

                        // Lưu ảnh vào MediaStore
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            context.contentResolver.openOutputStream(uri).use { outputStream ->
                                file.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream!!)
                                }
                            }
                            onCaptureImage(uri) // Lưu URI ảnh vào trạng thái
                        }

                        // Đặt lại trạng thái
                        isShotTaken = false
                        tempPhotoFile = null
                        onCaptureImage(null)
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
                                        onCaptureImage(Uri.fromFile(tempPhotoFile!!)) // Hiển thị ảnh nhưng chưa lưu
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
                    painter = painterResource(id = R.drawable.rec),
                    contentDescription = "Shot",
                    tint = Color(0xFF4383DC),
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
    onGridToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRatio by remember { mutableStateOf("16:9") }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main TopBar content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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