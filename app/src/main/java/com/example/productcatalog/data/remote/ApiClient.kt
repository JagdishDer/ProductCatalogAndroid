
package com.example.productcatalog.data.remote

import com.example.productcatalog.data.security.SecureTokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(
    private val tokenStore: SecureTokenStore
) {

    val api: DummyJsonApi by lazy {

        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->

                val original = chain.request()
                val builder = original.newBuilder()

                if (
                    original.url.encodedPath != "/auth/login"
                ) {
                    tokenStore.readToken()?.let { token ->
                        builder.header(
                            "Authorization",
                            "Bearer $token"
                        )
                    }
                }

                chain.proceed(builder.build())
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://dummyjson.com/")
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(DummyJsonApi::class.java)
    }
}
