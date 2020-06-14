package database

import database.data.User
import database.data.Users
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import msforms.Form
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.random.Random

@UnstableDefault
class Database(f: File, minutes: Int, offset: Int) {
    private var file = f
    private val users = mutableMapOf<String, User>()
    private var time: Long = (minutes*60*1000).toLong()
    private var taken = false
    private var zoneOffset = ZoneOffset.ofHours(offset)

    init {
        if (file.exists())
            println("Database file found")
        else {
            println("Database file not found, creating new file")
            f.createNewFile()
            saveData()
        }
        loadData()

        Timer().scheduleAtFixedRate(timerTask {
            val now = timeNow()
            if (now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY) {
                if (LocalDateTime.now().hour == 6 && !taken) {
                    taken = true
                    val remaining = 120 - now.minute.toLong()
                    users.forEach {
                        val delay = Random.nextLong(0, remaining) * 60 * 1000
                        Timer().schedule(timerTask {
                            Form.fillForm(
                                it.value.msEmail,
                                it.value.msPassword,
                                Random.nextInt(360, 370).toFloat() / 10
                            )
                        }, delay)
                    }
                } else if (now.hour > 9) taken = false
            }
        },2000,time)
    }

    fun timeNow() = LocalDateTime.now().atZone(ZoneOffset.UTC).withZoneSameInstant(zoneOffset).toLocalDateTime()

    fun exists(discordUsername: String) = users.containsKey(discordUsername)

    fun register(discordUsername: String, msEmail: String, msPassword: String) {
        users[discordUsername] = User(discordUsername, msEmail, msPassword)
        saveData()
    }

    fun deregister(discordUsername: String) {
        users.remove(discordUsername)
        saveData()
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