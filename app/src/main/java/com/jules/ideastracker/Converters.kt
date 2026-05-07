package com.jules.ideastracker

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: IdeaStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): IdeaStatus {
        return IdeaStatus.valueOf(value)
    }
}
