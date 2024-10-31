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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yalantis.ucrop.UCrop
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGlassSphereFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageKuwaharaFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageMonochromeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSwirlFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageScreen(
    image: GalleryImage,
    onClose: () -> Unit,
    onSave: suspend () -> Unit
) {
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }
    var sharpness by remember { mutableStateOf(0f) }
    var exposure by remember { mutableStateOf(0f) }
    var warmth by remember { mutableStateOf(0f) }
    var showColorAdjust by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val gpuImage = remember { GPUImage(context) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Thêm state để theo dõi ảnh preview
    var previewUri by remember { mutableStateOf<Uri?>(image.uri) }

    // Thêm state cho filter
    var showFilters by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf<GPUImageFilter>(GPUImageFilter()) }

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
    // Cập nhật cropLauncher để update preview
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
                    // Cập nhật preview URI
                    previewUri = uri
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

    // Cập nhật hàm saveCurrentImage để lưu và cập nhật preview
    fun saveCurrentImage() {
        bitmap?.let { currentBitmap ->
            scope.launch(Dispatchers.IO) {
                try {
                    // Tạo filter group mới
                    val filterGroup = GPUImageFilterGroup()

                    // Thêm current filter trước (nếu có)
                    if (currentFilter !is GPUImageFilter || currentFilter.javaClass != GPUImageFilter::class.java) {
                        filterGroup.addFilter(currentFilter)
                    }

                    // Thêm các color adjustment filters
                    if (showColorAdjust) {
                        filterGroup.addFilter(GPUImageBrightnessFilter(brightness))
                        filterGroup.addFilter(GPUImageContrastFilter(contrast))
                        filterGroup.addFilter(GPUImageSaturationFilter(saturation))
                        filterGroup.addFilter(GPUImageSharpenFilter(sharpness))
                        filterGroup.addFilter(GPUImageExposureFilter(exposure))
                        filterGroup.addFilter(GPUImageWhiteBalanceFilter(5500f + (warmth * 4000f), warmth * 0.1f))
                    }

                    // Áp dụng filter group vào GPUImage
                    gpuImage.setImage(currentBitmap)
                    gpuImage.setFilter(filterGroup)

                    // Lấy bitmap đã được áp dụng filter
                    val editedBitmap = gpuImage.bitmapWithFilterApplied

                    // Log để debug
                    Log.d("SaveImage", "Current Filter: ${currentFilter.javaClass.simpleName}")
                    Log.d("SaveImage", "Filter Count: ${filterGroup.filters.size}")

                    // Lưu bitmap và lấy URI
                    val savedUri = saveBitmapToGallery(context, editedBitmap)

                    // Cập nhật preview URI sau khi lưu
                    withContext(Dispatchers.Main) {
                        savedUri?.let { uri ->
                            previewUri = uri
                        }
                        onSave()
                    }
                } catch (e: Exception) {
                    Log.e("EditImageScreen", "Error saving image", e)
                }
            }
        }
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
                    IconButton(
                        onClick = when {
                            showColorAdjust -> { { showColorAdjust = false } }
                            showFilters -> { { showFilters = false } }
                            else -> onClose
                        }
                    ) {
                        Icon(
                            imageVector = if (showColorAdjust || showFilters) {
                                Icons.Default.ArrowBack
                            } else {
                                Icons.Default.Close
                            },
                            contentDescription = if (showColorAdjust || showFilters) "Back" else "Close",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveCurrentImage() }
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
                    if (showColorAdjust || showFilters) {
                        // Hiển thị GPUImageView khi đang adjust color hoặc filter
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
                                        if (showFilters) {
                                            addFilter(currentFilter)
                                        }
                                        if (showColorAdjust) {
                                            addFilter(GPUImageBrightnessFilter(brightness))
                                            addFilter(GPUImageContrastFilter(contrast))
                                            addFilter(GPUImageSaturationFilter(saturation))
                                            addFilter(GPUImageSharpenFilter(sharpness))
                                            addFilter(GPUImageExposureFilter(exposure))
                                            addFilter(GPUImageWhiteBalanceFilter(5500f + (warmth * 4000f), warmth * 0.1f))
                                        }
                                    }
                                    view.filter = filterGroup
                                    // Log để debug
                                    Log.d("Preview", "Updating view with filter: ${currentFilter.javaClass.simpleName}")
                                }
                            )
                        }
                    } else {
                        // Hiển thị ảnh thường hoặc ảnh đã crop
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(previewUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Image Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Bottom Controls với animation
            AnimatedVisibility(
                visible = !showColorAdjust && !showFilters,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
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
                                onClick = { showColorAdjust = true }
                            )
                            EditButton(
                                icon = painterResource(id = R.drawable.filter),
                                label = "Filter",
                                onClick = { showFilters = true }
                            )
                        }
                    }
                }
            }

            // Color Adjust Controls với animation
            AnimatedVisibility(
                visible = showColorAdjust,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2A2A2A)
                ) {
                    ColorAdjustControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        sharpness = sharpness,
                        exposure = exposure,
                        warmth = warmth,
                        onBrightnessChange = { brightness = it },
                        onContrastChange = { contrast = it },
                        onSaturationChange = { saturation = it },
                        onSharpnessChange = { sharpness = it },
                        onExposureChange = { exposure = it },
                        onWarmthChange = { warmth = it }
                    )
                }
            }

            // Filter Controls
            AnimatedVisibility(
                visible = showFilters,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2A2A2A)
                ) {
                    FilterControls(
                        onFilterSelected = { filter ->
                            currentFilter = filter
                            // Cập nhật bitmap khi áp dụng filter
                            bitmap?.let { currentBitmap ->
                                gpuImage.setImage(currentBitmap)
                                gpuImage.setFilter(filter)
                                bitmap = gpuImage.bitmapWithFilterApplied
                                Log.d("FilterApply", "Filter applied: ${filter.javaClass.simpleName}")
                                Log.d("FilterApply", "Bitmap updated: ${bitmap?.width}x${bitmap?.height}")
                            }
                        }
                    )
                }
            }
        }
    }

    // Cập nhật GPUImage filters
    val filterGroup = GPUImageFilterGroup().apply {
        addFilter(GPUImageBrightnessFilter(brightness))
        addFilter(GPUImageContrastFilter(contrast))
        addFilter(GPUImageSaturationFilter(saturation))
        addFilter(GPUImageSharpenFilter(sharpness))
        addFilter(GPUImageExposureFilter(exposure))
        addFilter(GPUImageWhiteBalanceFilter(5500f + (warmth * 4000f), warmth * 0.1f))
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

// Enum class để quản lý các loại điều chỉnh màu
enum class ColorAdjustType {
    NONE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    SHARPNESS,
    EXPOSURE,
    WARMTH
}

@Composable
private fun ColorAdjustControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    sharpness: Float = 0f,
    exposure: Float = 0f,
    warmth: Float = 0f,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onSharpnessChange: (Float) -> Unit = {},
    onExposureChange: (Float) -> Unit = {},
    onWarmthChange: (Float) -> Unit = {},
) {
    var selectedAdjustment by remember { mutableStateOf(ColorAdjustType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Slider với animation
        AnimatedVisibility(
            visible = selectedAdjustment != ColorAdjustType.NONE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                when (selectedAdjustment) {
                    ColorAdjustType.BRIGHTNESS -> AdjustSlider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = -1f..1f,
                        label = "Brightness"
                    )
                    ColorAdjustType.CONTRAST -> AdjustSlider(
                        value = contrast,
                        onValueChange = onContrastChange,
                        valueRange = 0.5f..2f,
                        label = "Contrast"
                    )
                    ColorAdjustType.SATURATION -> AdjustSlider(
                        value = saturation,
                        onValueChange = onSaturationChange,
                        valueRange = 0f..2f,
                        label = "Saturation"
                    )
                    ColorAdjustType.SHARPNESS -> AdjustSlider(
                        value = sharpness,
                        onValueChange = onSharpnessChange,
                        valueRange = 0f..4f,
                        label = "Sharpness"
                    )
                    ColorAdjustType.EXPOSURE -> AdjustSlider(
                        value = exposure,
                        onValueChange = onExposureChange,
                        valueRange = -2f..2f,
                        label = "Exposure"
                    )
                    ColorAdjustType.WARMTH -> AdjustSlider(
                        value = warmth,
                        onValueChange = onWarmthChange,
                        valueRange = -1f..1f,
                        label = "Warmth"
                    )
                    ColorAdjustType.NONE -> {}
                }
            }
        }

        // Adjustment buttons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ColorAdjustType.values().filter { it != ColorAdjustType.NONE }) { type ->
                AdjustButton(
                    type = type,
                    isSelected = selectedAdjustment == type,
                    onClick = {
                        selectedAdjustment = if (selectedAdjustment == type) {
                            ColorAdjustType.NONE
                        } else {
                            type
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AdjustSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = String.format("%.1f", value),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdjustButton(
    type: ColorAdjustType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            painter = when (type) {
                ColorAdjustType.BRIGHTNESS -> painterResource(id = R.drawable.brightness)
                ColorAdjustType.CONTRAST -> painterResource(id = R.drawable.contrast)
                ColorAdjustType.SATURATION -> painterResource(id = R.drawable.saturation)
                ColorAdjustType.SHARPNESS -> painterResource(id = R.drawable.bleach)
                ColorAdjustType.EXPOSURE -> painterResource(id = R.drawable.exposure)
                ColorAdjustType.WARMTH -> painterResource(id = R.drawable.thermometer)
                ColorAdjustType.NONE -> painterResource(id = R.drawable.close)
            },
            contentDescription = type.name,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = type.name.lowercase().capitalize(),
            color = if (isSelected) Color.White else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp
        )

        // Indicator for selected item
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

// Cập nhật hàm saveBitmapToGallery để trả về Uri của ảnh đã lưu
private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val filename = "edited_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                uri
            }
        } catch (e: Exception) {
            Log.e("SaveImage", "Error saving image", e)
            null
        }
    }
}

enum class GPUFilterType {
    NONE,
    //SEPIA,
    GRAYSCALE,
    SKETCH,
    TOON,
    POSTERIZE,
    MONOCHROME,
    INVERT,
    VIGNETTE,
    KUWAHARA,
    GLASS_SPHERE,
    SWIRL
}

@Composable
private fun FilterControls(
    onFilterSelected: (GPUImageFilter) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(GPUFilterType.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(GPUFilterType.values()) { filterType ->
                FilterButton(
                    type = filterType,
                    isSelected = selectedFilter == filterType,
                    onClick = {
                        selectedFilter = filterType
                        val filter = when (filterType) {
                            GPUFilterType.NONE -> GPUImageFilter()
                            //GPUFilterType.SEPIA -> GPUImageSepiaFilter()
                            GPUFilterType.GRAYSCALE -> GPUImageGrayscaleFilter()
                            GPUFilterType.SKETCH -> GPUImageSketchFilter()
                            GPUFilterType.TOON -> GPUImageToonFilter()
                            GPUFilterType.POSTERIZE -> GPUImagePosterizeFilter()
                            GPUFilterType.MONOCHROME -> GPUImageMonochromeFilter()
                            GPUFilterType.INVERT -> GPUImageColorInvertFilter()
                            GPUFilterType.VIGNETTE -> GPUImageVignetteFilter()
                            GPUFilterType.KUWAHARA -> GPUImageKuwaharaFilter()
                            GPUFilterType.GLASS_SPHERE -> GPUImageGlassSphereFilter()
                            GPUFilterType.SWIRL -> GPUImageSwirlFilter()
                        }
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterButton(
    type: GPUFilterType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // Thumbnail preview của filter (có thể thêm sau)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = type.name.lowercase().capitalize(),
            color = if (isSelected) Color.White else Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

