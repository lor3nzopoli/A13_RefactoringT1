/*MODIFICA (5/11/2024) - Refactoring task T1
 * UtilsController ora si occupa solo del mapping dei servizi aggiunti.
 */
package com.groom.manvsclass.controller;

import java.io.IOException;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.groom.manvsclass.service.AchievementService;

import com.groom.manvsclass.service.Util; 


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.DeleteMapping;


import com.groom.manvsclass.model.interaction;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/utils")
public class UtilsController {

    @Autowired
    private Util utilsService;

    @GetMapping("/elencaInt")
    public List<interaction> elencaInt() {
        return utilsService.elencaInt();
    }

    @GetMapping("/elencaReport")
    public List<interaction> elencaReport() {
        return utilsService.elencaReport();
    }

    @GetMapping("/likes/{name}")
    public long likes(@PathVariable String name) {
        return utilsService.likes(name);
    }

    @PostMapping("/uploadInteraction")
    public interaction uploadInteraction(@RequestBody interaction interazione) {
        return utilsService.uploadInteraction(interazione);
    }

    @PostMapping("/newLike/{name}")
    public String newLike(@PathVariable String name) {
        return utilsService.newLike(name);
    }

    @PostMapping("/newReport/{name}")
    public String newReport(@PathVariable String name, @RequestBody String commento) {
        return utilsService.newReport(name, commento);
    }

    @DeleteMapping("/eliminaInteraction/{id_i}")
    public interaction eliminaInteraction(@PathVariable int id_i) {
        return utilsService.eliminaInteraction(id_i);
    }
}