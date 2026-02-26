package ru.netology.nework.data.api

import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log
import ru.netology.nework.data.datastore.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthInterceptor"

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenManager.token.value
        val requestBuilder = original.newBuilder()

        if (!token.isNullOrBlank() &&
            !original.url.encodedPath.contains("/authentication") &&
            !original.url.encodedPath.contains("/registration")
        ) {
            requestBuilder.addHeader("Authorization", token) // просто токен, без Bearer
            Log.d(TAG, "Added token (no Bearer) for ${original.url}")
        }

        return chain.proceed(requestBuilder.build())
    }
}