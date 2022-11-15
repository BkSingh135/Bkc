package com.bkc

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by Sonali on 8-Nov-22.
 */
class RetrofitClient {
    var client: OkHttpClient? = null

    /**
     * prepare api client setup for one time only.
     *
     * @return api service.
     */
    val apiService: ApiService
        get() {
            if (retrofit == null) {
                    retrofit = getClient()?.let {
                    Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(it)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build()
                }
            }
            return retrofit!!.create(ApiService::class.java)
        }

    /**
     * prepared custom client here.
     *
     * @return OkHttpClient.
     */
    @JvmName("getClient1")
    fun getClient(): OkHttpClient? {
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
               /* if (!NetworkUtils.isOnline(MyApplication.context!!)) {
                    throw NoConnectivityException()
                }*/
                val original = chain.request()
                val request = original.newBuilder() //.header("CLIENT-OS", "ANDROID")
                        .method(original.method, original.body)
                        .build()
                chain.proceed(request)
            })
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        return client
    }

    /**
     * get interceptor here.
     *
     * @return Interceptor.
     */
    private val interceptor: Interceptor
        get() {
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            return interceptor
        }

    companion object {
        // Main Link
        const val discussion_image_URL = "http://103.205.64.197/fs/post/"
        const val NEWS_image_URL = "http://103.205.64.197/forecast/appnews/"
        const val NEWS_image_URL2 = "http://103.205.64.197/forecast/appadvice/"

        //Live Base Url
        const val BASE_URL = "http://103.205.64.197/fasalsalahgold/"
      //  const val BASE_URL = "http://192.168.101.105:80/sidsapi/"

        //
        //public static final String BASE_URL = "http://192.168.101.232:8085/AgroAdvisoryWebServices/gateWay/userNewSubscription1/mandi/8777305878/";
        // Development or Local link(Aakash Sir)
        // public static final String BASE_URL = "http://192.168.101.232:8085/AgroAdvisoryWebServices/";
        //For testing of recent app (it is not uploaded on Play store)
        // public static final String BASE_URL = "http://192.168.101.105/AgroAdvisoryWebServicesxyz/";
        //  public static final String BASE_URL = "http://103.62.239.77/AgroAdvisoryWebServicesxyz/";
        private var retrofit: Retrofit? = null
        val instance = RetrofitClient()
    }
}