package com.teleyah.photobooth.model

import java.io.File
import java.io.Serializable

data class PhotoCollage(
    val name: String,
    val path: File
) : Serializable
