function extractLeftOfSpace() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getActiveSheet(); // アクティブなシートを取得
  var range = sheet.getRange("B:B"); // B列を取得
  var values = range.getValues(); // B列の全データを取得

  var output = []; // C列に格納するデータ
  const regex = /第(\d+)条/;  // "第" の後に数字があり、"条" で終わる

  for (var i = 0; i < values.length; i++) {
    var cellValue = values[i][0]; // B列の各セルの値
    const match = cellValue.match(regex);

    if (!match) {
      output.push([""]); // 空のセルなら空白を追加
      continue;
    }
    
    //var extractedText = cellValue.split(" ")[0]; // 最初のスペースで分割し、左側を取得
    var extractedText = match[1]; // 最初のスペースで分割し、左側を取得
    var convertedText = convertFullwidthToHalfwidth(extractedText)
    output.push([convertedText]); // 配列に追加
  }

  // D列にデータを書き込む
  sheet.getRange(1, 4, output.length, 1).setValues(output);
  
  Logger.log("抽出完了: D列に出力しました。");
}

function convertFullwidthToHalfwidth(text) {
  // 全角数字と全角小数点を半角に変換するマッピング
  const fullwidth = "０１２３４５６７８９．";
  const halfwidth = "0123456789.";

  let result = "";
  for (let i = 0; i < text.length; i++) {
    let char = text[i];
    let index = fullwidth.indexOf(char);
    result += index !== -1 ? halfwidth[index] : char;
  }
  return result;
}