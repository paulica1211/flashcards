/**
 * Google Apps Script for Flashcard App Backend
 *
 * Setup Instructions:
 * 1. Open your Google Sheet with flashcards:
 *    - Column A = Front side
 *    - Column B = Back side
 *    - Column G = Importance (0-3)
 *    - Column C = Front pronunciation (optional)
 *    - Column D = Back pronunciation (optional)
 * 2. Go to Extensions → Apps Script
 * 3. Delete any existing code and paste this entire file
 * 4. Click Deploy → New deployment
 * 5. Choose "Web app" as deployment type
 * 6. Set "Execute as" to "Me"
 * 7. Set "Who has access" to "Anyone" (or "Anyone with the link")
 * 8. Click Deploy and copy the Web app URL
 * 9. Paste that URL into ApiClient.java BASE_URL
 */

// Configuration - Update these values
const SHEET_NAME = "Sheet1"; // Name of your sheet tab
const FRONT_COLUMN = 1; // Column A (1-indexed)
const BACK_COLUMN = 2;  // Column B
const IMPORTANCE_COLUMN = 7; // Column G - Importance level (0-3)
const FRONT_PRONUNCIATION_COLUMN = 3; // Column C - Pronunciation for front side
const BACK_PRONUNCIATION_COLUMN = 4; // Column D - Pronunciation for back side
const FRONT_EXTRA_COLUMN = 5; // Column E
const BACK_EXTRA_COLUMN = 6;  // Column F
const FIRST_ROW = 2;    // Start from row 2 (assuming row 1 has headers)

// Settings sheet for tracking progress (optional)
const SETTINGS_SHEET = "Settings";
const SETTINGS_SHEET_NAME_COLUMN = 1; // Column A: Sheet name
const SETTINGS_CARD_NUMBER_COLUMN = 2; // Column B: Current card number
const SETTINGS_FIRST_ROW = 2; // Start from row 2 (row 1 has headers)

/**
 * Main entry point for all API requests
 */
function doGet(e) {
  const action = e.parameter.action;

  try {
    switch(action) {
      case 'getFlashcard':
        return getFlashcard(e);
      case 'getFlashcardBatch':
        return getFlashcardBatch(e);
      case 'getFlashcardStartingInfo':
        return getFlashcardStartingInfo(e);
      case 'getAvailableSheets':
        return getAvailableSheets(e);
      default:
        return errorResponse('Unknown action: ' + action);
    }
  } catch (error) {
    Logger.log('Error: ' + error.toString());
    return errorResponse(error.toString());
  }
}

/**
 * Get a specific flashcard by card number
 */
function getFlashcard(e) {
  const cardNumber = parseInt(e.parameter.cardNumber);
  const sheetName = e.parameter.sheetName || SHEET_NAME;

  if (!cardNumber || cardNumber < 1) {
    return errorResponse('Invalid card number');
  }

  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  if (!sheet) {
    return errorResponse('Sheet not found: ' + sheetName);
  }

  // Count only rows with data in the front column
  const lastRow = sheet.getLastRow();
  const frontColumn = sheet.getRange(FIRST_ROW, FRONT_COLUMN, lastRow - FIRST_ROW + 1, 1).getValues();
  let totalCards = 0;
  for (let i = 0; i < frontColumn.length; i++) {
    if (frontColumn[i][0] && frontColumn[i][0].toString().trim() !== '') {
      totalCards++;
    }
  }

  if (cardNumber > totalCards) {
    return errorResponse('Card number exceeds total cards');
  }

  const row = FIRST_ROW + cardNumber - 1;
  const frontSide = sheet.getRange(row, FRONT_COLUMN).getValue();
  const backSide = sheet.getRange(row, BACK_COLUMN).getValue();
  const importance = sheet.getRange(row, IMPORTANCE_COLUMN).getValue() || 0;
  const frontPronunciation = sheet.getRange(row, FRONT_PRONUNCIATION_COLUMN).getValue() || "";
  const backPronunciation = sheet.getRange(row, BACK_PRONUNCIATION_COLUMN).getValue() || "";
  const frontSideExtra = sheet.getRange(row, FRONT_EXTRA_COLUMN).getValue() || "";
  const backSideExtra = sheet.getRange(row, BACK_EXTRA_COLUMN).getValue() || "";

  // Get rich text formatting
  const frontRichText = sheet.getRange(row, FRONT_COLUMN).getRichTextValue();
  const backRichText = sheet.getRange(row, BACK_COLUMN).getRichTextValue();

  const flashcard = {
    frontSide: convertRichTextToHtml(frontSide, frontRichText),
    backSide: convertRichTextToHtml(backSide, backRichText),
    currentNum: cardNumber,
    totalCards: totalCards,
    importance: parseInt(importance) || 0,
    frontPronunciation: frontPronunciation.toString(),
    backPronunciation: backPronunciation.toString(),
    frontSideExtra: frontSideExtra.toString(),
    backSideExtra: backSideExtra.toString()
  };

  return jsonResponse(flashcard);
}

/**
 * Get multiple flashcards in a batch
 */
function getFlashcardBatch(e) {
  const startCard = parseInt(e.parameter.startCard);
  const count = parseInt(e.parameter.count) || 10; // Default to 10 cards
  const sheetName = e.parameter.sheetName || SHEET_NAME;

  if (!startCard || startCard < 1) {
    return errorResponse('Invalid start card number');
  }

  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  if (!sheet) {
    return errorResponse('Sheet not found: ' + sheetName);
  }

  // Count only rows with data in the front column
  const lastRow = sheet.getLastRow();
  const frontColumn = sheet.getRange(FIRST_ROW, FRONT_COLUMN, lastRow - FIRST_ROW + 1, 1).getValues();
  let totalCards = 0;
  for (let i = 0; i < frontColumn.length; i++) {
    if (frontColumn[i][0] && frontColumn[i][0].toString().trim() !== '') {
      totalCards++;
    }
  }

  const endCard = Math.min(startCard + count - 1, totalCards);

  if (startCard > totalCards) {
    return errorResponse('Start card number exceeds total cards');
  }

  const flashcards = [];
  const startRow = FIRST_ROW + startCard - 1;
  const numRows = endCard - startCard + 1;

  // Fetch all data at once for better performance (7 columns: front, back, frontPronunciation, backPronunciation, frontSideExtra, backSideExtra, importance)
  const data = sheet.getRange(startRow, FRONT_COLUMN, numRows, 7).getValues();
  const richTextData = sheet.getRange(startRow, FRONT_COLUMN, numRows, 2).getRichTextValues();

  for (let i = 0; i < data.length; i++) {
    flashcards.push({
      frontSide: convertRichTextToHtml(data[i][0], richTextData[i][0]),
      backSide: convertRichTextToHtml(data[i][1], richTextData[i][1]),
      currentNum: startCard + i,
      totalCards: totalCards,
      importance: parseInt(data[i][6]) || 0,
      frontPronunciation: (data[i][2] || "").toString(),
      backPronunciation: (data[i][3] || "").toString(),
      frontSideExtra: (data[i][4] || "").toString(),
      backSideExtra: (data[i][5] || "").toString()
    });
  }

  return jsonResponse({ cards: flashcards });
}

/**
 * Get starting info (current card number for a specific sheet)
 */
function getFlashcardStartingInfo(e) {
  const sheetName = e.parameter.sheetName || SHEET_NAME;

  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    let settingsSheet = ss.getSheetByName(SETTINGS_SHEET);

    // Create settings sheet if it doesn't exist
    if (!settingsSheet) {
      settingsSheet = ss.insertSheet(SETTINGS_SHEET);
      settingsSheet.getRange("A1").setValue("Sheet Name");
      settingsSheet.getRange("B1").setValue("Current Card");
    }

    // Find the row for this sheet
    const lastRow = settingsSheet.getLastRow();
    const data = settingsSheet.getRange(SETTINGS_FIRST_ROW, SETTINGS_SHEET_NAME_COLUMN,
                                        Math.max(1, lastRow - SETTINGS_FIRST_ROW + 1), 2).getValues();

    let cardNumber = 1;
    for (let i = 0; i < data.length; i++) {
      if (data[i][0] === sheetName) {
        cardNumber = parseInt(data[i][1]) || 1;
        break;
      }
    }

    const info = {
      sheetName: sheetName,
      questionNumber: cardNumber
    };

    return jsonResponse(info);
  } catch (error) {
    Logger.log('Error in getFlashcardStartingInfo: ' + error.toString());
    return jsonResponse({
      sheetName: sheetName,
      questionNumber: 1
    });
  }
}

/**
 * POST handler (for future features like saving progress)
 */
function doPost(e) {
  const action = e.parameter.action;

  try {
    switch(action) {
      case 'saveProgress':
        return saveProgress(e);
      case 'saveImportance':
        return saveImportance(e);
      case 'saveCardContent':
        return saveCardContent(e);
      default:
        return errorResponse('Unknown action: ' + action);
    }
  } catch (error) {
    Logger.log('Error: ' + error.toString());
    return errorResponse(error.toString());
  }
}

/**
 * Save current card progress for a specific sheet
 */
function saveProgress(e) {
  const cardNumber = parseInt(e.parameter.cardNumber);
  const sheetName = e.parameter.sheetName || SHEET_NAME;

  if (!cardNumber || cardNumber < 1) {
    return errorResponse('Invalid card number');
  }

  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let settingsSheet = ss.getSheetByName(SETTINGS_SHEET);

  // Create settings sheet if it doesn't exist
  if (!settingsSheet) {
    settingsSheet = ss.insertSheet(SETTINGS_SHEET);
    settingsSheet.getRange("A1").setValue("Sheet Name");
    settingsSheet.getRange("B1").setValue("Current Card");
  }

  // Find or create row for this sheet
  const lastRow = settingsSheet.getLastRow();
  const data = settingsSheet.getRange(SETTINGS_FIRST_ROW, SETTINGS_SHEET_NAME_COLUMN,
                                      Math.max(1, lastRow - SETTINGS_FIRST_ROW + 1), 2).getValues();

  let rowIndex = -1;
  for (let i = 0; i < data.length; i++) {
    if (data[i][0] === sheetName) {
      rowIndex = SETTINGS_FIRST_ROW + i;
      break;
    }
  }

  // If sheet not found, add new row
  if (rowIndex === -1) {
    rowIndex = lastRow + 1;
    settingsSheet.getRange(rowIndex, SETTINGS_SHEET_NAME_COLUMN).setValue(sheetName);
  }

  // Update card number
  settingsSheet.getRange(rowIndex, SETTINGS_CARD_NUMBER_COLUMN).setValue(cardNumber);

  return jsonResponse({ success: true });
}

/**
 * Save importance level for a card
 */
function saveImportance(e) {
  const cardNumber = parseInt(e.parameter.cardNumber);
  const importance = parseInt(e.parameter.importance);
  const sheetName = e.parameter.sheetName || SHEET_NAME;

  if (!cardNumber || cardNumber < 1) {
    return errorResponse('Invalid card number');
  }

  if (importance < 0 || importance > 3) {
    return errorResponse('Invalid importance level (must be 0-3)');
  }

  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  if (!sheet) {
    return errorResponse('Sheet not found: ' + sheetName);
  }

  // Count only rows with data in the front column
  const lastRow = sheet.getLastRow();
  const frontColumn = sheet.getRange(FIRST_ROW, FRONT_COLUMN, lastRow - FIRST_ROW + 1, 1).getValues();
  let totalCards = 0;
  for (let i = 0; i < frontColumn.length; i++) {
    if (frontColumn[i][0] && frontColumn[i][0].toString().trim() !== '') {
      totalCards++;
    }
  }

  if (cardNumber > totalCards) {
    return errorResponse('Card number exceeds total cards');
  }

  const row = FIRST_ROW + cardNumber - 1;
  sheet.getRange(row, IMPORTANCE_COLUMN).setValue(importance);

  return jsonResponse({ success: true });
}

/**
 * Helper: Return JSON response
 */
function jsonResponse(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Get list of available sheets (excluding Settings sheet)
 */
function getAvailableSheets(e) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheets = ss.getSheets();

  const sheetNames = sheets
    .map(sheet => sheet.getName())
    .filter(name => name !== SETTINGS_SHEET); // Exclude Settings sheet

  return jsonResponse({ sheets: sheetNames });
}

/**
 * Convert rich text formatting to HTML
 */
function convertRichTextToHtml(plainText, richTextValue) {
  if (!richTextValue || !plainText) {
    return plainText ? plainText.toString() : "";
  }

  const text = plainText.toString();
  let html = "";
  let currentIndex = 0;

  // Get text runs (segments with different formatting)
  const runs = richTextValue.getRuns();

  for (let i = 0; i < runs.length; i++) {
    const run = runs[i];
    const runText = run.getText();
    const textStyle = run.getTextStyle();

    let styledText = runText;

    // Apply bold
    if (textStyle.isBold()) {
      styledText = "<b>" + styledText + "</b>";
    }

    // Apply italic
    if (textStyle.isItalic()) {
      styledText = "<i>" + styledText + "</i>";
    }

    // Apply underline
    if (textStyle.isUnderline()) {
      styledText = "<u>" + styledText + "</u>";
    }

    // Apply strikethrough
    if (textStyle.isStrikethrough()) {
      styledText = "<strike>" + styledText + "</strike>";
    }

    // Apply foreground color
    const color = textStyle.getForegroundColor();
    if (color && color !== "#000000") {
      styledText = '<font color="' + color + '">' + styledText + '</font>';
    }

    html += styledText;
  }

  return html || text;
}

/**
 * Save card content (with rich text formatting) back to Google Sheets
 */
function saveCardContent(e) {
  const cardNumber = parseInt(e.parameter.cardNumber);
  const sheetName = e.parameter.sheetName || SHEET_NAME;
  const side = e.parameter.side; // "front" or "back"
  const htmlContent = e.parameter.html;

  if (!cardNumber || cardNumber < 1) {
    return errorResponse('Invalid card number');
  }

  if (!side || (side !== 'front' && side !== 'back')) {
    return errorResponse('Invalid side (must be "front" or "back")');
  }

  if (!htmlContent) {
    return errorResponse('No HTML content provided');
  }

  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  if (!sheet) {
    return errorResponse('Sheet not found: ' + sheetName);
  }

  // Count only rows with data in the front column
  const lastRow = sheet.getLastRow();
  const frontColumn = sheet.getRange(FIRST_ROW, FRONT_COLUMN, lastRow - FIRST_ROW + 1, 1).getValues();
  let totalCards = 0;
  for (let i = 0; i < frontColumn.length; i++) {
    if (frontColumn[i][0] && frontColumn[i][0].toString().trim() !== '') {
      totalCards++;
    }
  }

  if (cardNumber > totalCards) {
    return errorResponse('Card number exceeds total cards');
  }

  const row = FIRST_ROW + cardNumber - 1;
  const column = (side === 'front') ? FRONT_COLUMN : BACK_COLUMN;

  try {
    // Convert HTML to RichTextValue
    const richTextValue = htmlToRichText(htmlContent);

    // Save to sheet
    sheet.getRange(row, column).setRichTextValue(richTextValue);

    Logger.log('Saved ' + side + ' content for card ' + cardNumber + ' in sheet ' + sheetName);
    return jsonResponse({ success: true });
  } catch (error) {
    Logger.log('Error saving card content: ' + error.toString());
    return errorResponse('Failed to save: ' + error.toString());
  }
}

/**
 * Convert HTML to RichTextValue for Google Sheets
 * Supports: <b>, <i>, <u>, <font color="">
 */
function htmlToRichText(html) {
  // Strip HTML tags and get plain text
  const plainText = html.replace(/<[^>]*>/g, '');

  // Create RichTextValue builder
  const builder = SpreadsheetApp.newRichTextValue().setText(plainText);

  // Parse HTML and apply styles
  let currentPos = 0;
  const segments = parseHtmlSegments(html);

  for (let i = 0; i < segments.length; i++) {
    const segment = segments[i];
    const startPos = currentPos;
    const endPos = currentPos + segment.text.length;

    if (segment.text.length > 0) {
      let style = SpreadsheetApp.newTextStyle();

      // Apply bold
      if (segment.bold) {
        style = style.setBold(true);
      }

      // Apply italic
      if (segment.italic) {
        style = style.setItalic(true);
      }

      // Apply underline
      if (segment.underline) {
        style = style.setUnderline(true);
      }

      // Apply color
      if (segment.color) {
        style = style.setForegroundColor(segment.color);
      }

      builder.setTextStyle(startPos, endPos, style.build());
      currentPos = endPos;
    }
  }

  return builder.build();
}

/**
 * Parse HTML into segments with style information
 */
function parseHtmlSegments(html) {
  const segments = [];
  const stack = []; // Stack to track nested tags
  let currentText = '';
  let currentStyles = { bold: false, italic: false, underline: false, color: null };

  // Simple HTML parser using regex
  const tagRegex = /<(\/?)(b|i|u|font)([^>]*)>/gi;
  let lastIndex = 0;
  let match;

  while ((match = tagRegex.exec(html)) !== null) {
    const isClosing = match[1] === '/';
    const tagName = match[2].toLowerCase();
    const attributes = match[3];

    // Add text before this tag
    const textBefore = html.substring(lastIndex, match.index);
    if (textBefore) {
      // Strip any remaining tags from text
      const cleanText = textBefore.replace(/<[^>]*>/g, '');
      if (cleanText) {
        segments.push({
          text: cleanText,
          bold: currentStyles.bold,
          italic: currentStyles.italic,
          underline: currentStyles.underline,
          color: currentStyles.color
        });
      }
    }

    // Update current styles
    if (!isClosing) {
      // Opening tag
      if (tagName === 'b') {
        stack.push('bold');
        currentStyles.bold = true;
      } else if (tagName === 'i') {
        stack.push('italic');
        currentStyles.italic = true;
      } else if (tagName === 'u') {
        stack.push('underline');
        currentStyles.underline = true;
      } else if (tagName === 'font') {
        // Extract color attribute
        const colorMatch = attributes.match(/color\s*=\s*["']([^"']*)["']/i);
        if (colorMatch) {
          stack.push({ tag: 'color', value: currentStyles.color });
          currentStyles.color = colorMatch[1];
        }
      }
    } else {
      // Closing tag
      if (tagName === 'b') {
        currentStyles.bold = false;
        // Remove from stack
        const index = stack.lastIndexOf('bold');
        if (index !== -1) stack.splice(index, 1);
      } else if (tagName === 'i') {
        currentStyles.italic = false;
        const index = stack.lastIndexOf('italic');
        if (index !== -1) stack.splice(index, 1);
      } else if (tagName === 'u') {
        currentStyles.underline = false;
        const index = stack.lastIndexOf('underline');
        if (index !== -1) stack.splice(index, 1);
      } else if (tagName === 'font') {
        // Pop color from stack
        for (let i = stack.length - 1; i >= 0; i--) {
          if (typeof stack[i] === 'object' && stack[i].tag === 'color') {
            currentStyles.color = stack[i].value;
            stack.splice(i, 1);
            break;
          }
        }
      }
    }

    lastIndex = match.index + match[0].length;
  }

  // Add remaining text
  const remainingText = html.substring(lastIndex);
  if (remainingText) {
    const cleanText = remainingText.replace(/<[^>]*>/g, '');
    if (cleanText) {
      segments.push({
        text: cleanText,
        bold: currentStyles.bold,
        italic: currentStyles.italic,
        underline: currentStyles.underline,
        color: currentStyles.color
      });
    }
  }

  return segments;
}

/**
 * Helper: Return error response
 */
function errorResponse(message) {
  return ContentService
    .createTextOutput(JSON.stringify({ error: message }))
    .setMimeType(ContentService.MimeType.JSON);
}
