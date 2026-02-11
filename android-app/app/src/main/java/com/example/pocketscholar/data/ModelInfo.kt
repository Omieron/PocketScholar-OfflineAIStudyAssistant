package com.example.pocketscholar.data

/**
 * Quantization seviyesi — 4-bit daha az RAM kullanır, 8-bit daha kaliteli sonuç verir.
 */
enum class QuantizationMode(val label: String, val badge: String) {
    Q4_K_M("4-bit", "Q4"),
    Q8_0("8-bit", "Q8")
}

/**
 * Uygulama içinde indirilebilecek GGUF model bilgisi.
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,       // Kullanıcıya gösterilecek boyut (ör. "1.8 GB")
    val sizeInBytes: Long,       // Yaklaşık boyut (progress hesabı için)
    val downloadUrl: String,     // Hugging Face resolve/main URL
    val huggingFaceUrl: String,  // Hugging Face model sayfası (kullanıcıya gösterilecek)
    val fileName: String,        // Kaydedilecek dosya adı
    val ramRequirement: String,  // Önerilen minimum RAM
    val tier: ModelTier,         // Cihaz seviyesi
    val quantization: QuantizationMode = QuantizationMode.Q4_K_M,  // Quantization seviyesi
    val baseModelId: String? = null  // Q8 varyantı ise, Q4 karşılığının ID'si (gruplama için)
)

/**
 * Model seviyesi — kullanıcıya cihazına uygun modeli seçmesini kolaylaştırır.
 */
enum class ModelTier(val label: String, val emoji: String) {
    LIGHTWEIGHT("Hafif", "⚡"),
    BALANCED("Dengeli", "⚖️"),
    POWERFUL("Güçlü", "\uD83D\uDE80"),
    ADVANCED("İleri Seviye", "🧠")
}

/**
 * Varsayılan model listesi — tüm modeller Hugging Face'den indirilir.
 * Her model için Q4_K_M (4-bit) ve Q8_0 (8-bit) varyantları mevcut.
 */
object AvailableModels {
    val list: List<ModelInfo> = listOf(
        // ══════════════════════════════════════════════════════════
        // Hafif (düşük RAM cihazlar, 3-4 GB RAM)
        // ══════════════════════════════════════════════════════════

        // ── Qwen 2.5 (0.5B) ──
        ModelInfo(
            id = "qwen2.5-0.5b",
            name = "Qwen 2.5 (0.5B)",
            description = "En küçük model. Eski/düşük RAM cihazlarda çalışır. Basit sorular için uygundur.",
            sizeLabel = "400 MB",
            sizeInBytes = 420_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            ramRequirement = "2 GB RAM",
            tier = ModelTier.LIGHTWEIGHT,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "qwen2.5-0.5b-q8",
            name = "Qwen 2.5 (0.5B) Q8",
            description = "En küçük model, yüksek kalite quantization. Daha iyi doğruluk, biraz daha fazla RAM.",
            sizeLabel = "530 MB",
            sizeInBytes = 530_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            fileName = "qwen2.5-0.5b-instruct-q8_0.gguf",
            ramRequirement = "3 GB RAM",
            tier = ModelTier.LIGHTWEIGHT,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "qwen2.5-0.5b"
        ),

        // ── TinyLlama (1.1B) ──
        ModelInfo(
            id = "tinyllama-1.1b",
            name = "TinyLlama (1.1B)",
            description = "Çok hafif ve hızlı. Düşük RAM'li cihazlarda sorunsuz çalışır.",
            sizeLabel = "670 MB",
            sizeInBytes = 670_000_000L,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            huggingFaceUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            ramRequirement = "3 GB RAM",
            tier = ModelTier.LIGHTWEIGHT,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "tinyllama-1.1b-q8",
            name = "TinyLlama (1.1B) Q8",
            description = "Çok hafif ve hızlı, yüksek kalite quantization. Daha iyi doğruluk.",
            sizeLabel = "1.1 GB",
            sizeInBytes = 1_100_000_000L,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            fileName = "tinyllama-1.1b-chat-v1.0.Q8_0.gguf",
            ramRequirement = "4 GB RAM",
            tier = ModelTier.LIGHTWEIGHT,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "tinyllama-1.1b"
        ),

        // ══════════════════════════════════════════════════════════
        // Dengeli (orta seviye cihazlar, 4-6 GB RAM)
        // ══════════════════════════════════════════════════════════

        // ── SmolLM2 (1.7B) ──
        ModelInfo(
            id = "smollm2-1.7b",
            name = "SmolLM2 (1.7B)",
            description = "Dengeli performans. Orta seviye cihazlarda iyi sonuçlar verir.",
            sizeLabel = "1.0 GB",
            sizeInBytes = 1_060_000_000L,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            huggingFaceUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF",
            fileName = "smollm2-1.7b-instruct-q4_k_m.gguf",
            ramRequirement = "4 GB RAM",
            tier = ModelTier.BALANCED,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "smollm2-1.7b-q8",
            name = "SmolLM2 (1.7B) Q8",
            description = "Dengeli performans, yüksek kalite quantization. Daha iyi doğruluk.",
            sizeLabel = "1.8 GB",
            sizeInBytes = 1_800_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-1.7B-Instruct-GGUF/resolve/main/SmolLM2-1.7B-Instruct-Q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/SmolLM2-1.7B-Instruct-GGUF",
            fileName = "SmolLM2-1.7B-Instruct-Q8_0.gguf",
            ramRequirement = "6 GB RAM",
            tier = ModelTier.BALANCED,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "smollm2-1.7b"
        ),

        // ── Gemma 2 (2B) ──
        ModelInfo(
            id = "gemma-2-2b",
            name = "Gemma 2 (2B)",
            description = "Google'ın hafif modeli. İyi Türkçe desteği, dengeli sonuçlar.",
            sizeLabel = "1.7 GB",
            sizeInBytes = 1_710_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF",
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            ramRequirement = "4 GB RAM",
            tier = ModelTier.BALANCED,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "gemma-2-2b-q8",
            name = "Gemma 2 (2B) Q8",
            description = "Google'ın hafif modeli, yüksek kalite quantization. Daha iyi Türkçe desteği.",
            sizeLabel = "2.7 GB",
            sizeInBytes = 2_700_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF",
            fileName = "gemma-2-2b-it-Q8_0.gguf",
            ramRequirement = "6 GB RAM",
            tier = ModelTier.BALANCED,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "gemma-2-2b"
        ),

        // ══════════════════════════════════════════════════════════
        // Güçlü (iyi cihazlar, 6-8 GB RAM)
        // ══════════════════════════════════════════════════════════

        // ── Llama 3.2 (3B) ──
        ModelInfo(
            id = "llama-3.2-3b",
            name = "Llama 3.2 (3B) ⭐",
            description = "Önerilen model! En iyi doğruluk/hız dengesi. Çoğu modern cihazda sorunsuz çalışır.",
            sizeLabel = "1.9 GB",
            sizeInBytes = 2_020_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            ramRequirement = "6 GB RAM",
            tier = ModelTier.POWERFUL,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "llama-3.2-3b-q8",
            name = "Llama 3.2 (3B) Q8 ⭐",
            description = "Önerilen model, yüksek kalite quantization! En iyi doğruluk, daha fazla RAM gerektirir.",
            sizeLabel = "3.4 GB",
            sizeInBytes = 3_420_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF",
            fileName = "Llama-3.2-3B-Instruct-Q8_0.gguf",
            ramRequirement = "8 GB RAM",
            tier = ModelTier.POWERFUL,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "llama-3.2-3b"
        ),

        // ══════════════════════════════════════════════════════════
        // İleri Seviye (flagship cihazlar, 8+ GB RAM)
        // ══════════════════════════════════════════════════════════

        // ── Phi 3.5 Mini (3.8B) ──
        ModelInfo(
            id = "phi-3.5-mini",
            name = "Phi 3.5 Mini (3.8B)",
            description = "Microsoft'un güçlü modeli. Yüksek doğruluk ama daha fazla RAM gerektirir.",
            sizeLabel = "2.4 GB",
            sizeInBytes = 2_390_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF",
            fileName = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
            ramRequirement = "8 GB RAM",
            tier = ModelTier.ADVANCED,
            quantization = QuantizationMode.Q4_K_M
        ),
        ModelInfo(
            id = "phi-3.5-mini-q8",
            name = "Phi 3.5 Mini (3.8B) Q8",
            description = "Microsoft'un güçlü modeli, yüksek kalite quantization. En yüksek doğruluk.",
            sizeLabel = "4.0 GB",
            sizeInBytes = 4_060_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q8_0.gguf",
            huggingFaceUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF",
            fileName = "Phi-3.5-mini-instruct-Q8_0.gguf",
            ramRequirement = "12 GB RAM",
            tier = ModelTier.ADVANCED,
            quantization = QuantizationMode.Q8_0,
            baseModelId = "phi-3.5-mini"
        )
    )

    /** Sadece Q4 (4-bit) modelleri döner */
    fun q4Models(): List<ModelInfo> = list.filter { it.quantization == QuantizationMode.Q4_K_M }

    /** Sadece Q8 (8-bit) modelleri döner */
    fun q8Models(): List<ModelInfo> = list.filter { it.quantization == QuantizationMode.Q8_0 }

    /** Belirli bir modelin Q4 ↔ Q8 karşılığını bul */
    fun findCounterpart(modelId: String): ModelInfo? {
        val model = list.find { it.id == modelId } ?: return null
        return if (model.quantization == QuantizationMode.Q4_K_M) {
            // Q4 → Q8 karşılığını bul
            list.find { it.baseModelId == model.id }
        } else {
            // Q8 → Q4 karşılığını bul
            list.find { it.id == model.baseModelId }
        }
    }
}
