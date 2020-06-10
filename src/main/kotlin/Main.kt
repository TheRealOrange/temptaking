import com.jessecorbett.diskord.dsl.bot
import java.io.File

val BOT_TOKEN = File("./token").readText()

suspend fun main() {
    bot(BOT_TOKEN){
        
    }
}