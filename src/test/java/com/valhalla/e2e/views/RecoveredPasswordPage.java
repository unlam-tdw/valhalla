package com.valhalla.e2e.views;

import com.microsoft.playwright.Page;

/** Confirmation view reached by POSTing the recovery form, which renders the temporary password. */
public class RecoveredPasswordPage extends WebPage {

  public RecoveredPasswordPage(Page page) {
    super(page);
  }

  public String getTempPassword() {
    return this.getElementText("#temp-password").trim();
  }
}
