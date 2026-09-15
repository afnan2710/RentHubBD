package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public HomeController(PropertyRepository propertyRepository,
                          UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/home")
    public String home(Model model) {
        long published = propertyRepository.countByStatus(ListingStatus.PUBLISHED);
        long owners = userRepository.countByUserType(com.aoop.renthubbd.model.UserType.OWNER);
        long renters = userRepository.countByUserType(com.aoop.renthubbd.model.UserType.RENTER);

        model.addAttribute("statPublished", published);
        model.addAttribute("statOwners", owners);
        model.addAttribute("statRenters", renters);
        model.addAttribute("statTotal", published + owners + renters);
        return "home";
    }

    @GetMapping("/about-us")
    public String about() { return "about-us"; }
}