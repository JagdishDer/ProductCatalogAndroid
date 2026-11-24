# Product Catalog — Native Android (Kotlin)

> **Mobile Developer Assessment — Task 1**  
> Kotlin · Jetpack Compose · MVVM · Retrofit · Android Keystore · Coil

A compact native Android product-catalog application using the [DummyJSON API](https://dummyjson.com/docs). It supports secure login, a category-filtered paginated catalog, simulated CRUD operations, product photos from the camera or gallery, and freehand image annotation with undo and pinch-to-zoom.

**Assessment scope:** This repository/folder contains **Task 1 (Android)**. Task 2 (iOS Swift, no AI permitted) and Task 3 (React Native with custom camera modules) are separate deliverables. AI assistance is permitted for this Android task and is documented in [`AI_PROMPT_LOG.md`](AI_PROMPT_LOG.md).

## Features

| Feature | Implementation |
|---|---|
| Login | `POST /auth/login` using Retrofit; invalid credentials and network errors are reported |
| Secure token | Access token encrypted with AES-GCM and an Android Keystore-backed key; Bearer interceptor on subsequent requests |
| Session restoration | Saved access token verified with `GET /auth/me` when the app starts |
| Catalog | Product title, category, description and thumbnail displayed using Compose and Coil |
| Pagination | `GET /products?limit=20&skip=N`, loading more as the user scrolls |
| Category filtering | `GET /products/categories` and `GET /products/category/{slug}` |
| Add product | `POST /products/add`; simulated response added to in-memory session list |
| Edit product | `PUT /products/{id}`; returned fields overlay the current list |
| Swipe-to-delete | `DELETE /products/{id}`; existing row removed only after API confirmation |
| Gallery | Android Photo Picker; selected image copied into app-private files |
| Camera | System camera via `TakePicture` and `FileProvider`; explicit permission-denial UI |
| Annotation | Compose Canvas freehand strokes, undo, draw/zoom modes, pinch and pan |
| Image export | Annotated PNG written to app-private storage and referenced by the product row |
| UI states | Loading, empty, error/retry, paginating, and mutation feedback |

## Requirements

- **Android Studio:** A current version capable of building the generated Compose project; use the versions checked in with the project.
- **JDK / Gradle:** Use the Gradle JDK selected by Android Studio and the repository's `gradlew` wrapper; do not force older plugin/Kotlin versions.
- **Android minimum SDK:** **API 26 (Android 8.0)**, as selected when creating the project. Verify the actual value in `app/build.gradle.kts` before submitting.
- **Compile and target SDK:** **Use the exact values in `app/build.gradle.kts`**; the assessment conversation did not establish their final numeric values.
- **Testing device:** Android emulator with camera support or a physical Android device. A physical device is preferable for camera and gesture tests.
- **Internet:** Required to reach `https://dummyjson.com`.

## Project structure

```text
app/src/main/java/com/example/productcatalog/
├── MainActivity.kt
├── data/
│   ├── image/ImageStorage.kt
│   ├── model/
│   │   ├── AuthModels.kt
│   │   ├── ProductModels.kt
│   │   ├── ProductWriteModels.kt
│   │   └── ProductSessionLogic.kt
│   ├── remote/{ApiClient,DummyJsonApi}.kt
│   ├── repository/{AuthRepository,ProductRepository}.kt
│   └── security/SecureTokenStore.kt
├── ui/
│   ├── login/LoginScreen.kt
│   ├── products/ProductListScreen.kt
│   └── editor/{ProductEditorScreen,ImageAnnotationScreen}.kt
└── viewmodel/{AuthViewModel,ProductViewModel}.kt

app/src/test/java/com/example/productcatalog/data/model/
└── ProductSessionLogicTest.kt
```

## Build and run

1. Clone/download the repository and open the **Android project root** (the directory containing `settings.gradle.kts`) in Android Studio.
2. Let Gradle Sync complete. Use the SDK versions required by the project; install missing SDK packages using SDK Manager.
3. Start an emulator or attach an Android device with USB debugging enabled.
4. Use Android Studio **Run**, or run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The generated debug APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

> The APK is a **debug build** for assessment/review. It is not a signed release artifact.

### Demo credentials

| Field | Value |
|---|---|
| Username | `emilys` |
| Password | `emilyspass` |

These public test credentials are documented by DummyJSON. Do not hardcode real credentials or include real access tokens in source control.

## How to use

1. Log in with the test credentials. The token is securely stored and verified using `/auth/me`.
2. Browse the catalog; scroll down to load additional pages.
3. Select a category to narrow results, or choose **All categories**.
4. Tap **+ Add** to create a product or tap an existing row to edit it.
5. Choose a photo from **Gallery** or **Camera**. Use **Draw / Annotate / Zoom** to open the image editor.
6. Draw strokes, tap **Undo**, switch to **Move / Zoom** for pinch-to-zoom/pan, then save the annotated PNG.
7. Save the product. The local image appears in its catalog row.
8. Swipe an existing row left to delete. If the API operation fails, the row stays visible.
9. Logout to clear the saved session token and reset in-memory product mutations.

## API endpoints

Base URL: `https://dummyjson.com/`

| Method | Path | Use |
|---|---|---|
| POST | `/auth/login` | Login |
| GET | `/auth/me` | Verify Bearer access token |
| GET | `/products?limit=20&skip=0` | First product page |
| GET | `/products/categories` | Category dropdown |
| GET | `/products/category/{slug}` | Filter by category |
| POST | `/products/add` | Simulated add |
| PUT | `/products/{id}` | Simulated edit |
| DELETE | `/products/{id}` | Simulated delete |

**Important:** DummyJSON **does not persist add/update/delete operations**. The app overlays successful API responses over subsequently fetched pages for the current session. Locally added products use unique negative client IDs because the fake API may return repeated simulated server IDs. Editing/deleting those locally added rows happens locally after their initial POST because they have no persisted server record.

## Local image and storage design

- A system Photo Picker chooses gallery media without broad gallery access.
- Camera capture writes through a `FileProvider` URI into app-private files.
- Gallery photos are copied into app-private files so the preview does not depend on a temporary picker grant.
- The annotation editor tracks source bitmap coordinates; viewport scaling and pan only affect on-screen rendering. Saving exports the image with strokes overlaid as a local PNG.
- Images are not uploaded to DummyJSON. The catalog references either a server thumbnail URL or a local image URI.
- The image files can remain on disk even after their in-memory product association disappears; orphan cleanup is not included.

## Tests

Run exactly the two locally scoped JVM tests supplied with this assessment:

```bash
./gradlew :app:testDebugUnitTest
```

The test class `ProductSessionLogicTest` contains:

1. **Session overlay:** edited and added products remain visible while deleted products stay hidden after a fake server refresh and category filter.
2. **Pagination:** the next `skip` is determined by the server response size, not by locally visible rows.

JUnit test reports can be found at `app/build/reports/tests/testDebugUnitTest/index.html` after execution.

### Manual acceptance checks

- [ ] Valid login, invalid credentials and network failure
- [ ] Token restored on app relaunch, and removed at logout
- [ ] Loading, empty, retry and category-filter states
- [ ] Pagination and filter changes without duplicates
- [ ] Add, edit and swipe-delete; changes preserved after refresh during session
- [ ] Denied camera permission and camera cancellation
- [ ] Gallery selection and camera photo capture
- [ ] Draw, undo, pinch zoom, pan and export annotated image
- [ ] Saved local image displayed in the product list
- [ ] `:app:testDebugUnitTest` passes
- [ ] `:app:assembleDebug` passes

**Verification status as of 2026-11-24:** The developer reported that Phases 1–4 **build and work**, including the image features. The Phase 5 test files and integration changes were generated afterward; their execution on the developer's Mac and the final APK still require independent verification. Do not mark the checks above as completed without running them.

## Scope cuts / known limitations

- No search, offline browsing, or image upload, per assessment instructions.
- No production-grade UI polish, accessibility audit, analytics or comprehensive test suite.
- DummyJSON mutations exist only in app memory and reset after logout or process recreation.
- Locally added products cannot be PUT/DELETE'd on the fake server; their later edits and deletions are local-only.
- Saved images may remain as unused files after editing, cancellation or deletion.
- Annotation is limited to a single red pen and basic undo; no redo, variable pen sizes, or full EXIF mirrored-orientation support.
- Images are decoded at a bounded size to reduce memory pressure, not exported at original camera resolution.
- Token refresh is not implemented; expired access tokens may require a new login.
- Camera permission is explicitly requested and denial handled; capture depends on an available camera application.
- The local image storage is not encrypted (the **token** is encrypted). Images are in app-private files.

## What I would do next

1. Improve token-expiration handling, refresh, and separation of API/session dependencies.
2. Add automatic orphan-image cleanup and smaller thumbnails.
3. Add UI tests for swiping, permission denial, drawing/zoom coordinates and configuration changes.
4. Replace temporary screen switching with Navigation Compose as the product grows.
5. Improve image orientation, drawing tools, error recovery and accessibility.

## AI usage and development decisions

AI tools were allowed for Android Task 1. See [`AI_PROMPT_LOG.md`](AI_PROMPT_LOG.md) for actual prompts, errors, fixes and verification observations. The code was checked through local Android Studio builds by the developer. The final Phase 5 tests still must be executed on the developer's machine.

## Submission

Submit the Android source, Gradle wrapper/version catalog, this README, the AI prompt log, the two tests, and either the debug APK or repeatable APK build instructions. Include meaningful Git commit history. Do **not** commit `local.properties`, developer SDK paths, `.gradle/`, build directories, private keys, or tokens.

If submitting all three assessment tasks in one repository, keep separate folders for **Android**, **iOS**, and **React Native**, and have a top-level README link to each task's README. The iOS task's no-AI declaration belongs in its own README and must be truthful.

---

**Official references:** [DummyJSON docs](https://dummyjson.com/docs) · [Android local unit testing](https://developer.android.com/training/testing/local-tests) · [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) · [Android FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
