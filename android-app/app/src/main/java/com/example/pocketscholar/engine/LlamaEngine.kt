package com.example.pocketscholar.engine

import android.content.Context
import android.util.Log
import com.example.pocketscholar.data.SettingsRepository
import java.io.File

/**
 * JNI wrapper for llama.cpp.
 * Call init() once (e.g. in Application), then loadModel() and prompt().
 *
 * Bu sınıf aynı zamanda son yüklenen model yolunu ve yüklenme durumunu da takip eder;
 * böylece UI tarafı "model yüklü mü, hangi dosyadan?" sorusuna kolayca cevap verebilir.
 */
object LlamaEngine {

    private const val TAG = "LlamaEngine"

    // Kotlin tarafında tutulan basit durum bilgisi; JNI tarafı başarısız olursa false'a çekilir.
    @Volatile
    private var isLoadedFlag: Boolean = false

    @Volatile
    var currentModelPath: String? = null
        private set

    init {
        System.loadLibrary("llama_jni")
    }

    /** Initialize backend. Pass native library dir (e.g. from context.applicationInfo.nativeLibraryDir). */
    external fun init(nativeLibDir: String)

    /** Native: Load GGUF model from path. Returns true on success. */
    external fun loadModel(modelPath: String): Boolean

    /** Run prompt and return generated text. */
    external fun prompt(prompt: String): String?

    /** Unload model and free memory. */
    external fun unload()

    /** Convenience: init using context. */
    fun init(context: Context) {
        init(context.applicationInfo.nativeLibraryDir)
    }

    /** Kotlin tarafındaki flag üzerinden modelin yüklü olup olmadığını bildirir. */
    fun isModelLoaded(): Boolean = isLoadedFlag

    /**
     * SharedPreferences'ta kayıtlı (varsa) son model yolunu döner (SettingsRepository üzerinden);
     * dosyanın hala var olup olmadığını kontrol ETMEZ, sadece string'i okur.
     */
    fun getPersistedModelPath(context: Context): String? =
        SettingsRepository(context).getLlmModelPath()

    /**
     * Modeli belirtilen path'ten yükler, başarılı olursa SharedPreferences'a kaydeder.
     * JNI çağrısı başarısız olursa anlamlı log mesajı üretir ve false döner.
     */
    fun loadModel(context: Context, modelPath: String): Boolean {
        val file = File(modelPath)
        if (!file.exists() || !file.isFile) {
            Log.e(TAG, "Model file does not exist: $modelPath")
            isLoadedFlag = false
            currentModelPath = null
            return false
        }

        Log.i(TAG, "Loading GGUF model from: $modelPath (size=${file.length()} bytes)")
        val success = try {
            loadModel(modelPath)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI loadModel failed. Native library not initialized?", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while loading model", e)
            false
        }

        if (success) {
            isLoadedFlag = true
            currentModelPath = modelPath
            val repo = SettingsRepository(context)
            repo.setLlmModelPath(modelPath)
            repo.setLlmModelLastLoadedAt(System.currentTimeMillis())
            Log.i(TAG, "Model loaded successfully and persisted to prefs.")
        } else {
            isLoadedFlag = false
            currentModelPath = null
            Log.e(TAG, "Failed to load model from path: $modelPath")
        }

        return success
    }

    /**
     * Son kullanılan model yolunu SettingsRepository'den okuyup varsa tekrar yüklemeyi dener.
     * Dosya bulunamazsa veya yükleme başarısız olursa false döner.
     */
    fun restoreLastModelIfAvailable(context: Context): Boolean {
        val path = SettingsRepository(context).getLlmModelPath() ?: return false
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "Persisted model path no longer exists on disk: $path")
            return false
        }
        return loadModel(context, path)
    }

    /**
     * JNI unload çağrısı sonrası Kotlin tarafındaki state'i de sıfırlar.
     */
    fun unloadAndReset() {
        try {
            unload()
        } catch (e: Exception) {
            Log.e(TAG, "Error while unloading model", e)
        } finally {
            isLoadedFlag = false
            currentModelPath = null
        }
    }

    /**
     * Persisted model bilgisini temizler (SettingsRepository) ve lokal state'i sıfırlar.
     * Genellikle kullanıcı "modeli kaldır" dediğinde çağrılır.
     */
    fun clearPersistedModel(context: Context) {
        SettingsRepository(context).clearLlmModel()
        isLoadedFlag = false
        currentModelPath = null
    }
}
