//testtesttest


function doGet() {
  return HtmlService.createTemplateFromFile('Index')
    .evaluate()
    .setTitle("Patent Quiz")
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
    .addMetaTag('viewport','width=device-width, initial-scale=1, user-scalable=no'); // Index.html ファイルを表示
}

// クイズの開始時に表示する質問番号を取得する関数
function getStartingQuestionNumberAndSheet() {
  // 「Setting」シートを取得
  var settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');

  // A1セルに記録されている開始質問番号を取得
  var startingQuestionNumber
  
  // B1セルに記録されている開始シートを取得
  var startingSheetName = settingSheet.getRange('B1').getValue();

  if (startingSheetName === "特許") {
    startingQuestionNumber = settingSheet.getRange('A1').getValue();
  } else if (startingSheetName === "意匠") {
    startingQuestionNumber = settingSheet.getRange('A2').getValue();
  } else if (startingSheetName === "商標") {
    startingQuestionNumber = settingSheet.getRange('A3').getValue();
  } else if (startingSheetName === "条約") {
    startingQuestionNumber = settingSheet.getRange('A4').getValue();
  } else if (startingSheetName === "著作") {
    startingQuestionNumber = settingSheet.getRange('A5').getValue();
  } else if (startingSheetName === "不競") {
    startingQuestionNumber = settingSheet.getRange('A6').getValue();        
  } else {
    throw new Error("Unknown sheet name: " + sheetName); // 未知のシート名に対するエラー
  }

  // 開始質問番号が存在しない場合はデフォルト値を設定
  startingQuestionNumber = startingQuestionNumber ? parseInt(startingQuestionNumber) : 1;

  // 開始シート名が存在しない場合はデフォルト値を設定
  startingSheetName = startingSheetName || "DefaultSheet";

  // オブジェクト形式で開始質問番号とシート名を返す
  return {
    questionNumber: startingQuestionNumber,
    sheetName: startingSheetName
  };
}

// クイズの質問番号を取得する関数
function getQuestionNumber(selectedSheetName) {
  // 「Setting」シートを取得
  var settingSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('Setting');

  // A1セルに記録されている開始質問番号を取得
  var currentQuestionNumber
 
  if (selectedSheetName === "特許") {
    currentQuestionNumber = settingSheet.getRange('A1').getValue();
  } else if (selectedSheetName === "意匠") {
    currentQuestionNumber = settingSheet.getRange('A2').getValue();
  } else if (selectedSheetName === "商標") {
    currentQuestionNumber = settingSheet.getRange('A3').getValue();
  } else if (selectedSheetName === "条約") {
    currentQuestionNumber = settingSheet.getRange('A4').getValue();
  } else if (selectedSheetName === "著作") {
    currentQuestionNumber = settingSheet.getRange('A5').getValue();
  } else if (selectedSheetName === "不競") {
    currentQuestionNumber = settingSheet.getRange('A6').getValue();
  } else {
    throw new Error("Unknown sheet name: " + selectedSheetName); // 未知のシート名に対するエラー
  }


  // 開始質問番号が存在しない場合はデフォルト値を設定
  currentQuestionNumber = currentQuestionNumber ? parseInt(currentQuestionNumber) : 1;

  // オブジェクト形式で開始質問番号とシート名を返す
  return currentQuestionNumber;
}

// 指定された質問番号に対応する質問データを取得する関数
function getCurrentQuestionFromSheet(sheetName, questionNumber) {
  // アクティブなスプレッドシートのアクティブシートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);


  // 質問番号に基づいて行番号を計算（ヘッダー無）
  var row = questionNumber;

  // 指定された行のA列からJ列までのデータを1行分取得
  var data = sheet.getRange(row, 1, 1, 10).getValues()[0];

  var link1 = sheet.getRange(row, 13).getFormula();
  var link2 = sheet.getRange(row, 14).getFormula();
  var link3 = sheet.getRange(row, 15).getFormula();
  var link4 = sheet.getRange(row, 16).getFormula();

  //PCT
  var link5 = sheet.getRange(row, 19).getValues()[0][0].toString();
  var link6 = sheet.getRange(row, 20).getValues()[0][0].toString();
  var link7 = sheet.getRange(row, 21).getValues()[0][0].toString();


  // HTML形式でフォーマットされた質問文（B列）を取得
  var questionCell = sheet.getRange(row, 2); // B列のセル（質問文）
  var questionHTML = getCellAsHTML(questionCell); // リッチテキストをHTML形式に変換

  // HTML形式でフォーマットされた解説文（C列）を取得
  var explanationCell = sheet.getRange(row, 3); // C列のセル（解説文）
  var explanationHTML = getCellAsHTML(explanationCell); // リッチテキストをHTML形式に変換

  // シートの最終行を取得
  var lastRow = sheet.getLastRow();

  // 質問データをオブジェクト形式で返却
  return {
    article: data[0],           // 記事情報（A列）
    question: questionHTML,     // HTML形式の質問文
    explanation: explanationHTML, // HTML形式の解説文
    answer: data[3],            // 正解（D列）
    timeToAnswer: data[4],      // 回答時間（E列）
    correctCount: data[5],      // 正解数（F列）
    incorrectCount: data[6],    // 不正解数（G列）
    correctRate: data[7],       // 正答率（H列）
    ofImportance: data[8],       // 重要フラグ（I列）
    questionYear: data[9],       // 出題年（J列）
    linkM: link1,       // 重要フラグ（M列）
    linkN: link2,       // 重要フラグ（N列）
    linkO: link3,
    linkP: link4,
    art1: link5,
    art2: link6,
    art3: link7,
    allQuestion: lastRow, //問題数
    currentNum: row
  };
}

// 現在の質問番号を基に次の質問を取得する関数
function goToNextQuestion(sheetName, currentQuestionNumber, importanceLevel) {
  // アクティブなスプレッドシートとアクティブシートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  // シートの最終行を取得
  var lastRow = sheet.getLastRow();

  var nextQuestionNumber = currentQuestionNumber + 1;

  // ofImportanceが1でない次の質問を取得
  while (nextQuestionNumber <= lastRow) {
    var importance = sheet.getRange(nextQuestionNumber, 9).getValue(); // I列（重要度）

    // importanceが1でない場合、その質問を返す
    if (importance >= importanceLevel) {
      return getCurrentQuestionFromSheet(sheetName,nextQuestionNumber);
    }

    // importanceが1の場合、次の質問に進む
    nextQuestionNumber++;
  }

  // 最後の質問に到達した場合は null を返す
  return null;
}

// 現在の質問番号を基に前の質問を取得する関数
function goToPreviousQuestion(sheetName, currentQuestionNumber, importanceLevel) {
  // アクティブなスプレッドシートと指定シートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(sheetName);

  var nextQuestionNumber = currentQuestionNumber - 1;

  // ofImportanceが1の前の質問を取得
  while (nextQuestionNumber > 0) {
    var importance = sheet.getRange(nextQuestionNumber, 9).getValue(); // I列（重要度）

    // importanceが1でない場合、その質問を返す
    if (importance >= importanceLevel) {
      return getCurrentQuestionFromSheet(sheetName,nextQuestionNumber);
    }
        // importanceが1でない場合、前の質問に進む
    nextQuestionNumber--;
  }

  // 最初の質問に到達した場合は null を返す
  return null;
}

// 指定されたセルの内容をHTML形式で取得する関数
function getCellAsHTML(cell) {
  // セルのリッチテキスト値を取得
  var richTextValue = cell.getRichTextValue();

  // リッチテキスト値が存在しない場合、通常の値を返却
  if (!richTextValue) {
    return cell.getValue();
  }
  
  // リッチテキスト全体の文字列とスタイル実行（Runs）を取得
  var text = richTextValue.getText(); // 全体のテキストを取得
  var runs = richTextValue.getRuns(); // 各スタイルセグメントを取得
  var html = ""; // 生成するHTML文字列を格納する変数
  
  // 各スタイルセグメントをHTMLに変換
  runs.forEach(function(run) {
    var start = run.getStartIndex(); // セグメントの開始位置
    var end = run.getEndIndex();     // セグメントの終了位置
    var style = run.getTextStyle(); // セグメントのスタイルを取得

    // 現在のセグメントの文字列を抽出
    var textSegment = text.substring(start, end);

    // 初期値としてセグメント文字列を設定
    var htmlSegment = textSegment;

    // 改行文字（\n）をHTMLの <br> タグに置換
    htmlSegment = htmlSegment.replace(/\n/g, "<br>");

    // 太字スタイルをHTMLの <b> タグで適用
    if (style.isBold()) {
      htmlSegment = "<b>" + htmlSegment + "</b>";
    }

    // イタリックスタイルをHTMLの <i> タグで適用
    if (style.isItalic()) {
      htmlSegment = "<i>" + htmlSegment + "</i>";
    }

    // 下線スタイルをHTMLの <u> タグで適用
    if (style.isUnderline()) {
      htmlSegment = '<u>' + htmlSegment + '</u>';
    }

    // 前景色（文字色）をHTMLの <span> タグで適用
    if (style.getForegroundColor()) {
      htmlSegment = "<span style='color:" + style.getForegroundColor() + "'>" + htmlSegment + "</span>";
    }

    // 生成したHTMLセグメントを結果に追加
    html += htmlSegment;
  });

  // 最終的なHTML文字列を返却
  return html;
}


/*
// 指定した質問番号の質問データを取得する関数
function selectQuestion(questionNumber) {
  // getCurrentQuestion関数を呼び出し、質問データを取得
  return getCurrentQuestion(questionNumber);
}
*/


function pickArticle(spreadsheetId, sheetName, index) {
  const ss = SpreadsheetApp.openById(spreadsheetId); // スプレッドシートを指定
  const sheet = ss.getSheetByName(sheetName); // 指定されたシートを取得
  if (!sheet) {
    throw new Error("シートが見つかりません: " + sheetName);
  }
  html1 = getCellAsHTML(sheet.getRange(index + 1, 1));
  html2 = getCellAsHTML(sheet.getRange(index + 1, 2));
  html3 = getCellAsHTML(sheet.getRange(index + 1, 3));
  return html1 + "<br>" + html2 + "<br>" + html3;

}

function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('Quiz')
      .addItem('Start Quiz', 'showQuiz')
      .addToUi();
}

