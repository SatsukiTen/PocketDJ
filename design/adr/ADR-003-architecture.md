# ADR-003: アーキテクチャパターンの選定

| 項目 | 内容 |
|---|---|
| **ID** | ADR-003 |
| **状態** | 確定 |
| **決定日** | 2026-04-25 |
| **関連制約** | Constitution §2 テスト可能性、仕様駆動開発との整合 |

---

## 背景

仕様駆動開発ではユースケース単位でテストを書くため、UIとビジネスロジックを分離した
アーキテクチャが不可欠。また音声エンジン（C++）とUI（Kotlin）の明確な境界も必要。

---

## 選択肢の比較

### Option A: **Clean Architecture + MVI（推奨）**

```
┌─────────────────────────────────────┐
│  Presentation Layer                  │
│  Compose UI ← ViewModel (MVI)        │
│  State / Intent / SideEffect         │
├─────────────────────────────────────┤
│  Domain Layer                        │
│  UseCase  (UC-001〜008に1対1対応)    │
│  DomainModel / Repository Interface  │
├─────────────────────────────────────┤
│  Data Layer                          │
│  RepositoryImpl / MediaStore         │
│  Settings (DataStore)                │
├─────────────────────────────────────┤
│  Audio Engine Layer（C++ / NDK）     │
│  OboeEngine / AudioProcessor         │
│  JNI Bridge                          │
└─────────────────────────────────────┘
```

| 項目 | 評価 |
|---|---|
| テスト容易性 | ◎ 各層が独立。UseCaseはPure Kotlinでテスト可能 |
| 仕様との対応 | ◎ UseCaseがUC-001〜008に1対1対応 |
| 変更容易性 | ◎ 音声エンジン差し替えもRepositoryパターンで吸収 |
| 実装コスト | △ 層の数が多く初期セットアップがやや多い |
| 個人開発向き | ○ 初期コストはかかるが中長期では生産性が上がる |

### Option B: MVVM（シンプル構成）

```
Compose UI ← ViewModel ← Repository ← MediaStore / AudioEngine
```

| 項目 | 評価 |
|---|---|
| テスト容易性 | ○ ViewModelのテストは可能だが、ビジネスロジックが肥大化しやすい |
| 仕様との対応 | △ ViewModelにロジックが混在するとUCとの対応が曖昧になる |
| 変更容易性 | △ 規模拡大時にViewModelが神クラス化するリスク |
| 実装コスト | ◎ シンプルで始めやすい |
| 個人開発向き | ◎ 小規模では十分 |

---

## 推奨: **Option A — Clean Architecture + MVI**

### 理由
- 仕様書のUC-001〜008が**UseCaseクラスに1対1で対応**し、仕様駆動開発と自然に整合する
- 音声エンジン（C++層）をDomain LayerのRepositoryインターフェース越しに抽象化することで、
  PoCや将来の音声API変更の影響を最小化できる
- MVIパターンにより、再生状態・フェーダー位置・BPM値などの複雑な画面状態を
  単一のState classで管理でき、バグが混入しにくい

### MVI の State 例（DeckA）
```kotlin
data class DeckState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val playheadSec: Float = 0f,
    val bpm: Float? = null,
    val isSynced: Boolean = false,
    val loopRange: ClosedFloatingPointRange<Float>? = null,
    val eq: EqState = EqState()
)
```

---

## 決定

**Option A — Clean Architecture + MVI を採用する。**
UC-001〜008がUseCaseクラスに1対1対応し、仕様駆動開発と整合する。
Domain LayerをPure Kotlinに保つことでテスト容易性を確保する。
