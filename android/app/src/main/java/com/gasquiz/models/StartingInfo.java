package com.gasquiz.models;

import com.google.gson.annotations.SerializedName;

public class StartingInfo {
    @SerializedName("questionNumber")
    private int questionNumber;

    @SerializedName("sheetName")
    private String sheetName;

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }
}
