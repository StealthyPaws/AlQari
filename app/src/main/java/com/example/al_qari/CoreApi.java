package com.example.al_qari;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CoreApi {
    @POST("/intent")
    Call<Outbound> classifyIntent(@Body Inbound body);

    @POST("/practice")
    Call<OutboundPractice> checkPractice(@Body InboundPractice body);

}
