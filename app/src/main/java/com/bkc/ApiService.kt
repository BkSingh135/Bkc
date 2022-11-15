package com.bkc

import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("editdetails/appbanner/{languageId}")
    fun poster(@Path("languageId") languageId: String?): Call<ArrayList<BanModel>>

    @GET("main/getTicker")
    fun getTicker(): Call<Ticker>

    /*val poster:*/

    /*@GET("news2/jobs/{phoneNo}/{languageId}/{jobType}")
    fun getJobs(
        @Path("phoneNo") mobile: String?,
        @Path("languageId") languageId: String?,
        @Path("jobType") jobType: String?
    ): Observable<ArrayList<Job?>?>?*/
}