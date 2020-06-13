import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.util.authorId
import config.Config
import database.Database
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import msforms.Form
import org.slf4j.LoggerFactory
import java.io.File

@UnstableDefault
val CONFIG = Json.parse(Config.serializer(),File("./app_config.json").readText())
val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
@UnstableDefault
lateinit var database: Database

@UnstableDefault
suspend fun main() {
    root.level = Level.INFO

    database = Database(File(CONFIG.userFile), CONFIG.polling)
    Form.setTimeout(CONFIG.waitTime.toLong())

    bot(CONFIG.token) {
        commands("$") {
            command("help-temptaking") {
                if (guildId != null) reply("Register with me via DM, and I can help you automatically submit your temperature every morning\nDM \$help-temptaking for more detail")
                else reply("Commands:\n```\$register [email] [password]```    use this command to register with the bot\n```\$deregister```    use this command to deregister")
            }

            command("register") {
                if (guildId != null) {
                    if (CONFIG.delete) this.delete()
                    reply("Please perform operations by DM")
                } else if (this.content.split(" ").size != 3) {
                    //this.delete()
                    reply("please use \$register [email] [password]")
                } else {
                    reply("Validating credentials")
                    //this.delete()
                    val params = this.content.split(" ")
                    val username = params[1]
                    val password = params[2]
                    if (!Form.verifyLogin(username, password))
                        reply("Invalid username and/or password, please try again")
                    else {
                        if (database.exists(authorId)) reply("You have already registered, type \$deregister to deregister.")
                        else {
                            database.register(authorId, username, password)
                            reply("You have been successfully registered")
                        }
                    }
                }
            }

            command("deregister") {
                if (guildId != null) {
                    if (CONFIG.delete) this.delete()
                    reply("Please perform operations by DM")
                } else {
                    if (database.exists(authorId)) {
                        database.deregister(authorId)
                        reply("You have successfully deregistered")
                    } else reply("You have not yet registered, use \$register [email] [password] to register")
                }
            }
        }
    }
}