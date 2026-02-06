/**
 * JNI wrapper for llama.cpp - minimal loadModel + prompt
 * Package: com.example.pocketscholar.engine.LlamaEngine
 */
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>

#include "llama.h"
#include "ggml.h"
#include "ggml-backend.h"

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static int ggml_level_to_android(enum ggml_log_level level) {
    switch (level) {
        case GGML_LOG_LEVEL_ERROR: return ANDROID_LOG_ERROR;
        case GGML_LOG_LEVEL_WARN:  return ANDROID_LOG_WARN;
        case GGML_LOG_LEVEL_INFO:  return ANDROID_LOG_INFO;
        case GGML_LOG_LEVEL_DEBUG: return ANDROID_LOG_DEBUG;
        default:                   return ANDROID_LOG_INFO;
    }
}

static void llama_log_to_android(enum ggml_log_level level, const char* text, void*) {
    __android_log_write(ggml_level_to_android(level), LOG_TAG, text);
}

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;

static bool g_backend_init = false;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_pocketscholar_engine_LlamaEngine_init(JNIEnv* env, jobject, jstring nativeLibDir) {
    if (g_backend_init) return;
    llama_log_set(llama_log_to_android, nullptr);
    const char* path = env->GetStringUTFChars(nativeLibDir, nullptr);
    ggml_backend_load_all_from_path(path);
    env->ReleaseStringUTFChars(nativeLibDir, path);
    llama_backend_init();
    g_backend_init = true;
    LOGI("Backend initialized");
}

JNIEXPORT jboolean JNICALL
Java_com_example_pocketscholar_engine_LlamaEngine_loadModel(JNIEnv* env, jobject, jstring modelPath) {
    if (!g_backend_init) {
        LOGE("Call init() first");
        return JNI_FALSE;
    }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    llama_model_params mparams = llama_model_default_params();
    g_model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 2048;
    cparams.n_batch = 512;
    g_ctx = llama_init_from_model(g_model, cparams);
    if (!g_ctx) {
        llama_model_free(g_model);
        g_model = nullptr;
        LOGE("Failed to init context");
        return JNI_FALSE;
    }
    LOGI("Model loaded");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_example_pocketscholar_engine_LlamaEngine_prompt(JNIEnv* env, jobject, jstring promptJ) {
    if (!g_model || !g_ctx) {
        return env->NewStringUTF("[Model not loaded. Call loadModel() first.]");
    }

    const char* prompt = env->GetStringUTFChars(promptJ, nullptr);
    const llama_vocab* vocab = llama_model_get_vocab(g_model);

    int n_prompt = -llama_tokenize(vocab, prompt, (int32_t)strlen(prompt), nullptr, 0, true, true);
    if (n_prompt <= 0) {
        env->ReleaseStringUTFChars(promptJ, prompt);
        return env->NewStringUTF("[Empty or invalid prompt]");
    }

    std::vector<llama_token> tokens(n_prompt);
    if (llama_tokenize(vocab, prompt, (int32_t)strlen(prompt), tokens.data(), (int32_t)tokens.size(), true, true) < 0) {
        env->ReleaseStringUTFChars(promptJ, prompt);
        return env->NewStringUTF("[Tokenize failed]");
    }
    env->ReleaseStringUTFChars(promptJ, prompt);

    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    const int n_predict = 256;  // Increased for complete answers; "list all" queries need more tokens
    std::string result;
    // Limit prompt tokens to avoid batch overflow (n_batch=512). RagService caps context at 800 chars (~530 tokens).
    // Template + query ≈ 150 tokens (longer template for "list all" instructions), so max_prompt_tokens=500 is safe.
    const int max_prompt_tokens = 500;
    int n_use = (int)tokens.size();
    if (n_use > max_prompt_tokens) {
        LOGE("Prompt too long (%d tokens), truncating to %d", n_use, max_prompt_tokens);
        n_use = max_prompt_tokens;
    }
    llama_batch batch = llama_batch_get_one(tokens.data(), n_use);

    for (int i = 0; i < n_predict; i++) {
        if (llama_decode(g_ctx, batch) != 0) break;
        llama_token next = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;

        char buf[128];
        int n = llama_token_to_piece(vocab, next, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);
        batch = llama_batch_get_one(&next, 1);
    }

    llama_sampler_free(smpl);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_pocketscholar_engine_LlamaEngine_unload(JNIEnv*, jobject) {
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    LOGI("Model unloaded");
}

}
