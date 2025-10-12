//該当のPCTの条文を参照する。

function processExistingData() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet1 = ss.getSheetByName("条約"); // シート1
  const sheet2 = ss.getSheetByName("PCTreg"); // シート2
  
  // シート1のA列を取得（PCT規則XX.Xのデータ）
  const rangeA = sheet1.getRange("A1:A" + sheet1.getLastRow()); 
  const valuesA = rangeA.getValues();
  
  // シート2のD列（キー）とC列（対応データ）を取得
  const rangeD = sheet2.getRange("D1:D406");
  console.log(rangeD.getValues());
  const valuesD = rangeD.getValues().flat(); // キー列
  const rangeC = sheet2.getRange("C1:C" + sheet2.getLastRow());
  const valuesC = rangeC.getValues().flat(); // 取得したい値の列
  
  const regex = /(?<=【ＰＣＴ規則)(\d+\.\d+)(?=】)/; // PCT規則XX.Xを抽出
  
  for (let i = 0; i < valuesA.length; i++) {
    const value = valuesA[i][0];
    
    if (value) {
      const match = value.match(regex); // PCT規則の数字部分を取得
      
      if (match) {
        
        const extractedNum = convertFullWidthToHalfWidth(match[1]); // 全角→半角変換
        console.log(extractedNum);

        // シート2のD列で一致する行を探す
       const index = valuesD.map(String).indexOf(String(extractedNum));
        console.log("matched:",index);
        
        if (index !== -1) {
          const correspondingValue = valuesC[index]; // シート2のC列の対応データ
          sheet1.getRange(i + 1, 15).setValue(correspondingValue); // J列に設定（10列目）
        }
      }
    }
  }
}

function processExistingData2() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet1 = ss.getSheetByName("条約"); // シート1
  const sheet2 = ss.getSheetByName("PCTreg"); // シート2
  const sheet3 = ss.getSheetByName("PCTmain"); // シート3
  
  // シート1のA列を取得（PCT規則XX.Xのデータ）
  const rangeA = sheet1.getRange("A1:A" + sheet1.getLastRow()); 
  const valuesA = rangeA.getValues();
  
  // シート2のD列（キー）とC列（対応データ）を取得
  const rangeD = sheet2.getRange("D1:D" + sheet2.getLastRow());
  const valuesD = rangeD.getValues().flat().map(String); // キー列を文字列化
  const rangeC = sheet2.getRange("C1:C" + sheet2.getLastRow());
  const valuesC = rangeC.getValues().flat(); // 取得したい値の列

  const rangeD2 = sheet3.getRange("D1:D" + sheet3.getLastRow());
  const valuesD2 = rangeD2.getValues().flat().map(String); // キー列を文字列化
  const rangeC2 = sheet3.getRange("C1:C" + sheet3.getLastRow());
  const valuesC2 = rangeC2.getValues().flat(); // 取得したい値の列
  
  //const regex = /【ＰＣＴ規則(\d+\.\d+)】/g; // PCT規則XX.X を全て取得
  const regex = /(?<=【ＰＣＴ規則)(\d+(?:の\d+)?\.\d+)/g;
  const regex2  = /ＰＣＴ(\d+)条/g;

  for (let i = 0; i < valuesA.length; i++) {
    const value = valuesA[i][0];

    if (value) {
      let matches = [...value.matchAll(regex)]; // 正規表現にすべて一致するものを取得
      let matches2 = [...value.matchAll(regex2)]; // 正規表現にすべて一致するものを取得

      if (matches.length > 0 || matches2.length > 0) {
        let columnOffset = 0; // J列（15列目）からのオフセット

        matches.forEach(match => {
          const extractedNum = convertFullWidthToHalfWidth(match[1]); // 全角→半角変換
          console.log("抽出された値:", extractedNum);

          // シート2のD列で一致する行を探す
          const index = valuesD.indexOf(String(extractedNum));
          console.log("一致したインデックス:", index);

          if (index !== -1) {
            //const correspondingValue = valuesC[index]; // シート2のC列の対応データ
            const correspondingIndex = "reg_" + index;
            sheet1.getRange(i + 1, 15 + columnOffset).setValue(correspondingIndex); // J列から右へ
            columnOffset++; // 右の列に移動
          }
        });

        matches2.forEach(match => {
          const extractedNum = convertFullWidthToHalfWidth(match[1]); // 全角→半角変換
          console.log("抽出された値:", extractedNum);

          // シート3のD列で一致する行を探す
          const index = valuesD2.indexOf(String(extractedNum));
          console.log("一致したインデックス:", index);

          if (index !== -1) {
            const correspondingIndex = "PCT_" + index;
            sheet1.getRange(i + 1, 15 + columnOffset).setValue(correspondingIndex); // J列から右へ
            columnOffset++; // 右の列に移動
          }
        });
      }
    }
  }
}



// 全角数字・小数点を半角に変換する関数
function convertFullWidthToHalfWidth(text) {
  const fullwidth = "０１２３４５６７８９．";
  const halfwidth = "0123456789.";
  
  return text.split("").map(char => {
    const index = fullwidth.indexOf(char);
    return index !== -1 ? halfwidth[index] : char;
  }).join("");
}
