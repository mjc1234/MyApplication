package com.mjc.core.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mjc.core.database.dao.UserDao
import com.mjc.core.database.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
