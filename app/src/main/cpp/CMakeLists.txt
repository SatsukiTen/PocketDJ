cmake_minimum_required(VERSION 3.22.1)
project(djapp VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---------------------------------------------------------------------------
# T-203: Oboe ソース統合
# PrefabはJava 24のJNA制限により現環境で非対応のため、add_subdirectory方式を採用。
# ADR-001 参照。
# ---------------------------------------------------------------------------
set(OBOE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/oboe)
add_subdirectory(${OBOE_DIR} oboe_build)

# ---------------------------------------------------------------------------
# djapp 共有ライブラリ
# ---------------------------------------------------------------------------
add_library(djapp SHARED
    djapp_jni.cpp
    AudioEngine.cpp
    DeckPlayer.cpp
    EqProcessor.cpp
    SamplePlayer.cpp
)

# インクルードパス
target_include_directories(djapp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${OBOE_DIR}/include
)

# リンクライブラリ
target_link_libraries(djapp
    oboe
    android
    log
    mediandk      # AMediaExtractor / AMediaCodec
    OpenSLES
)
