package com.flashcardapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlashcardBatch {
    @SerializedName("cards")
    private List<Flashcard> cards;

    public List<Flashcard> getCards() {
        return cards;
    }

    public void setCards(List<Flashcard> cards) {
        this.cards = cards;
    }
}
