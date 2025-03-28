/*
function replaceAndUnderlineMultipleMatches() {
  // アクティブなスプレッドシートの取得
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  
  // B列とC列全体を取得
  var range = sheet.getRange("B:C");
  var values = range.getValues();

  // 正規表現：置換対象（「！！！任意の文字！！！」の形式）
  var regex = /＃＃(.*?)＃＃/g;

  for (var row = 0; row < values.length; row++) {
    for (var col = 0; col < values[row].length; col++) {
      var cell = range.getCell(row + 1, col + 1);
      var text = cell.getValue();
      if (!text) continue; // 空のセルはスキップ

      // 初期化
      var newText = text; // 更新用の新しいテキスト
      var offset = 0; // インデックス調整用オフセット
      var richTextBuilder = SpreadsheetApp.newRichTextValue().setText(newText);

      // マッチ部分を逐次処理
      var match;
      while ((match = regex.exec(text)) !== null) {
        var originalText = match[0]; // 「！！！任意の文字！！！」
        var innerText = match[1];    // 「任意の文字」

        // 置換による新しいテキストの構築
        newText = newText.slice(0, match.index + offset) +
                  innerText +
                  newText.slice(match.index + offset + originalText.length);

        // 書式設定のインデックスを計算
        var startIndex = match.index + offset;
        var endIndex = startIndex + innerText.length;

        // 下線スタイルを適用
        richTextBuilder.setTextStyle(startIndex, endIndex, SpreadsheetApp.newTextStyle()
          .setUnderline(true) // 下線を適用
          .build());

        // オフセットを更新
        offset += innerText.length - originalText.length;
      }

      // セルの内容を更新
      richTextBuilder.setText(newText); // 更新されたテキストをセット
      cell.setValue(newText); // テキストを更新
      cell.setRichTextValue(richTextBuilder.build()); // 書式を適用
    }
  }
}
*/