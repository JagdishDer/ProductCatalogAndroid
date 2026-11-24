
package com.example.productcatalog.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.productcatalog.data.image.ImageStorage
import com.example.productcatalog.data.model.Product
import com.example.productcatalog.data.model.ProductWriteRequest
import com.example.productcatalog.viewmodel.ProductUiState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProductEditorScreen(
    product: Product?,
    state: ProductUiState,
    onCancel: () -> Unit,
    onSave: (ProductWriteRequest, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable(product?.id) {
        mutableStateOf(product?.title.orEmpty())
    }

    var description by rememberSaveable(product?.id) {
        mutableStateOf(product?.description.orEmpty())
    }

    var category by rememberSaveable(product?.id) {
        mutableStateOf(product?.category.orEmpty())
    }

    var imageUri by rememberSaveable(product?.id) {
        mutableStateOf(product?.thumbnail)
    }

    var annotationOpen by rememberSaveable(product?.id) {
        mutableStateOf(false)
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    var imageError by remember {
        mutableStateOf<String?>(null)
    }

    var cameraPermissionDenied by rememberSaveable {
        mutableStateOf(false)
    }

    var pendingCameraPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Gallery picker: no broad storage permission required.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->

        if (selectedUri != null) {
            scope.launch {
                try {
                    imageError = null

                    imageUri = ImageStorage.importFromGallery(
                        context,
                        selectedUri
                    )
                } catch (e: Exception) {
                    imageError = e.message
                        ?: "Unable to import image"
                }
            }
        }
    }

    // Camera: TakePicture writes the full-sized photo
    // into the FileProvider URI supplied to it.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->

        val path = pendingCameraPath
        pendingCameraPath = null

        if (
            success &&
            path != null &&
            File(path).exists() &&
            File(path).length() > 0
        ) {
            imageUri = Uri.fromFile(
                File(path)
            ).toString()

            imageError = null
        } else {
            path?.let { File(it).delete() }

            imageError =
                "Photo capture was cancelled or failed."
        }
    }

    fun startCameraCapture() {
        try {
            val target = ImageStorage.createCameraTarget(
                context
            )

            pendingCameraPath = target.file.absolutePath
            imageError = null

            cameraLauncher.launch(target.contentUri)
        } catch (e: Exception) {
            pendingCameraPath?.let {
                File(it).delete()
            }

            pendingCameraPath = null

            imageError = e.message
                ?: "Unable to open camera"
        }
    }

    // Explicit permission flow for the assessment.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            cameraPermissionDenied = false
            startCameraCapture()
        } else {
            cameraPermissionDenied = true

            imageError =
                "Camera permission denied. Enable it in app settings."
        }
    }

    // Show full-screen annotation editor.
    if (annotationOpen && !imageUri.isNullOrBlank()) {

        ImageAnnotationScreen(
            source = imageUri!!,
            onBack = {
                annotationOpen = false
            },
            onSaved = { savedUri ->
                imageUri = savedUri
                imageError = null
                annotationOpen = false
            }
        )

        return
    }

    val canSave =
        title.isNotBlank() &&
        category.isNotBlank() &&
        !state.isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !state.isSaving
            ) {
                Text("Back")
            }

            Text(
                if (product == null)
                    "Add Product"
                else
                    "Edit Product",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("Product Name *")

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Name") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Description")

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 5,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Category *")

        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { categoryExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                val selectedName = state.categories
                    .firstOrNull { it.slug == category }
                    ?.name
                    ?: category.ifBlank {
                        "Select category"
                    }

                Text(selectedName)
                Spacer(Modifier.width(8.dp))
                Text("▾")
            }

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = {
                    categoryExpanded = false
                }
            ) {
                state.categories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            category = item.slug
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "Product Image",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(10.dp))

        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Selected product image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                color =
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image selected")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts
                                .PickVisualMedia.ImageOnly
                        )
                    )
                },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Gallery")
            }

            Button(
                onClick = {
                    val granted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        cameraPermissionDenied = false
                        startCameraCapture()
                    } else {
                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Camera")
            }
        }

        if (cameraPermissionDenied) {
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse(
                            "package:${context.packageName}"
                        )
                    )
                    context.startActivity(intent)
                }
            ) {
                Text("Open App Settings")
            }
        }

        if (!imageUri.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    annotationOpen = true
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Draw / Annotate / Zoom")
            }
        }

        imageError?.let { error ->
            Spacer(Modifier.height(10.dp))

            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.mutationError?.let { error ->
            Spacer(Modifier.height(10.dp))

            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = {
                onSave(
                    ProductWriteRequest(
                        title = title.trim(),
                        description = description.trim(),
                        category = category
                    ),
                    imageUri
                )
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (product == null)
                        "Add Product"
                    else
                        "Update Product"
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
