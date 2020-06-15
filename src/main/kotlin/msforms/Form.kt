package msforms

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import root
import java.time.Duration

object Form {
    val url = "https://forms.office.com/Pages/ResponsePage.aspx?id=cnEq1_jViUiahddCR1FZKi_YUnieBUBCi4vce5KjIHVUMkoxVUdBMVo2VUJTNFlSU1dFNEtNWUwxNS4u"

    val userNameField = "//*[@id=\"i0116\"]"
    val passwordField = "//*[@id=\"i0118\"]"
    val dontStaySignedIn = "//*[@id=\"idBtn_Back\"]"

    val usernameWait = "//*[@id=\"displayName\"]"

    val temperatureField = "//*[@id=\"form-container\"]/div/div/div[1]/div/div[1]/div[2]/div[2]/div/div[2]/div[3]/div/div/div/input"
    val sendEmailReceipt = "//*[@id=\"form-container\"]/div/div/div[1]/div/div[1]/div[2]/div[3]/div/div/label/input"
    val submitButton = "//*[@id=\"form-container\"]/div/div/div[1]/div/div[1]/div[2]/div[4]/div/button/div"

    val options = ChromeOptions()

    var waitTime: Long = 5

    init {
        options.addArguments(
            "--headless",
            "--disable-gpu",
            "--window-size=1920,1200",
            "--disable-extensions",
            "--no-sandbox",
            "--whitelisted-ip"
        )
    }

    fun setTimeout(time: Long) {
        waitTime = time
    }

    fun verifyLogin(userName: String, password:String): Boolean {
        val driver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(waitTime))
        var valid = false
        try {
            root.info("validating user [user $userName] connecting")
            driver.get(url)
            val usernameBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(userNameField)))
            usernameBox.sendKeys(userName + Keys.ENTER)
            root.info("validating user [user $userName] username filled")

            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.xpath(usernameWait), userName))
            val passwordBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(passwordField)))
            passwordBox.sendKeys(password + Keys.ENTER)
            root.info("validating user [user $userName] password filled")

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(dontStaySignedIn))).click()

            val tempInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(temperatureField)))
            root.info("validating user [user $userName] validated")
            valid = true
        } catch(e: TimeoutException) {
            root.info("validating user [user $userName] failed to validate")
            valid = false
        } finally {
            driver.quit()
        }
        return valid
    }

    fun fillForm(userName: String, password:String, temp: Float, email: Boolean) {
        val driver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(waitTime))
        try {
            root.info("filling form [user $userName] connecting")
            driver.get(url)
            val usernameBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(userNameField)))
            usernameBox.sendKeys(userName + Keys.ENTER)
            root.info("filling form [user $userName] username filled")

            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.xpath(usernameWait), userName))
            val passwordBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(passwordField)))
            passwordBox.sendKeys(password + Keys.ENTER)
            root.info("filling form [user $userName] password filled")

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(dontStaySignedIn))).click()

            val tempInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(temperatureField)))
            val sendReceipt = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(sendEmailReceipt)))
            val submitForm = wait.until(ExpectedConditions.presenceOfElementLocated((By.xpath(submitButton))))
            root.info("filling form [user $userName] form loaded")

            tempInput.sendKeys(String.format("%.1f", temp))
            if (email) sendReceipt.click()
            submitForm.click()
            root.info("filling form [user $userName] done")
        } finally {
            driver.quit()
        }
    }
}