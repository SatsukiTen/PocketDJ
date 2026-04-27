package com.djapp.data.repository

import android.content.Context
import android.net.Uri
import com.djapp.data.source.AudioEngineDataSource
import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-005 / T-207: AudioEngineRepositoryの実装。
 * JNIブリッジ（AudioEngineDataSource）を呼び出し、
 * DeckId などのドメイン型をIntへ変換してC++側に渡す。
 *
 * Android 10+ では content URI をファイルディスクリプタ経由で C++ に渡す。
 * エンジンは最初の loadTrack() 時にレイジー初期化する。
 */
@Singleton
class AudioEngineRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: AudioEngineDataSource,
) : AudioEngineRepository {

    @Volatile private var engineInitialized = false

    private fun ensureInitialized() {
        if (!engineInitialized) {
            synchronized(this) {
                if (!engineInitialized) {
                    dataSource.nativeInitEngine()
                    engineInitialized = true
                }
            }
        }
    }

    override fun initEngine(): Boolean {
        ensureInitialized()
        return engineInitialized
    }

    override fun destroyEngine() {
        dataSource.nativeDestroyEngine()
        engineInitialized = false
    }

    /**
     * content URI をファイルディスクリプタに変換して C++ に渡す。
     * detachFd() で fd の所有権を JNI 側に移譲し、C++ が close() する。
     */
    override fun loadTrack(deckId: DeckId, filePath: String): Boolean {
        ensureInitialized()
        return try {
            val uri = Uri.parse(filePath)
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return false
            val fd = pfd.detachFd()  // 所有権を JNI 側に移譲（C++ が close する）
            dataSource.nativeLoadTrackFd(deckId.ordinal, fd)
        } catch (e: Exception) {
            false
        }
    }

    override fun play(deckId: DeckId)  = dataSource.nativePlay(deckId.ordinal)
    override fun pause(deckId: DeckId) = dataSource.nativePause(deckId.ordinal)
    override fun stop(deckId: DeckId)  = dataSource.nativeStop(deckId.ordinal)

    override fun getPlayheadSec(deckId: DeckId) = dataSource.nativeGetPlayheadSec(deckId.ordinal)
    override fun getDurationSec(deckId: DeckId) = dataSource.nativeGetDurationSec(deckId.ordinal)

    override fun setCrossfaderPosition(position: Float) =
        dataSource.nativeSetCrossfaderPosition(position)

    override fun setCrossfaderCurve(curve: CrossfaderCurve) =
        dataSource.nativeSetCrossfaderCurve(curve.ordinal)

    override fun setEqLow(deckId: DeckId, gainDb: Float)  = dataSource.nativeSetEq(deckId.ordinal, 0, gainDb)
    override fun setEqMid(deckId: DeckId, gainDb: Float)  = dataSource.nativeSetEq(deckId.ordinal, 1, gainDb)
    override fun setEqHigh(deckId: DeckId, gainDb: Float) = dataSource.nativeSetEq(deckId.ordinal, 2, gainDb)

    override fun setPitchRatio(deckId: DeckId, ratio: Float) =
        dataSource.nativeSetPitch(deckId.ordinal, ratio)

    override fun getBpm(deckId: DeckId): Float =
        dataSource.nativeGetBpm(deckId.ordinal)

    override fun getSampleRate(deckId: DeckId): Int =
        dataSource.nativeGetSampleRate(deckId.ordinal)

    override fun setLoop(deckId: DeckId, loopInSample: Long, loopOutSample: Long) =
        dataSource.nativeSetLoop(deckId.ordinal, loopInSample, loopOutSample)

    override fun clearLoop(deckId: DeckId) =
        dataSource.nativeClearLoop(deckId.ordinal)

    override fun seekTo(deckId: DeckId, positionSec: Float) =
        dataSource.nativeSeekTo(deckId.ordinal, positionSec)

    override fun captureSample(slotId: Int, deckId: DeckId, inFrame: Long, outFrame: Long) =
        dataSource.nativeCaptureSample(slotId, deckId.ordinal, inFrame, outFrame)

    override fun playSample(slotId: Int, loop: Boolean) {
        ensureInitialized()
        dataSource.nativePlaySample(slotId, loop)
    }

    override fun stopSample(slotId: Int) =
        dataSource.nativeStopSample(slotId)

    override fun isSamplePlaying(slotId: Int) =
        dataSource.nativeIsSamplePlaying(slotId)

    override fun clearSample(slotId: Int) =
        dataSource.nativeClearSample(slotId)

    override fun startScratch(deckId: DeckId) =
        dataSource.nativeStartScratch(deckId.ordinal)

    override fun setScratchSpeed(deckId: DeckId, speed: Float) =
        dataSource.nativeSetScratchSpeed(deckId.ordinal, speed)

    override fun stopScratch(deckId: DeckId) =
        dataSource.nativeStopScratch(deckId.ordinal)

    override fun getLatencyMs() = dataSource.nativeGetLatencyMs()
    override fun getXRunCount() = dataSource.nativeGetXRunCount()
}
