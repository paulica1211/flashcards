//　問題文・解答文の改行整理。

/*
function replaceWithLineBreak() {
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    const range = sheet.getRange(1, 3, sheet.getLastRow(), 3); // B列とC列の範囲を取得
    const values = range.getValues();

    for (let i = 0; i < values.length; i++) {
        for (let j = 0; j < values[i].length; j++) {
            if (values[i][j]) {
                // 置換対象の文字を改行に置き換える
                //values[i][j] = values[i][j].toString().replace(/\s+$/, '');
                values[i][j] = values[i][j].toString().split('。').join('。\n　');

                //values[i][j] = values[i][j].toString().split('　\n　').join('　');

                // 改行2個（\n\n）を改行1個（\n）に置き換える
                //values[i][j] = values[i][j].toString().replace(/\n\n/g, '\n');
            }
        }
    }

    range.setValues(values);
}
*/