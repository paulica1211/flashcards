function getSheetNames() {
  // アクティブなスプレッドシートのすべてのシート名を取得
  const sheets = SpreadsheetApp.getActiveSpreadsheet().getSheets();
  return sheets.map(sheet => sheet.getName()); // シート名の配列を返す
}

function saveSettings(sheetName, questionNumber) {
  const settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');
    // シート名に応じて対応するセルを保存
  if (sheetName === "特許") {
    settingSheet.getRange("A1").setValue(questionNumber); // apple → A1
  } else if (sheetName === "意匠") {
    settingSheet.getRange("A2").setValue(questionNumber); // tomato → A2
  } else if (sheetName === "商標") {
    settingSheet.getRange("A3").setValue(questionNumber); // orange → A3
  } else if (sheetName === "条約") {
    settingSheet.getRange("A4").setValue(questionNumber); // orange → A3
  } else if (sheetName === "著作") {
    settingSheet.getRange("A5").setValue(questionNumber); // orange → A3
  } else if (sheetName === "不競") {
    settingSheet.getRange("A6").setValue(questionNumber); // orange → A3    
  } else {
    throw new Error("Unknown sheet name: " + sheetName); // 未知のシート名に対するエラー
  }
  settingSheet.getRange('B1').setValue(sheetName); // シート名をB1セルに保存
}