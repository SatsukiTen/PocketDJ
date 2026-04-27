# アーキテクチャ設計書 — DJ App

> ステータス: **確定（Sprint 8完了時点）**
> 根拠: ADR-003（Clean Architecture + MVI）採用確定。Sprint 0〜8を通じて実装済み

---

## 全体構成図

```
┌──────────────────────────────────────────────────────────┐
│  Presentation Layer（Kotlin / Jetpack Compose）           │
│                                                          │
│  MainScreen                                              │
│  ├── DeckASection ──→ DeckViewModel (MVI)                │
│  ├── DeckBSection ──→ DeckViewModel (MVI)                │
│  ├── CrossfaderSection ──→ MixerViewModel                │
│  └── LibraryBrowser ──→ LibraryViewModel                 │
└──────────────────┬───────────────────────────────────────┘
                   │ UseCase呼び出し
┌──────────────────▼───────────────────────────────────────┐
│  Domain Layer（Pure Kotlin）                              │
│                                                          │
│  UseCase（仕様書UC-001〜008に対応）                        │
│  ├── LoadTrackUseCase          (UC-001)                  │
│  ├── PlaybackControlUseCase   (UC-002)                   │
│  ├── CrossfaderUseCase        (UC-003)                   │
│  ├── BpmSyncUseCase           (UC-004)                   │
│  ├── EqualizerUseCase         (UC-005)                   │
│  ├── LoopUseCase              (UC-006)                   │
│  ├── ScratchUseCase           (UC-007)                   │
│  └── LibraryUseCase           (UC-008)                   │
│                                                          │
│  DomainModel: Track / DeckState / MixerState / EqState   │
│  Repository Interface: AudioEngineRepository             │
│                        TrackRepository                   │
│                        SettingsRepository                │
└──────────────────┬───────────────────────────────────────┘
                   │ Repository実装
┌──────────────────▼───────────────────────────────────────┐
│  Data Layer（Kotlin）                                     │
│                                                          │
│  TrackRepositoryImpl ──→ Android MediaStore              │
│  SettingsRepositoryImpl ──→ DataStore（設定値永続化）      │
│  AudioEngineRepositoryImpl ──→ JNI Bridge                │
└──────────────────┬───────────────────────────────────────┘
                   │ JNI
┌──────────────────▼───────────────────────────────────────┐
│  Audio Engine Layer（C++ / NDK）                          │
│                                                          │
│  OboeEngine（音声I/O）                                    │
│  AudioProcessor                                          │
│  ├── DeckPlayer × 2（再生・停止・位置制御）                │
│  ├── Crossfader（Linear / Equal Powerカーブ）             │
│  ├── EqProcessor × 2（3バンドBiquadフィルター）            │
│  ├── LoopProcessor × 2（ループ区間管理）                   │
│  └── ScratchProcessor × 2（スクラッチ速度変換）           │
│                                                          │
│  BpmDetector（TarsosDSP経由 or aubio）                   │
└──────────────────────────────────────────────────────────┘
```

---

## データフロー（例: クロスフェーダー操作）

```
1. ユーザーがComposeのスライダーをドラッグ
2. CrossfaderSection → CrossfaderUseCase.setCrossfaderPosition(value)
3. AudioEngineRepository.setCrossfader(value)
4. JNI → C++ Crossfader.setPosition(value)
5. 次の音声バッファ処理時に音量バランスが反映（<20ms）
6. StateFlow経由でUIのスライダー位置が更新
```

---

## JNI境界の設計方針

```kotlin
// Kotlin側（AudioEngineRepositoryImpl）
external fun nativeSetCrossfaderPosition(value: Float)
external fun nativeSetEq(deckId: Int, band: Int, gainDb: Float)
external fun nativeStartScratch(deckId: Int)
external fun nativeSetScratchSpeed(deckId: Int, speed: Float)
external fun nativeStopScratch(deckId: Int)
```

- JNI関数は粒度を細かくし、1関数1操作を原則とする
- C++側のコールバック（バッファ完了・位置更新）はOboeのコールバックスレッドから
  Kotlinへ通知する（`JavaVM::AttachCurrentThread` 経由）

---

## ViewModel の状態管理（MVI）

```kotlin
// 画面全体の状態を単一のStateで管理
data class MainUiState(
    val deckA: DeckState = DeckState(),
    val deckB: DeckState = DeckState(),
    val crossfaderPosition: Float = 0.5f,  // 0.0(A) 〜 1.0(B)
    val crossfaderCurve: CrossfaderCurve = CrossfaderCurve.EQUAL_POWER,
    val isLibraryOpen: Boolean = false
)

sealed class MainIntent {
    data class LoadTrack(val deckId: DeckId, val track: Track) : MainIntent()
    data class SetCrossfader(val position: Float) : MainIntent()
    data class ToggleSync(val deckId: DeckId) : MainIntent()
    // ...
}
```

---

## モジュール構成

```
app/
├── src/main/
│   ├── kotlin/
│   │   ├── presentation/   # Compose UI + ViewModel
│   │   ├── domain/         # UseCase + DomainModel + Repository Interface
│   │   └── data/           # RepositoryImpl + DataSource
│   └── cpp/
│       ├── audio_engine.cpp
│       ├── deck_player.cpp
│       ├── crossfader.cpp
│       ├── eq_processor.cpp
│       ├── loop_processor.cpp
│       └── scratch_processor.cpp
└── CMakeLists.txt
```
