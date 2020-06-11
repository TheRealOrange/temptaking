package database.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val discordUsername: String,
    val msEmail: String,
    val msPassword:String
)