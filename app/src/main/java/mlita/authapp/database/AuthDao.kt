package exams.mlita.authenticationapp.database
import androidx.room.*
import mlita.authapp.database.User

@Dao
interface AuthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Transaction
    @Query("SELECT * FROM user")
    suspend fun getUsers(): List<User>
}