package com.example.smartexpense.data.remote.exchange

import com.example.smartexpense.model.ExchangeRateResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service phân hệ Tỷ giá (v1.4): đọc JSON online từ open.er-api.com
 * (API miễn phí, không cần key).
 */
interface ExchangeRateApi {

    /** GET https://open.er-api.com/v6/latest/USD -> tỷ giá mới nhất từ USD ra mọi mã. */
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): ExchangeRateResponse
}

/** Client Retrofit dùng chung (một instance cho cả app). */
object ExchangeRateClient {

    private const val BASE_URL = "https://open.er-api.com/"

    val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}
