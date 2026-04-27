#include "SamplePlayer.h"
#include <algorithm>

void SamplePlayer::loadPcm(const float* data, int64_t frameCount, int32_t sampleRate) {
    std::lock_guard<std::mutex> lock(mDataMutex);
    mLoaded.store(false,  std::memory_order_release);
    mIsPlaying.store(false, std::memory_order_relaxed);
    mPlayheadFrame.store(0, std::memory_order_relaxed);

    mSampleRate  = sampleRate;
    mTotalFrames = frameCount;
    mPcmBuffer.assign(data, data + static_cast<size_t>(frameCount) * 2);

    mLoaded.store(true, std::memory_order_release);
}

void SamplePlayer::play(bool loopEnabled) {
    if (!mLoaded.load(std::memory_order_acquire)) return;
    mLoopEnabled.store(loopEnabled, std::memory_order_relaxed);
    mPlayheadFrame.store(0, std::memory_order_relaxed);
    mIsPlaying.store(true, std::memory_order_release);
}

void SamplePlayer::stop() {
    mIsPlaying.store(false, std::memory_order_release);
    mPlayheadFrame.store(0, std::memory_order_relaxed);
}

void SamplePlayer::clear() {
    std::lock_guard<std::mutex> lock(mDataMutex);
    mIsPlaying.store(false, std::memory_order_relaxed);
    mLoaded.store(false, std::memory_order_release);
    mPcmBuffer.clear();
    mPcmBuffer.shrink_to_fit();
    mTotalFrames = 0;
    mPlayheadFrame.store(0, std::memory_order_relaxed);
}

float SamplePlayer::getDurationSec() const {
    if (mSampleRate == 0) return 0.f;
    return static_cast<float>(mTotalFrames) / static_cast<float>(mSampleRate);
}

void SamplePlayer::renderAudio(float* output, int32_t numFrames,
                                int32_t channelCount, float gain) {
    if (!mIsPlaying.load(std::memory_order_acquire)) return;
    if (!mLoaded.load(std::memory_order_acquire)) return;

    std::unique_lock<std::mutex> lock(mDataMutex, std::try_to_lock);
    if (!lock.owns_lock()) return;

    const bool  loop  = mLoopEnabled.load(std::memory_order_relaxed);
    int64_t     head  = mPlayheadFrame.load(std::memory_order_relaxed);
    const int64_t total = mTotalFrames;

    for (int32_t f = 0; f < numFrames; ++f) {
        if (head >= total) {
            if (loop) {
                head = 0;
            } else {
                mIsPlaying.store(false, std::memory_order_release);
                break;
            }
        }

        const float l = mPcmBuffer[static_cast<size_t>(head) * 2];
        const float r = mPcmBuffer[static_cast<size_t>(head) * 2 + 1];
        ++head;

        if (channelCount >= 2) {
            output[f * channelCount]     += l * gain;
            output[f * channelCount + 1] += r * gain;
        } else {
            output[f] += (l + r) * 0.5f * gain;
        }
    }

    mPlayheadFrame.store(head, std::memory_order_relaxed);
}
