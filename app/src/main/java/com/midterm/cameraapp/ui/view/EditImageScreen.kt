package com.midterm.cameraapp.ui.view

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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var rotation by remember { mutableStateOf(0f) }
    var showColorAdjust by remember { mutableStateOf(false) }
    var showRotateOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentRotation by remember { mutableStateOf(0f) }
    val gpuImage = remember { GPUImage(context) }

    LaunchedEffect(image.uri) {
        isLoading = true
        try {
            // Load ảnh từ URI
            withContext(Dispatchers.IO) {
                val loadedBitmap =
                    MediaStore.Images.Media.getBitmap(context.contentResolver, image.uri)
                // Đọc orientation của ảnh
                val orientation = getImageOrientation(context, image.uri)
                // Xoay bitmap theo orientation nếu cần
                bitmap = if (orientation != 0) {
                    rotateBitmap(loadedBitmap, orientation.toFloat())
                } else {
                    loadedBitmap
                }
                gpuImage.setImage(bitmap)
            }
        } catch (e: Exception) {
            Log.e("EditImageScreen", "Error loading image", e)
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                                bitmap?.let {
                                    try {
                                        val editedBitmap = gpuImage.bitmapWithFilterApplied
                                        saveBitmapToGallery(context, editedBitmap)
                                        onSave()
                                    } catch (e: Exception) {
                                        Log.e("EditImageScreen", "Error applying filter", e)
                                    }
                                }
                            },
                            enabled = bitmap != null
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
                    bitmap?.let { bmp ->
                        AndroidView(
                            factory = { context ->
                                GPUImageView(context).apply {
                                    setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
                                    setImage(bmp)
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

                // Bottom Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2A2A2A)
                ) {
                    Column {
                        // Trong EditImageScreen, phần Row chứa các buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EditButton(
                                icon = painterResource(id = R.drawable.crop),
                                label = "Rotate",
                                onClick = {
                                    showRotateOptions = !showRotateOptions
                                    if (showRotateOptions) {
                                        showColorAdjust = false
                                    }
                                }
                            )
                            EditButton(
                                icon = painterResource(id = R.drawable.adjust_color),
                                label = "Adjust",
                                onClick = {
                                    showColorAdjust = !showColorAdjust
                                    if (showColorAdjust) {
                                        showRotateOptions = false
                                    }
                                }
                            )
                        }

                        // Rotate Options
                        AnimatedVisibility(
                            visible = showRotateOptions,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(
                                    onClick = {
                                        Log.d("EditImageScreen", "Rotate Left Button Clicked")
                                        bitmap?.let { bmp ->
                                            Log.d("EditImageScreen", "Current rotation: $currentRotation")
                                            Log.d("EditImageScreen", "Original bitmap - Width: ${bmp.width}, Height: ${bmp.height}")

                                            currentRotation = (currentRotation - 90) % 360
                                            Log.d("EditImageScreen", "New rotation value: $currentRotation")

                                            try {
                                                val rotatedBitmap = rotateBitmap(bmp, -90f)
                                                Log.d("EditImageScreen", "Rotated bitmap - Width: ${rotatedBitmap.width}, Height: ${rotatedBitmap.height}")

                                                bitmap = rotatedBitmap
                                                Log.d("EditImageScreen", "New bitmap set")

                                                gpuImage.setImage(rotatedBitmap)
                                                Log.d("EditImageScreen", "GPUImage updated with new bitmap")

                                                gpuImage.requestRender()
                                                Log.d("EditImageScreen", "Render requested")
                                            } catch (e: Exception) {
                                                Log.e("EditImageScreen", "Error during rotation", e)
                                            }
                                        } ?: Log.e("EditImageScreen", "Bitmap is null")
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rotate_left),
                                        contentDescription = "Rotate Left",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        Log.d("EditImageScreen", "Rotate Right Button Clicked")
                                        bitmap?.let { bmp ->
                                            Log.d("EditImageScreen", "Current rotation: $currentRotation")
                                            Log.d("EditImageScreen", "Original bitmap - Width: ${bmp.width}, Height: ${bmp.height}")

                                            currentRotation = (currentRotation + 90) % 360
                                            Log.d("EditImageScreen", "New rotation value: $currentRotation")

                                            try {
                                                val rotatedBitmap = rotateBitmap(bmp, 90f)
                                                Log.d("EditImageScreen", "Rotated bitmap - Width: ${rotatedBitmap.width}, Height: ${rotatedBitmap.height}")

                                                bitmap = rotatedBitmap
                                                Log.d("EditImageScreen", "New bitmap set")

                                                gpuImage.setImage(rotatedBitmap)
                                                Log.d("EditImageScreen", "GPUImage updated with new bitmap")

                                                gpuImage.requestRender()
                                                Log.d("EditImageScreen", "Render requested")
                                            } catch (e: Exception) {
                                                Log.e("EditImageScreen", "Error during rotation", e)
                                            }
                                        } ?: Log.e("EditImageScreen", "Bitmap is null")
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rotate_right),
                                        contentDescription = "Rotate Right",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Color Adjust Controls
                    AnimatedVisibility(
                        visible = showColorAdjust,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Brightness", color = Color.White)
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
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
                                onValueChange = { contrast = it },
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
                                onValueChange = { saturation = it },
                                valueRange = 0f..2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.Gray
                                )
                            )
                        }
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

// Trong hàm rotateBitmap
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    Log.d("EditImageScreen", "rotateBitmap called with degrees: $degrees")
    Log.d("EditImageScreen", "Input bitmap - Width: ${bitmap.width}, Height: ${bitmap.height}")

    val matrix = Matrix().apply {
        postRotate(degrees)
    }

    return try {
        val startTime = System.currentTimeMillis()
        Log.d("EditImageScreen", "Starting bitmap rotation")

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        val endTime = System.currentTimeMillis()
        Log.d("EditImageScreen", "Rotation completed in ${endTime - startTime}ms")
        Log.d("EditImageScreen", "Output bitmap - Width: ${rotatedBitmap.width}, Height: ${rotatedBitmap.height}")

        if (rotatedBitmap != bitmap) {
            Log.d("EditImageScreen", "Recycling old bitmap")
            bitmap.recycle()
        }

        rotatedBitmap
    } catch (e: OutOfMemoryError) {
        Log.e("EditImageScreen", "OutOfMemoryError during rotation", e)
        bitmap
    } catch (e: Exception) {
        Log.e("EditImageScreen", "Error during rotation", e)
        bitmap
    }
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

