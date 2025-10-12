//解答文にある根拠条文番号・判例名を抽出する。

function extractWordsInBrackets() {
  // アクティブなスプレッドシートを取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  
  // C列のデータを取得（C1からC列の最終行まで）
  var range = sheet.getRange("C1:C" + sheet.getLastRow());
  var values = range.getValues();
  
  // C列の各セルについて処理を行う
  var extractedWords = [];
  
  for (var i = 0; i < values.length; i++) {
    var text = values[i][0]; // 各セルの文章
    //var matches = text.match(/【[^】]+】/g); // 【】で囲まれた部分を正規表現で抽出
    //var matches = text.match(/「[^」]*事件」/g); // 【】で囲まれた部分を正規表現で抽出
    var matches = text.match(/【[^】]+】|「[^」]*事件」/g);

    if (matches) {
      // マッチした部分を配列に追加
      extractedWords.push([matches.join(", ")]); // 複数のマッチをコンマ区切りで1つのセルに
    } else {
      // マッチがなければ空のセルを追加
      extractedWords.push([""]);
    }
  }
  
  // 結果をD列に出力（D1からD列の最終行まで）
  sheet.getRange(1, 1, extractedWords.length, 1).setValues(extractedWords);
}
