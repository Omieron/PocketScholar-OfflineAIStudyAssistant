package com.example.pocketscholar.data

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
    val fileName: String,        // Kaydedilecek dosya adı
    val ramRequirement: String,  // Önerilen minimum RAM
    val tier: ModelTier          // Cihaz seviyesi
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
 */
object AvailableModels {
    val list: List<ModelInfo> = listOf(
        // ── Hafif (düşük RAM cihazlar, 3-4 GB RAM) ──
        ModelInfo(
            id = "qwen2.5-0.5b",
            name = "Qwen 2.5 (0.5B)",
            description = "En küçük model. Eski/düşük RAM cihazlarda çalışır. Basit sorular için uygundur.",
            sizeLabel = "400 MB",
            sizeInBytes = 420_000_000L,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            ramRequirement = "2 GB RAM",
            tier = ModelTier.LIGHTWEIGHT
        ),
        ModelInfo(
            id = "tinyllama-1.1b",
            name = "TinyLlama (1.1B)",
            description = "Çok hafif ve hızlı. Düşük RAM'li cihazlarda sorunsuz çalışır.",
            sizeLabel = "670 MB",
            sizeInBytes = 670_000_000L,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            ramRequirement = "3 GB RAM",
            tier = ModelTier.LIGHTWEIGHT
        ),

        // ── Dengeli (orta seviye cihazlar, 4-6 GB RAM) ──
        ModelInfo(
            id = "smollm2-1.7b",
            name = "SmolLM2 (1.7B)",
            description = "Dengeli performans. Orta seviye cihazlarda iyi sonuçlar verir.",
            sizeLabel = "1.0 GB",
            sizeInBytes = 1_060_000_000L,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            fileName = "smollm2-1.7b-instruct-q4_k_m.gguf",
            ramRequirement = "4 GB RAM",
            tier = ModelTier.BALANCED
        ),
        ModelInfo(
            id = "gemma-2-2b",
            name = "Gemma 2 (2B)",
            description = "Google'ın hafif modeli. İyi Türkçe desteği, dengeli sonuçlar.",
            sizeLabel = "1.7 GB",
            sizeInBytes = 1_710_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            ramRequirement = "4 GB RAM",
            tier = ModelTier.BALANCED
        ),

        // ── Güçlü (iyi cihazlar, 6-8 GB RAM) ──
        ModelInfo(
            id = "llama-3.2-3b",
            name = "Llama 3.2 (3B) ⭐",
            description = "Önerilen model! En iyi doğruluk/hız dengesi. Çoğu modern cihazda sorunsuz çalışır.",
            sizeLabel = "1.9 GB",
            sizeInBytes = 2_020_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            ramRequirement = "6 GB RAM",
            tier = ModelTier.POWERFUL
        ),

        // ── İleri Seviye (flaghip cihazlar, 8+ GB RAM) ──
        ModelInfo(
            id = "phi-3.5-mini",
            name = "Phi 3.5 Mini (3.8B)",
            description = "Microsoft'un güçlü modeli. Yüksek doğruluk ama daha fazla RAM gerektirir.",
            sizeLabel = "2.4 GB",
            sizeInBytes = 2_390_000_000L,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            fileName = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
            ramRequirement = "8 GB RAM",
            tier = ModelTier.ADVANCED
        )
    )
}
