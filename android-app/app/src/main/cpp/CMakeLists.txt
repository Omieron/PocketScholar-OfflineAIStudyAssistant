cmake_minimum_required(VERSION 3.22.1)
project("llama_jni" VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

# Path to llama.cpp (from app/src/main/cpp/ -> android-app/third_party/llama.cpp)
set(LLAMA_SRC ${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/llama.cpp)

if(NOT EXISTS ${LLAMA_SRC}/CMakeLists.txt)
    message(FATAL_ERROR "llama.cpp not found at ${LLAMA_SRC}. Run: git submodule update --init android-app/third_party/llama.cpp")
endif()

# Options for llama.cpp build (minimal, no common/tools/examples)
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL ON CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS ON CACHE BOOL "" FORCE)
# Disable KleidiAI - its FetchContent download fails on some CMake/NDK setups
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)

# Android ABI settings (optional optimization)
if(DEFINED ANDROID_ABI)
    message(STATUS "Android ABI: ${ANDROID_ABI}")
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM")
        set(GGML_OPENMP ON)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86")
        set(GGML_OPENMP OFF)
    endif()
endif()

# Include llama.cpp as subdirectory (builds ggml + llama)
add_subdirectory(${LLAMA_SRC} ${CMAKE_BINARY_DIR}/build-llama)

# JNI shared library
add_library(llama_jni SHARED
    llama_jni.cpp
)

target_include_directories(llama_jni PRIVATE
    ${LLAMA_SRC}
    ${LLAMA_SRC}/include
    ${LLAMA_SRC}/ggml/include
    ${LLAMA_SRC}/ggml/src
)

target_link_libraries(llama_jni
    llama
    android
    log
)
