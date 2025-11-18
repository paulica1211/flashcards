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

    // Getters
    public String getFrontSide() { return frontSide; }
    public String getBackSide() { return backSide; }
    public int getCurrentNum() { return currentNum; }
    public int getTotalCards() { return totalCards; }
    public int getImportance() { return importance; }

    // Setters
    public void setFrontSide(String frontSide) { this.frontSide = frontSide; }
    public void setBackSide(String backSide) { this.backSide = backSide; }
    public void setCurrentNum(int currentNum) { this.currentNum = currentNum; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }
    public void setImportance(int importance) { this.importance = importance; }
}
