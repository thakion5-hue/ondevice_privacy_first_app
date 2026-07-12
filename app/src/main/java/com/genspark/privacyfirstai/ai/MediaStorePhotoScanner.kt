package com.genspark.privacyfirstai.ai

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.genspark.privacyfirstai.domain.model.DevicePhotoSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaStorePhotoScanner {
    fun scanRecent(context: Context, limit: Int = 80): List<DevicePhotoSnapshot> {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)
        val results = mutableListOf<DevicePhotoSnapshot>()

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val relativePathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext() && results.size < limit) {
                val id = cursor.getLong(idIndex)
                val displayName = cursor.getString(nameIndex) ?: "IMG_$id"
                val dateTaken = cursor.getLong(dateTakenIndex)
                val dateAddedMillis = cursor.getLong(dateAddedIndex) * 1000L
                val timestamp = if (dateTaken > 0L) dateTaken else dateAddedMillis
                val width = cursor.getInt(widthIndex)
                val height = cursor.getInt(heightIndex)
                val bucketName = cursor.getString(bucketIndex).orEmpty().ifBlank { "기기 사진" }
                val relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex).orEmpty() else ""
                val lowerName = listOf(displayName, bucketName, relativePath).joinToString(" ").lowercase()
                val isScreenshot = listOf("screenshot", "screen_shot", "캡처", "스크린샷", "screenrecord").any { lowerName.contains(it) }
                val portraitDoc = width > 0 && height > (width * 1.18f)
                val documentLike = portraitDoc || listOf("scan", "document", "문서", "invoice", "paper").any { lowerName.contains(it) }
                val receiptLike = documentLike && listOf("receipt", "영수증", "invoice", "결제").any { lowerName.contains(it) }
                val contentUri = ContentUris.withAppendedId(collection, id)
                val dateLabel = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter)

                results += DevicePhotoSnapshot(
                    id = id.toString(),
                    title = displayName,
                    album = bucketName,
                    dateLabel = dateLabel,
                    contentUri = contentUri.toString(),
                    width = width,
                    height = height,
                    sizeBytes = cursor.getLong(sizeIndex),
                    isScreenshot = isScreenshot,
                    documentLike = documentLike,
                    receiptLike = receiptLike
                )
            }
        }
        return results
    }
}
