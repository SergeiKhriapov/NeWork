package ru.netology.nework.data.api

import okhttp3.Interceptor
import okhttp3.Response
import ru.netology.nework.BuildConfig

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .addHeader("Api-Key", BuildConfig.API_KEY)
            .build()
        return chain.proceed(request)
    }
}