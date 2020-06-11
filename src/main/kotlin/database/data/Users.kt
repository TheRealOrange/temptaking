package database.data

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class Users(
    val users: List<User>
)