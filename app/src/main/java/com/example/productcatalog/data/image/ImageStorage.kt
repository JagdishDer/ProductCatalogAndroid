
package com.example.productcatalog.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

data class CameraTarget(
    val file: File,
    val contentUri: Uri
)

data class AnnotationStroke(
    val points: List<Offset>
)

object ImageStorage {

    private const val DIRECTORY = "product_images"
    private const val MAX_IMAGE_SIZE = 2048

    private fun imageDirectory(context: Context): File {
        return File(context.filesDir, DIRECTORY).apply {
            if (!exists() && !mkdirs()) {
                error("Could not create image directory")
            }
        }
    }

    fun createCameraTarget(context: Context): CameraTarget {
        val file = File.createTempFile(
            "camera_",
            ".jpg",
            imageDirectory(context)
        )

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return CameraTarget(file, uri)
    }

    suspend fun importFromGallery(
        context: Context,
        selectedUri: Uri
    ): String = withContext(Dispatchers.IO) {

        val mime = context.contentResolver.getType(selectedUri)

        val extension = when (mime) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/heic", "image/heif" -> ".heic"
            else -> ".jpg"
        }

        val destination = File.createTempFile(
            "gallery_",
            extension,
            imageDirectory(context)
        )

        try {
            val source = context.contentResolver
                .openInputStream(selectedUri)
                ?: error("Could not open selected image")

            source.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }

            require(destination.length() > 0) {
                "Selected image is empty"
            }

            Uri.fromFile(destination).toString()

        } catch (e: Exception) {
            destination.delete()
            throw e
        }
    }

    private fun openStream(
        context: Context,
        source: String
    ): InputStream {

        return if (source.startsWith("https://")) {
            val connection = URL(source).openConnection()

            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            connection.getInputStream()
        } else {
            context.contentResolver.openInputStream(
                Uri.parse(source)
            ) ?: error("Unable to open image")
        }
    }

    suspend fun loadBitmap(
        context: Context,
        source: String
    ): Bitmap = withContext(Dispatchers.IO) {

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        openStream(context, source).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        require(
            bounds.outWidth > 0 &&
                bounds.outHeight > 0
        ) {
            "Unsupported image format"
        }

        var sampleSize = 1

        while (
            bounds.outWidth / sampleSize > MAX_IMAGE_SIZE ||
            bounds.outHeight / sampleSize > MAX_IMAGE_SIZE
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = openStream(context, source).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Failed to decode image")

        // Correct common camera EXIF rotations.
        val rotation = runCatching {
            openStream(context, source).use {
                val exif = ExifInterface(it)

                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)

        if (rotation == 0f) {
            decoded
        } else {
            val rotated = Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotation) },
                true
            )

            if (rotated !== decoded) {
                decoded.recycle()
            }

            rotated
        }
    }

    suspend fun saveAnnotated(
        context: Context,
        sourceBitmap: Bitmap,
        strokes: List<AnnotationStroke>
    ): String = withContext(Dispatchers.IO) {

        val result = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeWidth = maxOf(
                3f,
                sourceBitmap.width * 0.006f
            )
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }

        strokes.forEach { stroke ->
            val points = stroke.points

            if (points.size == 1) {
                canvas.drawCircle(
                    points[0].x,
                    points[0].y,
                    paint.strokeWidth / 2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.RED
                        style = Paint.Style.FILL
                    }
                )
            } else if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)

                    points.drop(1).forEach {
                        lineTo(it.x, it.y)
                    }
                }

                canvas.drawPath(path, paint)
            }
        }

        val file = File.createTempFile(
            "annotated_",
            ".png",
            imageDirectory(context)
        )

        try {
            FileOutputStream(file).use { stream ->
                check(
                    result.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        stream
                    )
                ) {
                    "Could not save annotated image"
                }
            }

            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            result.recycle()
        }
    }
}
