/*
 *   Copyright (c) 2024 Stefano Marano https://github.com/StefanoMarano80017
 *   All rights reserved.

 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.g2.t5;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2.Model.*;
import com.g2.Service.AchievementService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.g2.Components.GenericObjectComponent;
import com.g2.Components.PageBuilder;
import com.g2.Components.ServiceObjectComponent;
import com.g2.Interfaces.ServiceManager;
import com.g2.Model.Game;
import com.g2.Model.ScalataGiocata;
import com.g2.Model.User;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin
@Controller
public class GuiController {

    private final ServiceManager serviceManager;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    public GuiController(RestTemplate restTemplate) {
        this.serviceManager = new ServiceManager(restTemplate);
    }

    @GetMapping("/main")
    public String GUIController(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "main", model);
        main.SetAuth(jwt); //con questo metodo abilito l'autenticazione dell'utente
        return main.handlePageRequest();
    }

    @GetMapping("/profile")
    public String profilePagePersonal(Model model, @CookieValue(name = "jwt", required = false) String jwt)
    {
        byte[] decodedUserObj = Base64.getDecoder().decode(jwt.split("\\.")[1]);
        String decodedUserJson = new String(decodedUserObj, StandardCharsets.UTF_8);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(decodedUserJson, Map.class);

            String userId = map.get("userId").toString();

            return profilePage(model, userId, jwt);
        }
        catch (Exception e) {
            System.out.println("(/profile) Error requesting profile: " + e.getMessage());
        }

        return "error";
    }

    @GetMapping("/profile/{playerID}")
    public String profilePage(Model model,
                              @PathVariable(value="playerID") String playerID,
                              @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder profile = new PageBuilder(serviceManager, "profile", model);
        profile.SetAuth(jwt);

        int userId = Integer.parseInt(playerID);

        List<AchievementProgress> achievementProgresses = achievementService.getProgressesByPlayer(userId);
        List<StatisticProgress> statisticProgresses = achievementService.getStatisticsByPlayer(userId);

        List<Statistic> allStatistics = achievementService.getStatistics();
        Map<String, Statistic> IdToStatistic = new HashMap<>();

        for (Statistic stat : allStatistics)
            IdToStatistic.put(stat.getID(), stat);

        GenericObjectComponent objAchievementProgresses = new GenericObjectComponent("achievementProgresses", achievementProgresses);
        GenericObjectComponent objStatisticProgresses = new GenericObjectComponent("statisticProgresses", statisticProgresses);
        GenericObjectComponent objIdToStatistic = new GenericObjectComponent("IdToStatistic", IdToStatistic);
        GenericObjectComponent objUserID = new GenericObjectComponent("userID", userId);

        profile.setObjectComponents(objAchievementProgresses);
        profile.setObjectComponents(objStatisticProgresses);
        profile.setObjectComponents(objIdToStatistic);
        profile.setObjectComponents(objUserID);

        return profile.handlePageRequest();
    }

    @GetMapping("/gamemode")
    public String gamemodePage(Model model,
            @CookieValue(name = "jwt", required = false) String jwt,
            @RequestParam("mode") String mode) {
       
        if("Sfida".equals(mode) || "Allenamento".equals(mode)){
            PageBuilder gamemode = new PageBuilder(serviceManager, "gamemode", model);
            ServiceObjectComponent lista_classi = new ServiceObjectComponent(serviceManager, "lista_classi", "T1", "getClasses");        
            gamemode.setObjectComponents(lista_classi);
            List<String> list_robot = new ArrayList<>();
            // Aggiungere elementi alla lista
            list_robot.add("Randoop");
            list_robot.add("EvoSuite");
            GenericObjectComponent lista_robot = new GenericObjectComponent("lista_robot", list_robot);
            gamemode.setObjectComponents(lista_robot);
            gamemode.SetAuth(jwt);
            return gamemode.handlePageRequest();
        }
        if("Scalata".equals(mode)){
            PageBuilder gamemode = new PageBuilder(serviceManager, "gamemode_scalata", model);
            gamemode.SetAuth(jwt);
            return gamemode.handlePageRequest();
        }
            return "main";
    }

    @GetMapping("/editor")
    public String editorPage(Model model,
            @CookieValue(name = "jwt", required = false) String jwt,
            @RequestParam("ClassUT") String ClassUT) {

        PageBuilder editor = new PageBuilder(serviceManager, "editor", model);
        ServiceObjectComponent ClasseUT = new ServiceObjectComponent(serviceManager, "classeUT",
                "T1", "getClassUnderTest", ClassUT);
        editor.setObjectComponents(ClasseUT);
        editor.SetAuth(jwt);
        return editor.handlePageRequest();
    }
    
    @GetMapping("/leaderboard")
    public String leaderboard(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder leaderboard = new PageBuilder(serviceManager, "leaderboard", model);
        ServiceObjectComponent lista_utenti = new ServiceObjectComponent(serviceManager, "listaPlayers",
                "T23", "GetUsers");
        leaderboard.setObjectComponents(lista_utenti);
        leaderboard.SetAuth(jwt);
        return leaderboard.handlePageRequest();
    }

    @GetMapping("/edit_profile")
    public String edit_profile(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "Edit_Profile", model);

        
        User player_placeholder = new User((long) 1, "placeholder", "placeholder", "email", "password",
                true, "studies", "resetToke");

        GenericObjectComponent player = new GenericObjectComponent("player", player_placeholder);
        main.setObjectComponents(player);
        main.SetAuth(jwt);
        return main.handlePageRequest();
    }

    @GetMapping("/report")
    public String reportPage(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        Boolean Auth = (Boolean) serviceManager.handleRequest("T23", "GetAuthenticated", jwt);
        if (Auth) {
            return "report";
        }
        return "redirect:/login";
    }

    // TODO: Salvataggio della ScalataGiocata
    @PostMapping("/save-scalata")
    public ResponseEntity<String> saveScalata(@RequestParam("playerID") int playerID,
            @RequestParam("scalataName") String scalataName,
            HttpServletRequest request) {
        /*
         * Nella schermata /gamemode_scalata, il player non dovrà far altro che che selezionare una delle
         * "Scalate" presenti nella lista e dunque, le informazioni da elaborare saranno esclusivamente:
         * playerID
         * scalataName, dal quale è possibile risalire a tutte le informazioni relative quella specifica "Scalata"
         */

 /*
        * Verifica dell'autenticità del player controllando che l'header identificato dal: "X-UserID" sia lo stesso
        * associato all'utente identificato da "playerID"
         */
        if (!request.getHeader("X-UserID").equals(String.valueOf(playerID))) {

            System.out.println("(/save-scalata)[T5] Player non autorizzato.");
            return ResponseEntity.badRequest().body("Unauthorized");
        } else {

            // Player autorizzato.
            System.out.println("(/save-scalata)[T5] Player autorizzato.");

            // Recupero della data e dell'ora di inizio associata alla ScalataGiocata
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime currentHour = LocalTime.now();
            LocalDate currentDate = LocalDate.now();
            String fomattedHour = currentHour.format(formatter);
            System.out.println("(/save-scalata)[T5] Data ed ora di inizio recuperate con successo.");

            // Creazione di un oggetto scalataDataWriter
            ScalataDataWriter scalataDataWriter = new ScalataDataWriter();

            // Creazione di un oggetto ScalataGiocata
            ScalataGiocata scalataGiocata = new ScalataGiocata();

            // Inizializzazione dell'oggetto ScalataGiocata
            scalataGiocata.setPlayerID(playerID);
            scalataGiocata.setScalataName(scalataName);
            scalataGiocata.setCreationDate(currentDate);
            scalataGiocata.setCreationTime(fomattedHour);

            JSONObject ids = scalataDataWriter.saveScalata(scalataGiocata);
            System.out.println("(/save-scalata)[T5] Creazione dell'oggetto scalataDataWriter avvenuta con successo.");

            if (ids == null) {
                return ResponseEntity.badRequest().body("Bad Request");
            }

            return ResponseEntity.ok(ids.toString());
        }
    }

    @PostMapping("/save-data")
    public ResponseEntity<String> saveGame(@RequestParam("playerId") int playerId, @RequestParam("robot") String robot,
            @RequestParam("classe") String classe, @RequestParam("difficulty") String difficulty, @RequestParam("gamemode") String gamemode,
            @RequestParam("username") String username, @RequestParam("selectedScalata") Optional<Integer> selectedScalata, HttpServletRequest request) {

        if (!request.getHeader("X-UserID").equals(String.valueOf(playerId))) {
            return ResponseEntity.badRequest().body("Unauthorized");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime oraCorrente = LocalTime.now();
        String oraFormattata = oraCorrente.format(formatter);

        GameDataWriter gameDataWriter = new GameDataWriter();
        // g.setGameId(gameDataWriter.getGameId());
        Game g = new Game(playerId, gamemode, "nome", difficulty, username);
        // g.setPlayerId(pl);
        // g.setPlayerClass(classe);
        // g.setRobot(robot);
        g.setData_creazione(LocalDate.now());
        g.setOra_creazione(oraFormattata);
        g.setClasse(classe);
        g.setUsername(username);
        // System.out.println(g.getUsername() + " " + g.getGameId());

        System.out.println("ECCO LO USERNAME : " + username);       //in realtà stampa l'indirizzo e-mail del player...

        // globalID = g.getGameId();
        JSONObject ids = gameDataWriter.saveGame(g, username, selectedScalata);
        if (ids == null) {
            return ResponseEntity.badRequest().body("Bad Request");
        }

        System.out.println("Checking achievements...");
        achievementService.updateProgressByPlayer(playerId);

        return ResponseEntity.ok(ids.toString());
    }

    @GetMapping("/leaderboardScalata")
    public String getLeaderboardScalata(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        Boolean Auth = (Boolean) serviceManager.handleRequest("T23", "GetAuthenticated", jwt);
        if (Auth) {
            return "leaderboardScalata";
        }
        return "redirect:/login";
    }

    @GetMapping("/editor_old")
    public String getEditorOld(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        PageBuilder main = new PageBuilder(serviceManager, "editor_old", model);
        main.SetAuth(jwt); //con questo metodo abilito l'autenticazione dell'utente
        return main.handlePageRequest();
    }
}
