package database

import database.data.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File

@UnstableDefault
class Database {
    lateinit var file: File
    val users = mutableMapOf<String, User>()

    fun init(file: File) {
        this.file = file
        loadData()
    }

    fun register(discordUsername: String, msEmail: String, msPassword: String) {
        users[discordUsername] = User(discordUsername, msEmail, msPassword)
    }

    fun loadData() {
        val data = file.readText()
        val saveUsers = Json.parse(Users.serializer(), data)
        saveUsers.users.forEach { users[it.discordUsername] = it }
    }

    fun saveData() {
        val saveUsers = Users(users.values.toList())
        val data = Json.stringify(Users.serializer(), saveUsers)
        file.writeText(data)
    }
}