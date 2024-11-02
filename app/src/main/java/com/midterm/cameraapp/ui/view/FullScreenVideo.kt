package com.midterm.cameraapp.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.ui.draw.clip
import com.midterm.cameraapp.data.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenVideoScreen(
    video: VideoItem,
    videos: List<VideoItem>,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onVideoChange: (VideoItem) -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Tìm vị trí hiện tại của video trong danh sách
    val currentIndex = videos.indexOf(video)

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete this video?") },
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
    ) {
        // Video Player
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setMediaController(MediaController(context).apply {
                        setAnchorView(this@apply)
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { videoView ->
                videoView.setVideoURI(video.uri)
                videoView.start()
            }
        )

        // Previous/Next Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous indicator
            AnimatedVisibility(
                visible = currentIndex > 0 && showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onVideoChange(videos[currentIndex - 1]) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Next indicator
            AnimatedVisibility(
                visible = currentIndex < videos.size - 1 && showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { onVideoChange(videos[currentIndex + 1]) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
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
                title = { Text(video.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
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

        // Bottom Control Bar
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