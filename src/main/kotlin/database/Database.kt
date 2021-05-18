package database

import database.data.User
import database.data.Users
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import msforms.Form
import root
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.random.Random

@UnstableDefault
class Database(f: File, minutes: Int, offset: Int, randomise:Boolean, notifyScheduled: ((discordId: String, time: LocalDateTime)->Unit) = { _, _ -> }, notifyFilled: ((discordId: String, time: LocalDateTime)->Unit) = { _, _ -> }, fillFailed: ((discordId: String, time: LocalDateTime)->Unit) = { _, _ -> }) {
    private var file = f
    private val users = mutableMapOf<String, User>()
    private var time: Long = (minutes*60*1000).toLong()
    private var taken = false

    private var zoneOffset = ZoneOffset.ofHours(offset)

    private val randomTime = randomise

    private val scheduled = notifyScheduled
    private val filled = notifyFilled
    private val failed = fillFailed

    private val scheduledTasks = mutableMapOf<String, Pair<Timer?, LocalDateTime?>>()

    init {
        if (file.exists())
            root.info("Database file found")
        else {
            root.info("Database file not found, creating new file")
            f.createNewFile()
            saveData()
        }
        loadData()

        Timer().scheduleAtFixedRate(timerTask {
            root.info("checking temperature task")
            val now = timeNow()
            root.info("-> current hour ${now.hour}, taken today $taken")
            if (now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY) {
                if (now.hour == 6 && !taken) {
                    root.info("assigning tasks")
                    scheduledTasks.clear()
                    taken = true
                    val remaining = 120 - now.minute.toLong()
                    users.forEach {
                        root.info("assigning user: ${it.value.discordUsername}")
                        var delay = 5000L
                        if (randomTime) delay = Random.nextLong(0, remaining) * 60 * 1000
                        if (it.value.notify) scheduled.invoke(it.value.discordUsername, now.plusSeconds(delay/1000))
                        scheduleTask(it.value, delay, now)
                    }
                } else if (now.hour > 9) taken = false
            }
        },10000,time)
    }
    fun scheduleTask(user: User, delay: Long, now: LocalDateTime) {
        val t = Timer()
        t.schedule(timerTask {
            val success = Form.fillForm(
                user.msEmail,
                user.msPassword,
                Random.nextInt(360, 370).toFloat() / 10,
                user.emailReceipt
            )
            if (user.notify) filled.invoke(user.discordUsername, timeNow())
            if (!success) failed.invoke(user.discordUsername, timeNow())
        }, delay)
        scheduledTasks[user.discordUsername]= Pair(t, now.plusSeconds(delay/1000))
    }

    fun scheduleTask(discordUsername: String, delay: Long, now: LocalDateTime) {
        users[discordUsername]?.let { scheduleTask(it, delay, now) }
    }

    fun timeNow(): LocalDateTime = LocalDateTime.now().atZone(ZoneOffset.UTC).withZoneSameInstant(zoneOffset).toLocalDateTime()

    fun exists(discordUsername: String) = users.containsKey(discordUsername)

    fun register(discordUsername: String, msEmail: String, msPassword: String) {
        users[discordUsername] = User(discordUsername, msEmail, msPassword, emailReceipt = true, notify = false)
        saveData()
    }

    fun deregister(discordUsername: String) {
        users.remove(discordUsername)
        saveData()
    }

    fun user(discordUsername: String) = listOf(users[discordUsername]?.emailReceipt, users[discordUsername]?.notify)

    fun setEmailReceipt(discordUsername: String, enable: Boolean) {
        users[discordUsername]?.emailReceipt  = enable
        saveData()
    }

    fun setNotify(discordUsername: String, enable: Boolean) {
        users[discordUsername]?.notify  = enable
        saveData()
    }

    fun task(discordUsername: String) = scheduledTasks[discordUsername]?.second

    fun cancel(discordUsername: String) {
        scheduledTasks[discordUsername]?.first?.cancel()
        scheduledTasks[discordUsername] = Pair(null, null)
    }

    private fun loadData() {
        val data = file.readText()
        val saveUsers = Json.parse(Users.serializer(), data)
        saveUsers.users.forEach { users[it.discordUsername] = it }
    }

    private fun saveData() {
        val saveUsers = Users(users.values.toList())
        val data = Json.stringify(Users.serializer(), saveUsers)
        file.writeText(data)
    }
}