package com.valhalla.e2e.views;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.regex.Pattern;

/** Page object for the three {@code /plans} views: the listing, the create form and the detail. */
public class PlansPage extends WebPage {

  private static final String LIST_PATH = "/plans";
  private static final String NEW_PLAN_PATH = "/plans/new";

  public PlansPage(Page page) {
    super(page);
  }

  public void navigateToPlans() {
    page.navigate(baseUrl() + LIST_PATH);
  }

  public void navigateToNewPlan() {
    page.navigate(baseUrl() + NEW_PLAN_PATH);
  }

  public void typeName(String name) {
    this.typeIntoElement("#name", name);
  }

  public void typeDescription(String description) {
    this.typeIntoElement("#description", description);
  }

  /** {@code <input type="date">}, so the value has to be an ISO date. */
  public void typeEventDate(String isoDate) {
    this.typeIntoElement("#eventDate", isoDate);
  }

  public void clickCreate() {
    this.clickElement("button:has-text('Crear plan')");
  }

  /**
   * The create and update handlers redirect to {@code /plans/{id} } and the id comes from the
   * backend, so the detail page can only be awaited as a shape. {@link #waitForPath(String)} quotes
   * its argument and therefore cannot express it, and a glob ending in {@code /plans} never matched
   * this URL — which is exactly why the delete assertion below used to run against the list page.
   */
  public void waitForDetailPath() {
    page.waitForURL(
      Pattern.compile(
        "[a-zA-Z][a-zA-Z0-9+.-]*://[^/]*" + Pattern.quote(LIST_PATH) + "/\\d+(?:[?;][^#]*)?$"
      )
    );
  }

  public String getDetailHeading() {
    return this.getElementText("main h2");
  }

  /** Rows of the listing table. Zero is the only value that proves a delete really deleted. */
  public int getPlanCount() {
    return this.page.locator("tbody tr").count();
  }

  public boolean hasPlanNamed(String name) {
    return (
      this.page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(name)).count() > 0
    );
  }

  public void clickDetailOnRow(String name) {
    this.rowWith(name).locator("a:has-text('Ver detalle')").click();
  }

  /** The per-row delete form, which posts {@code _method=DELETE} to {@code /plans/{id}/delete}. */
  public void deletePlanNamed(String name) {
    this.rowWith(name).locator("form button[type=submit]").click();
  }

  private Locator rowWith(String name) {
    return this.page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(name));
  }
}
