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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    onImageClick: (Uri) -> Unit,
    onClose: () -> Unit,
    onDeleteImage: (GalleryImage) -> Unit,
    onEditImage: (GalleryImage) -> Unit,
) {
    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }
    var showFullscreen by remember { mutableStateOf<GalleryImage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<GalleryImage?>(null) }
    var showEditScreen by remember { mutableStateOf<GalleryImage?>(null) }

    // Delete Confirmation Dialog
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
                        showFullscreen = null // Đóng fullscreen nếu đang mở
                        selectedImage = null // Đóng bottom action bar nếu đang mở
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (showEditScreen != null) {
            EditImageScreen(
                image = showEditScreen!!,
                onClose = { showEditScreen = null },
                onSave = {
                    // Implement save functionality
                    showEditScreen = null
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
                    ) { selectedImage = null }
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
                        items(images) { image ->
                            GalleryItem(
                                image = image,
                                onImageClick = { showFullscreen = image },
                                onLongPress = { selectedImage = image }
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            AnimatedVisibility(
                visible = selectedImage != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier.clickable(enabled = false) {}
                ) {
                    BottomActionBar(
                        onDelete = {
                            showDeleteDialog = selectedImage // Hiển thị dialog xác nhận
                        },
                        onEdit = {
                            selectedImage?.let { onEditImage(it) }
                            selectedImage = null
                        },
                        onDismiss = { selectedImage = null }
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