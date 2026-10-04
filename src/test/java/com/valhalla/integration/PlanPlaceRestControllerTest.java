package com.valhalla.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.valhalla.domain.place.*;
import com.valhalla.domain.plan.*;
import com.valhalla.domain.planplace.*;
import com.valhalla.domain.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@WebIntegrationTest
@Transactional
class PlanPlaceRestControllerTest extends PlanPlaceWebFixture {

  @Test
  void I01_addPlace() throws Exception {
    add(first);
    assertEquals(1, entries.findByPlanId(plan.getId()).get(0).getSortOrder());
  }

  @Test
  void I02_samePlaceCanBeAddedTwice() throws Exception {
    add(first);
    add(first);
    var result = entries.findByPlanId(plan.getId());
    assertEquals(2, result.size());
    assertEquals(2, result.get(1).getSortOrder());
  }

  @Test
  void I03_orderedItineraryDoesNotExposeOwner() throws Exception {
    add(first);
    add(second);
    mvc
      .perform(get(url).with(user("apl@test.com")))
      .andExpect(status().isOk())
      .andExpect(jsonValue("/0/placeId", first))
      .andExpect(jsonValue("/1/sortOrder", 2))
      .andExpect(jsonMissing("/0/plan"));
  }

  @Test
  void I04_updatesScheduleAndClearsIt() throws Exception {
    Long id = add(first);
    mvc
      .perform(
        put(url + "/" + id)
          .with(user("apl@test.com"))
          .with(csrf())
          .contentType("application/json")
          .content("{\"visitDate\":\"2026-12-01\",\"visitTime\":\"10:30\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonValue("/visitDate", "2026-12-01"))
      .andExpect(jsonValue("/visitTime", "10:30"));
    mvc
      .perform(
        put(url + "/" + id)
          .with(user("apl@test.com"))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isOk());
    assertNull(entries.findById(id).orElseThrow().getVisitDate());
  }

  @Test
  void I05_deletesEntry() throws Exception {
    Long id = add(first);
    mvc
      .perform(delete(url + "/" + id).with(user("apl@test.com")).with(csrf()))
      .andExpect(status().isOk());
    assertTrue(entries.findById(id).isEmpty());
  }

  @Test
  void I06_reorders() throws Exception {
    Long firstEntry = add(first);
    Long secondEntry = add(second);
    mvc
      .perform(
        post(url + "/reorder")
          .with(user("apl@test.com"))
          .with(csrf())
          .contentType("application/json")
          .content("[" + secondEntry + "," + firstEntry + "]")
      )
      .andExpect(status().isOk());
    assertEquals(second, entries.findByPlanId(plan.getId()).get(0).getPlace().getId());
  }

  @Test
  void invalidDateReturns400AndDoesNotModify() throws Exception {
    Long id = add(first);
    mvc
      .perform(
        put(url + "/" + id)
          .with(user("apl@test.com"))
          .with(csrf())
          .contentType("application/json")
          .content("{\"visitDate\":\"bad\"}")
      )
      .andExpect(status().isBadRequest());
    assertNull(entries.findById(id).orElseThrow().getVisitDate());
  }

  @Test
  void invalidReorderReturns400() throws Exception {
    add(first);
    mvc
      .perform(
        post(url + "/reorder")
          .with(user("apl@test.com"))
          .with(csrf())
          .contentType("application/json")
          .content("[]")
      )
      .andExpect(status().isBadRequest());
    assertEquals(1, entries.findByPlanId(plan.getId()).get(0).getSortOrder());
  }

  @Test
  void I11_placesIncludesOnlyOwnerPlans() throws Exception {
    mvc
      .perform(get("/places").with(user("apl@test.com")))
      .andExpect(status().isOk())
      .andExpect(model().attribute("userPlans", org.hamcrest.Matchers.hasSize(1)));
    mvc
      .perform(get("/places").with(user("other@test.com")))
      .andExpect(model().attribute("userPlans", org.hamcrest.Matchers.empty()));
  }

  @Test
  void I12_anonymousPlacesHasNoUserPlans() throws Exception {
    mvc
      .perform(get("/places"))
      .andExpect(status().isOk())
      .andExpect(model().attributeDoesNotExist("userPlans"));
  }
}
