
function processExistingData() {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  const range = sheet.getRange("A1:A"); // A列全体を対象
  const values = range.getValues(); // A列のデータを取得

  for (let i = 0; i < values.length; i++) {
    const value = values[i][0]; // A列の各セルの値を取得

    if (value) {
      // すべての「特XX条」または「特XX条のXX」を抽出
      //const matches = value.match(/特(\d+)(?:条の([０-９]+))?/g);
      //const matches = value.match(/特(\d+)\s*条(?:の([０-９]+))?/g);
      //const regex = /特\s*(\d+)\s*条(?:の([０-９]+))?/g;

      //const regex = /特\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;
      const regex = /特\s*([\d０-９]+)\s*条(?:の([\d０-９]+))?/g;

      //const regex = /実\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;
      //const regex = /意\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;
      //const regex = /商\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;
      //const regex = /不競\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;
      //const regex = /著\s*([\d０-９]+)\s*条(?:の([０-９]+))?/g;

      var j = 0;
      let match;
      while ((match = regex.exec(value)) !== null) {
        const mainNumber = convertFullWidthToHalfWidth(match[1]); // メイン番号
        const subNumber = match[2] ? convertFullWidthToHalfWidth(match[2]) : null; // サブ番号（全角→半角変換）

        const mergedNum = subNumber ? `${mainNumber}_${subNumber}` : mainNumber; // 例: 67_6 または 67

        const apiLink = patentCreateApiLink(mergedNum);
        //const apiLink = utilityCreateApiLink(mergedNum);
        //const apiLink = designCreateApiLink(mergedNum);
        //const apiLink = trademarkCreateApiLink(mergedNum);
        //const apiLink = copyrightCreateApiLink(mergedNum);
        //const apiLink = competitionCreateApiLink(mergedNum);

        const targetCell = sheet.getRange(i + 1, 13 + j); // J列の該当行（1行目から開始）
        //targetCell.setValue(apiLink);
        targetCell.setFormula(`=HYPERLINK("${apiLink}", "特${mergedNum}条")`);
        j++;

      }
    }
  }
}

// 取得した数字を使って特許法のAPIリンクを作成する関数
function patentCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=334AC0000000121;article=";
  return `${baseUrl}${Number}`;
}

// 取得した数字を使って実用新案法のAPIリンクを作成する関数
function utilityCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=334AC0000000123;article=";
  return `${baseUrl}${Number}`;
}

// 取得した数字を使って意匠法のAPIリンクを作成する関数
function designCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=334AC0000000125;article=";
  return `${baseUrl}${Number}`;
}

// 取得した数字を使って商標法のAPIリンクを作成する関数
function trademarkCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=334AC0000000127;article=";
  return `${baseUrl}${Number}`;
}

// 取得した数字を使って著作権法のAPIリンクを作成する関数
function copyrightCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=345AC0000000048;article=";
  return `${baseUrl}${Number}`;
}

// 取得した数字を使って不競法のAPIリンクを作成する関数
function competitionCreateApiLink(Number) {
  const baseUrl = "https://laws.e-gov.go.jp/api/1/articles;lawId=405AC0000000047;article=";
  return `${baseUrl}${Number}`;
}

// 全角数字を半角数字に変換する関数
function convertFullWidthToHalfWidth(fullWidthNumber) {
  return fullWidthNumber.replace(/[０-９]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xFEE0));
}
