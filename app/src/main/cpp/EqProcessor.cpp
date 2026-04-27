#include "EqProcessor.h"
#include <algorithm>

static constexpr float kPi = 3.14159265358979f;

EqProcessor::EqProcessor() {
    for (int i = 0; i < 3; ++i) {
        mPending[i].store(0.f, std::memory_order_relaxed);
        mDirty[i].store(false, std::memory_order_relaxed);
        mGainDb[i]  = 0.f;
        mLinGain[i] = 1.f;
    }
    calcLR4(mLP_low,  mHP_low,   300.f);
    calcLR4(mLP_high, mHP_high, 3500.f);
}

void EqProcessor::init(int32_t sampleRate) {
    mSr = sampleRate;
    mLP_low.reset();
    mHP_low.reset();
    mLP_high.reset();
    mHP_high.reset();
    calcLR4(mLP_low,  mHP_low,   300.f);
    calcLR4(mLP_high, mHP_high, 3500.f);
}

void EqProcessor::setGainDb(int band, float gainDb) {
    if (band < 0 || band >= 3) return;
    float v = std::clamp(gainDb, KILL_DB, MAX_DB);
    mPending[band].store(v, std::memory_order_relaxed);
    mDirty[band].store(true, std::memory_order_release);
}

void EqProcessor::beginBuffer() {
    for (int b = 0; b < 3; ++b) {
        if (!mDirty[b].load(std::memory_order_acquire)) continue;
        mDirty[b].store(false, std::memory_order_relaxed);
        float db = mPending[b].load(std::memory_order_relaxed);
        mGainDb[b]  = db;
        // Kill (-60dB) → 完全消音。それ以外は dB → 線形変換。
        mLinGain[b] = (db <= KILL_DB) ? 0.f : std::pow(10.f, db / 20.f);
    }
}

void EqProcessor::processFrame(float& left, float& right) {
    for (int ch = 0; ch < 2; ++ch) {
        float& x   = (ch == 0) ? left : right;
        float  lo  = mLP_low.process(x, ch);         // LOW band
        float  up  = mHP_low.process(x, ch);          // above 200 Hz
        float  mid = mLP_high.process(up, ch);        // MID band
        float  hi  = mHP_high.process(up, ch);        // HIGH band
        x = mLinGain[0]*lo + mLinGain[1]*mid + mLinGain[2]*hi;
    }
}

bool EqProcessor::isFlat() const {
    return mLinGain[0] == 1.f && mLinGain[1] == 1.f && mLinGain[2] == 1.f;
}

// ---------------------------------------------------------------------------
// Butterworth 2nd order LP/HP を 2 段カスケード → LR4 ペアを構成
// Q = 1/sqrt(2) ≈ 0.7071 （Butterworth 最大平坦）
// alpha = sin(w0) / (2*Q) = sin(w0) * 0.7071
// ---------------------------------------------------------------------------
void EqProcessor::calcLR4(Cascade2Biquad& lp, Cascade2Biquad& hp, float fc) {
    const float w0    = 2.f * kPi * fc / static_cast<float>(mSr);
    const float cosw  = std::cos(w0);
    const float sinw  = std::sin(w0);
    const float alpha = sinw * 0.7071067812f;   // sin(w0) / (2 * Q), Q = 1/sqrt(2)
    const float a0    = 1.f + alpha;

    // LP: b0=(1-cos)/2, b1=1-cos, b2=(1-cos)/2
    {
        const float b0 = (1.f - cosw) * 0.5f;
        const float b1 =  1.f - cosw;
        lp.f1.b0 = b0/a0;  lp.f1.b1 = b1/a0;  lp.f1.b2 = b0/a0;
        lp.f1.a1 = -2.f*cosw/a0;  lp.f1.a2 = (1.f - alpha)/a0;
        lp.f2 = lp.f1;
    }

    // HP: b0=(1+cos)/2, b1=-(1+cos), b2=(1+cos)/2
    {
        const float b0 = (1.f + cosw) * 0.5f;
        const float b1 = -(1.f + cosw);
        hp.f1.b0 = b0/a0;  hp.f1.b1 = b1/a0;  hp.f1.b2 = b0/a0;
        hp.f1.a1 = -2.f*cosw/a0;  hp.f1.a2 = (1.f - alpha)/a0;
        hp.f2 = hp.f1;
    }
}
