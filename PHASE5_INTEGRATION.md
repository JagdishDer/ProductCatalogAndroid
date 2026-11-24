# Phase 5 — Integration steps for the two JVM tests

These files are **additions**, not a replacement for the working Android Studio project. Do not replace existing `ProductModels.kt`, `ProductViewModel.kt`, or Gradle files wholesale.

## 1. Copy the files to your project

- `app/src/main/java/com/example/productcatalog/data/model/ProductSessionLogic.kt`
- `app/src/test/java/com/example/productcatalog/data/model/ProductSessionLogicTest.kt`

The `Product` data class and the Phase 3/4 `ProductViewModel` must already be present.

## 2. Wire the tested functions into ProductViewModel

Add this import if your file doesn't already use `import com.example.productcatalog.data.model.*`:

```kotlin
import com.example.productcatalog.data.model.ProductSessionLogic
```

Replace **only** the existing `private fun visibleProducts(): List<Product>` function with:

```kotlin
private fun visibleProducts(): List<Product> =
    ProductSessionLogic.visibleProducts(
        remoteProducts = remoteProducts,
        addedProducts = addedProducts,
        editedProducts = editedProducts,
        deletedIds = deletedIds,
        selectedCategory = _state.value.selectedCategory
    )
```

In `refresh()` and `loadNextPage()`, replace **both occurrences** of:

```kotlin
val next = ProductSessionLogic.nextServerOffset(
    serverSkip = page.skip,
    returnedCount = page.products.size
)
```

with:

```kotlin
val next = ProductSessionLogic.nextServerOffset(
    serverSkip = page.skip,
    returnedCount = page.products.size
)
```

Do not modify the existing `saveProduct`, `deleteProduct`, `publishProducts`, authentication, image storage, or annotation code.

## 3. Confirm the test dependency

If `app/build.gradle.kts` already contains `testImplementation(libs.junit)`, keep it. Otherwise add `testImplementation("junit:junit:4.13.2")` to `dependencies`.

## 4. Test and build

From the **actual** Android project root:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Open the HTML report at `app/build/reports/tests/testDebugUnitTest/index.html` if a test fails.

## 5. Important caveat

These two tests verify JVM-only logic. They do **not** replace manual camera, permission, pinch-to-zoom, annotation export, or device testing. Record results accurately in README before submitting.
