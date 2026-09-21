package com.valhalla.e2e.views;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

public class WebPage {

  protected Page page;

  public WebPage(Page page) {
    this.page = page;
  }

  public URL getCurrentUrl() throws MalformedURLException {
    return URI.create(page.url()).toURL();
  }

  public void waitForPath(String path) {
    page.waitForURL(Pattern.compile(".*" + Pattern.quote(path) + "(;[^/?#]*)?$"));
  }

  /** Base URL for E2E navigation; override with -De2e.baseUrl (default http://localhost:8080). */
  public String baseUrl() {
    return System.getProperty("e2e.baseUrl", "http://localhost:8080");
  }

  public void navigate(String url) {
    page.navigate(url);
  }

  protected String getElementText(String cssSelector) {
    return this.getElement(cssSelector).textContent();
  }

  protected void clickElement(String cssSelector) {
    this.getElement(cssSelector).click();
  }

  protected void typeIntoElement(String cssSelector, String text) {
    this.getElement(cssSelector).fill(text);
    // Dispatch input event so Vue v-model picks up the value
    this.getElement(cssSelector)
      .evaluate("el => el.dispatchEvent(new Event('input', {bubbles: true}))");
  }

  private Locator getElement(String cssSelector) {
    return page.locator(cssSelector);
  }
}
