// API.js - Add this to your Google Apps Script project
// This provides JSON API endpoints for the native Android app

function doGet(e) {
  var action = e.parameter.action;

  try {
    switch(action) {
      case 'getStartingInfo':
        return jsonResponse(getStartingQuestionNumberAndSheet());

      case 'getQuestion':
        var sheetName = e.parameter.sheetName;
        var questionNumber = parseInt(e.parameter.questionNumber);
        return jsonResponse(getCurrentQuestionFromSheet(sheetName, questionNumber));

      case 'getNextQuestion':
        var sheetName = e.parameter.sheetName;
        var currentNum = parseInt(e.parameter.currentNum);
        var importanceLevel = parseInt(e.parameter.importanceLevel);
        return jsonResponse(goToNextQuestion(sheetName, currentNum, importanceLevel));

      case 'getPreviousQuestion':
        var sheetName = e.parameter.sheetName;
        var currentNum = parseInt(e.parameter.currentNum);
        var importanceLevel = parseInt(e.parameter.importanceLevel);
        return jsonResponse(goToPreviousQuestion(sheetName, currentNum, importanceLevel));

      case 'getSheetNames':
        return jsonResponse({sheetNames: getSheetNames()});

      case 'getQuestionNumberForSheet':
        var sheetName = e.parameter.sheetName;
        return jsonResponse({questionNumber: getQuestionNumberForSheet(sheetName)});

      default:
        return jsonResponse({error: 'Invalid action'}, 400);
    }
  } catch (error) {
    return jsonResponse({error: error.toString()}, 500);
  }
}

function doPost(e) {
  var action = e.parameter.action;

  try {
    switch(action) {
      case 'recordAnswer':
        var sheetName = e.parameter.sheetName;
        var questionNumber = parseInt(e.parameter.questionNumber);
        var isCorrect = e.parameter.isCorrect === 'true';
        var elapsedTime = parseFloat(e.parameter.elapsedTime);
        recordAnswer(sheetName, questionNumber, isCorrect, elapsedTime);
        return jsonResponse({success: true});

      case 'markImportant':
        var sheetName = e.parameter.sheetName;
        var questionNumber = parseInt(e.parameter.questionNumber);
        var importance = parseInt(e.parameter.importance);
        markImportant(sheetName, questionNumber, importance);
        return jsonResponse({success: true});

      case 'saveSettings':
        var sheetName = e.parameter.sheetName;
        var questionNumber = parseInt(e.parameter.questionNumber);
        saveSettings(sheetName, questionNumber);
        return jsonResponse({success: true});

      default:
        return jsonResponse({error: 'Invalid action'}, 400);
    }
  } catch (error) {
    return jsonResponse({error: error.toString()}, 500);
  }
}

function jsonResponse(data, statusCode) {
  statusCode = statusCode || 200;

  var output = ContentService.createTextOutput(JSON.stringify(data));
  output.setMimeType(ContentService.MimeType.JSON);

  return output;
}

// Helper function for marking importance
function markImportant(sheetName, questionNumber, importance) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);
  sheet.getRange(questionNumber, 9).setValue(importance); // I column
}

// Helper function to get all sheet names
function getSheetNames() {
  var sheets = SpreadsheetApp.getActiveSpreadsheet().getSheets();
  return sheets.map(function(sheet) {
    return sheet.getName();
  }).filter(function(name) {
    // Filter out system sheets
    return name !== 'Setting' && name !== 'Record';
  });
}

// Helper function to save settings
function saveSettings(sheetName, questionNumber) {
  var settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');

  // Save question number based on sheet name
  if (sheetName === "特許") {
    settingSheet.getRange("A1").setValue(questionNumber);
  } else if (sheetName === "意匠") {
    settingSheet.getRange("A2").setValue(questionNumber);
  } else if (sheetName === "商標") {
    settingSheet.getRange("A3").setValue(questionNumber);
  } else if (sheetName === "条約") {
    settingSheet.getRange("A4").setValue(questionNumber);
  } else if (sheetName === "著作") {
    settingSheet.getRange("A5").setValue(questionNumber);
  } else if (sheetName === "不競") {
    settingSheet.getRange("A6").setValue(questionNumber);
  }

  // Save current sheet name to B1
  settingSheet.getRange('B1').setValue(sheetName);
}

// Helper function to get saved question number for a specific sheet
function getQuestionNumberForSheet(sheetName) {
  var settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');
  var questionNumber = 1; // Default

  // Get question number based on sheet name
  if (sheetName === "特許") {
    questionNumber = settingSheet.getRange("A1").getValue() || 1;
  } else if (sheetName === "意匠") {
    questionNumber = settingSheet.getRange("A2").getValue() || 1;
  } else if (sheetName === "商標") {
    questionNumber = settingSheet.getRange("A3").getValue() || 1;
  } else if (sheetName === "条約") {
    questionNumber = settingSheet.getRange("A4").getValue() || 1;
  } else if (sheetName === "著作") {
    questionNumber = settingSheet.getRange("A5").getValue() || 1;
  } else if (sheetName === "不競") {
    questionNumber = settingSheet.getRange("A6").getValue() || 1;
  }

  return questionNumber;
}
