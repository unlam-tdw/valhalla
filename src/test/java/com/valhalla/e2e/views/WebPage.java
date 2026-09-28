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

  /**
   * Waits until the browser sits on {@code path}. Tolerates a query string as well as the
   * {@code ;jsessionid} suffix: Spring Security lands on {@code ?logout=true} and {@code
   * ?error=true} after these flows, and a pattern that only allowed the session id would time out
   * on them instead of matching.
   */
  public void waitForPath(String path) {
    page.waitForURL(Pattern.compile(".*" + Pattern.quote(path) + "(?:[?;][^#]*)?$"));
  }

  /**
   * Base URL for E2E navigation. Failsafe derives this from ${jetty.port} (see pom.xml), so
   * there is no local default: one that silently said 8080 would let the tests keep hitting
   * the old port after the server moved.
   */
  public String baseUrl() {
    String baseUrl = System.getProperty("e2e.baseUrl");
    if (baseUrl == null) {
      throw new IllegalStateException(
        "e2e.baseUrl is not set. Run through failsafe, or pass -De2e.baseUrl when running" +
        " from an IDE."
      );
    }
    return baseUrl;
  }

  public void navigate(String url) {
    page.navigate(url);
  }

  /**
   * Brand text of the shared navbar, which every view carries. Anonymous state: the plain brand
   * plus the Register and Login links.
   */
  public String getNavbarBrand() {
    return this.getElementText("nav a.navbar-brand");
  }

  /**
   * "Signed in as &lt;email&gt;", present only once a session exists. This is the branch of the
   * navbar fragment that {@link #getNavbarBrand()} cannot see, because a logged-out visitor never
   * renders it.
   */
  public String getNavbarSignedInAs() {
    return this.getElementText("nav span.text-gray-300").replaceAll("\\s+", " ").trim();
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
