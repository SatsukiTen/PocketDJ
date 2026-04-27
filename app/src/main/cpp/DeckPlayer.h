#pragma once

#include <jni.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <atomic>
#include <mutex>
#include <vector>
#include "EqProcessor.h"

/**
 * T-203 / T-404: デッキプレイヤー
 *
 * ロード戦略（2フェーズ）:
 *  Phase 1 — 最初の 5 秒を同期デコード → mLoaded = true → Kotlin に即返す
 *  Phase 2 — 残りをバックグラウンドスレッドで継続デコード
 *
 * スレッド安全性:
 *  - mTotalFrames: atomic（decode thread が書き込み、renderAudio が読み取り）
 *  - mPcmBuffer:   事前 reserve() で reallocation なし。
 *                  decode thread は [mTotalFrames*ch, ...) に書き込み、
 *                  renderAudio は [0, mTotalFrames*ch) を読み取り → 非重複。
 *  - renderAudio はミューテックス不要（すべての状態が atomic）
 */
class DeckPlayer {
public:
    DeckPlayer();
    ~DeckPlayer();

    bool  loadTrackFd(int fd);
    void  unload();

    void  play();
    void  pause();
    void  stop();

    bool  isPlaying()      const { return mIsPlaying.load(); }
    float getPlayheadSec() const;
    float getDurationSec() const;

    void    setPitchRatio(float ratio) { mPitchRatio.store(ratio); }
    float   getPitchRatio()            const { return mPitchRatio.load(); }
    float   getBpm()                   const { return mDetectedBpm.load(); }
    int32_t getSampleRate()            const { return mSampleRate; }
    void    getEqGains(float gainDb[3]) const;

    // band: 0=Low, 1=Mid, 2=High
    void  setEq(int band, float gainDb) { mEq.setGainDb(band, gainDb); }

    void  setLoop(int64_t loopInSample, int64_t loopOutSample);
    void  clearLoop();

    void  seekTo(float positionSec);

    // inFrame..outFrame の PCM をインターリーブ stereo float でコピーする。
    // 範囲が未デコードの場合は false を返す。
    bool  copyPcmRange(std::vector<float>& dest,
                       int64_t inFrame, int64_t outFrame) const;

    void  startScratch();
    void  setScratchSpeed(float speed);
    void  stopScratch();

    void  renderAudio(float* outputData, int32_t numFrames,
                      int32_t channelCount, float gainLeft, float gainRight);

private:
    // デコーダを初期化してメタデータを読み取る（高速、< 100ms）。
    // extractor / codec の所有権を呼び出し元に返す。
    bool openDecoder(int fd,
                     AMediaExtractor** outExtractor,
                     AMediaCodec**     outCodec);

    // デコードループ。maxFrames に達するか EOS かキャンセル（世代不一致）で停止。
    // EOS に達した場合 true を返す。
    bool decodeLoop(AMediaExtractor* extractor,
                    AMediaCodec*     codec,
                    bool&            inputDone,
                    int64_t          maxFrames,
                    int64_t          gen);

    // BPM 検出をローカルコピーで非同期実行する。
    void startBpmDetectionAsync(int64_t gen);

    // BPM 検出本体（ロック不要・別スレッド可）。
    static float detectBpmFromData(const float* data, int64_t frames,
                                   int32_t channels, int32_t sampleRate);

    // 複数窓サンプリング + ヒストグラム投票による高精度 BPM 検出。
    static float detectBpmMultiWindow(const float* data, int64_t totalFrames,
                                      int32_t channels, int32_t sampleRate);

    // PCM バッファ（インターリーブ float, stereo/mono）
    std::vector<float>       mPcmBuffer;
    int32_t                  mChannels   = 2;
    int32_t                  mSampleRate = 48000;

    // mTotalFrames: decode thread が fetch_add(release) し renderAudio が load(acquire) する
    std::atomic<int64_t>     mTotalFrames{0};
    // フォーマットから取得した完全な長さ（背景デコード中でも getDurationSec が正確）
    std::atomic<int64_t>     mFullDurationFrames{0};

    // 再生ヘッド・状態（すべて atomic → renderAudio でミューテックス不要）
    std::atomic<int64_t>     mPlayheadFrame{0};
    std::atomic<float>       mPlayheadFrac{0.f};
    std::atomic<bool>        mIsPlaying{false};
    std::atomic<bool>        mLoaded{false};      // Phase 1 完了後 true
    std::atomic<bool>        mLoopEnabled{false};
    std::atomic<int64_t>     mLoopInFrame{0};
    std::atomic<int64_t>     mLoopOutFrame{0};
    std::atomic<float>       mPitchRatio{1.0f};
    std::atomic<float>       mDetectedBpm{0.f};
    std::atomic<int64_t>     mLoadGeneration{0};  // 世代カウンタ（バックグラウンドスレッド制御）
    std::atomic<bool>        mIsScratch{false};
    std::atomic<float>       mScratchSpeed{1.0f};

    EqProcessor              mEq;

    // loadTrackFd / unload の排他（renderAudio は使用しない）
    mutable std::mutex       mLoadMutex;
};
