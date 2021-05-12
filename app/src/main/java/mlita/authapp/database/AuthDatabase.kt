package mlita.authapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import exams.mlita.authenticationapp.database.AuthDao

@Database(
    entities = [
        User::class
    ],
    version = 1
)
abstract class AuthDatabase : RoomDatabase() {

    abstract val authDao: AuthDao

    companion object {
        @Volatile
        private var INSTANCE: AuthDatabase? = null

        fun getInstance(context: Context): AuthDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuthDatabase::class.java,
                    "auth_db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }

}