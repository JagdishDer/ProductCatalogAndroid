# AI Prompt & Verification Log — Android Task 1

**Date:** 24 November 2026  
**Permitted AI tool used:** ChatGPT (code suggestions and troubleshooting)  
**Human tools used:** Android Studio, Gradle, Android emulator/device, project files and build logs  
**Assessment policy:** AI allowed for Task 1; AI **not allowed** for Task 2 Swift implementation. This log documents **only Task 1**.

> This is a factual log derived from the actual assessment conversation and the user's reported build results. It does not claim unseen test execution, Git commits, or independently verified device behavior. Add exact timestamps, commits and test results from your own environment if required.

## Prompt 1 — Start from a fresh Android project

**User prompt (verbatim):**

> My fresh Android Studio ProductCatalogAndroid project builds successfully. Start Phase 1 secure DummyJSON login. First help me check the generated Gradle version catalog, then provide compatible dependencies and complete working Kotlin code step by step.

**AI assistance:** Suggested using the generated Android Studio Compose and Gradle setup instead of overriding its Kotlin/AGP versions. Provided Retrofit auth models/API, MVVM UI state, Android Keystore AES-GCM access token storage, Bearer interceptor, login and logout flow.

**Verification reported:** User later stated Phase 1 secure login was implemented and built successfully.

## Prompt 2 — Product list, categories and pagination

**User prompt (verbatim):**

> Android Task 1 Phase 1 secure login is implemented and builds successfully. Continue to Phase 2. Provide the complete Kotlin files for DummyJSON product models, Retrofit endpoints, repository, ViewModel, paginated Jetpack Compose list, category filtering, loading/error/empty states and integration with MainActivity. Preserve my existing secure authentication code.

**AI assistance:** Produced Product models, category and paginated endpoints, repository, ViewModel, Compose list using Coil, and activity integration. Explained server skip/limit semantics and category filtering.

**Verification reported:** A Kotlin compiler error was subsequently reported and corrected (see Error Case B). User proceeded to Phase 3 after the correction.

## Prompt 3 — Add/edit/delete and session overlay

**User prompt (verbatim):**

> we can proceed to Phase 3

**AI assistance:** Suggested POST/PUT/DELETE Retrofit endpoints, an Add/Edit form, Material3 swipe-left-to-delete, and session overlay for simulated DummyJSON mutations. Used unique negative IDs for locally added products to avoid collision with nonpersisted simulated server IDs.

**Verification reported:** User stated, "Android Task 1 Phase 3 builds and CRUD works." This is the developer's report, not an independent execution by AI.

## Prompt 4 — Camera, gallery and image annotation

**User prompt (verbatim excerpt):**

> Android Task 1 Phase 3 builds and CRUD works. Start Phase 4. Provide complete Kotlin code for gallery picker, native camera capture using FileProvider, freehand annotation with undo, pinch-to-zoom and pan, saving the annotated bitmap locally, and displaying the saved image in product rows.

**AI assistance:** Suggested Photo Picker, `TakePicture` + FileProvider, permission-denial handling, app-private file copying, bitmap decode/export, freehand Canvas strokes in bitmap coordinates, undo, pinch zoom and pan, and passing local image URIs through the existing session overlay.

**Verification reported:** User encountered a Kotlin import error (see Error Case C), then stated Phase 4 built and all image features worked.

## Prompt 5 — Final tests and submission

**User prompt (verbatim):**

> Android Task 1 Phase 4 now builds and all image features work. Start Phase 5: write two unit tests, create a professional Android README, update the AI prompt log with actual issues from our assessment work, and give me the final APK build and GitHub submission checklist.

**AI assistance:** Provided a pure Kotlin `ProductSessionLogic` helper, two JUnit4 unit tests, wiring instructions, README and this log.

**Verification status:** Tests and final APK **not yet run/verified on the developer's Mac** at the time this file was generated. Record actual results after running the Gradle commands.

---

## Error Case A — Original project toolchain mismatch (not just an API problem)

**Observed error:** An older Android project used **AGP 7.3.1 and Kotlin 1.7.x**, while AndroidX dependencies were compiled with substantially newer Kotlin metadata, including versions associated with Kotlin 2.1.

**Earlier AI issue:** Suggestions to add newer Lifecycle dependencies before confirming the existing Gradle toolchain were too aggressive; a later downgrade recommendation did not remove all transitive version conflicts.

**How discovered:** Android Studio compilation logs showed variant-resolution issues for `androidx.lifecycle:lifecycle-runtime-ktx:2.9.4` and messages similar to `Metadata: 2.1.0, Expected: 1.7.1`.

**Correction:** Started a **new Android Studio Compose project**, verified its starter app built successfully, and kept its generated Gradle/plugin versions. This avoids pretending that the prior project's dependency downgrade was independently verified to solve every conflict.

## Error Case B — Wrong `snapshotFlow` import

**Observed compiler error:**

```text
ProductListScreen.kt:23:32 Unresolved reference 'snapshotFlow'
```

**Incorrect AI-generated import:**

```kotlin
import kotlinx.coroutines.flow.snapshotFlow
```

**Correction:** Remove that import and use:

```kotlin
import androidx.compose.runtime.snapshotFlow
```

(or rely on `import androidx.compose.runtime.*`). Keep `kotlinx.coroutines.flow.distinctUntilChanged`.

**Why:** `snapshotFlow` is declared in the Compose Runtime package, not Kotlin Coroutines Flow.

**Verification:** User continued to Phase 3 and later reported successful CRUD builds. The exact local Gradle command output after this fix was not pasted in the conversation.

## Error Case C — Wrong `consume` import

**Observed compiler error:**

```text
ImageAnnotationScreen.kt:22:42 Unresolved reference 'consume'
```

**Incorrect AI-generated import:**

```kotlin
import androidx.compose.ui.input.pointer.consume
```

**Correction:** Remove the import and keep the valid member call:

```kotlin
change.consume()
```

**Why:** In the installed Compose version, `consume()` is a member on the pointer input change object; it does not need that top-level import.

**Verification:** User subsequently reported, "Android Task 1 Phase 4 now builds and all image features work."

---

## Human review and validation checklist

- [ ] Review new session reducer and integration diff before commit.
- [ ] Run `./gradlew :app:testDebugUnitTest` (record result and date).
- [ ] Run `./gradlew :app:assembleDebug` (record result and date).
- [ ] Test camera permission denial and cancellation on a device/emulator.
- [ ] Confirm exported PNG displays correctly at multiple zoom levels.
- [ ] Confirm simulated CRUD changes survive refresh/filter during current session.
- [ ] Check Git commit history reflects actual development progress.
- [ ] Verify no tokens, signing keys, `local.properties`, or build artifacts are accidentally committed.

**What I learned:** AI-generated code must be verified against the project's actual Kotlin/Compose versions. Compiler output and behavior tests are more reliable than assuming a suggested import, dependency version or API is compatible.
