import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CameraGrid(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Duong doc
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(canvasWidth / 3f, 0f),
            end = Offset(canvasWidth / 3f, canvasHeight),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(2 * canvasWidth / 3f, 0f),
            end = Offset(2 * canvasWidth / 3f, canvasHeight),
            strokeWidth = 1f
        )

        // Duong ngang
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, canvasHeight / 3f),
            end = Offset(canvasWidth, canvasHeight / 3f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, 2 * canvasHeight / 3f),
            end = Offset(canvasWidth, 2 * canvasHeight / 3f),
            strokeWidth = 1f
        )
    }
}