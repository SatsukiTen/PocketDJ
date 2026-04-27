#pragma once
#include <atomic>
#include <mutex>
#include <vector>
#include <cstdint>

/**
 * サンプルパッド再生器。
 * loadPcm() で PCM（インターリーブ stereo float）を受け取り、
 * play(loop) で再生を開始する。
 *
 * スレッド安全:
 *   loadPcm / clear  — JNI スレッド（mDataMutex で保護）
 *   play / stop       — JNI スレッド（atomic 書き込み）
 *   renderAudio       — オーディオスレッド（mDataMutex try_lock）
 */
class SamplePlayer {
public:
    SamplePlayer() = default;

    // data: interleaved stereo float (2 floats per frame)
    void loadPcm(const float* data, int64_t frameCount, int32_t sampleRate);
    void play(bool loopEnabled);
    void stop();
    void clear();

    bool  isLoaded()  const { return mLoaded.load(std::memory_order_acquire); }
    bool  isPlaying() const { return mIsPlaying.load(std::memory_order_acquire); }
    float getDurationSec() const;

    // Called from audio thread
    void renderAudio(float* output, int32_t numFrames, int32_t channelCount, float gain);

private:
    mutable std::mutex   mDataMutex;
    std::vector<float>   mPcmBuffer;   // interleaved stereo
    int64_t              mTotalFrames = 0;
    int32_t              mSampleRate  = 48000;

    std::atomic<int64_t> mPlayheadFrame{0};
    std::atomic<bool>    mIsPlaying{false};
    std::atomic<bool>    mLoopEnabled{false};
    std::atomic<bool>    mLoaded{false};
};
