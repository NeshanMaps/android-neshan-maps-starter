package org.neshan.sample.starter.network;

import org.neshan.sample.starter.model.address.NeshanAddress;
import org.neshan.sample.starter.model.search.NeshanSearch;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Url;

public interface GetDataService {

    // TODO: replace "YOUR_API_KEY" with your api key
    @Headers("Api-Key: service.kREahwU7lND32ygT9ZgPFXbwjzzKukdObRZsnUAJ")
    @GET
    Call<NeshanAddress> getNeshanAddress(@Url String url);


    @Headers("Api-Key: service.PnRV9ocd8zm9QYYlJUNLJoAihE3hfy34WUZ6jcjr")
    @GET
    Call<NeshanSearch> getNeshanSearch(@Url String url);
}