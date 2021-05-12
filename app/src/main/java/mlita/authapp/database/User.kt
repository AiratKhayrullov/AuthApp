package mlita.authapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
data class User(
        @PrimaryKey(autoGenerate = false)
        var userName : String,
        var login : String,
        var hashCode : String,
        var salt : String
)