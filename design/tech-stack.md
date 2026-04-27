# 技術スタック — DJ App

> ステータス: **確定（2026-04-25）**

---

## 言語・ランタイム

| 用途 | 技術 | バージョン | 選定理由 |
|---|---|---|---|
| アプリ本体 | Kotlin | 2.x | Android公式推奨言語 |
| 音声エンジン | C++ | C++17 | 低レイテンシ音声処理（NDK） |

---

## Androidビルド

| 項目 | 設定 |
|---|---|
| compileSdk | 36（Android 16） |
| targetSdk | 36（Android 16） |
| minSdk | 26（Android 8.0） |
| ビルドツール | Gradle + CMake（C++部分） |

---

## 主要ライブラリ（案）

### 音声処理
| ライブラリ | 用途 | ライセンス | ADR |
|---|---|---|---|
| Oboe | 低レイテンシ音声I/O | Apache 2.0 | ADR-001 |
| TarsosDSP | BPM検出（Phase 1） | Apache 2.0 | ADR-004 |

### UI
| ライブラリ | 用途 | ライセンス | ADR |
|---|---|---|---|
| Jetpack Compose | UIフレームワーク | Apache 2.0 | ADR-002 |
| Compose Material 3 | デザインシステム | Apache 2.0 | ADR-002 |

### アーキテクチャ
| ライブラリ | 用途 | ライセンス | ADR |
|---|---|---|---|
| Hilt | 依存性注入（DI） | Apache 2.0 | ADR-003 |
| ViewModel + StateFlow | 状態管理 | Apache 2.0 | ADR-003 |
| DataStore（Preferences） | 設定値の永続化 | Apache 2.0 | — |
| Room | BPMキャッシュ用途で依存追加済みだが実装未使用 | Apache 2.0 | — |

### メディア
| ライブラリ | 用途 | ライセンス | ADR |
|---|---|---|---|
| Android MediaStore API | 端末内音楽ファイル取得 | Android標準 | — |

### テスト
| ライブラリ | 用途 | ライセンス |
|---|---|---|
| JUnit 4 | 単体テスト | EPL 1.0 |
| Mockk | モック生成（Kotlin向け） | Apache 2.0 |
| Turbine | Flow テスト | Apache 2.0 |
| Espresso | UIテスト | Apache 2.0 |

---

## 備考

- BPM検出: Phase 1はTarsosDSP、Phase 2で精度未達の場合はaubioへ切り替え（ADR-004）
- クロスフェーダーカーブ（Linear / Equal Power）はC++ AudioProcessor内に実装する
