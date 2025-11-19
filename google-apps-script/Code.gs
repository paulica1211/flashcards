/**
 * Google Apps Script for Flashcard App Backend
 *
 * Setup Instructions:
 * 1. Open your Google Sheet with flashcards (Column A = Front, Column B = Back)
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
const IMPORTANCE_COLUMN = 3; // Column C - Importance level (0-3)
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

  // Get rich text formatting
  const frontRichText = sheet.getRange(row, FRONT_COLUMN).getRichTextValue();
  const backRichText = sheet.getRange(row, BACK_COLUMN).getRichTextValue();

  const flashcard = {
    frontSide: convertRichTextToHtml(frontSide, frontRichText),
    backSide: convertRichTextToHtml(backSide, backRichText),
    currentNum: cardNumber,
    totalCards: totalCards,
    importance: parseInt(importance) || 0
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

  // Fetch all data at once for better performance (3 columns: front, back, importance)
  const data = sheet.getRange(startRow, FRONT_COLUMN, numRows, 3).getValues();
  const richTextData = sheet.getRange(startRow, FRONT_COLUMN, numRows, 2).getRichTextValues();

  for (let i = 0; i < data.length; i++) {
    flashcards.push({
      frontSide: convertRichTextToHtml(data[i][0], richTextData[i][0]),
      backSide: convertRichTextToHtml(data[i][1], richTextData[i][1]),
      currentNum: startCard + i,
      totalCards: totalCards,
      importance: parseInt(data[i][2]) || 0
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
 * Helper: Return error response
 */
function errorResponse(message) {
  return ContentService
    .createTextOutput(JSON.stringify({ error: message }))
    .setMimeType(ContentService.MimeType.JSON);
}
