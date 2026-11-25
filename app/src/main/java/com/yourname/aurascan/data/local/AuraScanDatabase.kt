package com.yourname.aurascan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

class AuraScanConverters {
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }
}

@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AuraScanConverters::class)
abstract class AuraScanDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AuraScanDatabase? = null

        fun getInstance(context: Context): AuraScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraScanDatabase::class.java,
                    "aurascan.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
