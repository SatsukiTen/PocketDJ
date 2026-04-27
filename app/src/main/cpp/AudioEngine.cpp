#include "AudioEngine.h"
#include "EqProcessor.h"
#include <android/log.h>
#include <cstring>
#include <cmath>
#include <algorithm>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioEngine& AudioEngine::getInstance() {
    static AudioEngine instance;
    return instance;
}

bool AudioEngine::init() {
    if (mInitialized) return true;

    mDecks[0] = std::make_unique<DeckPlayer>();
    mDecks[1] = std::make_unique<DeckPlayer>();
    for (auto& sp : mSamplePlayers) sp = std::make_unique<SamplePlayer>();

    if (!openStream()) return false;

    mInitialized = true;
    LOGI("AudioEngine initialized");
    return true;
}

void AudioEngine::destroy() {
    if (mStream) {
        mStream->requestStop();
        mStream->close();
        mStream.reset();
    }
    mDecks[0].reset();
    mDecks[1].reset();
    for (auto& sp : mSamplePlayers) sp.reset();
    mInitialized = false;
    LOGI("AudioEngine destroyed");
}

bool AudioEngine::openStream() {
    oboe::AudioStreamBuilder builder;
    // Oboe セッターは AudioStreamBuilder* を返すのでチェーンに -> を使う
    oboe::Result result = builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setDataCallback(this)
           ->setErrorCallback(this)
           ->openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        mStream->close();
        mStream.reset();
        return false;
    }

    LOGI("Stream opened: sampleRate=%d  framesPerBurst=%d",
         mStream->getSampleRate(), mStream->getFramesPerBurst());
    {
        auto lat = mStream->calculateLatencyMillis();
        LOGI("Estimated output latency: %.2f ms", lat ? lat.value() : -1.0);
    }
    return true;
}

// ---------------------------------------------------------------------------
// デッキ操作
// ---------------------------------------------------------------------------
bool AudioEngine::loadTrackFd(int deckId, int fd) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return false;
    return mDecks[deckId]->loadTrackFd(fd);
}

void AudioEngine::play(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->play();
}

void AudioEngine::pause(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->pause();
}

void AudioEngine::stop(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->stop();
}

float AudioEngine::getPlayheadSec(int deckId) const {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return 0.f;
    return mDecks[deckId]->getPlayheadSec();
}

float AudioEngine::getDurationSec(int deckId) const {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return 0.f;
    return mDecks[deckId]->getDurationSec();
}

void AudioEngine::setCrossfaderPosition(float position) {
    mCrossfaderPos.store(std::clamp(position, 0.f, 1.f));
}

void AudioEngine::setCrossfaderCurve(int curveType) {
    mCurveType.store(curveType);
}

void AudioEngine::setEq(int deckId, int band, float gainDb) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->setEq(band, gainDb);
}

void AudioEngine::setPitchRatio(int deckId, float ratio) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->setPitchRatio(ratio);
}

float AudioEngine::getBpm(int deckId) const {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return 0.f;
    return mDecks[deckId]->getBpm();
}

int32_t AudioEngine::getSampleRate(int deckId) const {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return 48000;
    return mDecks[deckId]->getSampleRate();
}

void AudioEngine::setLoop(int deckId, int64_t loopInSample, int64_t loopOutSample) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->setLoop(loopInSample, loopOutSample);
}

void AudioEngine::clearLoop(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->clearLoop();
}

void AudioEngine::seekTo(int deckId, float positionSec) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->seekTo(positionSec);
}

void AudioEngine::startScratch(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->startScratch();
}

void AudioEngine::setScratchSpeed(int deckId, float speed) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->setScratchSpeed(speed);
}

void AudioEngine::stopScratch(int deckId) {
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return;
    mDecks[deckId]->stopScratch();
}

// ---------------------------------------------------------------------------
// サンプルパッド
// ---------------------------------------------------------------------------
bool AudioEngine::captureSample(int slotId, int deckId, int64_t inFrame, int64_t outFrame) {
    if (slotId < 0 || slotId >= 4) return false;
    if (deckId < 0 || deckId > 1 || !mDecks[deckId]) return false;
    if (!mSamplePlayers[slotId]) return false;

    std::vector<float> pcm;
    if (!mDecks[deckId]->copyPcmRange(pcm, inFrame, outFrame)) return false;

    int32_t sr        = mDecks[deckId]->getSampleRate();
    int64_t srcFrames = outFrame - inFrame;

    // ピッチリサンプル: 再生時の速度・音程をサンプルに焼き込む
    const float pitchRatio = mDecks[deckId]->getPitchRatio();
    if (std::abs(pitchRatio - 1.0f) > 1e-4f) {
        int64_t dstFrames = std::max<int64_t>(
            static_cast<int64_t>(srcFrames / pitchRatio + 0.5f), 1);
        std::vector<float> resampled(static_cast<size_t>(dstFrames) * 2);
        for (int64_t i = 0; i < dstFrames; ++i) {
            float   srcF = static_cast<float>(i) * pitchRatio;
            int64_t si   = static_cast<int64_t>(srcF);
            float   frac = srcF - static_cast<float>(si);
            int64_t ni   = std::min(si + 1, srcFrames - 1);
            resampled[i * 2 + 0] = pcm[si * 2 + 0] * (1.f - frac) + pcm[ni * 2 + 0] * frac;
            resampled[i * 2 + 1] = pcm[si * 2 + 1] * (1.f - frac) + pcm[ni * 2 + 1] * frac;
        }
        pcm       = std::move(resampled);
        srcFrames = dstFrames;
    }

    // EQ オフライン適用: 現在のデッキ EQ 設定をサンプルに焼き込む
    float gainDb[3];
    mDecks[deckId]->getEqGains(gainDb);
    if (gainDb[0] != 0.f || gainDb[1] != 0.f || gainDb[2] != 0.f) {
        EqProcessor eq;
        eq.init(sr);
        eq.setGainDb(0, gainDb[0]);
        eq.setGainDb(1, gainDb[1]);
        eq.setGainDb(2, gainDb[2]);
        eq.beginBuffer();
        for (int64_t i = 0; i < srcFrames; ++i) {
            eq.processFrame(pcm[i * 2 + 0], pcm[i * 2 + 1]);
        }
    }

    mSamplePlayers[slotId]->loadPcm(pcm.data(), srcFrames, sr);
    return true;
}

void AudioEngine::playSample(int slotId, bool loop) {
    if (slotId < 0 || slotId >= 4 || !mSamplePlayers[slotId]) return;
    mSamplePlayers[slotId]->play(loop);
}

void AudioEngine::stopSample(int slotId) {
    if (slotId < 0 || slotId >= 4 || !mSamplePlayers[slotId]) return;
    mSamplePlayers[slotId]->stop();
}

bool AudioEngine::isSamplePlaying(int slotId) const {
    if (slotId < 0 || slotId >= 4 || !mSamplePlayers[slotId]) return false;
    return mSamplePlayers[slotId]->isPlaying();
}

void AudioEngine::clearSample(int slotId) {
    if (slotId < 0 || slotId >= 4 || !mSamplePlayers[slotId]) return;
    mSamplePlayers[slotId]->clear();
}

// ---------------------------------------------------------------------------
// T-802: レイテンシ計測
// ---------------------------------------------------------------------------
double AudioEngine::getLatencyMs() const {
    if (!mStream) return 0.0;
    auto result = mStream->calculateLatencyMillis();
    return result ? result.value() : 0.0;
}

int32_t AudioEngine::getXRunCount() const {
    if (!mStream) return 0;
    auto result = mStream->getXRunCount();
    return result ? result.value() : 0;
}

// ---------------------------------------------------------------------------
// ストリーム切断時の自動再起動（ヘッドフォン抜き差し・デバイス切替対応）
// ---------------------------------------------------------------------------
void AudioEngine::onErrorAfterClose(oboe::AudioStream* /*audioStream*/,
                                    oboe::Result result) {
    LOGE("Stream error after close: %s — restarting", oboe::convertToText(result));
    openStream();
}

// ---------------------------------------------------------------------------
// クロスフェーダーゲイン計算
// ---------------------------------------------------------------------------
void AudioEngine::computeGains(float position, int curveType,
                                float& gainA, float& gainB) const {
    // position: 0.0(A のみ) 〜 1.0(B のみ)
    if (curveType == 0) {
        // Linear
        gainA = 1.f - position;
        gainB = position;
    } else {
        // Equal Power (sin/cos カーブ)
        constexpr float PI_HALF = 3.14159265f * 0.5f;
        gainA = std::cos(position * PI_HALF);
        gainB = std::sin(position * PI_HALF);
    }
}

// ---------------------------------------------------------------------------
// Oboe コールバック（オーディオスレッド）
// ---------------------------------------------------------------------------
oboe::DataCallbackResult AudioEngine::onAudioReady(
        oboe::AudioStream* /*audioStream*/,
        void* audioData,
        int32_t numFrames) {

    auto* output = static_cast<float*>(audioData);
    const int32_t channelCount = 2;  // Stereo

    // ゼロ初期化（各 DeckPlayer が加算書き込みする）
    memset(output, 0, sizeof(float) * numFrames * channelCount);

    float gainA = 1.f, gainB = 1.f;
    computeGains(mCrossfaderPos.load(), mCurveType.load(), gainA, gainB);

    if (mDecks[0]) mDecks[0]->renderAudio(output, numFrames, channelCount, gainA, gainA);
    if (mDecks[1]) mDecks[1]->renderAudio(output, numFrames, channelCount, gainB, gainB);

    for (auto& sp : mSamplePlayers) {
        if (sp) sp->renderAudio(output, numFrames, channelCount, 1.0f);
    }

    // HALアイドル防止: -100dBFS の微小ディザで無音ストリームの低電力移行を阻止
    // XorShift RNG。最大振幅 ≈ 2^31 * 4.7e-15 ≈ 1e-5 (-100dBFS) → 実質無音
    static uint32_t ditherRng = 0xDEADBEEFu;
    constexpr float DITHER_SCALE = 4.7e-15f;
    for (int32_t i = 0; i < numFrames * channelCount; ++i) {
        ditherRng ^= ditherRng << 13;
        ditherRng ^= ditherRng >> 17;
        ditherRng ^= ditherRng << 5;
        output[i] += static_cast<int32_t>(ditherRng) * DITHER_SCALE;
    }

    // クリッピング防止
    for (int32_t i = 0; i < numFrames * channelCount; ++i) {
        output[i] = std::clamp(output[i], -1.f, 1.f);
    }

    return oboe::DataCallbackResult::Continue;
}
