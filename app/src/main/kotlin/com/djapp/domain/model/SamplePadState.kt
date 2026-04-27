package com.djapp.domain.model

enum class SamplePlayMode { SINGLE, LOOP }

data class SamplePadState(
    val id: Int,
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val playMode: SamplePlayMode = SamplePlayMode.SINGLE,
    val durationSec: Float = 0f,
)
