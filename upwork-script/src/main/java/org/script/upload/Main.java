package org.script.upload;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.*;


public class Main {
    public static final String PATH_TO_FILE = "/Users/Andrii_Frolov/Root/Projects/Sandbox/selenium-java-upload-script/upwork-script/src/main/resources/";
    public static final String EXCEL_FILE_NAME = PATH_TO_FILE + "A1Contents Automation V2_copy.xls";
    private static final String LOGIN = "a1contents";
    private static final String PASSWORD = "DELETED PASSWORD CAUSE IT IS SENSITIVE INFO!!!!!";

    private static String browser = "chrome"; //Choose between chrome, safari

    public static void main(String[] args) throws IOException, InvalidFormatException, InterruptedException {
        List<Product> products = getProducts();
        List<String> createdLinks = new ArrayList<>();
        Configuration.webdriverLogsEnabled = false;
        Configuration.clickViaJs = true;
        Configuration.headless = true; //Set to true if you don't want to see browser open
        signIn(true);


        for (int i = 1; i <= products.size(); i++) {
            log("Processing product " + i + " out of " + products.size());
            Product product = products.get(i - 1);
            if (product.isAlreadyProcessed()) {
                log("-- Not saving product '" + product.getProductName() + "' cause it is marked as Processed in Excel (line " + product.getLine() + " )");
            } else {
                log("-- Saving " + product.getProductName() + "' (line " + product.getLine() + " )");
                Optional<String> linkToCreatedProduct;
                linkToCreatedProduct = tryToSubmitProduct(product);

                if (linkToCreatedProduct.isPresent()) {
                    log("Successfully saved " + linkToCreatedProduct.get());
                    createdLinks.add(linkToCreatedProduct.get());
                    addAlreadyProcessedFlag(product);
                }
            }
        }
        log("All created links:");
        createdLinks.stream().forEach(Main::log);
    }

    private static Optional<String> tryToSubmitProduct(Product product) throws InterruptedException {
        Optional<String> linkToCreatedProduct;
        try {
            linkToCreatedProduct = submitProduct(product);
        } catch (Exception e) {
            log(e.getMessage());
            //try one more time with this product
            linkToCreatedProduct = submitProduct(product);
        }
        return linkToCreatedProduct;
    }

    private static Optional<String> submitProduct(Product product) throws InterruptedException {
        open("https://a1contents.com/wp-admin/post-new.php?post_type=product");
        //Let page fully load
        Thread.sleep(5 * 1000);

        step(2);
        $(By.id("title")).sendKeys(product.getProductName());

        step(4);
        $(By.id("_downloadable")).click(); //downloadabkle checkbox

        step(8);
        $(By.xpath("//a[@class='button insert']")).click();

        step(5);
        //IMPORTANT: you told me to hardcode here 'Download Template'
        //If you will need smth else here in future - just add column 'File name' to Excel sheet
        $(By.xpath("//input[@placeholder='File name']")).sendKeys(product.getFilename());

        step(6);
        $(By.xpath("//input[@placeholder='http://']")).sendKeys(product.getLink());

        step(7);
        $(By.xpath("//a[@class='button upload_file_button']")).click();
        $(By.id("menu-item-browse")).click();
        //choose 1st image
        $$(By.xpath("//ul[contains(@class, 'attachments')]//li[1]")).get(0).click();
        $(By.xpath("//button[contains(@class, 'media-button-select')]")).click();
        //webDriver.findElement(By.xpath("//input[@type='file']")).sendKeys();


        step(9);
        //(I was told that categories must exist when script is running, currently not all of them exist)
        ElementsCollection elements = $$(By.xpath(String.format("//label[contains(text(), '%s')]/input", product.getCategory())));
        if (elements.isEmpty()) {
            log("Attention! There are no checkbox for category " + product.getCategory() + ". Aborting save for this product");
            return Optional.empty();
        }
        log("Attempting to click on checkbox " + product.getCategory());
        elements.get(0).click();

        step(10);


        log("Step 3");
        WebElement productDescriptionFrame = $(By.id("content_ifr"));
        //switch to frame with description
        switchTo().frame(productDescriptionFrame);
        //Chrome driver does not support non-BMP symbols (e.g. smiles)
        String description = browser.equals("chrome") ? transformToBMP(product.getProductDescription()) : product.getProductDescription();
        $(By.id("tinymce")).sendKeys(description);
        switchTo().defaultContent();


        log("Step 11");
        $(By.id("publish")).click();

        String link = $(By.id("woocommerce-product-updated-message-view-product__link")).getAttribute("href");
        return Optional.of(link);


    }

    private static void signIn(boolean isAutomated) throws InterruptedException {
        open("https://a1contents.com/wp-admin/");
        if (isAutomated) {
            $(By.id("user_login")).sendKeys(LOGIN);
            $(By.id("user_pass")).sendKeys(PASSWORD);
            $(By.id("wp-submit")).click();
        } else {
            log("I will wait for 20 seconds, enter credentials and click sign in button");
            Thread.sleep(20 * 1000);
        }
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private static void step(int step) {
        System.out.println("---- Step " + step);
    }

    private static List<Product> getProducts() throws IOException, InvalidFormatException {
        Excel excel = new Excel(EXCEL_FILE_NAME, false, 0);
        excel.open();
        List<Map<String, String>> rows = excel.readExcel();
        List<Product> products = rows.stream().map(Product::new).collect(Collectors.toList());

        excel.close();
        return products;
    }

    private static void addAlreadyProcessedFlag(Product product) throws IOException, InvalidFormatException {
        Excel excel = new Excel(EXCEL_FILE_NAME, false, 0);
        excel.open();
        excel.addFlagThatLineProcessed(product.getLine());
        excel.save();
        excel.close();
    }

    public static String transformToBMP(String message) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            int codePoint = message.codePointAt(i);

            if (Character.isBmpCodePoint(codePoint)) {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append("\\u").append(String.format("%04X", codePoint));
            }

            if (i != message.length() - 1 && Character.isSurrogatePair(message.charAt(i), message.charAt(i + 1))) {
                i++; // Increment i by 1 if the character is a surrogate pair
            }
        }

        return builder.toString();
    }

    private static void secondUploadStep10() {
        //(again uploading image?)
//        $(By.id("set-post-thumbnail")).click();
//
//        //choose 1st image
//        $$(By.xpath("//ul[contains(@class, 'attachments')]//li[1]")).get(1).click();
//        $$(By.xpath("//button[contains(@class, 'media-button-select')]")).get(1).click();

    }

}