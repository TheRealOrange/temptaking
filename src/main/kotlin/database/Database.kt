package database

import database.data.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File

@UnstableDefault
class Database(f: File) {
    private var file = f
    private val users = mutableMapOf<String, User>()

    init {
        if (file.exists())
            println("Database file found")
        else {
            println("Database file not found, creating new file")
            f.createNewFile()
            saveData()
        }
        loadData()
    }

    fun exists(discordUsername: String) = users.containsKey(discordUsername)

    fun register(discordUsername: String, msEmail: String, msPassword: String) {
        users[discordUsername] = User(discordUsername, msEmail, msPassword)
    }

    fun deregister(discordUsername: String) = users.remove(discordUsername)

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