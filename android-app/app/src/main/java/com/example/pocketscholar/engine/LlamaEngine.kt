package com.example.pocketscholar.engine

import android.content.Context

/**
 * JNI wrapper for llama.cpp.
 * Call init() once (e.g. in Application), then loadModel() and prompt().
 */
object LlamaEngine {

    init {
        System.loadLibrary("llama_jni")
    }

    /** Initialize backend. Pass native library dir (e.g. from context.applicationInfo.nativeLibraryDir). */
    external fun init(nativeLibDir: String)

    /** Load GGUF model from path. Returns true on success. */
    external fun loadModel(modelPath: String): Boolean

    /** Run prompt and return generated text. */
    external fun prompt(prompt: String): String?

    /** Unload model and free memory. */
    external fun unload()

    /** Convenience: init using context. */
    fun init(context: Context) {
        init(context.applicationInfo.nativeLibraryDir)
    }
}
