package com.gasquiz.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SheetNames {
    @SerializedName("sheetNames")
    private List<String> sheetNames;

    public List<String> getSheetNames() {
        return sheetNames;
    }

    public void setSheetNames(List<String> sheetNames) {
        this.sheetNames = sheetNames;
    }
}
