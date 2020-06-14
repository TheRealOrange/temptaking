package config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val bot_token: String,
    val user_file: String,
    val webdriver_wait_time: Int,
    val polling_rate: Int,
    val delete_msgs_in_server: Boolean,
    val notify_scheduled: Boolean,
    val notify_filled: Boolean,
    val randomise_time: Boolean,
    val UTC_offset_hrs: Int
)