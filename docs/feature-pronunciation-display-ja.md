# 機能: 発音表示（D列・E列）

**ステータス:** 計画中
**優先度:** 中
**日付:** 2025-01-24
**更新:** 2025-01-24（E列追加）

## 概要

- **D列**の発音テキストを、表面（A列）のコンテンツの上に薄い色で表示
- **E列**の発音テキストを、裏面（B列）のコンテンツの上に薄い色で表示
- 発音テキストは視覚的に表示されますが、TTS音声読み上げには含まれません

## ユースケース

### 表面（D列）
- **日本語**: ふりがなを漢字の上に表示
- **中国語**: ピンイン（nǐ hǎo）を漢字の上に表示
- **英語**: 発音記号（prənʌnsiéiʃən）を単語の上に表示

### 裏面（E列）
- **日本語**: 英単語の発音記号を表示
- **中国語**: 日本語訳のふりがなを表示
- **英語**: 日本語訳のふりがなを表示

## データ構造

### Flashcardモデルの更新

`Flashcard.java`に新しいフィールドを追加:

```java
public class Flashcard {
    private String frontSide;              // A列（表面）
    private String backSide;               // B列（裏面）
    private int importance;                // C列（重要度）
    private String frontPronunciation;     // D列（表面の発音） - 新規
    private String backPronunciation;      // E列（裏面の発音） - 新規
    private int currentNum;
    private int totalCards;

    // ゲッター・セッターを追加
    public String getFrontPronunciation() { return frontPronunciation; }
    public void setFrontPronunciation(String frontPronunciation) {
        this.frontPronunciation = frontPronunciation;
    }

    public String getBackPronunciation() { return backPronunciation; }
    public void setBackPronunciation(String backPronunciation) {
        this.backPronunciation = backPronunciation;
    }
}
```

### Google Sheetsの構造

| A列（表面） | B列（裏面） | C列（重要度） | D列（表面発音） | E列（裏面発音） |
|-----------|----------|------------|--------------|--------------|
| 漢字 | kanji | 1 | かんじ | kǽndʒi |
| 你好 | こんにちは | 2 | nǐ hǎo | - |
| pronunciation | 発音 | 0 | prənʌnsiéiʃən | はつおん |

### APIレスポンス

Google Apps Scriptは以下を返す:

```json
{
  "frontSide": "<b>漢字</b>",
  "backSide": "kanji",
  "importance": 1,
  "frontPronunciation": "かんじ",
  "backPronunciation": "kǽndʒi",
  "currentNum": 1,
  "totalCards": 100
}
```

## UI設計

### レイアウト変更

#### 縦向き: `activity_flashcard.xml`

**現在:**
```xml
<!-- 表面 -->
<TextView
    android:id="@+id/frontSideText"
    android:textSize="18sp"
    android:textColor="#000000" />

<!-- 裏面 -->
<TextView
    android:id="@+id/backSideText"
    android:textSize="18sp"
    android:textColor="#000000"
    android:visibility="gone" />
```

**更新後:**
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center">

    <!-- 表面の発音（D列） -->
    <TextView
        android:id="@+id/frontPronunciationText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="#999999"
        android:gravity="center"
        android:visibility="gone"
        android:layout_marginBottom="4dp" />

    <!-- 表面テキスト（A列） -->
    <TextView
        android:id="@+id/frontSideText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:textColor="#000000"
        android:gravity="center"
        android:visibility="visible" />

    <!-- 裏面の発音（E列） -->
    <TextView
        android:id="@+id/backPronunciationText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="#999999"
        android:gravity="center"
        android:visibility="gone"
        android:layout_marginBottom="4dp" />

    <!-- 裏面テキスト（B列） -->
    <TextView
        android:id="@+id/backSideText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:textColor="#000000"
        android:gravity="center"
        android:visibility="gone" />
</LinearLayout>
```

#### 横向き: `layout-land/activity_flashcard.xml`

縦向きと同じ変更を適用。

### 表示スタイル

- **発音テキスト（D列・E列）**:
  - フォントサイズ: 12sp（メインテキストより小さい）
  - 色: #999999（薄いグレー）
  - 位置: メインテキストの上
  - 表示: D列またはE列が空の場合は非表示

- **メインテキスト（A列・B列）**:
  - フォントサイズ: 18sp（変更なし）
  - 色: #000000（黒）

## 実装方法

### 1. モデルの変更

ファイル: `app/src/main/java/com/flashcardapp/models/Flashcard.java`

```java
// フィールドを追加
private String frontPronunciation;
private String backPronunciation;

// ゲッター・セッターを追加
public String getFrontPronunciation() {
    return frontPronunciation;
}

public void setFrontPronunciation(String frontPronunciation) {
    this.frontPronunciation = frontPronunciation;
}

public String getBackPronunciation() {
    return backPronunciation;
}

public void setBackPronunciation(String backPronunciation) {
    this.backPronunciation = backPronunciation;
}
```

### 2. レイアウトの変更

ファイル:
- `app/src/main/res/layout/activity_flashcard.xml`
- `app/src/main/res/layout-land/activity_flashcard.xml`

UI設計セクションに示した通り、以下を追加:
- `frontSideText`の上に`frontPronunciationText` TextView
- `backSideText`の上に`backPronunciationText` TextView

### 3. Activityの変更

ファイル: `app/src/main/java/com/flashcardapp/ui/FlashcardActivity.java`

#### Viewの参照を追加

```java
private TextView frontPronunciationText;
private TextView backPronunciationText;

private void initViews() {
    // ... 既存のコード ...
    frontPronunciationText = findViewById(R.id.frontPronunciationText);
    backPronunciationText = findViewById(R.id.backPronunciationText);
}
```

#### 表示ロジックの更新

```java
private void displayFlashcard(Flashcard flashcard) {
    cardProgressText.setText("Card " + flashcard.getCurrentNum() + " of " + flashcard.getTotalCards());

    // 表面と裏面のHTMLをパース
    frontSideText.setText(parseHtml(flashcard.getFrontSide()));
    backSideText.setText(parseHtml(flashcard.getBackSide()));

    // 表面の発音が利用可能な場合は表示（新規）
    String frontPronunciation = flashcard.getFrontPronunciation();
    if (frontPronunciation != null && !frontPronunciation.isEmpty()) {
        frontPronunciationText.setText(frontPronunciation);
        frontPronunciationText.setVisibility(View.VISIBLE);
    } else {
        frontPronunciationText.setVisibility(View.GONE);
    }

    // 裏面の発音が利用可能な場合は表示（新規）
    String backPronunciation = flashcard.getBackPronunciation();
    if (backPronunciation != null && !backPronunciation.isEmpty()) {
        backPronunciationText.setText(backPronunciation);
        backPronunciationText.setVisibility(View.VISIBLE);
    } else {
        backPronunciationText.setVisibility(View.GONE);
    }

    // TTSのためにテキストを事前にパースしてキャッシュ（発音は含まれない）
    cachedFrontText = stripHtmlTags(flashcard.getFrontSide());
    cachedBackText = stripHtmlTags(flashcard.getBackSide());

    // ... 既存のコードの続き ...
}
```

### 4. Google Apps Scriptの変更

ファイル: `Code.gs`（Google Apps Scriptプロジェクト内）

`getFlashcard()`関数を更新してD列とE列を含める:

```javascript
function getFlashcard(e) {
  var sheetName = e.parameter.sheetName || 'Sheet1';
  var cardNumber = parseInt(e.parameter.cardNumber) || 1;

  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();

  // ヘッダー行をスキップ
  var dataRow = values[cardNumber];

  return ContentService
    .createTextOutput(JSON.stringify({
      frontSide: dataRow[0] || '',             // A列
      backSide: dataRow[1] || '',              // B列
      importance: dataRow[2] || 0,             // C列
      frontPronunciation: dataRow[3] || '',    // D列（新規）
      backPronunciation: dataRow[4] || '',     // E列（新規）
      currentNum: cardNumber,
      totalCards: values.length - 1
    }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

## TTS処理

**重要**: 発音テキスト（D列・E列）はTTS音声に含めるべきではありません。

現在の実装で既に正しく処理されています:

```java
// A列とB列のコンテンツのみをTTS用にキャッシュ
cachedFrontText = stripHtmlTags(flashcard.getFrontSide());
cachedBackText = stripHtmlTags(flashcard.getBackSide());

// TTSはキャッシュされたテキストを使用（D列・E列は含まれない）
textToSpeech.speak(cachedFrontText, ...);
textToSpeech.speak(cachedBackText, ...);
```

TTSロジックの変更は不要です。

## エッジケース

### D列・E列が空の場合
- D列またはE列が空またはnullの場合、該当の発音TextViewを非表示（`visibility=GONE`）
- レイアウトやTTSへの影響なし

### D列・E列にHTMLタグがある場合
- 決定が必要: 発音フィールドにHTMLタグを許可するか？
- 推奨: プレーンテキストのまま（HTMLパース不要）

### 長い発音テキスト
- 非常に長い発音文字列の場合、最大行数や省略記号を検討
- フォントサイズやレイアウトの調整が必要な場合あり

### 片方だけに発音がある場合
- 表面のみ発音（D列のみ）: 正常に動作
- 裏面のみ発音（E列のみ）: 正常に動作
- 両方に発音: 正常に動作

## 設定オプション（将来）

追加可能な設定:

1. **発音の表示/非表示**: 設定で表示を切り替え
2. **発音フォントサイズ**: 調整可能なサイズ（10sp - 14sp）
3. **発音の色**: カスタマイズ可能な色
4. **発音の位置**: メインテキストの上または下

## テストチェックリスト

### 表面の発音（D列）
- [ ] D列が空の場合（visibility=GONE）
- [ ] D列にデータがある場合（正常表示）
- [ ] 日本語のふりがな表示
- [ ] 中国語のピンイン表示
- [ ] 英語の発音記号表示

### 裏面の発音（E列）
- [ ] E列が空の場合（visibility=GONE）
- [ ] E列にデータがある場合（正常表示）
- [ ] カード反転時の発音表示

### TTS
- [ ] TTSが表面の発音（D列）を読まない
- [ ] TTSが裏面の発音（E列）を読まない
- [ ] A列のみが読まれる（表面）
- [ ] B列のみが読まれる（裏面）

### レイアウト
- [ ] 縦向き表示
- [ ] 横向き表示
- [ ] 長い発音テキスト
- [ ] カード反転アニメーション
- [ ] ScrollViewでの発音表示の挙動

### 組み合わせ
- [ ] D列のみ（表面発音のみ）
- [ ] E列のみ（裏面発音のみ）
- [ ] D列とE列両方（両面に発音）
- [ ] D列もE列も空（発音なし）

## 実装見積もり

- モデル変更: 15分
- レイアウト変更: 30分（表裏両面）
- 表示ロジック: 20分
- Google Apps Script: 15分
- テスト: 40分

**合計**: 約2時間

## 備考

- この機能はオプション（既存データとの後方互換性あり）
- 既存機能への破壊的変更なし
- D列のみ、E列のみ、両方、どちらも空、すべてのパターンに対応
- 発音表示は純粋に視覚的（TTSや音声機能への影響なし）

## 参考

- 元の議論: [日付: 2025-01-24]
- 更新: E列追加（2025-01-24）
- 関連機能: TTS、自動再生、言語選択

## 実装例（視覚イメージ）

### 表面（日本語 → 英語）
```
【表面】
かんじ           ← D列、薄いグレー、12sp
漢字の勉強        ← A列、黒、18sp

【裏面】
kǽndʒi          ← E列、薄いグレー、12sp
kanji study     ← B列、黒、18sp
```

### 表面（中国語 → 日本語）
```
【表面】
nǐ hǎo          ← D列、薄いグレー、12sp
你好            ← A列、黒、18sp

【裏面】
こんにちは       ← B列、黒、18sp
（E列なし）
```

### 表面（英語 → 日本語）
```
【表面】
prənʌnsiéiʃən   ← D列、薄いグレー、12sp
pronunciation   ← A列、黒、18sp

【裏面】
はつおん         ← E列、薄いグレー、12sp
発音            ← B列、黒、18sp
```
