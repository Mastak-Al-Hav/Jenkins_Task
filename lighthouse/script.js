const fs = require('fs');
const path = require('path');
const os = require('os');
const puppeteer = require('puppeteer');
const lighthouse = require('lighthouse/lighthouse-core/fraggle-rock/api.js');

const waitTillHTMLRendered = async (page, timeout = 30000) => {
    const checkDurationMsecs = 1000;
    const maxChecks = timeout / checkDurationMsecs;
    let lastHTMLSize = 0;
    let checkCounts = 1;
    let countStableSizeIterations = 0;
    const minStableSizeIterations = 3;

    while (checkCounts++ <= maxChecks) {
        let html = '';
        try {
            html = await page.content();
        } catch (e) {
            await new Promise(r => setTimeout(r, checkDurationMsecs));
            continue;
        }

        const currentHTMLSize = html.length;
        if (lastHTMLSize !== 0 && currentHTMLSize === lastHTMLSize) {
            countStableSizeIterations++;
        } else {
            countStableSizeIterations = 0;
        }

        if (countStableSizeIterations >= minStableSizeIterations) {
            console.log("HTML stable:", page.url());
            break;
        }
        lastHTMLSize = currentHTMLSize;
        await new Promise(r => setTimeout(r, checkDurationMsecs));
    }
};

async function captureReport() {
   
    const baseURL = process.argv[2] || "http://wp:80/";
    console.log(`Target URL: ${baseURL}`);

    let chromePath;
    if (os.platform() === 'win32') {
        
        chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    } else {
       
        chromePath = '/usr/bin/chromium-browser';
    }

    console.log(`Launching browser at: ${chromePath}`);

    const browser = await puppeteer.launch({
        executablePath: chromePath,
        headless: 'new',
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-gpu',
            '--ignore-certificate-errors'
        ]
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
    await page.setDefaultTimeout(30000);

    console.log("Starting Lighthouse User Flow...");
    const flow = await lighthouse.startFlow(page, {
        name: 'Performance Testing User Flow',
        configContext: {
            settingsOverrides: {
                throttlingMethod: "simulate",
                formFactor: "desktop",
                onlyCategories: ['performance'],
                screenEmulation: {
                    mobile: false,
                    width: 1920,
                    height: 1080,
                    deviceScaleFactor: 1,
                    disabled: false
                }
            }
        }
    });

    const tablesLink = 'a[href*="tables"], li.page-item-13 a, .right-menu a[href*="tables"]';
    const productCardSelector = '.product-list .al_archive';
    const addToCartBtn = 'form.add-to-shopping-cart button[type="submit"], button.button.green-box.ic-design';
    const cartLink = 'span.cart-added-info a[href*="/cart"], a[href*="/cart"]';
    const placeOrderBtn = 'input.to_cart_submit, input[value="Place an order"], .checkout-button';

    const billingFields = {
        name: 'input[name="cart_name"]',
        address: 'input[name="cart_address"]',
        postal: 'input[name="cart_postal"]',
        city: 'input[name="cart_city"]',
        country: 'select[name="cart_country"]',
        phone: 'input[name="cart_phone"]',
        email: 'input[name="cart_email"]',
        submit: 'input[name="cart_submit"]'
    };


    // 1. Open Home Page
    await flow.navigate(baseURL, { stepName: 'Open Home Page' });
    console.log("Home page opened");
    await waitTillHTMLRendered(page);

    // 2. Navigate to Tables
    await page.waitForSelector(tablesLink, { visible: true });
    await flow.startTimespan({ stepName: 'Navigate to Tables' });
    await page.click(tablesLink);
    await waitTillHTMLRendered(page);
    await flow.endTimespan();
    console.log("Tables opened");

    // 3. Open Random Product
    await flow.startTimespan({ stepName: 'Open Random Product' });
    await page.waitForSelector(productCardSelector, { visible: true });
    const cards = await page.$$(productCardSelector);
    if (!cards.length) throw new Error("No products found");
    const randomIndex = Math.floor(Math.random() * cards.length);
    await cards[randomIndex].click();
    await page.waitForSelector(addToCartBtn, { visible: true });
    await waitTillHTMLRendered(page);
    await flow.endTimespan();
    console.log("Product opened");

    // 4. Add to Cart
    await flow.startTimespan({ stepName: 'Add to Cart' });
    await page.click(addToCartBtn);
    await page.waitForSelector(cartLink, { visible: true });
    await waitTillHTMLRendered(page);
    await flow.endTimespan();
    console.log("Added to cart");

    // 5. Open Cart
    await flow.startTimespan({ stepName: 'Open Cart' });
    await page.click(cartLink);
    await page.waitForSelector(placeOrderBtn, { visible: true });
    await waitTillHTMLRendered(page);
    await flow.endTimespan();
    console.log("Cart opened");

    // 6. Checkout / Submit Order
    await flow.startTimespan({ stepName: 'Submit Order' });
    await page.click(placeOrderBtn);
    await page.waitForSelector(billingFields.name, { visible: true });

    await page.type(billingFields.name, 'Performance Tester');
    await page.type(billingFields.address, 'Main Street 1');
    await page.type(billingFields.postal, '01001');
    await page.type(billingFields.city, 'Kyiv');
    await page.select(billingFields.country, 'UA');
    await page.type(billingFields.phone, '+380000000000');
    await page.type(billingFields.email, 'test@example.com');

    await page.click(billingFields.submit);
    await waitTillHTMLRendered(page);
    await flow.endTimespan();
    console.log("Order submitted");

    const report = await flow.generateReport();
    
    const resultsDir = path.join(__dirname, '..', 'testResults');
    const reportPath = path.join(resultsDir, 'lighthouse-report.html');
    
    if (!fs.existsSync(resultsDir)) {
        fs.mkdirSync(resultsDir, { recursive: true });
    }

    fs.writeFileSync(reportPath, report);
    console.log("Report saved successfully at:", reportPath);

    await browser.close();
}

captureReport().catch(err => {
    console.error("SCRIPT FAILED:");
    console.error(err);
    process.exit(1);
});
