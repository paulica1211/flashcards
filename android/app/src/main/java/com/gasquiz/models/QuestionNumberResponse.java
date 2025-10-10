package com.gasquiz.models;

import com.google.gson.annotations.SerializedName;

public class QuestionNumberResponse {
    @SerializedName("questionNumber")
    private int questionNumber;

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }
}
