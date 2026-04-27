#pragma once
#include <atomic>
#include <cmath>
#include <cstring>

// 2次 Biquad フィルター（Direct Form II 転置型）
// stereo: ch=0(L), ch=1(R) で状態を分離
struct BiquadFilter {
    float b0 = 1.f, b1 = 0.f, b2 = 0.f;
    float a1 = 0.f, a2 = 0.f;
    float w1[2] = {};
    float w2[2] = {};

    float process(float x, int ch) {
        float y  = b0 * x + w1[ch];
        w1[ch]   = b1 * x - a1 * y + w2[ch];
        w2[ch]   = b2 * x - a2 * y;
        return y;
    }
    void reset() { w1[0] = w1[1] = w2[0] = w2[1] = 0.f; }
};

// LR4 クロスオーバー用カスケード 2 段 Biquad
struct Cascade2Biquad {
    BiquadFilter f1, f2;
    float process(float x, int ch) { return f2.process(f1.process(x, ch), ch); }
    void reset() { f1.reset(); f2.reset(); }
};

/**
 * T-501: 3バンド LR4 パラレルクロスオーバー EQ
 *
 * トポロジー（並列 / クロスオーバー周波数固定）:
 *   LOW  : LR4 LP @ 200 Hz
 *   MID  : LR4 HP @ 200 Hz → LR4 LP @ 3500 Hz
 *   HIGH : LR4 HP @ 200 Hz → LR4 HP @ 3500 Hz
 *
 * バンドゲインはスカラー乗算のみ（係数再計算なし）。
 * 各バンドの Kill (-60dB) で完全消音（0.0 乗算）。
 *
 * スレッド安全:
 *   setGainDb()  — JNI スレッドから呼ぶ（atomic 書き込み）
 *   beginBuffer() / processFrame() — オーディオスレッドのみ
 */
class EqProcessor {
public:
    static constexpr float KILL_DB = -60.f;
    static constexpr float MAX_DB  =   6.f;

    EqProcessor();

    // トラックロード後にサンプルレートが確定したら呼ぶ。ゲイン値は保持。
    void init(int32_t sampleRate);

    // JNI スレッドから呼ぶ（thread-safe）
    // band: 0=Low, 1=Mid, 2=High
    void setGainDb(int band, float gainDb);

    // ---- オーディオスレッド専用 ----
    void beginBuffer();                         // コールバック先頭で 1 回呼ぶ
    void processFrame(float& left, float& right);
    bool  isFlat() const;
    // キャプチャ時のオフライン処理用（JNI スレッドから safe）
    float getGainDb(int band) const {
        if (band < 0 || band >= 3) return 0.f;
        return mPending[band].load(std::memory_order_relaxed);
    }

private:
    // Butterworth 2nd order LP/HP を 2 段カスケードして LR4 ペアを生成
    void calcLR4(Cascade2Biquad& lp, Cascade2Biquad& hp, float fc);

    int32_t        mSr = 48000;
    float          mGainDb[3]  = {};
    float          mLinGain[3] = { 1.f, 1.f, 1.f };

    Cascade2Biquad mLP_low;    // LOW  : LP @ 200 Hz
    Cascade2Biquad mHP_low;    // MID+HIGH split 入力 : HP @ 200 Hz
    Cascade2Biquad mLP_high;   // MID  : LP @ 3500 Hz （mHP_low 出力に適用）
    Cascade2Biquad mHP_high;   // HIGH : HP @ 3500 Hz （mHP_low 出力に適用）

    std::atomic<float> mPending[3];
    std::atomic<bool>  mDirty[3];
};
