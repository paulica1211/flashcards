package com.gasquiz.network;

import com.gasquiz.models.Question;
import com.gasquiz.models.QuestionNumberResponse;
import com.gasquiz.models.StartingInfo;
import com.gasquiz.models.SheetNames;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface QuizApiService {

    @GET("exec")
    Call<StartingInfo> getStartingInfo(@Query("action") String action);

    @GET("exec")
    Call<Question> getQuestion(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("questionNumber") int questionNumber
    );

    @GET("exec")
    Call<Question> getNextQuestion(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("currentNum") int currentNum,
            @Query("importanceLevel") int importanceLevel
    );

    @GET("exec")
    Call<Question> getPreviousQuestion(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("currentNum") int currentNum,
            @Query("importanceLevel") int importanceLevel
    );

    @POST("exec")
    Call<Void> recordAnswer(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("questionNumber") int questionNumber,
            @Query("isCorrect") boolean isCorrect,
            @Query("elapsedTime") double elapsedTime
    );

    @POST("exec")
    Call<Void> markImportant(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("questionNumber") int questionNumber,
            @Query("importance") int importance
    );

    @GET("exec")
    Call<SheetNames> getSheetNames(@Query("action") String action);

    @GET("exec")
    Call<QuestionNumberResponse> getQuestionNumberForSheet(
            @Query("action") String action,
            @Query("sheetName") String sheetName
    );

    @POST("exec")
    Call<Void> saveSettings(
            @Query("action") String action,
            @Query("sheetName") String sheetName,
            @Query("questionNumber") int questionNumber
    );
}
