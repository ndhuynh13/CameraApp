package com.midterm.cameraapp.ui.view

import android.app.Activity
import android.content.ContentValues
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.effect.Crop
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.midterm.cameraapp.R
import com.midterm.cameraapp.data.GalleryImage
import jp.co.cyberagent.android.gpuimage.GPUImage
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yalantis.ucrop.UCrop
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageScreen(
    image: GalleryImage,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }
    var showColorAdjust by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val gpuImage = remember { GPUImage(context) }
    var isLoading by remember { mutableStateOf(true) }

    // Load bitmap khi màn hình được tạo
    LaunchedEffect(image.uri) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                // Load bitmap từ URI
                var loadedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, image.uri)

                // Lấy orientation của ảnh
                val orientation = getImageOrientation(context, image.uri)

                // Kiểm tra orientation và xoay bitmap nếu cần
                if (orientation != 0) {
                    val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
                    loadedBitmap = Bitmap.createBitmap(
                        loadedBitmap, 0, 0,
                        loadedBitmap.width, loadedBitmap.height,
                        matrix, true
                    )
                }

                bitmap = loadedBitmap
                gpuImage.setImage(loadedBitmap)
            }
        } catch (e: Exception) {
            Log.e("EditImageScreen", "Error loading image", e)
        } finally {
            isLoading = false
        }
    }
    // Khởi tạo launcher cho uCrop
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let { uri ->
                    // Cập nhật bitmap mới sau khi crop
                    val newBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    bitmap = newBitmap
                    gpuImage.setImage(newBitmap)
                }
            } catch (e: Exception) {
                Log.e("EditImageScreen", "Error getting crop result", e)
            }
        }
    }

    // Hàm mở uCrop
    fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val uCropIntent = UCrop.of(uri, destinationUri)
            .withAspectRatio(0f, 0f) // Tự do điều chỉnh tỷ lệ
            .withOptions(UCrop.Options().apply {
                setToolbarColor(Color.Black.toArgb())
                setStatusBarColor(Color.Black.toArgb())
                setToolbarWidgetColor(Color.White.toArgb())
                setFreeStyleCropEnabled(true)
            })
            .getIntent(context)

        cropLauncher.launch(uCropIntent)
    }

    // UI Code
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            SmallTopAppBar(
                title = { Text("Edit Image", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                        }
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )

            // Image Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else {
                    // Chỉ hiển thị AsyncImage, không cần GPUImageView khi chưa edit
                    if (!showColorAdjust) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Image Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Chỉ hiển thị GPUImageView khi đang adjust
                        bitmap?.let { bmp ->
                            AndroidView(
                                factory = { context ->
                                    GPUImageView(context).apply {
                                        setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
                                        setImage(bmp)
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { view ->
                                    view.setImage(bmp)
                                    val filterGroup = GPUImageFilterGroup().apply {
                                        addFilter(GPUImageBrightnessFilter(brightness))
                                        addFilter(GPUImageContrastFilter(contrast))
                                        addFilter(GPUImageSaturationFilter(saturation))
                                    }
                                    view.filter = filterGroup
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2A2A2A)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditButton(
                            icon = painterResource(id = R.drawable.crop),
                            label = "Crop",
                            onClick = {
                                image.uri?.let { startCrop(it) }
                            }
                        )
                        EditButton(
                            icon = painterResource(id = R.drawable.adjust_color),
                            label = "Adjust",
                            onClick = {
                                showColorAdjust = !showColorAdjust
                            }
                        )
                    }

                    // Color Adjust Controls
                    AnimatedVisibility(
                        visible = showColorAdjust,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ColorAdjustControls(
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation,
                            onBrightnessChange = { brightness = it },
                            onContrastChange = { contrast = it },
                            onSaturationChange = { saturation = it }
                        )
                    }
                }
            }
        }
    }
}

// Hàm hỗ trợ đọc orientation của ảnh
private fun getImageOrientation(context: Context, uri: Uri): Int {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
        null, null, null
    )

    return cursor?.use {
        if (it.moveToFirst()) {
            val orientation = it.getInt(0)
            orientation
        } else {
            0
        }
    } ?: 0
}

@Composable
private fun EditButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White)
    }
}

@Composable
private fun ColorAdjustControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Brightness", color = Color.White)
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = -1f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Contrast", color = Color.White)
        Slider(
            value = contrast,
            onValueChange = onContrastChange,
            valueRange = 0.5f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Saturation", color = Color.White)
        Slider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray
            )
        )
    }
}

// Hàm hỗ trợ lưu ảnh
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "edited_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val uri =
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
    }
}

