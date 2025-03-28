// ユーザーの回答を記録する関数
function recordAnswer(sheetName, questionNumber, isCorrect, elapsedTime) {
  // アクティブなスプレッドシートと問題シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // 質問番号に基づいて行番号を計算（ヘッダー無）
  var row = questionNumber;

  // 現在の正解数と不正解数を取得
  var correctCount = sheet.getRange(row, 6).getValue(); // 正解数（F列）
  var incorrectCount = sheet.getRange(row, 7).getValue(); // 不正解数（G列）

  // 回答時間を記録（E列）
  sheet.getRange(row, 5).setValue(elapsedTime);

  // 正解の場合は正解数をインクリメント、不正解の場合は不正解数をインクリメント
  if (isCorrect) {
    correctCount++;
    sheet.getRange(row, 6).setValue(correctCount); // 正解数を更新
  } else {
    incorrectCount++;
    sheet.getRange(row, 7).setValue(incorrectCount); // 不正解数を更新
  }

  // 総試行回数（正解数 + 不正解数）を計算
  var totalAttempts = correctCount + incorrectCount;

  // 正答率を計算し（正解数 / 総試行回数）、H列に記録
  var correctRate = correctCount / totalAttempts;
  sheet.getRange(row, 8).setValue(correctRate);

  // 現在解いた質問番号を「Setting」シートに記録
  var settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');
  if (sheetName === "特許") {
    settingSheet.getRange("A1").setValue(row + 1); // 特許 → A1
  } else if (sheetName === "意匠") {
    settingSheet.getRange("A2").setValue(row + 1); // 意匠 → A2
  } else if (sheetName === "商標") {
    settingSheet.getRange("A3").setValue(row + 1); // 商標 → A3
  } else if (sheetName === "条約") {
    settingSheet.getRange("A4").setValue(row + 1); // 条約 → A4
  } else if (sheetName === "著作") {
    settingSheet.getRange("A5").setValue(row + 1); // 著作 → A5
  } else if (sheetName === "不競") {
    settingSheet.getRange("A6").setValue(row + 1); // 不競 → A6    
  } else {
    throw new Error("Unknown sheet name: " + sheetName); // 未知のシート名に対するエラー
  }
}

// 現在の質問を「超重要」としてマークする関数
function markImportant3(sheetName,questionNumber) {
  // アクティブなスプレッドシートと問題シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // 質問番号に基づいて行番号を計算（1行目はヘッダーと仮定）
  var row = questionNumber;

  // 重要フラグをI列に記録
  sheet.getRange(row, 9).setValue(3); // I列に 3 をセット
}

// 現在の質問を「少し重要」としてマークする関数
function markImportant2(sheetName,questionNumber) {
  // アクティブなスプレッドシートと問題シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // 質問番号に基づいて行番号を計算（1行目はヘッダーと仮定）
  var row = questionNumber;

  // 重要フラグをI列に記録
  sheet.getRange(row, 9).setValue(2); // I列に 2 をセット
}

// 現在の質問を「重要」としてマークする関数
function markImportant1(sheetName,questionNumber) {
  // アクティブなスプレッドシートと問題シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // 質問番号に基づいて行番号を計算（1行目はヘッダーと仮定）
  var row = questionNumber;

  // 重要フラグをI列に記録
  sheet.getRange(row, 9).setValue(1); // I列に 1 をセット
}

// 現在の質問を「普通」としてマークする関数
function markImportant0(sheetName,questionNumber) {
  // アクティブなスプレッドシートと問題シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // 質問番号に基づいて行番号を計算（1行目はヘッダーと仮定）
  var row = questionNumber;

  // 重要フラグをI列に記録
  sheet.getRange(row, 9).setValue(0); // I列に 1 をセット
}