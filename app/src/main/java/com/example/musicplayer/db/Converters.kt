package com.example.musicplayer.db

import android.net.Uri
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): Uri? = value?.let { Uri.parse(it) }

    @TypeConverter
    fun uriToString(uri: Uri?): String? = uri?.toString()
}