import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.logback.LoggingAppender
import com.google.cloud.logging.logback.LoggingEventEnhancer
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.api.rest.client.ChannelClient
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import config.Config
import database.Database
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import msforms.Form
import org.slf4j.LoggerFactory
import java.io.File
import java.time.format.DateTimeFormatter

@UnstableDefault
val CONFIG = Json.parse(Config.serializer(),File("data/app_config.json").readText())
val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
@UnstableDefault
lateinit var database: Database

@UnstableDefault
suspend fun main() {
    root.level = Level.INFO
    val dateFormatter = "EEE yyyy-MM-dd HH:mm:ss"
    val timeFormatter = "EEE HH:mm:ss"

    if (CONFIG.google_cloud_logging) {
        val googleLogger = LoggingAppender()
        googleLogger.context = root.loggerContext
        googleLogger.addLoggingEventEnhancer(LoggingEventEnhancer { builder: LogEntry.Builder, event: ILoggingEvent ->
            builder.addLabel("thread", event.threadName)
        }.toString())
        googleLogger.setFlushLevel(Level.INFO)
        googleLogger.setLog("temptaking.log")
        googleLogger.start()
        root.addAppender(googleLogger)
    }

    var started = false

    bot(CONFIG.bot_token) {
        started {
            if (!started) {
                started = true
                database = Database(File(CONFIG.user_file), CONFIG.polling_rate, CONFIG.UTC_offset_hrs, CONFIG.randomise_time,
                    notifyScheduled = { s, t ->
                        GlobalScope.launch {
                            val dm = clientStore.discord.createDM(CreateDM(s))
                            ChannelClient(CONFIG.bot_token, dm.id).sendMessage("Randomised temperature taking scheduled for ${DateTimeFormatter.ofPattern(timeFormatter).format(t)}")
                            root.info("NOTIFY user [${dm.id}] temperature taking scheduled")
                        }
                    },
                    notifyFilled = { s, t ->
                        GlobalScope.launch {
                            val dm = clientStore.discord.createDM(CreateDM(s))
                            ChannelClient(CONFIG.bot_token, dm.id).sendMessage("Temperature taken at ${DateTimeFormatter.ofPattern(timeFormatter).format(t)}")
                            root.info("NOTIFY user [${dm.id}] temperature taking task executed")
                        }
                    },
                    fillFailed = { s, t ->
                        GlobalScope.launch {
                            val dm = clientStore.discord.createDM(CreateDM(s))
                            ChannelClient(CONFIG.bot_token, dm.id).sendMessage("Error FAILED to fill form at ${DateTimeFormatter.ofPattern(timeFormatter).format(t)}, please fill in the form yourself\n${Form.url}")
                            root.info("NOTIFY user [${dm.id}] temperature taking task FAILED")
                        }
                    })
                Form.setTimeout(CONFIG.webdriver_wait_time.toLong())
            }
        }
        commands("$") {
            command("help") {
                root.info("command HELP-TEMPTAKING issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) reply("Register with me via DM, and I can help you automatically submit your temperature every morning\nDM \$help-temptaking for more detail")
                else reply("Commands:\n`\$register [email] [password]`        use this command to register with the bot\n`\$deregister`        use this command to deregister\n`\$settings help`        use this command to change settings\n`\$task help`        use this command to manage active task")
            }

            command("register") {
                root.info("command REGISTER issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) {
                    if (CONFIG.delete_msgs_in_server) this.delete()
                    reply("Please perform operations by DM")
                } else if (words.size != 3) {
                    //this.delete()
                    reply("please use `\$register [email] [password]`")
                } else {
                    reply("Validating credentials")
                    //this.delete()
                    val params = words
                    val username = params[1]
                    val password = params[2]
                    if (!Form.verifyLogin(username, password))
                        reply("Invalid username and/or password, please try again")
                    else {
                        if (database.exists(authorId)) reply("You have already registered, type `\$deregister` to deregister.")
                        else {
                            database.register(authorId, username, password)
                            reply("You have been successfully registered\n")
                            reply("email notifications ${ if(database.user(authorId)[0]!!) "enabled" else "disabled" } \n")
                            reply("discord notifications ${ if(database.user(authorId)[1]!!) "enabled" else "disabled" } \n")
                            reply("\nuse `\$settings help` to find out more about settings")
                        }
                    }
                }
            }

            command("deregister") {
                root.info("command DEREGISTER issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) {
                    if (CONFIG.delete_msgs_in_server) this.delete()
                    reply("Please perform operations by DM")
                } else {
                    if (database.exists(authorId)) {
                        database.deregister(authorId)
                        reply("You have successfully deregistered")
                    } else reply("You have not yet registered, use `\$register [email] [password]` to register")
                }
            }

            command("time") {
                root.info("command TIME issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                reply("System time: ${DateTimeFormatter.ofPattern(dateFormatter).format(database.timeNow())}")
            }

            command("settings") {
                root.info("command SETTINGS issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) {
                    if (CONFIG.delete_msgs_in_server) this.delete()
                    reply("Please perform operations by DM")
                } else {
                    if (database.exists(authorId)) {
                        var helptext = "format: `\$settings [options] [values]`\n"
                        helptext += "\noptions:\n"
                        helptext += "     `help` to show this again\n"
                        helptext += "     `list` to show current settings\n"
                        helptext += "     `email enable|disable` to toggle email receipt on form fill\n"
                        helptext += "     `notify enable|disable` to toggle discord notifications on form fill\n"
                        if (words.size < 2)
                            reply(helptext)
                        else if (words.size > 1) {
                            when (words[1]) {
                                "help" -> reply(helptext)
                                "list" -> {
                                    reply("email notifications ${if (database.user(authorId)[0]!!) "enabled" else "disabled"} \n")
                                    reply("discord notifications ${if (database.user(authorId)[1]!!) "enabled" else "disabled"} \n")
                                }
                                "email" -> {
                                    if (words.size < 3 || !(words[2] == "enable" || words[2] == "disable")) reply("invalid\n$helptext")
                                    else {
                                        database.setEmailReceipt(authorId, words[2] == "enable")
                                        reply("email notifications ${if (database.user(authorId)[0]!!) "enabled" else "disabled"} \n")
                                    }
                                }
                                "notify" -> {
                                    if (words.size < 3 || !(words[2] == "enable" || words[2] == "disable")) reply("invalid\n$helptext")
                                    else {
                                        database.setNotify(authorId, words[2] == "enable")
                                        reply("discord notifications ${if (database.user(authorId)[1]!!) "enabled" else "disabled"} \n")
                                    }
                                }
                                else -> reply("invalid\n$helptext")
                            }
                        } else reply("invalid\n$helptext")
                    } else reply("You have not yet registered, use `\$register [email] [password]` to register")
                }
            }

            command("task") {
                root.info("command TASK issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) {
                    if (CONFIG.delete_msgs_in_server) this.delete()
                    reply("Please perform operations by DM")
                } else {
                    if (database.exists(authorId)) {
                        var helptext = "format: `\$task [options]`\n"
                        helptext += "\noptions:\n"
                        helptext += "     `help` to show this again\n"
                        helptext += "     `show` to show current settings\n"
                        helptext += "     `cancel` to cancel the current task\n"
                        if (words.size < 2)
                            reply(helptext)
                        else if (words.size > 1) {
                            when (words[1]) {
                                "help" -> reply(helptext)
                                "show" -> {
                                    val t = database.task(authorId)
                                    if (t != null) {
                                        reply("You have a temperature taking task scheduled for ${DateTimeFormatter.ofPattern(timeFormatter).format(t)}")
                                        if (t.isAfter(database.timeNow())) reply("The temperature taking task has been executed")
                                        else reply("Use `\$task cancel` to cancel")
                                    } else reply("You have no temperature taking task scheduled")
                                }
                                "cancel" -> {
                                    val t = database.task(authorId)
                                    if (t != null) {
                                        if (t.isAfter(database.timeNow())) reply("The temperature taking task has already been executed")
                                        else {
                                            database.cancel(authorId)
                                            reply("The temperature taking task scheduled for ${DateTimeFormatter.ofPattern(timeFormatter).format(t)} has been cancelled")
                                        }
                                    } else reply("You have no temperature taking task scheduled")
                                }
                                else -> reply("invalid\n$helptext")
                            }
                        } else reply("invalid\n$helptext")
                    } else reply("You have not yet registered, use `\$register [email] [password]` to register")
                }
            }
        }
    }
}