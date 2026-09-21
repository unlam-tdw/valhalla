package com.valhalla.presentation.landing;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LandingController {

  @GetMapping("/")
  public ModelAndView landing() {
    return new ModelAndView("pages/landing");
  }
}
