package database.data

import kotlinx.serialization.Serializable

@Serializable
class Users(
    val users: List<User>
)