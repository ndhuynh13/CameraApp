import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.midterm.cameraapp.data.GalleryImage
import com.midterm.cameraapp.ui.view.EditImageScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onImageClick: (Uri) -> Unit,
    onClose: () -> Unit,
    onDeleteImage: (GalleryImage) -> Unit,
    onEditImage: (GalleryImage) -> Unit,
    onReload: suspend () -> Unit
) {
    var selectedImages by remember { mutableStateOf<Set<GalleryImage>>(emptySet()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf<GalleryImage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<GalleryImage?>(null) }
    var showEditScreen by remember { mutableStateOf<GalleryImage?>(null) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) } // Thêm state mới

    val scope = rememberCoroutineScope()

    // Sắp xếp lại danh sách ảnh theo thời gian mới nhất
    val sortedImages = remember(images) {
        images.sortedByDescending { it.dateAdded }
    }
    // Thêm hàm xử lý chọn nhiều ảnh
    val handleImageSelection = { image: GalleryImage ->
        if (isMultiSelectMode) {
            selectedImages = if (selectedImages.contains(image)) {
                selectedImages - image
            } else {
                selectedImages + image
            }
            if (selectedImages.isEmpty()) {
                isMultiSelectMode = false
            }
        } else {
            showFullscreen = image
        }
    }

// Thêm hàm xử lý long press
    val handleLongPress = { image: GalleryImage ->
        if (!isMultiSelectMode) {
            isMultiSelectMode = true
            selectedImages = setOf(image)
        }
    }

    // Sử dụng coroutine scope để gọi suspend function
    LaunchedEffect(Unit) {
        scope.launch {
            onReload()
        }
    }
    // Single Delete Dialog
    showDeleteDialog?.let { imageToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Image") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteImage(imageToDelete)
                        showDeleteDialog = null
                        selectedImages = emptySet()
                        isMultiSelectMode = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2A2A2A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Multi Delete Dialog
    if (showMultiDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            title = { Text("Delete Images") },
            text = { Text("Are you sure you want to delete ${selectedImages.size} images?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedImages.forEach { onDeleteImage(it) }
                        selectedImages = emptySet()
                        isMultiSelectMode = false
                        showMultiDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMultiDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2A2A2A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showEditScreen != null) {
            EditImageScreen(
                image = showEditScreen!!,
                onClose = { showEditScreen = null },
                onSave = {
                    // Implement save functionality
                    showEditScreen = null
                    scope.launch {
                        onReload() // Reload gallery sau khi save
                    }
                }
            )
        } else if (showFullscreen != null) {
            FullscreenImageScreen(
                image = showFullscreen!!,
                images = images,
                onClose = { showFullscreen = null },
                onDelete = {
                    showDeleteDialog = showFullscreen
                },
                onImageChange = { newImage -> showFullscreen = newImage },
                onEdit = { image -> showEditScreen = image }
            )
        } else {
            // Background Box để xử lý click ra ngoài
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Bỏ hiệu ứng ripple
                    ) { selectedImages = emptySet() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top App Bar
                    SmallTopAppBar(
                        title = { Text("Gallery") },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            titleContentColor = Color.White
                        )
                    )

                    // Grid of images
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.clickable(enabled = false) {} // Ngăn chặn click event từ parent
                    ) {
                        items(sortedImages) { image ->
                            GalleryItem(
                                image = image,
                                isSelected = selectedImages.contains(image),
                                onImageClick = { handleImageSelection(image) },
                                onLongPress = { handleLongPress(image) }
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier.clickable(enabled = false) {}
                ) {
                    BottomActionBar(
                        onDelete = {
                            if (selectedImages.size == 1) {
                                showDeleteDialog = selectedImages.first()
                            } else {
                                showMultiDeleteDialog = true
                            }
                        },
                        onEdit = {
                            // Disable edit when multiple images selected
                        },
                        onDismiss = {
                            selectedImages = emptySet()
                            isMultiSelectMode = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryItem(
    image: GalleryImage,
    isSelected: Boolean,
    onImageClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onImageClick,
                onLongClick = onLongPress
            )
    ) {
        Image(
            painter = rememberAsyncImagePainter(image.uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Hiển thị dấu check khi ảnh được chọn
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.checkbox_on_background),
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A2A2A),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit Button
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2196F3))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
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