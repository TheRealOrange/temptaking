import ch.qos.logback.classic.Level
import com.jessecorbett.diskord.dsl.*
import ch.qos.logback.classic.Logger
import com.jessecorbett.diskord.util.*
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration

val BOT_TOKEN = File("./app_config").readText()
val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

suspend fun main() {
    root.level = Level.INFO
    bot(BOT_TOKEN){
        commands("$") {
            command("help-temptaking") {
                reply("hello")
            }

            command("register") {
                if (guildId != null) {
                    this.delete()
                    reply("please dm \$register to the bot instead")
                } else if (this.content.split(" ").size != 3) {
                    reply("please use \$register [email] [password]")
                } else {
                    val driver = ChromeDriver()
                    val wait = WebDriverWait(driver, Duration.ofSeconds(10))
                    try {
                        driver.get("https://google.com/ncr")
                        driver.findElement(By.name("q")).sendKeys("cheese" + Keys.ENTER)
                        val firstResult = wait.until(presenceOfElementLocated(By.cssSelector("h3>div")))
                        reply(firstResult.getAttribute("textContent"))
                    } finally {
                        driver.quit()
                    }
                }
            }
        }
    }
}