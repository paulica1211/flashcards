# Feature: Pronunciation Display (D & E Columns)

**Status:** Planned
**Priority:** Medium
**Date:** 2025-01-24
**Updated:** 2025-01-24 (Added E column)

## Overview

- Display pronunciation text from **D column** in a lighter color above the front side (A column) content
- Display pronunciation text from **E column** in a lighter color above the back side (B column) content
- The pronunciation text should be visible but not included in TTS audio reading

## Use Cases

- **Japanese**: Display furigana (ふりがな) above kanji
- **Chinese**: Display pinyin (nǐ hǎo) above characters
- **English**: Display phonetic symbols (prənʌnsiéiʃən) above words

## Data Structure

### Flashcard Model Update

Add new field to `Flashcard.java`:

```java
public class Flashcard {
    private String frontSide;      // A column (front)
    private String backSide;       // B column (back)
    private int importance;        // C column (importance)
    private String pronunciation;  // D column (pronunciation) - NEW
    private int currentNum;
    private int totalCards;

    // Add getter/setter
    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }
}
```

### Google Sheets Structure

| A (Front) | B (Back) | C (Importance) | D (Pronunciation) |
|-----------|----------|----------------|-------------------|
| 漢字 | kanji | 1 | かんじ |
| 你好 | hello | 2 | nǐ hǎo |
| pronunciation | 発音 | 0 | prənʌnsiéiʃən |

### API Response

Google Apps Script should return:

```json
{
  "frontSide": "<b>漢字</b>",
  "backSide": "意味",
  "importance": 1,
  "pronunciation": "かんじ",
  "currentNum": 1,
  "totalCards": 100
}
```

## UI Design

### Layout Changes

#### Portrait: `activity_flashcard.xml`

**Current:**
```xml
<TextView
    android:id="@+id/frontSideText"
    android:textSize="18sp"
    android:textColor="#000000" />
```

**Updated:**
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center">

    <!-- Pronunciation (D column) -->
    <TextView
        android:id="@+id/pronunciationText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="#999999"
        android:gravity="center"
        android:visibility="gone"
        android:layout_marginBottom="4dp" />

    <!-- Front side text (A column) -->
    <TextView
        android:id="@+id/frontSideText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:textColor="#000000"
        android:gravity="center" />
</LinearLayout>
```

#### Landscape: `layout-land/activity_flashcard.xml`

Apply same changes as portrait layout.

### Visual Style

- **Pronunciation text**:
  - Font size: 12sp (smaller than main text)
  - Color: #999999 (light gray)
  - Position: Above main text
  - Visibility: Hidden when D column is empty

- **Front side text**:
  - Font size: 18sp (unchanged)
  - Color: #000000 (black)

## Implementation

### 1. Model Changes

Fi `app/src/main/java/com/flashcardapp/models/Flashcard.java`

```java
// Add field
private String pronunciation;

// Add getter/setter
public String getPronunciation() {
    return pronunciation;
}

public void setPronunciation(String pronunciation) {
    this.pronunciation = pronunciation;
}
```

### 2. Layout Changes

Files:
- `app/src/main/res/layout/activity_flashcard.xml`
- `app/src/main/res/layout-land/activity_flashcard.xml`

Add `pronunciationText` TextView above `frontSideText` as shown in UI Design section.

### 3. Activity Changes

Fi `app/src/main/java/com/flashcardapp/ui/FlashcardActivity.java`

#### Add View Reference

```java
private TextView pronunciationText;

private void initViews() {
    // ... existing code ...
    pronunciationText = findViewById(R.id.pronunciationText);
}
```

#### Update Display Logic

```java
private void displayFlashcard(Flashcard flashcard) {
    cardProgressText.setText("Card " + flashcard.getCurrentNum() + " of " + flashcard.getTotalCards());

    // Parse HTML for front and back
    frontSideText.setText(parseHtml(flashcard.getFrontSide()));
    backSideText.setText(parseHtml(flashcard.getBackSide()));

    // Display pronunciation if available (NEW)
    String pronunciation = flashcard.getPronunciation();
    if (pronunciation != null && !pronunciation.isEmpty()) {
        pronunciationText.setText(pronunciation);
        pronunciationText.setVisibility(View.VISIBLE);
    } else {
        pronunciationText.setVisibility(View.GONE);
    }

    // Pre-parse and cache text for TTS (pronunciation is NOT included)
    cachedFrontText = stripHtmlTags(flashcard.getFrontSide());
    cachedBackText = stripHtmlTags(flashcard.getBackSide());

    // ... rest of existing code ...
}
```

### 4. Google Apps Script Changes

Fi `Code.gs` (in Google Apps Script project)

Update `getFlashcard()` function to include D column:

```javascript
function getFlashcard(e) {
  var sheetName = e.parameter.sheetName || 'Sheet1';
  var cardNumber = parseInt(e.parameter.cardNumber) || 1;

  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();

  // Skip header row
  var dataRow = values[cardNumber];

  return ContentService
    .createTextOutput(JSON.stringify({
      frontSide: dataRow[0] || '',        // A column
      backSide: dataRow[1] || '',         // B column
      importance: dataRow[2] || 0,        // C column
      pronunciation: dataRow[3] || '',    // D column (NEW)
      currentNum: cardNumber,
      totalCards: values.length - 1
    }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

## TTS Handling

**Important**: Pronunciation text (D column) should NOT be included in TTS audio.

Current implementation already handles this correctly:

```java
// Only A column content is cached for TTS
cachedFrontText = stripHtmlTags(flashcard.getFrontSide());

// TTS uses cached text (D column is not included)
textToSpeech.speak(cachedFrontText, ...);
```

No changes needed for TTS logic.

## Edge Cases

### Empty D Column
- If D column is empty or null, hide `pronunciationText` (set `visibility=GONE`)
- No impact on layout or TTS

### HTML in D Column
- Decision needed: Allow HTML tags in pronunciation field?
- Recommendation: Keep as plain text (no HTML parsing)

### Long Pronunciation Text
- Consider max lines or ellipsize for very long pronunciation strings
- May need to adjust font size or layout

### Back Side Pronunciation
- Current design: Only front side has pronunciation
- Future: Add E column for back side pronunciation?

## Configuration Options (Future)

Potential settings to add:

1. **Show/Hide Pronunciation**: Toggle visibility in settings
2. **Pronunciation Font Size**: Adjustable size (10sp - 14sp)
3. **Pronunciation Color**: Customizable color
4. **Pronunciation Position**: Above or below main text

## Testing Checklist

- [ ] Empty D column (visibility=GONE)
- [ ] Japanese furigana display
- [ ] Chinese pinyin display
- [ ] English phonetic symbols
- [ ] TTS does not read pronunciation
- [ ] Portrait orientation
- [ ] Landscape orientation
- [ ] Long pronunciation text
- [ ] Card flip animation
- [ ] ScrollView behavior with pronunciation

## Implementation Estimate

- Model changes: 10 minutes
- Layout changes: 20 minutes
- Display logic: 15 minutes
- Google Apps Script: 15 minutes
- Testing: 30 minutes

**Total**: ~1.5 hours

## Notes

- This feature is optional (backward compatible with existing data)
- No breaking changes to existing functionality
- Easy to extend for back side pronunciation (E column)
- Pronunciation display is purely visual (no impact on TTS or audio features)

## References

- Original discussion: [Date: 2025-01-24]
- Related features: TTS, Auto-play, Language selection
