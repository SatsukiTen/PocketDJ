#include <jni.h>
#include <android/log.h>
#include <string>
#include "AudioEngine.h"

#define LOG_TAG "DjApp_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// -----------------------------------------------------------------------
// T-203 / T-204: JNI ブリッジ（AudioEngine 委譲版）
// -----------------------------------------------------------------------

extern "C" {

// --- エンジン初期化・破棄 ---
JNIEXPORT jboolean JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeInitEngine(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    bool ok = AudioEngine::getInstance().init();
    LOGI("nativeInitEngine → %s", ok ? "OK" : "FAIL");
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeDestroyEngine(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    LOGI("nativeDestroyEngine");
    AudioEngine::getInstance().destroy();
}

// --- デッキ操作（UC-001 / UC-002）---
// Android 10+ 対応: Kotlin 側で content URI → fd 変換済みのディスクリプタを受け取る
JNIEXPORT jboolean JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeLoadTrackFd(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId, jint fd) {
    bool ok = AudioEngine::getInstance().loadTrackFd(deckId, fd);
    LOGI("nativeLoadTrackFd deckId=%d fd=%d → %s", deckId, fd, ok ? "OK" : "FAIL");
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativePlay(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    LOGI("nativePlay deckId=%d", deckId);
    AudioEngine::getInstance().play(deckId);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativePause(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    LOGI("nativePause deckId=%d", deckId);
    AudioEngine::getInstance().pause(deckId);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeStop(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    LOGI("nativeStop deckId=%d", deckId);
    AudioEngine::getInstance().stop(deckId);
}

// --- 再生位置取得（T-203 追加）---
JNIEXPORT jfloat JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetPlayheadSec(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    return AudioEngine::getInstance().getPlayheadSec(deckId);
}

JNIEXPORT jfloat JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetDurationSec(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    return AudioEngine::getInstance().getDurationSec(deckId);
}

// --- クロスフェーダー（UC-003 / Sprint 3 で本実装）---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetCrossfaderPosition(
        JNIEnv* /*env*/, jobject /*thiz*/, jfloat position) {
    AudioEngine::getInstance().setCrossfaderPosition(position);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetCrossfaderCurve(
        JNIEnv* /*env*/, jobject /*thiz*/, jint curveType) {
    AudioEngine::getInstance().setCrossfaderCurve(curveType);
}

// --- イコライザー（UC-005 / Sprint 5 で本実装）---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetEq(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId, jint band, jfloat gainDb) {
    AudioEngine::getInstance().setEq(deckId, band, gainDb);
}

// --- ピッチ / BPM 同期（UC-004 / T-405）---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetPitch(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId, jfloat pitchRatio) {
    AudioEngine::getInstance().setPitchRatio(deckId, pitchRatio);
}

JNIEXPORT jfloat JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetBpm(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    return AudioEngine::getInstance().getBpm(deckId);
}

JNIEXPORT jint JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetSampleRate(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    return AudioEngine::getInstance().getSampleRate(deckId);
}

// --- ループ（UC-006）---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetLoop(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId,
        jlong loopInSample, jlong loopOutSample) {
    AudioEngine::getInstance().setLoop(deckId, loopInSample, loopOutSample);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeClearLoop(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    AudioEngine::getInstance().clearLoop(deckId);
}

// --- シーク ---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSeekTo(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId, jfloat positionSec) {
    AudioEngine::getInstance().seekTo(deckId, positionSec);
}

// --- スクラッチ（UC-007）---
JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeStartScratch(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    AudioEngine::getInstance().startScratch(deckId);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeSetScratchSpeed(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId, jfloat speed) {
    AudioEngine::getInstance().setScratchSpeed(deckId, speed);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeStopScratch(
        JNIEnv* /*env*/, jobject /*thiz*/, jint deckId) {
    AudioEngine::getInstance().stopScratch(deckId);
}

// --- サンプルパッド ---
JNIEXPORT jboolean JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeCaptureSample(
        JNIEnv* /*env*/, jobject /*thiz*/, jint slotId, jint deckId,
        jlong inFrame, jlong outFrame) {
    bool ok = AudioEngine::getInstance().captureSample(slotId, deckId, inFrame, outFrame);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativePlaySample(
        JNIEnv* /*env*/, jobject /*thiz*/, jint slotId, jboolean loop) {
    AudioEngine::getInstance().playSample(slotId, loop == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeStopSample(
        JNIEnv* /*env*/, jobject /*thiz*/, jint slotId) {
    AudioEngine::getInstance().stopSample(slotId);
}

JNIEXPORT jboolean JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeIsSamplePlaying(
        JNIEnv* /*env*/, jobject /*thiz*/, jint slotId) {
    return AudioEngine::getInstance().isSamplePlaying(slotId) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeClearSample(
        JNIEnv* /*env*/, jobject /*thiz*/, jint slotId) {
    AudioEngine::getInstance().clearSample(slotId);
}

// --- T-802: レイテンシ計測 ---
JNIEXPORT jdouble JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetLatencyMs(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    return static_cast<jdouble>(AudioEngine::getInstance().getLatencyMs());
}

JNIEXPORT jint JNICALL
Java_com_djapp_data_source_AudioEngineDataSource_nativeGetXRunCount(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    return static_cast<jint>(AudioEngine::getInstance().getXRunCount());
}

} // extern "C"
