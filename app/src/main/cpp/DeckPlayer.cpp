#include "DeckPlayer.h"
#include <android/log.h>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <unistd.h>      // close()
#include <climits>       // LONG_MAX
#include <unordered_map> // detectBpmMultiWindow

#define LOG_TAG "DeckPlayer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

DeckPlayer::DeckPlayer() = default;

DeckPlayer::~DeckPlayer() {
    unload();
}

void DeckPlayer::getEqGains(float gainDb[3]) const {
    for (int i = 0; i < 3; ++i) gainDb[i] = mEq.getGainDb(i);
}

// ---------------------------------------------------------------------------
// openDecoder: fd からメタデータを読み取り extractor/codec を初期化する。
// fd は呼び出し元から所有権を受け取り、この関数内で close() する。
// ---------------------------------------------------------------------------
bool DeckPlayer::openDecoder(int fd,
                              AMediaExtractor** outExtractor,
                              AMediaCodec**     outCodec) {
    AMediaExtractor* extractor = AMediaExtractor_new();
    if (!extractor) { close(fd); return false; }

    off64_t fileSize = lseek64(fd, 0, SEEK_END);
    lseek64(fd, 0, SEEK_SET);

    media_status_t status = AMediaExtractor_setDataSourceFd(
        extractor, fd, 0, fileSize > 0 ? fileSize : LONG_MAX);
    close(fd);
    if (status != AMEDIA_OK) {
        LOGE("setDataSourceFd failed: %d", status);
        AMediaExtractor_delete(extractor);
        return false;
    }

    int audioTrack = -1;
    AMediaFormat* audioFormat = nullptr;
    size_t trackCount = AMediaExtractor_getTrackCount(extractor);
    for (size_t i = 0; i < trackCount; ++i) {
        AMediaFormat* fmt = AMediaExtractor_getTrackFormat(extractor, i);
        const char* mime = nullptr;
        AMediaFormat_getString(fmt, AMEDIAFORMAT_KEY_MIME, &mime);
        if (mime && strncmp(mime, "audio/", 6) == 0) {
            audioTrack = static_cast<int>(i);
            audioFormat = fmt;
            break;
        }
        AMediaFormat_delete(fmt);
    }
    if (audioTrack < 0 || !audioFormat) {
        LOGE("No audio track found");
        AMediaExtractor_delete(extractor);
        return false;
    }

    int32_t srcChannels   = 1;
    int32_t srcSampleRate = 44100;
    AMediaFormat_getInt32(audioFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &srcChannels);
    AMediaFormat_getInt32(audioFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE,   &srcSampleRate);
    mChannels   = srcChannels;
    mSampleRate = srcSampleRate;

    // フォーマットから完全長を取得（バックグラウンドデコード中でも getDurationSec が正確）
    int64_t durationUs = 0;
    AMediaFormat_getInt64(audioFormat, AMEDIAFORMAT_KEY_DURATION, &durationUs);
    if (durationUs > 0) {
        int64_t fullFrames = static_cast<int64_t>(
            static_cast<double>(durationUs) / 1e6 * srcSampleRate);
        mFullDurationFrames.store(fullFrames);
        // reallocation を排除するためバッファを事前 reserve
        mPcmBuffer.reserve(static_cast<size_t>(
            static_cast<double>(fullFrames) * srcChannels * 1.05 + 4096));
    }

    const char* mime = nullptr;
    AMediaFormat_getString(audioFormat, AMEDIAFORMAT_KEY_MIME, &mime);
    AMediaCodec* codec = AMediaCodec_createDecoderByType(mime);
    if (!codec) {
        LOGE("Failed to create codec for %s", mime);
        AMediaFormat_delete(audioFormat);
        AMediaExtractor_delete(extractor);
        return false;
    }
    AMediaCodec_configure(codec, audioFormat, nullptr, nullptr, 0);
    AMediaFormat_delete(audioFormat);
    AMediaCodec_start(codec);
    AMediaExtractor_selectTrack(extractor, audioTrack);

    *outExtractor = extractor;
    *outCodec     = codec;
    return true;
}

// ---------------------------------------------------------------------------
// decodeLoop: maxFrames に達するか EOS か世代不一致で停止。
// EOS に達した場合 true を返す。
// 入力は non-blocking(timeout=0) で一括フィードし、
// 出力は 5ms タイムアウトで待つ（スピンを避けつつ即応性を確保）。
// ---------------------------------------------------------------------------
bool DeckPlayer::decodeLoop(AMediaExtractor* extractor,
                             AMediaCodec*     codec,
                             bool&            inputDone,
                             int64_t          maxFrames,
                             int64_t          gen) {
    while (mLoadGeneration.load(std::memory_order_relaxed) == gen &&
           mTotalFrames.load(std::memory_order_relaxed) < maxFrames) {

        // 入力バッファを non-blocking で一括フィード
        if (!inputDone) {
            ssize_t bufIdx;
            while ((bufIdx = AMediaCodec_dequeueInputBuffer(codec, 0)) >= 0) {
                if (mLoadGeneration.load(std::memory_order_relaxed) != gen) return false;
                size_t   bufSize = 0;
                uint8_t* buf     = AMediaCodec_getInputBuffer(codec, bufIdx, &bufSize);
                ssize_t  sampleSize = AMediaExtractor_readSampleData(extractor, buf, bufSize);
                if (sampleSize < 0) {
                    AMediaCodec_queueInputBuffer(codec, bufIdx, 0, 0,
                        AMediaExtractor_getSampleTime(extractor),
                        AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                    inputDone = true;
                    break;
                }
                int64_t pts = AMediaExtractor_getSampleTime(extractor);
                AMediaCodec_queueInputBuffer(codec, bufIdx, 0, sampleSize, pts, 0);
                AMediaExtractor_advance(extractor);
            }
        }

        // 出力バッファを最大 5ms 待って取得
        AMediaCodecBufferInfo info;
        ssize_t outIdx = AMediaCodec_dequeueOutputBuffer(codec, &info, 5000);
        if (outIdx >= 0) {
            if (mLoadGeneration.load(std::memory_order_relaxed) == gen && info.size > 0) {
                size_t   outSize = 0;
                uint8_t* outBuf  = AMediaCodec_getOutputBuffer(codec, outIdx, &outSize);
                if (outBuf) {
                    int16_t* samples    = reinterpret_cast<int16_t*>(outBuf + info.offset);
                    int32_t  numSamples = info.size / sizeof(int16_t);
                    for (int32_t s = 0; s < numSamples; ++s) {
                        mPcmBuffer.push_back(samples[s] / 32768.0f);
                    }
                    // release: この store より前の mPcmBuffer 書き込みを renderAudio に可視化
                    mTotalFrames.fetch_add(numSamples / mChannels,
                                           std::memory_order_release);
                }
            }
            AMediaCodec_releaseOutputBuffer(codec, outIdx, false);
            if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) return true;
        }
        // INFO_OUTPUT_FORMAT_CHANGED / TRY_AGAIN_LATER: ループ継続
    }
    return false;
}

// ---------------------------------------------------------------------------
// loadTrackFd: 2フェーズロード
//   Phase 1 — 最初の 5 秒を同期デコード → mLoaded = true → 即返す（~0.2-1s）
//   Phase 2 — 残りをバックグラウンドスレッドで継続デコード
// ---------------------------------------------------------------------------
bool DeckPlayer::loadTrackFd(int fd) {
    // 世代カウンタを進めてバックグラウンドスレッドにキャンセルを通知
    mLoadGeneration.fetch_add(1, std::memory_order_relaxed);

    std::lock_guard<std::mutex> lock(mLoadMutex);

    mIsPlaying.store(false);
    mLoaded.store(false);
    mPcmBuffer.clear();
    mTotalFrames.store(0);
    mFullDurationFrames.store(0);
    mPlayheadFrame.store(0);
    mPlayheadFrac.store(0.f);
    mDetectedBpm.store(0.f);
    mLoopEnabled.store(false);
    mLoopInFrame.store(0);
    mLoopOutFrame.store(0);

    const int64_t gen = mLoadGeneration.load(std::memory_order_relaxed);

    AMediaExtractor* extractor = nullptr;
    AMediaCodec*     codec     = nullptr;
    if (!openDecoder(fd, &extractor, &codec)) return false;

    // Phase 1: 最初の 5 秒を同期デコード
    bool inputDone  = false;
    bool outputDone = decodeLoop(extractor, codec, inputDone,
                                 static_cast<int64_t>(5 * mSampleRate), gen);

    if (mTotalFrames.load() == 0) {
        AMediaCodec_stop(codec);
        AMediaCodec_delete(codec);
        AMediaExtractor_delete(extractor);
        LOGE("Phase 1: decoded 0 frames");
        return false;
    }

    // Phase 1 完了 — 再生可能状態に
    mEq.init(mSampleRate);  // サンプルレート確定後に EQ 係数を再計算
    mLoaded.store(true, std::memory_order_release);
    LOGI("Phase 1 done: frames=%lld  ch=%d  sr=%d",
         (long long)mTotalFrames.load(), mChannels, mSampleRate);

    // Phase 1 の 5 秒データで即時 BPM 推定を開始（Kotlin 側ポーリングに間に合わせるため）
    startBpmDetectionAsync(gen);

    if (outputDone) {
        // トラックが 5 秒以下: デコード完了
        AMediaCodec_stop(codec);
        AMediaCodec_delete(codec);
        AMediaExtractor_delete(extractor);
    } else {
        // Phase 2: 残りをバックグラウンドスレッドでデコード
        std::thread([this, extractor, codec, inputDone, gen]() mutable {
            bool done = decodeLoop(extractor, codec, inputDone, INT64_MAX, gen);
            AMediaCodec_stop(codec);
            AMediaCodec_delete(codec);
            AMediaExtractor_delete(extractor);
            LOGI("Phase 2 done: frames=%lld  done=%d  gen=%lld",
                 (long long)mTotalFrames.load(), (int)done, (long long)gen);
            // 30 秒分のデータで再検出し、精度を改善する
            if (done && mLoadGeneration.load() == gen) {
                startBpmDetectionAsync(gen);
            }
        }).detach();
    }

    return true;
}

void DeckPlayer::unload() {
    mLoadGeneration.fetch_add(1, std::memory_order_relaxed);
    std::lock_guard<std::mutex> lock(mLoadMutex);
    mIsPlaying.store(false);
    mLoaded.store(false);
    mPcmBuffer.clear();
    mPcmBuffer.shrink_to_fit();
    mTotalFrames.store(0);
    mFullDurationFrames.store(0);
    mPlayheadFrame.store(0);
    mPlayheadFrac.store(0.f);
    mDetectedBpm.store(0.f);
}

void DeckPlayer::play() {
    if (mLoaded.load()) mIsPlaying.store(true);
}

void DeckPlayer::pause() {
    mIsPlaying.store(false);
}

void DeckPlayer::stop() {
    mIsPlaying.store(false);
    mPlayheadFrame.store(0);
    mPlayheadFrac.store(0.f);
}

float DeckPlayer::getPlayheadSec() const {
    if (mSampleRate == 0) return 0.f;
    return static_cast<float>(mPlayheadFrame.load()) / static_cast<float>(mSampleRate);
}

float DeckPlayer::getDurationSec() const {
    if (mSampleRate == 0) return 0.f;
    int64_t full = mFullDurationFrames.load();
    if (full > 0) return static_cast<float>(full) / static_cast<float>(mSampleRate);
    return static_cast<float>(mTotalFrames.load()) / static_cast<float>(mSampleRate);
}

void DeckPlayer::setLoop(int64_t loopInSample, int64_t loopOutSample) {
    mLoopInFrame.store(loopInSample);
    mLoopOutFrame.store(loopOutSample);
    mLoopEnabled.store(true);
}

void DeckPlayer::clearLoop() {
    mLoopEnabled.store(false);
}

bool DeckPlayer::copyPcmRange(std::vector<float>& dest,
                               int64_t inFrame, int64_t outFrame) const {
    int64_t loaded = mTotalFrames.load(std::memory_order_acquire);
    if (inFrame < 0 || outFrame > loaded || inFrame >= outFrame) return false;

    int64_t frameCount = outFrame - inFrame;
    dest.resize(static_cast<size_t>(frameCount) * 2);  // always stereo output

    if (mChannels == 2) {
        const float* src = mPcmBuffer.data() + static_cast<size_t>(inFrame) * 2;
        std::copy(src, src + static_cast<size_t>(frameCount) * 2, dest.data());
    } else {
        // Mono → Stereo
        for (int64_t i = 0; i < frameCount; ++i) {
            float s = mPcmBuffer[static_cast<size_t>(inFrame + i)];
            dest[static_cast<size_t>(i) * 2]     = s;
            dest[static_cast<size_t>(i) * 2 + 1] = s;
        }
    }
    return true;
}

void DeckPlayer::seekTo(float positionSec) {
    if (!mLoaded.load(std::memory_order_acquire)) return;
    int64_t total  = mTotalFrames.load(std::memory_order_acquire);
    int64_t target = static_cast<int64_t>(positionSec * static_cast<float>(mSampleRate));
    if (target < 0)      target = 0;
    if (target >= total) target = (total > 0) ? total - 1 : 0;
    mPlayheadFrame.store(target, std::memory_order_release);
    mPlayheadFrac.store(0.f,    std::memory_order_relaxed);
}

void DeckPlayer::startScratch()               { mIsScratch.store(true); }
void DeckPlayer::setScratchSpeed(float speed) { mScratchSpeed.store(speed); }
void DeckPlayer::stopScratch()                { mIsScratch.store(false); mScratchSpeed.store(1.0f); }

// ---------------------------------------------------------------------------
// renderAudio: ミューテックス不要（すべての状態が atomic）
// mTotalFrames.load(acquire) を上限として mPcmBuffer を読み取る。
// バックグラウンドデコード中でも安全: decode thread は [total*ch, ...) に書き込み、
// この関数は [0, total*ch) を読み取る → 領域は非重複。
// ---------------------------------------------------------------------------
void DeckPlayer::renderAudio(float* outputData, int32_t numFrames,
                              int32_t channelCount, float gainLeft, float gainRight) {
    bool isScratch = mIsScratch.load(std::memory_order_relaxed);
    if (!isScratch && !mIsPlaying.load(std::memory_order_relaxed)) return;
    if (!mLoaded.load(std::memory_order_acquire)) return;

    mEq.beginBuffer();  // 保留中のゲイン変更を適用（コールバック先頭で 1 回）

    int64_t total   = mTotalFrames.load(std::memory_order_acquire);
    if (total == 0) return;

    int64_t head    = mPlayheadFrame.load(std::memory_order_relaxed);
    float   frac    = mPlayheadFrac.load(std::memory_order_relaxed);
    // スクラッチ中は mScratchSpeed を速度として使用（負値で逆再生）
    float   pitch   = isScratch
                      ? mScratchSpeed.load(std::memory_order_relaxed)
                      : mPitchRatio.load(std::memory_order_relaxed);
    // スクラッチ中はループを無効にする
    bool    loopOn  = !isScratch && mLoopEnabled.load(std::memory_order_relaxed);
    int64_t loopIn  = mLoopInFrame.load(std::memory_order_relaxed);
    int64_t loopOut = mLoopOutFrame.load(std::memory_order_relaxed);

    for (int32_t i = 0; i < numFrames; ++i) {
        if (loopOn && loopOut > loopIn && head >= loopOut) {
            head = loopIn;
            frac = 0.f;
        }

        // 逆再生スクラッチでヘッドが先頭を超えた場合はクランプ
        if (head < 0) { head = 0; frac = 0.f; }

        if (head >= total) {
            // バックグラウンドデコードが進んでいれば最新の total を再取得
            total = mTotalFrames.load(std::memory_order_acquire);
            if (head >= total) {
                // スクラッチ中は自然停止しない（ヘッドを末尾でクランプ）
                if (!isScratch) {
                    int64_t full = mFullDurationFrames.load();
                    if (full == 0 || total >= full) {
                        mIsPlaying.store(false);
                    }
                }
                break;
            }
        }

        int64_t next = (head + 1 < total) ? head + 1 : head;
        float sL0, sR0, sL1, sR1;
        if (mChannels >= 2) {
            sL0 = mPcmBuffer[static_cast<size_t>(head * mChannels + 0)];
            sR0 = mPcmBuffer[static_cast<size_t>(head * mChannels + 1)];
            sL1 = mPcmBuffer[static_cast<size_t>(next * mChannels + 0)];
            sR1 = mPcmBuffer[static_cast<size_t>(next * mChannels + 1)];
        } else {
            sL0 = sR0 = mPcmBuffer[static_cast<size_t>(head)];
            sL1 = sR1 = mPcmBuffer[static_cast<size_t>(next)];
        }
        float sL = sL0 + frac * (sL1 - sL0);
        float sR = sR0 + frac * (sR1 - sR0);

        if (!mEq.isFlat()) mEq.processFrame(sL, sR);

        if (channelCount >= 2) {
            outputData[i * channelCount + 0] += sL * gainLeft;
            outputData[i * channelCount + 1] += sR * gainRight;
        } else {
            outputData[i] += (sL + sR) * 0.5f * gainLeft;
        }

        // floor を使い frac を [0,1) に保つ（負速度の逆再生でも正確に動作）
        frac += pitch;
        int64_t step = static_cast<int64_t>(std::floor(frac));
        frac -= static_cast<float>(step);
        head += step;
    }

    mPlayheadFrame.store(head, std::memory_order_relaxed);
    mPlayheadFrac.store(frac,  std::memory_order_relaxed);
}

// ---------------------------------------------------------------------------
// detectBpmMultiWindow: 複数窓サンプリング + ヒストグラム投票による高精度BPM検出
//
// アルゴリズム:
//   1. トラック全体から等間隔に最大 MAX_WINS 個の 10 秒窓を抽出
//      （最初の 10% をスキップしてイントロを除外）
//   2. 各窓で detectBpmFromData を実行して BPM 候補を収集
//   3. 各候補を 4BPM 単位のビンに丸めて多数決
//   4. 本値 = 勝利ビンに属する候補の生 float 平均（精度を保持）
// ---------------------------------------------------------------------------
float DeckPlayer::detectBpmMultiWindow(const float* data, int64_t totalFrames,
                                       int32_t channels, int32_t sampleRate) {
    const int64_t WIN_FRAMES = 10 * static_cast<int64_t>(sampleRate);
    const int     MAX_WINS   = 10;

    // 最初の 10% をスキップ（イントロ・フェードイン対策）
    const int64_t skipFrames   = totalFrames / 10;
    const int64_t usableFrames = totalFrames - skipFrames;

    // 使用可能長が 2 窓未満なら単一窓にフォールバック
    if (usableFrames < WIN_FRAMES * 2) {
        return detectBpmFromData(data, totalFrames, channels, sampleRate);
    }

    const int numWins = static_cast<int>(std::min(
        static_cast<int64_t>(MAX_WINS),
        usableFrames / WIN_FRAMES));

    // 各窓の BPM を収集
    std::vector<float> candidates;
    candidates.reserve(numWins);
    for (int i = 0; i < numWins; ++i) {
        // 等間隔オフセット（最後の窓が usableFrames の終端に揃う）
        int64_t offset = skipFrames
            + (numWins > 1 ? (usableFrames - WIN_FRAMES) * i / (numWins - 1) : 0);
        float bpm = detectBpmFromData(
            data + offset * channels, WIN_FRAMES, channels, sampleRate);
        if (bpm > 0.f) candidates.push_back(bpm);
    }

    if (candidates.empty()) return 0.f;
    if (candidates.size() == 1) return candidates[0];

    // 4BPM 単位のビンで多数決
    // （同じ楽曲の窓間で ±2BPM 程度の揺らぎが生じるため、まとめて同一票として扱う）
    std::unordered_map<int, std::vector<float>> hist;
    for (float bpm : candidates) {
        int bin = static_cast<int>(std::round(bpm / 4.f)) * 4;
        hist[bin].push_back(bpm);
    }

    // 最多票ビンを選択
    int    peakBin   = -1;
    size_t peakCount = 0;
    for (auto& kv : hist) {
        if (kv.second.size() > peakCount) {
            peakCount = kv.second.size();
            peakBin   = kv.first;
        }
    }
    if (peakBin < 0) return 0.f;

    // 本値 = 勝利ビンの候補の平均（丸め前の float 精度を保持）
    const auto& winners = hist[peakBin];
    float sum = 0.f;
    for (float v : winners) sum += v;
    return sum / static_cast<float>(winners.size());
}

// ---------------------------------------------------------------------------
// startBpmDetectionAsync: データのローカルコピーを取り非同期検出
//   - データが 30 秒以上あれば多窓ヒストグラム投票（最大 120 秒を使用）
//   - 30 秒未満（Phase 1 直後）は単一窓で速報値を算出
// ---------------------------------------------------------------------------
void DeckPlayer::startBpmDetectionAsync(int64_t gen) {
    const int64_t MAX_BPM_FRAMES = 120 * static_cast<int64_t>(mSampleRate);
    const int64_t bpmFrames = std::min(
        mTotalFrames.load(std::memory_order_relaxed),
        MAX_BPM_FRAMES);
    std::vector<float> bpmData(
        mPcmBuffer.begin(),
        mPcmBuffer.begin() + static_cast<ptrdiff_t>(bpmFrames * mChannels));
    const int32_t sr = mSampleRate;
    const int32_t ch = mChannels;

    std::thread([this, gen, data = std::move(bpmData), frames = bpmFrames, sr, ch]() {
        const int64_t MIN_MULTIWINDOW = 30 * static_cast<int64_t>(sr);
        float bpm = (frames >= MIN_MULTIWINDOW)
            ? detectBpmMultiWindow(data.data(), frames, ch, sr)
            : detectBpmFromData(data.data(), frames, ch, sr);
        if (mLoadGeneration.load() == gen) {
            mDetectedBpm.store(bpm);
            LOGI("BPM detected: %.2f  windows=%s  frames=%lld  gen=%lld",
                 bpm,
                 (frames >= MIN_MULTIWINDOW ? "multi" : "single"),
                 (long long)frames, (long long)gen);
        }
    }).detach();
}

// ---------------------------------------------------------------------------
// detectBpmFromData: 自己相関法によるBPM検出
// ローカルコピーを受け取るので排他制御不要・別スレッド実行可能。
// 半周期ハーモニックチェック付き（半BPM誤検出を補正）。
// ---------------------------------------------------------------------------
float DeckPlayer::detectBpmFromData(const float* data, int64_t frames,
                                    int32_t channels, int32_t sampleRate) {
    if (frames < sampleRate) return 0.f;  // 1秒未満は検出不可

    const int32_t hopSize = 512;
    const int     N       = static_cast<int>(frames / hopSize);

    // エネルギーフラックスによるオンセット関数を構築
    std::vector<float> onset;
    onset.reserve(static_cast<size_t>(N) + 1);
    float prevEnergy = 0.f;

    for (int64_t f = 0; f < frames; f += hopSize) {
        const int64_t end = std::min(f + hopSize, frames);
        const int64_t len = end - f;
        float energy = 0.f;
        if (channels >= 2) {
            for (int64_t s = f; s < end; ++s) {
                float l = data[s * channels + 0];
                float r = data[s * channels + 1];
                energy += l * l + r * r;
            }
            energy /= static_cast<float>(2 * len);
        } else {
            for (int64_t s = f; s < end; ++s) {
                float m = data[s];
                energy += m * m;
            }
            energy /= static_cast<float>(len);
        }
        onset.push_back(std::max(0.f, energy - prevEnergy));
        prevEnergy = energy;
    }

    const int onsetN = static_cast<int>(onset.size());
    if (onsetN < 20) return 0.f;

    const float onsetSR   = static_cast<float>(sampleRate) / hopSize;
    const int   minPeriod = std::max(1, static_cast<int>(onsetSR * 60.f / 200.f));
    const int   maxPeriod = std::min(onsetN / 2,
                                     static_cast<int>(onsetSR * 60.f / 60.f) + 1);
    if (minPeriod >= maxPeriod) return 0.f;

    float bestCorr = 0.f;
    int   bestTau  = minPeriod;
    for (int tau = minPeriod; tau <= maxPeriod; ++tau) {
        const int count = onsetN - tau;
        float corr = 0.f;
        for (int i = 0; i < count; ++i) corr += onset[i] * onset[i + tau];
        corr /= static_cast<float>(count);
        if (corr > bestCorr) { bestCorr = corr; bestTau = tau; }
    }

    if (bestCorr <= 0.f) return 0.f;

    // 半周期の相関が 60% 以上なら高BPM側を優先（2拍周期の誤検出を補正）
    if (bestTau >= 2 * minPeriod) {
        const int   halfTau  = bestTau / 2;
        const int   count2   = onsetN - halfTau;
        float       halfCorr = 0.f;
        for (int i = 0; i < count2; ++i) halfCorr += onset[i] * onset[i + halfTau];
        halfCorr /= static_cast<float>(count2);
        if (halfCorr >= 0.60f * bestCorr) bestTau = halfTau;
    }

    float bpm = onsetSR * 60.f / static_cast<float>(bestTau);
    while (bpm < 60.f)  bpm *= 2.f;
    while (bpm > 200.f) bpm /= 2.f;
    return bpm;
}
