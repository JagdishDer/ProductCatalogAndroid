
package com.example.productcatalog.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.productcatalog.data.model.Product
import com.example.productcatalog.data.model.ProductCategory
import com.example.productcatalog.viewmodel.ProductUiState
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ProductListScreen(
    state: ProductUiState,
    onSelectCategory: (String?) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onRetryCategories: () -> Unit,
    onLogout: () -> Unit
) {
    val listState = rememberLazyListState()

    // Return to the top whenever the selected filter changes.
    LaunchedEffect(state.selectedCategory) {
        listState.scrollToItem(0)
    }

    // Automatically request the next page near the list bottom.
    LaunchedEffect(
        listState,
        state.products.size,
        state.hasMore,
        state.isLoading,
        state.isLoadingMore,
        state.loadMoreError,
        state.error
    ) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val nearBottom =
                    lastVisible >= state.products.size - 4

                if (
                    state.products.isNotEmpty() &&
                    nearBottom &&
                    state.hasMore &&
                    !state.isLoading &&
                    !state.isLoadingMore &&
                    state.error == null &&
                    state.loadMoreError == null
                ) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Products",
                style = MaterialTheme.typography.headlineSmall
            )

            Row {
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }

                TextButton(onClick = onLogout) {
                    Text("Logout")
                }
            }
        }

        // Category dropdown and product count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CategoryDropdown(
                    categories = state.categories,
                    selectedSlug = state.selectedCategory,
                    onSelected = onSelectCategory
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = "${state.products.size} / ${state.total}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (state.categoriesError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories unavailable",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onRetryCategories) {
                    Text("Retry")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        HorizontalDivider()

        // Content states
        when {
            state.isLoading -> {
                CenteredMessage(
                    message = "Loading products...",
                    showProgress = true
                )
            }

            state.error != null -> {
                ErrorContent(
                    message = state.error,
                    onRetry = onRefresh
                )
            }

            state.products.isEmpty() -> {
                ErrorContent(
                    message = "No products found.",
                    onRetry = onRefresh,
                    buttonText = "Refresh"
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        vertical = 8.dp
                    )
                ) {
                    items(
                        items = state.products,
                        key = { it.id }
                    ) { product ->
                        ProductRow(product)
                    }

                    item(key = "pagination_footer") {
                        PaginationFooter(
                            state = state,
                            onRetry = onRetryLoadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<ProductCategory>,
    selectedSlug: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedName = if (selectedSlug == null) {
        "All categories"
    } else {
        categories.firstOrNull {
            it.slug == selectedSlug
        }?.name ?: selectedSlug
    }

    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(
                text = selectedName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(6.dp))
            Text("▾")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All categories") },
                onClick = {
                    expanded = false
                    onSelected(null)
                }
            )

            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onSelected(category.slug)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (product.thumbnail.isNullOrBlank()) {
                Text(
                    text = "No image",
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                SubcomposeAsyncImage(
                    model = product.thumbnail,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No image",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = product.category.orEmpty()
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (!product.description.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(
            start = 114.dp,
            end = 16.dp
        ),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun PaginationFooter(
    state: ProductUiState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp)
                )
            }

            state.loadMoreError != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.loadMoreError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(onClick = onRetry) {
                        Text("Retry loading more")
                    }
                }
            }

            !state.hasMore -> {
                Text(
                    text = "All products loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    showProgress: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showProgress) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }

            Text(message)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    buttonText: String = "Retry"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text(buttonText)
            }
        }
    }
}
