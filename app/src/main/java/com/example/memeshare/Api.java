package com.example.memeshare;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Api {
    String BASE_URL ="https://meme-api.herokuapp.com/";

    @GET("gimme/IndianMeyMeys")
    Call<Meme> getMeme();
}
