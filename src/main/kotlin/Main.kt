import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
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
val CONFIG = Json.parse(Config.serializer(),File("./app_config.json").readText())
val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
@UnstableDefault
lateinit var database: Database

@UnstableDefault
suspend fun main() {
    root.level = Level.INFO

    bot(CONFIG.bot_token) {
        started {
            val DATE_FORMATTER = "EEE HH:mm:ss"
            database = Database(File(CONFIG.user_file), CONFIG.polling_rate, CONFIG.UTC_offset_hrs, CONFIG.randomise_time,
                notifyScheduled = { s, t ->
                    GlobalScope.launch {
                        val dm = clientStore.discord.createDM(CreateDM(s))
                        ChannelClient(CONFIG.bot_token, dm.id).sendMessage("Randomised temperature taking scheduled for ${DateTimeFormatter.ofPattern(DATE_FORMATTER).format(t)}")
                    }
                },
                notifyFilled = { s, t ->
                    GlobalScope.launch {
                        val dm = clientStore.discord.createDM(CreateDM(s))
                        ChannelClient(CONFIG.bot_token, dm.id).sendMessage("Temperature taken at ${DateTimeFormatter.ofPattern(DATE_FORMATTER).format(t)}")
                    }
                })
            Form.setTimeout(CONFIG.webdriver_wait_time.toLong())
        }
        commands("$") {
            command("help-temptaking") {
                root.info("command HELP-TEMPTAKING issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) reply("Register with me via DM, and I can help you automatically submit your temperature every morning\nDM \$help-temptaking for more detail")
                else reply("Commands:\n`\$register [email] [password]`        use this command to register with the bot\n`\$deregister`        use this command to deregister\n`\$settings help`        use this command to change settings")
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
                val FORMATTER = "EEE yyyy-MM-dd HH:mm:ss"
                reply("System time: ${DateTimeFormatter.ofPattern(FORMATTER).format(database.timeNow())}")
            }

            command("settings") {
                root.info("command SETTINGS issued by [${author.username}, ${authorId}] in guild ${if(guildId == null) "null" else guildId} text: $content")
                if (guildId != null) {
                    if (CONFIG.delete_msgs_in_server) this.delete()
                    reply("Please perform operations by DM")
                } else {
                    var helptext = "format: `\$settings [options] [values]`\n"
                    helptext += "\noptions:\n"
                    helptext += "     `help` to show this again\n"
                    helptext += "     `list` to show current settings\n"
                    helptext += "     `email enable|disable` to toggle email receipt on form fill\n"
                    helptext += "     `notify enable|disable` to toggle discord notifications on form fill\n"
                    if (words.size < 2)
                        reply(helptext)
                    when(words[1]) {
                        "help" -> reply(helptext)
                        "list" -> {
                            reply("email notifications ${ if(database.user(authorId)[0]!!) "enabled" else "disabled" } \n")
                            reply("discord notifications ${ if(database.user(authorId)[1]!!) "enabled" else "disabled" } \n")
                        }
                        "email" -> {
                            if (words.size < 3 || !(words[2] == "enable" || words[2] == "disable")) reply("invalid\n$helptext")
                            else {
                                database.setEmailReceipt(authorId, words[2] == "enable")
                                reply("email notifications ${ if(database.user(authorId)[0]!!) "enabled" else "disabled" } \n")
                            }
                        }
                        "notify" -> {
                            if (words.size < 3 || !(words[2] == "enable" || words[2] == "disable")) reply("invalid\n$helptext")
                            else {
                                database.setNotify(authorId, words[2] == "enable")
                                reply("discord notifications ${ if(database.user(authorId)[1]!!) "enabled" else "disabled" } \n")
                            }
                        }
                        else -> reply("invalid\n$helptext")
                    }
                }
            }
        }
    }
}