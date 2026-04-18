package com.app.mitvplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.app.mitvplayer.data.dao.ChannelDao
import com.app.mitvplayer.data.dao.EpgDao
import com.app.mitvplayer.data.dao.FavoriteDao
import com.app.mitvplayer.data.dao.ParentalDao
import com.app.mitvplayer.data.dao.PlaylistDao
import com.app.mitvplayer.data.dao.XtreamDao
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.data.models.EpgProgram
import com.app.mitvplayer.data.models.Favorite
import com.app.mitvplayer.data.models.ParentalLock
import com.app.mitvplayer.data.models.Playlist
import com.app.mitvplayer.data.models.XtreamAccount

@Database(
    entities = [
        Playlist::class,
        Channel::class,
        XtreamAccount::class,
        EpgProgram::class,
        Favorite::class,
        ParentalLock::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun xtreamDao(): XtreamDao
    abstract fun epgDao(): EpgDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun parentalDao(): ParentalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mitvplayer_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
