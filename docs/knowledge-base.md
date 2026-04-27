# 知見ベース — PocketDJ

> このドキュメントは開発中に得られた技術的知見・設計判断・トラブルシュートを記録する。
> 実装者・AIが次のタスクに着手する前に参照すること。

---

## 環境・ビルド

### Java 24 + Oboe Prefab 非互換（解決済み）

**現象:** `configureCMakeDebug` が無音で失敗。`prefab_stderr.txt` に JNA 警告のみ。

**原因:** Java 24 が `java.lang.System::load` を制限しており、Prefab CLI の JNA が動作しない。`--enable-native-access=ALL-UNNAMED` を `gradle.properties` に追加しても Prefab のサブプロセスには伝播しない。

**対処:** Oboe は `add_subdirectory` 方式でソースから組み込む（T-203 で実装予定）。
```cmake
# CMakeLists.txt での統合方法（T-203 実装時）
set(OBOE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/oboe)
add_subdirectory(${OBOE_DIR})
target_link_libraries(djapp oboe)
```

### TarsosDSP の取得方法（解決済み）

Maven Central / JitPack どちらも配布なし。

**対処:** [公式GitHub](https://github.com/JorenSix/TarsosDSP/releases) から AAR をダウンロードし `app/libs/` に配置。
```kotlin
// app/build.gradle.kts
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```
T-402（Sprint 4）で実装する。

### API 36 固有の設定（解決済み）

```properties
# gradle.properties に必須
android.suppressUnsupportedCompileSdk=36
org.gradle.jvmargs=... --enable-native-access=ALL-UNNAMED
```

```xml
<!-- themes.xml: 以下は API 36 に存在しない -->
<!-- NG: android:Theme.Material.NoTitleBar.Fullscreen -->
<!-- OK: android:Theme.DeviceDefault.NoActionBar -->
```

---

## アーキテクチャ・設計

### UseCase の blank/null 境界ケース

Repository に空クエリを渡さず、UseCase 層で分岐する。

```kotlin
// LibraryUseCase の実装パターン
fun searchTracks(query: String, ...): Flow<List<Track>> =
    if (query.isBlank()) getAllTracks(sortOrder)  // ← UseCase 層で吸収
    else trackRepository.searchTracks(query).map { ... }
```

**理由:** Repository（MediaStore / SQL）に空クエリを渡すと実装依存の挙動になる。UseCase が境界を守る。

### MVI StateFlow のベストプラクティス

```kotlin
// debounce + flatMapLatest の組み合わせ
_searchQuery.debounce(200)
    .flatMapLatest { query -> if (query.isBlank()) getAllTracks() else searchTracks(query) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)
```

- `debounce(200)`: 入力のたびに MediaStore を叩かない
- `WhileSubscribed(5_000)`: 画面回転など短い離脱でキャンセルしない

### JNI 境界の設計原則

- JNI 関数は「1関数 = 1操作」を原則とする
- Kotlin 側の型（`DeckId` enum）は Data 層で `Int`（`.ordinal`）に変換してから JNI に渡す
- Domain 層は JNI / C++ の存在を知らない（Repository Interface 越しに操作）

---

## Compose UI

### Material Icons Extended

`Sort` / `FilterList` / `LibraryMusic` など一般的でないアイコンは `material-icons-core` に含まれない。

```kotlin
// libs.versions.toml
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
// app/build.gradle.kts
implementation(libs.compose.material.icons.extended)
```

### ランチャーアイコンなしでのビルド

初期実装では `mipmap/ic_launcher` が存在しないため、AndroidManifest から `android:icon` / `android:roundIcon` を省く。Sprint 8 で追加する。

---

## テスト

### MockK + Turbine の組み合わせ

```kotlin
// Flow のテストパターン
every { repository.getAllTracks() } returns flowOf(listOf(track1, track2))
useCase.getAllTracks().test {
    val result = awaitItem()
    assertEquals(expected, result)
    awaitComplete()
}
```

- `flowOf(...)` でシングルエミットの Flow をモック
- `awaitComplete()` を忘れると Turbine がタイムアウト

---

## 未解決・要調査

| 項目 | Sprint | 内容 |
|---|---|---|
| TarsosDSP BPM 精度 | Sprint 4 | ±0.5 BPM 達成できなければ aubio に切り替え（ADR-004）。定量検証未実施 |
| Spotify SDK ライセンス | 将来 | 商用配布前に Spotify Developer Policy を確認 |

## 解決済み

| 項目 | Sprint | 解決内容 |
|---|---|---|
| Oboe レイテンシ実測 | Sprint 8 | T-802 完了。C++（`calculateLatencyMillis()`）→ JNI → Kotlin → UI の全レイヤーで計測実装済み。CrossfaderPanel に LATENCY / XRUN 表示。目標 20ms 以下（`constitution.md §4`） |
