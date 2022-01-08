package com.teleyah.photobooth.service

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.UserCredentials
import com.google.photos.library.v1.PhotosLibraryClient
import com.google.photos.library.v1.PhotosLibrarySettings
import com.google.photos.library.v1.proto.ShareAlbumResponse
import com.google.photos.types.proto.Album
import com.google.photos.types.proto.SharedAlbumOptions
import com.teleyah.photobooth.BuildConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.File
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePhotosService @Inject constructor() {

    @Inject
    lateinit var authService: GoogleAuthService

    @Inject
    lateinit var httpService: HttpService

    // TODO: inject dispatcher
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    companion object {
        const val clientId = BuildConfig.GOOGLE_PHOTOS_CLIENT_ID
        const val clientSecret = BuildConfig.GOOGLE_PHOTOS_CLIENT_SECRET

        val scopes = arrayOf(
            Scope("https://www.googleapis.com/auth/photoslibrary.readonly"),
            Scope("https://www.googleapis.com/auth/photoslibrary.sharing"),
            Scope("https://www.googleapis.com/auth/photoslibrary.appendonly")
        )
    }

    lateinit var accessToken: AccessToken
    lateinit var client: PhotosLibraryClient
    lateinit var album: Album
    lateinit var sharedAlbumUrl: String

    suspend fun initialize(account: GoogleSignInAccount, albumName: String) {
        withContext(ioDispatcher) {
            createClient(account)
            createSharedAlbum(albumName)
        }
    }

    suspend fun upload(fileToUpload: File) {
        withContext(ioDispatcher) {
            Timber.d("Starting photo upload ${fileToUpload.name}")
            if (!this@GooglePhotosService::client.isInitialized || !this@GooglePhotosService::album.isInitialized) {
                Timber.e("Photos service not initialized yet")
                return@withContext
            }

            val mimeType: String = URLConnection.guessContentTypeFromName(fileToUpload.name)

            val uploadToken: String =
                httpService.client.post("https://photoslibrary.googleapis.com/v1/uploads") {
                    headers {
                        append("X-Goog-Upload-Content-Type", mimeType)
                        append(HttpHeaders.Authorization, "Bearer ${accessToken.access_token}")
                        append("X-Goog-Upload-Protocol", "raw")
                    }
                    body = object : OutgoingContent.ReadChannelContent() {
                        override fun readFrom(): ByteReadChannel = fileToUpload.readChannel()
                    }
                }

            val items = BatchCreateMediaItemRequest(
                album.id,
                listOf(
                    NewMediaItem(
                        "Created by PhotoBooth app for ${album.title}",
                        SimpleMediaItem(fileToUpload.name, uploadToken)
                    )
                )
            )

            val createResponse: BatchCreateMediaItemResponse =
                httpService.client.post("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${accessToken.access_token}")
                    }
                    body = items
                }

            val result = createResponse.newMediaItemResults.first()

            Timber.d("Uploaded file ${result.mediaItem.filename} result ${result.status}")
        }
    }

    private fun createSharedAlbum(name: String) {
        album = findAlbum(name) ?: createAlbum(name)
        sharedAlbumUrl = album.shareInfo?.shareableUrl?.ifBlank { null } ?: album.share().shareInfo.shareableUrl
        Timber.i("Got shared album URL $sharedAlbumUrl")
    }

    private fun findAlbum(name: String): Album? =
        client.listAlbums().iterateAll().firstOrNull { it.title == name }

    private fun createAlbum(name: String): Album =
        client.createAlbum(name)

    private fun Album.share(): ShareAlbumResponse {
        val options =
            SharedAlbumOptions.newBuilder().setIsCollaborative(true).setIsCommentable(true).build()
        val response = client.shareAlbum(id, options)
        return response
    }

    private suspend fun createClient(account: GoogleSignInAccount) {
        Timber.d("Connecting to Google Photos")
        accessToken = authService.authorize(account, clientId, clientSecret)
        val credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setAccessToken(com.google.auth.oauth2.AccessToken(accessToken.access_token, null))
            .build()

        val photosClientSettings = PhotosLibrarySettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)
            )
            .build()
        client = PhotosLibraryClient.initialize(photosClientSettings)
    }
}

@Serializable
data class SimpleMediaItem(val fileName: String, val uploadToken: String)

@Serializable
data class NewMediaItem(val description: String, val simpleMediaItem: SimpleMediaItem)

@Serializable
data class BatchCreateMediaItemRequest(val albumId: String, val newMediaItems: List<NewMediaItem>)

@Serializable
data class BatchCreateMediaItemResponse(val newMediaItemResults: List<NewMediaItemResult>)

@Serializable
data class NewMediaItemResult(
    val uploadToken: String,
    val status: NewMediaItemStatus,
    val mediaItem: MediaItem
)

@Serializable
data class NewMediaItemStatus(val message: String, val code: Int? = null)

@Serializable
data class MediaItem(
    val id: String,
    val description: String,
    val productUrl: String,
    val mimeType: String,
    val filename: String
)
