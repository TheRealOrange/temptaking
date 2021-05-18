package msforms

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait
import root
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException


object Form {
    val url = "https://forms.office.com/Pages/ResponsePage.aspx?id=cnEq1_jViUiahddCR1FZKi_YUnieBUBCi4vce5KjIHVUMkoxVUdBMVo2VUJTNFlSU1dFNEtNWUwxNS4u"

    val userNameField = "//*[@id=\"i0116\"]"
    val passwordField = "//*[@id=\"i0118\"]"
    val dontStaySignedIn = "//*[@id=\"idBtn_Back\"]"

    val usernameWait = "//*[@id=\"displayName\"]"

    val temperatureField = "//*[@id=\"form-container\"]//input[@aria-labelledby=\"QuestionId_r97220fafc5db4128a434b19ba048f666\"]"
    val sendEmailReceipt = "//*[@id=\"form-container\"]//span[@class=\"office-form-email-receipt-checkbox-description\"]"
    val submitButton = "//*[@id=\"form-container\"]//button[@title=\"Submit\""

    val submittedValidate = "//*[@id=\"form-container\"]//div[@class=\"thank-you-page-container thank-you-page-message\"]/span"

    val options = ChromeOptions()

    var waitTime: Long = 5

    init {
        options.addArguments(
            "--headless",
            "--disable-gpu",
            "--window-size=1920,1200",
            "--disable-extensions",
            "--no-sandbox",
            "--whitelisted-ips"
        )
    }

    fun setTimeout(time: Long) {
        waitTime = time
    }
    fun waitFunc(locator:By): (WebDriver)->WebElement {
        return {
            it.findElement(locator)
        }
    }

    fun verifyLogin(userName: String, password:String): Boolean {
        val driver = ChromeDriver(options)
        val wait: Wait<WebDriver> = FluentWait<WebDriver>(driver)
            .withTimeout(Duration.ofSeconds(waitTime))
            .pollingEvery(Duration.ofSeconds(0.2.toLong()))
            .ignoring(NoSuchElementException::class.java)
        var valid = false
        try {
            root.info("validating user [user $userName] connecting")
            driver.get(url)
            val usernameBox = wait.until(waitFunc(By.xpath(userNameField)))
            usernameBox.sendKeys(userName + Keys.ENTER)
            root.info("validating user [user $userName] username filled")

            wait.until { it.findElement(By.xpath(userNameField)).text == userName }
            val passwordBox = wait.until(waitFunc(By.xpath(passwordField)))
            passwordBox.sendKeys(password + Keys.ENTER)
            root.info("validating user [user $userName] password filled")

            wait.until(waitFunc(By.xpath(dontStaySignedIn))).click()

            val tempInput = wait.until(waitFunc(By.xpath(temperatureField)))
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

    fun fillForm(userName: String, password:String, temp: Float, email: Boolean): Boolean {
        val driver = ChromeDriver(options)
        val wait = WebDriverWait(driver, Duration.ofSeconds(waitTime))
        var valid = false
        try {
            root.info("filling form [user $userName] connecting")
            driver.get(url)
            val usernameBox = wait.until(waitFunc(By.xpath(userNameField)))
            usernameBox.sendKeys(userName + Keys.ENTER)
            root.info("filling form [user $userName] username filled")

            wait.until { it.findElement(By.xpath(userNameField)).text == userName }
            val passwordBox = wait.until(waitFunc(By.xpath(passwordField)))
            passwordBox.sendKeys(password + Keys.ENTER)
            root.info("filling form [user $userName] password filled")

            wait.until(waitFunc(By.xpath(dontStaySignedIn))).click()

            val tempInput = wait.until(waitFunc(By.xpath(temperatureField)))
            val sendReceipt = wait.until(waitFunc(By.xpath(sendEmailReceipt)))
            val submitForm = wait.until(waitFunc((By.xpath(submitButton))))
            root.info("filling form [user $userName] form filled")

            tempInput.sendKeys(String.format("%.1f", temp))
            if (email) sendReceipt.click()
            submitForm.click()
            root.info("filling form [user $userName] done")

            wait.until { it.findElement(By.xpath(submittedValidate)).text == "Your response was submitted." }
            root.info("filling form [user $userName] validated form fill")
            valid = true
        } catch(e: TimeoutException) {
            root.info("filling form [user $userName] failed to validate")
            valid = false
        } finally {
            driver.quit()
        }
        return valid
    }
}