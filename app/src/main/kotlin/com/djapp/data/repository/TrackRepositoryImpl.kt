package com.djapp.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.djapp.domain.model.Track
import com.djapp.domain.repository.TrackRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-103: TrackRepositoryの実装。Android MediaStoreから音楽ファイルを取得する。
 * 対応フォーマット: MP3 / AAC(M4A) / FLAC / WAV / OGG。
 * UC-008 対応。
 */
@Singleton
class TrackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TrackRepository {

    /** MediaStoreから取得するカラム定義。 */
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.MIME_TYPE,
    )

    /** 対応MIMEタイプ（UC-008 補足より）。 */
    private val supportedMimeTypes = setOf(
        "audio/mpeg",                   // MP3
        "audio/mp4", "audio/m4a",      // AAC / M4A
        "audio/flac",                  // FLAC
        "audio/wav", "audio/x-wav",    // WAV
        "audio/ogg", "audio/vorbis",   // OGG
    )

    private val audioCollection: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    override fun getAllTracks(): Flow<List<Track>> = flow {
        emit(queryMediaStore(selection = null, selectionArgs = null))
    }.flowOn(Dispatchers.IO)

    override fun searchTracks(query: String): Flow<List<Track>> = flow {
        if (query.isBlank()) {
            emit(queryMediaStore(null, null))
        } else {
            val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                    "${MediaStore.Audio.Media.ARTIST} LIKE ?"
            val args = arrayOf("$query%", "$query%")
            emit(queryMediaStore(selection, args))
        }
    }.flowOn(Dispatchers.IO)

    private fun queryMediaStore(
        selection: String?,
        selectionArgs: Array<String>?,
    ): List<Track> {
        val tracks = mutableListOf<Track>()

        context.contentResolver.query(
            audioCollection,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeCol) ?: continue
                if (mimeType !in supportedMimeTypes) continue

                val id = cursor.getLong(idCol)
                // content URI を filePath として使用（Android 10+ でファイルパス直接アクセス不可のため）
                val contentUri = ContentUris.withAppendedId(audioCollection, id).toString()

                tracks += Track(
                    id         = id,
                    title      = cursor.getString(titleCol)  ?: "Unknown Title",
                    artist     = cursor.getString(artistCol) ?: "Unknown Artist",
                    album      = cursor.getString(albumCol)  ?: "Unknown Album",
                    durationMs = cursor.getLong(durationCol),
                    filePath   = contentUri,
                )
            }
        }

        return tracks
    }
}
