package com.flashcardapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SheetList {
    @SerializedName("sheets")
    private List<String> sheets;

    public List<String> getSheets() {
        return sheets;
    }

    public void setSheets(List<String> sheets) {
        this.sheets = sheets;
    }
}
