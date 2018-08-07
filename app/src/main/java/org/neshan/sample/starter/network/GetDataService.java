package org.neshan.sample.starter.network;

import org.neshan.sample.starter.model.NeshanAddress;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Url;


public interface GetDataService {

    // TODO: replace "YOUR_API_KEY" with your api key
    @Headers("Api-Key: YOUR_API_KEY")
    @GET
    Call<NeshanAddress> getNeshanAddress(@Url String url);
}