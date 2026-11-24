
package com.example.productcatalog.ui.editor

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.productcatalog.data.model.*
import com.example.productcatalog.viewmodel.ProductUiState

@Composable
fun ProductEditorScreen(
    product: Product?,
    state: ProductUiState,
    onCancel: () -> Unit,
    onSave: (ProductWriteRequest) -> Unit
) {
    val editing = product != null

    var title by rememberSaveable(product?.id) {
        mutableStateOf(product?.title.orEmpty())
    }

    var description by rememberSaveable(product?.id) {
        mutableStateOf(product?.description.orEmpty())
    }

    var category by rememberSaveable(product?.id) {
        mutableStateOf(product?.category.orEmpty())
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    val canSave = title.isNotBlank() &&
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !state.isSaving
            ) {
                Text("Back")
            }

            Text(
                text = if (editing) "Edit Product"
                else "Add Product",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Product Name *",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Enter product name") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        Text(
            "Description",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = {
                Text("Enter product description")
            },
            minLines = 3,
            maxLines = 5,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        Text(
            "Category *",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { categoryExpanded = true },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedName = state.categories
                    .firstOrNull { it.slug == category }
                    ?.name ?: category.ifBlank {
                    "Select Category"
                }

                Text(
                    selectedName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

        if (state.categoriesError != null) {
            Text(
                "Categories could not be loaded.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Product Image",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.height(8.dp))

        if (!product?.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = product?.thumbnail,
                contentDescription = "Product image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image selected")
                }
            }
        }

        Text(
            text = "Camera, gallery and annotation " +
                "will be added in Phase 4.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        state.mutationError?.let { message ->
            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    ProductWriteRequest(
                        title = title.trim(),
                        description = description.trim(),
                        category = category
                    )
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
                    if (editing) "Update Product"
                    else "Add Product"
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
