package com.teleyah.photobooth.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreService @Inject constructor(@ApplicationContext private val context: Context) {

    // TODO: inject dispatcher
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun insertImage(file: File, name: String, albumName: String? = null): Uri {

        return withContext(ioDispatcher) {
            runCatching {

                val resolver = context.contentResolver

                val imageCollection =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.getContentUri(
                            MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                val imageDetails = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    albumName?.let {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/$it"
                        )
                    }
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val contentUri = resolver.insert(imageCollection, imageDetails)!!


                resolver.openFileDescriptor(contentUri, "w", null).use { fd ->
                    ParcelFileDescriptor.AutoCloseOutputStream(fd).use { os ->
                        FileInputStream(file).use { it.copyTo(os) }
                    }
                }

                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(contentUri, imageDetails, null, null)

                contentUri
            }.getOrThrow()
        }
    }

}