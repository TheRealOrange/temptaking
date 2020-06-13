package config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val token: String,
    val userFile: String,
    val waitTime: Int,
    val polling: Int,
    val delete: Boolean
)