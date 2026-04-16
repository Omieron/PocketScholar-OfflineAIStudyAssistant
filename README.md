# PocketScholar

**PocketScholar** is an Android study assistant that runs a **local** large language model (LLM) on your device. You can import PDFs, build a searchable index, and chat with answers grounded in your documents (RAG), without sending prompts to a cloud API.

The mobile app lives under [`android-app/`](android-app/).

## What you get

- **On-device inference** using [llama.cpp](https://github.com/ggerganov/llama.cpp) built as native code and called from Kotlin via JNI.
- **Jetpack Compose** UI with tabs for chat, documents, model management, and simple stats.
- **RAG-style workflow**: PDF text is chunked, embedded with TensorFlow Lite, stored in **Room**, and retrieved for context before the LLM generates a reply.
- **Model discovery**: the app can use curated Hugging Face GGUF downloads (see `ModelInfo` / `AvailableModels`) and also scan **`*.gguf`** in the app private files directory and in **public Downloads** (`LocalModelScanner`).

## Requirements

- **Android Studio** (recent stable; the project targets compile SDK 36, min SDK 24).
- **Android NDK** and **CMake** (the app links llama.cpp through `app/src/main/cpp/CMakeLists.txt`).
- **JDK 17** (see `compileOptions` / Kotlin JVM target in Gradle).

## Clone and submodules

llama.cpp is included as a Git submodule:

```bash
git clone <your-repo-url> PocketScholar-OfflineAIStudyAssistant
cd PocketScholar-OfflineAIStudyAssistant
git submodule update --init --recursive android-app/third_party/llama.cpp
```

If the submodule directory is empty, native build will fail with a CMake error pointing at the expected path.

## Build and run

1. Open the **`android-app`** folder in Android Studio (or open the repo and set `android-app` as the Gradle project root if your IDE expects that layout).
2. Let Gradle sync; on first build, CMake compiles llama.cpp for `arm64-v8a` and `x86_64`.
3. Run on a **physical device or emulator** with enough RAM for the model you choose (see model metadata in the app’s model list).

Application id: `com.example.pocketscholar`.

## Models (GGUF)

- **In-app downloads** are configured in Kotlin (`AvailableModels` in [`ModelInfo.kt`](android-app/app/src/main/java/com/example/pocketscholar/data/ModelInfo.kt)): various small instruct models with Q4 and Q8 GGUF variants and Hugging Face URLs.
- **Manual placement**: put a `*.gguf` file in the app’s **files directory** or **Downloads**; the scanner surfaces it for loading. The `ModelRepository` also stores downloaded models under the app’s external files **`models/`** directory.

At startup, `MainActivity` tries to load an available model path from `ModelRepository` on a background thread.

## Embeddings (TensorFlow Lite)

For real vector search over chunks, the app expects an **`embedding_model.tflite`** under [`android-app/app/src/main/assets/`](android-app/app/src/main/assets/). See the comment in `.gitkeep` there; without a compatible TFLite embedding model, the RAG path may fall back or degrade depending on your build.

Embeddings use **Google Play Services TensorFlow Lite** (`TfLite` runtime) as wired in `EmbeddingEngine`.

## RAG and architecture notes

Implementation details and the phased RAG plan (including prompt shape, context limits, and chat integration) are documented in Turkish in [`android-app/README_RAG.md`](android-app/README_RAG.md). Even if you do not read Turkish, the file names, class table, and code references are useful for navigating the codebase:

- Chat: `ChatScreen.kt`, `ChatViewModel.kt`
- LLM JNI: `LlamaEngine`, native project under `app/src/main/cpp/`
- Retrieval: `RagService`, `VectorStoreRepository`, `EmbeddingEngine`, Room entities such as `ChunkEntity`

## Third-party software

- **llama.cpp** (submodule under `android-app/third_party/llama.cpp`) — follow its license and attribution in that tree.
- **PDFBox Android** for PDF parsing.
- **Room**, **Compose**, **Navigation**, **Coroutines** as declared in Gradle.

## Learn more

To understand how a small Android app can host llama.cpp, compare this repo’s `CMakeLists.txt` and JNI layer with the official [llama.cpp](https://github.com/ggerganov/llama.cpp) Android examples and documentation in the submodule.

Contributions and issues are welcome if you publish this repository publicly; keep model URLs and sizes in `AvailableModels` accurate if you change them.
