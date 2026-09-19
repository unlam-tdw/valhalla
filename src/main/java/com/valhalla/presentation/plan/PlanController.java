package com.valhalla.presentation.plan;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/planes")
public class PlanController {

    @GetMapping
    public String planList(){
        return "plans/list";
    }

    @GetMapping("/{id}")
    public String planDetail(@PathVariable Long id, Model model){
        return "plans/detail";
    }

    @GetMapping
    public String planCreate(){
        return "plans/create";
    }


}
