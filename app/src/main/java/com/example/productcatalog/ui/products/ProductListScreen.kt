
package com.example.productcatalog.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.productcatalog.data.model.*
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
    onLogout: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onClearMutationError: () -> Unit
) {
    val listState = rememberLazyListState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.selectedCategory) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(state.mutationError) {
        state.mutationError?.let { message ->
            snackbarHost.showSnackbar(message)
            onClearMutationError()
        }
    }

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
                if (
                    state.products.isNotEmpty() &&
                    lastVisible >= state.products.size - 4 &&
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

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Products",
                    style =
                        MaterialTheme.typography.headlineSmall
                )

                TextButton(onClick = onLogout) {
                    Text("Logout")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CategorySelector(
                        categories = state.categories,
                        selected = state.selectedCategory,
                        onSelected = onSelectCategory
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(onClick = onAdd) {
                    Text("+ Add")
                }
            }

            if (state.categoriesError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Categories unavailable",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error
                    )

                    TextButton(onClick = onRetryCategories) {
                        Text("Retry")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.products.size} session products",
                    style = MaterialTheme.typography.bodySmall
                )

                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            HorizontalDivider()

            when {
                state.isLoading -> {
                    CenterMessage(
                        "Loading products...",
                        loading = true
                    )
                }

                state.error != null -> {
                    RetryMessage(
                        state.error,
                        onRefresh
                    )
                }

                state.products.isEmpty() -> {
                    RetryMessage(
                        "No products found",
                        onRefresh
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
                            SwipeProductRow(
                                product = product,
                                deleting =
                                    product.id in state.deletingIds,
                                onEdit = {
                                    onEdit(product.id)
                                },
                                onDelete = {
                                    onDelete(product.id)
                                }
                            )
                        }

                        item(key = "footer") {
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
}

@Composable
private fun CategorySelector(
    categories: List<ProductCategory>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val label = if (selected == null) {
        "All categories"
    } else {
        categories.firstOrNull { it.slug == selected }
            ?.name ?: selected
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Text("▾")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
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
private fun SwipeProductRow(
    product: Product,
    deleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (
                target == SwipeToDismissBoxValue.EndToStart &&
                !deleting
            ) {
                onDelete()
            }

            // Wait for API confirmation before removing.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !deleting,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFEE2E2))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Delete",
                    color = Color(0xFFB91C1C),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = !deleting) {
                    onEdit()
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (product.thumbnail.isNullOrBlank()) {
                    Text(
                        "No image",
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
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No image")
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.category.orEmpty()
                        .replace("-", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (product.isSessionOnly) {
                    Text(
                        "Added this session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    HorizontalDivider()
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
                    modifier = Modifier.size(24.dp)
                )
            }

            state.loadMoreError != null -> {
                TextButton(onClick = onRetry) {
                    Text("Retry loading more")
                }
            }

            !state.hasMore -> {
                Text(
                    "All products loaded",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CenterMessage(
    text: String,
    loading: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }
            Text(text)
        }
    }
}

@Composable
private fun RetryMessage(
    message: String,
    onRetry: () -> Unit
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
            Text(message)

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
