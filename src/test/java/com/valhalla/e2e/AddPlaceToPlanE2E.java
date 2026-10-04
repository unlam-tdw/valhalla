package com.valhalla.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.valhalla.e2e.views.PlansPage;
import org.junit.jupiter.api.Test;

public class AddPlaceToPlanE2E extends E2eBase {

  @Test
  void E01_addPlaceAndReadItineraryWithBrowserSession() {
    signInAsAdmin();
    PlansPage plans = new PlansPage(page);
    plans.navigateToNewPlan();
    plans.typeName("APL E2E");
    plans.clickCreate();
    plans.waitForDetailPath();
    String planId = page.url().substring(page.url().lastIndexOf('/') + 1);
    // 05-APL-FE owns the Add to plan UI; exercise the backend via fetch in the authenticated browser.
    Object result = page.evaluate(
      """
      async planId => {
        const tokenInput = document.querySelector('input[name="_csrf"]');
        if (!tokenInput) throw new Error('Missing CSRF token');
        const catalog = await (await fetch('/api/places')).json();
        const url = '/api/plans/' + planId + '/places';
        const added = await fetch(url + '?placeId=' + catalog[0].id, {
          method: 'POST', headers: {'X-CSRF-TOKEN': tokenInput.value}
        });
        if (!added.ok) throw new Error('Add failed: ' + added.status);
        const itinerary = await (await fetch(url)).json();
        return itinerary.length === 1 && itinerary[0].placeId === catalog[0].id && itinerary[0].sortOrder === 1;
      }
      """,
      planId
    );
    assertEquals(Boolean.TRUE, result);
  }
}
