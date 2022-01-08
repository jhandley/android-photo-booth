package com.teleyah.photobooth.service

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AccessToken(
    val access_token: String,
    val expires_in: Int,
    val id_token: String,
    val token_type: String
)

@Singleton
class GoogleAuthService @Inject constructor() {

    @Inject
    lateinit var httpService: HttpService

    suspend fun authorize(
        account: GoogleSignInAccount,
        clientId: String,
        clientSecret: String
    ): AccessToken =
        httpService.client.submitForm(
            url = "https://www.googleapis.com/oauth2/v4/token",
            formParameters = Parameters.build {
                append("code", account.serverAuthCode!!)
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("redirect_uri", "")
                append("grant_type", "authorization_code")
            },
            encodeInQuery = false
        )
}

