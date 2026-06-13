package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.EventDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.TicketDao
import com.example.data.dao.UserDao
import com.example.data.model.EventEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EventEntity::class,
        TicketEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun ticketDao(): TicketDao
    abstract fun notificationDao(): NotificationDao
}
