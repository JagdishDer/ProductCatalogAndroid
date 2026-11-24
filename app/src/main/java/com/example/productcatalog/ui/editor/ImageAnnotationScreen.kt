
package com.example.productcatalog.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.productcatalog.data.image.AnnotationStroke
import com.example.productcatalog.data.image.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class AnnotationMode {
    DRAW,
    ZOOM
}

@Composable
fun ImageAnnotationScreen(
    source: String,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember(source) {
        mutableStateOf<Bitmap?>(null)
    }

    var loadError by remember(source) {
        mutableStateOf<String?>(null)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var mode by remember(source) {
        mutableStateOf(AnnotationMode.DRAW)
    }

    var zoom by remember(source) {
        mutableFloatStateOf(1f)
    }

    var pan by remember(source) {
        mutableStateOf(Offset.Zero)
    }

    var viewport by remember(source) {
        mutableStateOf(IntSize.Zero)
    }

    val strokes: SnapshotStateList<AnnotationStroke> =
        remember(source) {
            mutableStateListOf()
        }

    var currentPoints by remember(source) {
        mutableStateOf<List<Offset>>(emptyList())
    }

    LaunchedEffect(source) {
        try {
            bitmap = ImageStorage.loadBitmap(
                context,
                source
            )
        } catch (e: Exception) {
            loadError = e.message ?: "Unable to load image"
        }
    }

    BackHandler(enabled = !saving) {
        onBack()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                enabled = !saving
            ) {
                Text("Cancel")
            }

            Text(
                "Annotate Image",
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                enabled = bitmap != null && !saving,
                onClick = {
                    val currentBitmap = bitmap
                        ?: return@TextButton

                    saving = true

                    scope.launch {
                        try {
                            val result = ImageStorage.saveAnnotated(
                                context = context,
                                sourceBitmap = currentBitmap,
                                strokes = strokes.toList()
                            )
                            onSaved(result)
                        } catch (e: Exception) {
                            loadError = e.message
                                ?: "Unable to save image"
                        } finally {
                            saving = false
                        }
                    }
                }
            ) {
                Text(if (saving) "Saving..." else "Save")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    mode = AnnotationMode.DRAW
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (mode == AnnotationMode.DRAW)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Draw")
            }

            Button(
                onClick = {
                    mode = AnnotationMode.ZOOM
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (mode == AnnotationMode.ZOOM)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Move / Zoom")
            }

            TextButton(
                enabled = strokes.isNotEmpty() && !saving,
                onClick = {
                    if (strokes.isNotEmpty()) {
                        strokes.removeAt(strokes.lastIndex)
                    }
                }
            ) {
                Text("Undo")
            }
        }

        Spacer(Modifier.height(8.dp))

        val loaded = bitmap

        when {
            loadError != null -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            loaded == null -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val image = remember(loaded) {
                    loaded.asImageBitmap()
                }

                // Convert a touch point on screen into the
                // corresponding original bitmap coordinate.
                fun screenToBitmap(point: Offset): Offset? {
                    if (viewport.width == 0 ||
                        viewport.height == 0
                    ) return null

                    val baseScale = min(
                        viewport.width.toFloat() / loaded.width,
                        viewport.height.toFloat() / loaded.height
                    )

                    val factor = baseScale * zoom

                    val displayWidth = loaded.width * factor
                    val displayHeight = loaded.height * factor

                    val left =
                        (viewport.width - displayWidth) / 2f + pan.x

                    val top =
                        (viewport.height - displayHeight) / 2f + pan.y

                    val x = (point.x - left) / factor
                    val y = (point.y - top) / factor

                    if (x < 0f || y < 0f ||
                        x > loaded.width ||
                        y > loaded.height
                    ) return null

                    return Offset(x, y)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.DarkGray)
                        .onSizeChanged { viewport = it }
                        .pointerInput(mode, loaded) {
                            if (mode == AnnotationMode.DRAW) {
                                detectDragGestures(
                                    onDragStart = { point ->
                                        val bitmapPoint =
                                            screenToBitmap(point)

                                        currentPoints =
                                            if (bitmapPoint != null)
                                                listOf(bitmapPoint)
                                            else
                                                emptyList()
                                    },

                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            strokes.add(
                                                AnnotationStroke(
                                                    currentPoints
                                                )
                                            )
                                        }
                                        currentPoints = emptyList()
                                    },

                                    onDragCancel = {
                                        currentPoints = emptyList()
                                    }
                                ) { change, _ ->
                                    val bitmapPoint =
                                        screenToBitmap(change.position)

                                    if (bitmapPoint != null &&
                                        currentPoints.isNotEmpty()
                                    ) {
                                        currentPoints =
                                            currentPoints + bitmapPoint
                                    }

                                    change.consume()
                                }
                            } else {
                                detectTransformGestures {
                                    centroid,
                                    panChange,
                                    zoomChange,
                                    _ ->

                                    val oldZoom = zoom

                                    val newZoom =
                                        (oldZoom * zoomChange)
                                            .coerceIn(1f, 5f)

                                    val center = Offset(
                                        viewport.width / 2f,
                                        viewport.height / 2f
                                    )

                                    val ratio = newZoom / oldZoom

                                    val newPan = pan + panChange +
                                        (centroid - center - pan) *
                                        (1f - ratio)

                                    val baseScale = min(
                                        viewport.width.toFloat() /
                                            loaded.width,
                                        viewport.height.toFloat() /
                                            loaded.height
                                    )

                                    val maxX = max(
                                        0f,
                                        (
                                            loaded.width *
                                                baseScale * newZoom -
                                                viewport.width
                                        ) / 2f
                                    )

                                    val maxY = max(
                                        0f,
                                        (
                                            loaded.height *
                                                baseScale * newZoom -
                                                viewport.height
                                        ) / 2f
                                    )

                                    zoom = newZoom

                                    pan = if (newZoom <= 1.001f) {
                                        Offset.Zero
                                    } else {
                                        Offset(
                                            newPan.x.coerceIn(
                                                -maxX, maxX
                                            ),
                                            newPan.y.coerceIn(
                                                -maxY, maxY
                                            )
                                        )
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val baseScale = min(
                            size.width / loaded.width,
                            size.height / loaded.height
                        )

                        val factor = baseScale * zoom

                        val displayWidth =
                            loaded.width * factor

                        val displayHeight =
                            loaded.height * factor

                        val left =
                            (size.width - displayWidth) / 2f +
                                pan.x

                        val top =
                            (size.height - displayHeight) / 2f +
                                pan.y

                        drawImage(
                            image = image,
                            dstOffset = IntOffset(
                                left.roundToInt(),
                                top.roundToInt()
                            ),
                            dstSize = IntSize(
                                max(1, displayWidth.roundToInt()),
                                max(1, displayHeight.roundToInt())
                            )
                        )

                        strokes.forEach { stroke ->
                            drawStrokeOnScreen(
                                points = stroke.points,
                                left = left,
                                top = top,
                                factor = factor,
                                sourceWidth = loaded.width
                            )
                        }

                        drawStrokeOnScreen(
                            points = currentPoints,
                            left = left,
                            top = top,
                            factor = factor,
                            sourceWidth = loaded.width
                        )
                    }
                }
            }
        }

        if (saving) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = if (mode == AnnotationMode.DRAW)
                "Draw over the image with one finger."
            else
                "Pinch to zoom or drag to move the image.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun DrawScope.drawStrokeOnScreen(
    points: List<Offset>,
    left: Float,
    top: Float,
    factor: Float,
    sourceWidth: Int
) {
    if (points.isEmpty()) return

    fun transform(point: Offset): Offset {
        return Offset(
            left + point.x * factor,
            top + point.y * factor
        )
    }

    val lineWidth = max(
        2f,
        sourceWidth * 0.006f * factor
    )

    if (points.size == 1) {
        drawCircle(
            color = Color.Red,
            radius = lineWidth / 2f,
            center = transform(points[0])
        )
        return
    }

    val path = Path()

    points.forEachIndexed { index, point ->
        val position = transform(point)

        if (index == 0) {
            path.moveTo(position.x, position.y)
        } else {
            path.lineTo(position.x, position.y)
        }
    }

    drawPath(
        path = path,
        color = Color.Red,
        style = Stroke(
            width = lineWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
