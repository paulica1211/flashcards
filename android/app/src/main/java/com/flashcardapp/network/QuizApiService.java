package com.flashcardapp.network;

import com.flashcardapp.models.Flashcard;
import com.flashcardapp.models.FlashcardBatch;
import com.flashcardapp.models.SheetList;
import com.flashcardapp.models.StartingInfo;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface QuizApiService {

    // Flashcard API endpoints
    @GET("exec")
    Call<Flashcard> getFlashcard(
            @Query("action") String action,
            @Query("cardNumber") int cardNumber,
            @Query("sheetName") String sheetName
    );

    @GET("exec")
    Call<FlashcardBatch> getFlashcardBatch(
            @Query("action") String action,
            @Query("startCard") int startCard,
            @Query("count") int count,
            @Query("sheetName") String sheetName
    );

    @GET("exec")
    Call<StartingInfo> getFlashcardStartingInfo(
            @Query("action") String action,
            @Query("sheetName") String sheetName
    );

    @GET("exec")
    Call<SheetList> getAvailableSheets(@Query("action") String action);

    @POST("exec")
    Call<JsonObject> saveImportance(
            @Query("action") String action,
            @Query("cardNumber") int cardNumber,
            @Query("importance") int importance,
            @Query("sheetName") String sheetName
    );

    @POST("exec")
    Call<JsonObject> saveProgress(
            @Query("action") String action,
            @Query("cardNumber") int cardNumber,
            @Query("sheetName") String sheetName
    );
}
