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

package com.g2.Interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.g2.Model.Game;
import com.g2.Model.StatisticProgress;
import com.g2.Model.User;
import org.springframework.core.ParameterizedTypeReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class T4Service extends BaseService {

    // Costante che definisce l'URL di base per le richieste REST
    private static final String BASE_URL = "http://t4-g18-app-1:3000";

    // Costruttore della classe, inizializza il servizio con il RestTemplate e l'URL
    // di base
    public T4Service(RestTemplate restTemplate) {
        // Inizializzazione del servizio base con RestTemplate e URL specificato
        super(restTemplate, BASE_URL);

        // Registrazione dell'azione "getLevels" con una definizione specifica per
        // questa azione
        registerAction("getLevels", new ServiceActionDefinition(
                // Definizione di un'operazione lambda che invoca il metodo getLevels con un
                // parametro di tipo String
                params -> getLevels((String) params[0]),
                // L'azione è definita per accettare un parametro di tipo String
                String.class
        ));

        registerAction("getGames", new ServiceActionDefinition(
                params -> getGames((int) params[0]),
                Integer.class
        ));

        registerAction("getStatisticsProgresses", new ServiceActionDefinition(
                params -> getStatisticsProgresses((int) params[0]),
                Integer.class
        ));

        registerAction("CreateGame", new ServiceActionDefinition(
                params -> CreateGame((String) params[0], (String) params[1], (String) params[2], (String) params[3], (String) params[4]),
                String.class, String.class, String.class, String.class, String.class
        ));

        registerAction("EndGame", new ServiceActionDefinition(
            params -> EndGame((int) params[0], (String) params[1], (String) params[2], (int) params[3], (Boolean) params[4]),
            Integer.class, String.class, String.class, Integer.class, Boolean.class
        ));

        registerAction("CreateRound", new ServiceActionDefinition(
                params -> CreateRound((int) params[0], (String) params[1], (String) params[2]),
                Integer.class, String.class, String.class
        ));

        registerAction("EndRound", new ServiceActionDefinition(
                params -> EndRound((String) params[0], (int) params[1]),
                String.class, Integer.class
        ));

        registerAction("CreateTurn", new ServiceActionDefinition(
                params -> CreateTurn((String) params[0], (int) params[1], (String) params[2]),
                String.class, Integer.class, String.class
        ));

        registerAction("EndTurn", new ServiceActionDefinition(
                params -> EndTurn((String) params[0], (String) params[1], (int) params[2]),
                String.class, String.class, Integer.class
        ));

        registerAction("CreateScalata", new ServiceActionDefinition(
                params -> CreateScalata((String) params[0], (String) params[1], (String) params[2], (String) params[3]),
                String.class, String.class, String.class, String.class
        ));

        registerAction("GetRisultati", new ServiceActionDefinition(
                params -> GetRisultati((String) params[0], (String) params[1], (String) params[2]),
                String.class, String.class, String.class
        ));
    }

    // usa /games per ottenere una lista di giochi
    private List<Game> getGames(int playerId) {
        final String endpoint = "/games/player/" + playerId;
        try {
            return callRestGET(endpoint, null, new ParameterizedTypeReference<List<Game>>() {});
        }
        catch (RuntimeException e) {
            // Gestione degli errori durante la richiesta
            throw new IllegalArgumentException("[GETGAMES] Errore durante il recupero dei giochi: " + e.getMessage());
        }
    }

    private List<StatisticProgress> getStatisticsProgresses(int playerID) {
        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("pid", String.valueOf(playerID));

            String endpoint = "/phca/" + playerID;

            List<StatisticProgress> response = callRestGET(endpoint, formData, new ParameterizedTypeReference<List<StatisticProgress>>() {});
            return response;
        } catch (Exception e) {
            System.out.println("[GETSTATISTICSPROGRESSES] Errore nel prelievo delle statistiche: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // usa /robots per ottenere dati 
    private String GetRisultati(String className, String robot_type, String difficulty) {
        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("testClassId", className);          // Nome della classe
            formData.put("type", robot_type);               // Tipo di robot
            formData.put("difficulty", difficulty);        // Livello di difficoltà corrente

            String response = callRestGET("/robots", formData, String.class);
            return response;
        } catch (Exception e) {
            return "errore GetRisultati";
        }
    }

    /**
     * Metodo che invia richieste per ottenere diversi "livelli" (levels) in
     * base al nome della classe. Per ogni livello (da 0 a 10) e per ciascun
     * tipo di robot ("randoop", "evosuite"), viene effettuata una chiamata REST
     * per verificare la presenza di dati associati.
     *
     */
    private List<String> getLevels(String className) {
        // Inizializzazione di una lista per conservare i risultati
        List<String> result = new ArrayList<>();

        // Definizione dei tipi di robot che verranno utilizzati nella chiamata
        List<String> robot_type = List.of("randoop", "evosuite");

        // Iterazione su 11 livelli di difficoltà (da 0 a 10)
        for (int i = 0; i < 11; i++) {
            // Per ogni tipo di robot definito
            for (String robot_string : robot_type) {
                try {
                    // Creazione di una mappa per i parametri del form da inviare nella richiesta
                    // GET
                    Map<String, String> formData = new HashMap<>();
                    formData.put("testClassId", className); // Nome della classe
                    formData.put("type", robot_string); // Tipo di robot
                    formData.put("difficulty", String.valueOf(i)); // Livello di difficoltà corrente

                    // Invio della richiesta GET tramite il servizio Rest, con i parametri e attesa
                    // di una risposta di tipo String
                    // <<<<<Nota:Non sappiamo il motivo per il quale è stato implementato in questo
                    // modo>>>>
                    String response = callRestGET("/robots", formData, String.class);

                    // Se la risposta non è nulla, aggiungi il livello corrente alla lista dei
                    // risultati
                    if (response != null) {
                        result.add(String.valueOf(i));
                    }
                } catch (Exception e) {
                    // Gestione delle eccezioni, lancia un'eccezione personalizzata in caso di
                    // errore
                    break;
                }
            }
        }

        // Ritorna la lista dei livelli trovati
        return result;
    }

    private int CreateGame(String Time, String difficulty, String name, String description, String id) {
        final String endpoint = "/games";
        JSONObject obj = new JSONObject();
        obj.put("difficulty", difficulty);
        obj.put("name", name);
        obj.put("description", description);
        obj.put("startedAt", Time);

        JSONArray playersArray = new JSONArray();
        playersArray.put(String.valueOf(id));

        obj.put("players", playersArray);
        try {
            //Questa chiamata in risposta dà anche i valori che hai fornito, quindi faccio parse per avere l'id
            String respose = callRestPost(endpoint, obj, null, null,String.class);
            // Parsing della stringa JSON
            JSONObject jsonObject = new JSONObject(respose);
            // Estrazione del valore di id
            return jsonObject.getInt("id");
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateGame]: " + e.getMessage());
        }
    }

    private String EndGame(int gameid, String username, String closedAt, int Score, Boolean isWinner){
        final String endpoint = "/games/" + String.valueOf(gameid);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("closedAt", closedAt);
        formData.add("username", username);
        formData.add("score", Integer.toString(Score));
        formData.add("isWinner", isWinner ? "true" : "false");
        try {
            String respose = callRestPost(endpoint, formData, null, String.class);
            return respose;
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateGame]: " + e.getMessage());
        }
    }

    private int CreateRound(int game_id, String ClasseUT, String Time) {
        final String endpoint = "/rounds";
        JSONObject obj = new JSONObject();
        obj.put("gameId", game_id);
        obj.put("testClassId", ClasseUT);
        obj.put("startedAt", Time);
        try {
            String respose = callRestPost(endpoint, obj, null, null, String.class);
            // Parsing della stringa JSON
            JSONObject jsonObject = new JSONObject(respose);
            // Estrazione del valore di id
            return jsonObject.getInt("id");
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateRound]: " + e.getMessage());
        }
    }

    private String EndRound(String Time, int roundId) {
        //Anche qui non è stato previsto un parametro per la chiamata rest e quindi va costruito a mano
        final String endpoint = "rounds/" + String.valueOf(roundId);
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("closedAt", Time);
            String response = callRestPut(endpoint, formData, null, String.class);
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("[EndRound]: " + e.getMessage());
        }
    }

    private String CreateTurn(String Player_id, int Round_id, String Time) {
        final String endpoint = "/turns";
        JSONObject obj = new JSONObject();
        JSONArray playersArray = new JSONArray();
        playersArray.put(Player_id);
        obj.put("players", playersArray);
        obj.put("roundId", Round_id);
        obj.put("startedAt", Time);
        try {
            String respose = callRestPost(endpoint, obj, null, null, String.class);
            return respose;
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateTurn]: " + e.getMessage());
        }
    }

    private String EndTurn(String user_score, String Time, int turnId) {
        //Anche qui non è stato previsto un parametro per la chiamata rest e quindi va costruito a mano
        final String endpoint = "turns/" + String.valueOf(turnId);
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("scores", user_score);
            formData.add("closedAt", Time);
            String response = callRestPut(endpoint, formData, null, String.class);
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("[EndTurn]: " + e.getMessage());
        }
    }

    //Questa chiamata non è documentata nel materiale di caterina
    private String CreateScalata(String player_id, String scalata_name, String creation_Time, String creation_date) {
        final String endpoint = "/turns";
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("playerID", player_id);
        formData.add("scalataName", scalata_name);
        formData.add("creationTime", creation_Time);
        formData.add("creationDate", creation_date);

        try {
            String respose = callRestPost(endpoint, formData, null, String.class);
            return respose;
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateScalata]: " + e.getMessage());
        }
    }
}