package com.flashcardapp.models;

import com.google.gson.annotations.SerializedName;

public class Flashcard {
    @SerializedName("frontSide")
    private String frontSide;

    @SerializedName("backSide")
    private String backSide;

    @SerializedName("currentNum")
    private int currentNum;

    @SerializedName("totalCards")
    private int totalCards;

    @SerializedName("importance")
    private int importance; // 0-3 importance level

    @SerializedName("frontPronunciation")
    private String frontPronunciation; // D column - pronunciation for front side

    @SerializedName("backPronunciation")
    private String backPronunciation; // E column - pronunciation for back side

    // Getters
    public String getFrontSide() { return frontSide; }
    public String getBackSide() { return backSide; }
    public int getCurrentNum() { return currentNum; }
    public int getTotalCards() { return totalCards; }
    public int getImportance() { return importance; }
    public String getFrontPronunciation() { return frontPronunciation; }
    public String getBackPronunciation() { return backPronunciation; }

    // Setters
    public void setFrontSide(String frontSide) { this.frontSide = frontSide; }
    public void setBackSide(String backSide) { this.backSide = backSide; }
    public void setCurrentNum(int currentNum) { this.currentNum = currentNum; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }
    public void setImportance(int importance) { this.importance = importance; }
    public void setFrontPronunciation(String frontPronunciation) { this.frontPronunciation = frontPronunciation; }
    public void setBackPronunciation(String backPronunciation) { this.backPronunciation = backPronunciation; }
}
