package com.genspark.privacyfirstai.data.local

import androidx.room.TypeConverter

class AppTypeConverters {
    @TypeConverter
    fun fromStringList(values: List<String>): String = values.joinToString("||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("||")
}
