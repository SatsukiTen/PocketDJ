#pragma once

#include "DeckPlayer.h"
#include "SamplePlayer.h"
#include <oboe/Oboe.h>
#include <atomic>
#include <memory>
#include <array>

/**
 * T-203: 音声エンジン（シングルトン）
 * Oboe AudioStream を管理し、2つの DeckPlayer を混合して出力する。
 * onAudioReady() がオーディオスレッドから毎コールバック呼ばれる。
 */
class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    static AudioEngine& getInstance();

    bool init();
    void destroy();

    // デッキ操作（fd: Kotlin 側で openFileDescriptor().detachFd() した fd）
    bool  loadTrackFd(int deckId, int fd);
    void  play(int deckId);
    void  pause(int deckId);
    void  stop(int deckId);
    float getPlayheadSec(int deckId) const;
    float getDurationSec(int deckId) const;

    // クロスフェーダー（Sprint 3 で本実装）
    void setCrossfaderPosition(float position);  // 0.0(A) 〜 1.0(B)
    void setCrossfaderCurve(int curveType);       // 0=Linear, 1=EqualPower

    // EQ（Sprint 5 で本実装）
    void setEq(int deckId, int band, float gainDb);

    // ピッチ / BPM（T-404 / T-402）
    void    setPitchRatio(int deckId, float ratio);
    float   getBpm(int deckId) const;
    int32_t getSampleRate(int deckId) const;

    // ループ（Sprint 6 で本実装）
    void setLoop(int deckId, int64_t loopInSample, int64_t loopOutSample);
    void clearLoop(int deckId);

    // シーク
    void seekTo(int deckId, float positionSec);

    // スクラッチ（Sprint 7 で本実装）
    void startScratch(int deckId);
    void setScratchSpeed(int deckId, float speed);
    void stopScratch(int deckId);

    // サンプルパッド（4スロット）
    bool captureSample(int slotId, int deckId, int64_t inFrame, int64_t outFrame);
    void playSample(int slotId, bool loop);
    void stopSample(int slotId);
    bool isSamplePlaying(int slotId) const;
    void clearSample(int slotId);

    // レイテンシ計測（T-802）
    double  getLatencyMs()  const;
    int32_t getXRunCount()  const;

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* audioStream,
            void* audioData,
            int32_t numFrames) override;

    // oboe::AudioStreamErrorCallback — ストリーム切断時に自動再起動
    void onErrorAfterClose(oboe::AudioStream* audioStream,
                           oboe::Result result) override;

private:
    AudioEngine() = default;
    ~AudioEngine() { destroy(); }
    AudioEngine(const AudioEngine&) = delete;
    AudioEngine& operator=(const AudioEngine&) = delete;

    bool openStream();
    void computeGains(float position, int curveType,
                      float& gainA, float& gainB) const;

    std::shared_ptr<oboe::AudioStream> mStream;
    std::array<std::unique_ptr<DeckPlayer>,   2> mDecks;
    std::array<std::unique_ptr<SamplePlayer>, 4> mSamplePlayers;

    std::atomic<float> mCrossfaderPos{0.5f};  // 0.5 = 両デッキ等音量
    std::atomic<int>   mCurveType{1};          // 1 = EqualPower デフォルト

    bool mInitialized = false;
};
