import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.midterm.cameraapp.data.GalleryImage
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenImageScreen(
    image: GalleryImage,
    images: List<GalleryImage>, // Thêm danh sách ảnh
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onImageChange: (GalleryImage) -> Unit,
    onEdit: (GalleryImage) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Tìm vị trí hiện tại của ảnh trong danh sách
    val currentIndex = images.indexOf(image)
    // Reset values when image changes
    LaunchedEffect(image) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Image") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2A2A2A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { tapOffset ->
                        // Xử lý double tap zoom
                        if (scale > 1f) {
                            // Reset zoom
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Zoom vào điểm được click
                            val targetScale = 2f
                            val zoomFactor = targetScale / scale

                            // Tính toán center point của view
                            val centerX = size.width / 2
                            val centerY = size.height / 2

                            // Tính toán offset mới để giữ điểm click ở vị trí cũ
                            offsetX = (centerX - tapOffset.x) * (zoomFactor - 1)
                            offsetY = (centerY - tapOffset.y) * (zoomFactor - 1)

                            scale = targetScale
                        }
                    }
                )
            }
            .pointerInput(image) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (scale <= 1f) {
                        // Chỉ cho phép swipe khi không zoom
                        if (abs(pan.x) > abs(pan.y)) { // Đảm bảo swipe ngang
                            when {
                                pan.x > 50 && currentIndex > 0 -> {
                                    // Swipe phải -> ảnh trước
                                    onImageChange(images[currentIndex - 1])
                                }

                                pan.x < -50 && currentIndex < images.size - 1 -> {
                                    // Swipe trái -> ảnh tiếp
                                    onImageChange(images[currentIndex + 1])
                                }
                            }
                        }
                    } else {
                        // Xử lý zoom và pan bình thường
                        scale = (scale * zoom).coerceIn(0.5f..3f)
                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    }
                }
            }
    ) {
        // Zoomable Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )

        // Thêm indicators cho previous/next
        if (scale <= 1f) {
            // Previous indicator
            AnimatedVisibility(
                visible = currentIndex > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Next indicator
            AnimatedVisibility(
                visible = currentIndex < images.size - 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Top Bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }

        // Bottom Info Bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Share functionality */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { onEdit(image) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE53935))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}