package com.example.securing_web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/hello").setViewName("hello");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/about").setViewName("about");
        registry.addViewController("/contact").setViewName("contact");
        registry.addViewController("/services").setViewName("services");
        registry.addViewController("/categories").setViewName("categories");
        registry.addViewController("/categories/create").setViewName("createCategory");
        registry.addViewController("/").setViewName("index"); // Новая главная
        registry.addViewController("/home").setViewName("index");
    }

}