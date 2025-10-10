package com.gasquiz.models;

import com.google.gson.annotations.SerializedName;

public class Question {
    @SerializedName("article")
    private String article;

    @SerializedName("question")
    private String question;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("answer")
    private String answer;

    @SerializedName("timeToAnswer")
    private String timeToAnswer;

    @SerializedName("correctCount")
    private String correctCount;

    @SerializedName("incorrectCount")
    private String incorrectCount;

    @SerializedName("correctRate")
    private String correctRate;

    @SerializedName("ofImportance")
    private String ofImportance;

    @SerializedName("questionYear")
    private String questionYear;

    @SerializedName("linkM")
    private String linkM;

    @SerializedName("linkN")
    private String linkN;

    @SerializedName("linkO")
    private String linkO;

    @SerializedName("linkP")
    private String linkP;

    @SerializedName("art1")
    private String art1;

    @SerializedName("art2")
    private String art2;

    @SerializedName("art3")
    private String art3;

    @SerializedName("allQuestion")
    private int allQuestion;

    @SerializedName("currentNum")
    private int currentNum;

    // Getters
    public String getArticle() { return article; }
    public String getQuestion() { return question; }
    public String getExplanation() { return explanation; }
    public String getAnswer() { return answer; }
    public String getTimeToAnswer() { return timeToAnswer; }
    public String getCorrectCount() { return correctCount; }
    public String getIncorrectCount() { return incorrectCount; }
    public String getCorrectRate() { return correctRate; }
    public String getOfImportance() { return ofImportance; }

    // Helper methods to get numeric values with defaults
    public int getOfImportanceInt() {
        if (ofImportance == null || ofImportance.isEmpty()) return 0;
        try { return Integer.parseInt(ofImportance); }
        catch (NumberFormatException e) { return 0; }
    }
    public String getQuestionYear() { return questionYear; }
    public String getLinkM() { return linkM; }
    public String getLinkN() { return linkN; }
    public String getLinkO() { return linkO; }
    public String getLinkP() { return linkP; }
    public String getArt1() { return art1; }
    public String getArt2() { return art2; }
    public String getArt3() { return art3; }
    public int getAllQuestion() { return allQuestion; }
    public int getCurrentNum() { return currentNum; }

    // Setters
    public void setArticle(String article) { this.article = article; }
    public void setQuestion(String question) { this.question = question; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setTimeToAnswer(String timeToAnswer) { this.timeToAnswer = timeToAnswer; }
    public void setCorrectCount(String correctCount) { this.correctCount = correctCount; }
    public void setIncorrectCount(String incorrectCount) { this.incorrectCount = incorrectCount; }
    public void setCorrectRate(String correctRate) { this.correctRate = correctRate; }
    public void setOfImportance(String ofImportance) { this.ofImportance = ofImportance; }
    public void setQuestionYear(String questionYear) { this.questionYear = questionYear; }
    public void setLinkM(String linkM) { this.linkM = linkM; }
    public void setLinkN(String linkN) { this.linkN = linkN; }
    public void setLinkO(String linkO) { this.linkO = linkO; }
    public void setLinkP(String linkP) { this.linkP = linkP; }
    public void setArt1(String art1) { this.art1 = art1; }
    public void setArt2(String art2) { this.art2 = art2; }
    public void setArt3(String art3) { this.art3 = art3; }
    public void setAllQuestion(int allQuestion) { this.allQuestion = allQuestion; }
    public void setCurrentNum(int currentNum) { this.currentNum = currentNum; }
}
