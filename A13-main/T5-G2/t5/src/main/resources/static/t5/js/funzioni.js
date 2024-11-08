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

// Funzione per creare l'URL dell'API
function createApiUrl(formData, orderTurno) {
	const className = formData.get("className");
	const underTestClassName = formData.get("underTestClassName");
	const playerId = formData.get("playerId");

	// Costruisce il percorso per la classe
	const classePath = `VolumeT8/FolderTreeEvo/${className}/${className}SourceCode/${underTestClassName}`;

	// Ottiene il percorso del test generato
	const testPath = generaPercorsoTest(orderTurno, formData);

	// Costruisce l'URL dell'API
	const apiUrl = `/api/${classePath}+${testPath}+/app+${playerId}`;

	return apiUrl;
}

// Funzione per generare il percorso del test
function generaPercorsoTest(orderTurno, formData) {
	const modalita = localStorage.getItem("modalita");
	const playerId = formData.get("playerId");
	const gameId = formData.get("gameId");
	const roundId = formData.get("roundId");
	const classeLocal = formData.get("className");

	// Verifica la modalità e costruisce il percorso appropriato
	if (modalita === "Scalata" || modalita === "Sfida") {
		const scalataPart =
			modalita === "Scalata"
				? `/${localStorage.getItem("SelectedScalata")}${localStorage.getItem(
						"scalataId"
				  )}`
				: "";

		return `/VolumeT8/FolderTreeEvo/StudentLogin/Player${playerId}/${modalita}${scalataPart}/${classeLocal}/Game${gameId}/Round${roundId}/Turn${orderTurno}/TestReport`;
	} else {
		console.error("Errore: modalità non trovata");
		window.location.href = "/main";
		return null;
	}
}

// Funzione per estrarre la terza colonna di un CSV
function extractThirdColumn(csvContent) {
	const rows = csvContent.split("\n"); // Divide le righe
	const thirdColumnValues = [];

	// Inizia il ciclo dalla seconda riga (indice 1)
	rows.slice(1).forEach((row) => {
		const cells = row.split(","); // Divide le celle
		if (cells.length >= 3) {
			thirdColumnValues.push(cells[2]); // Aggiunge la terza colonna
		}
	});

	return thirdColumnValues;
}

you_win = `
__     ______  _    _  __          _______ _   _ 
\\ \\   / / __ \\| |  | | \\ \\        / /_   _| \\ | |
 \\ \\_/ / |  | | |  | |  \\ \\  /\\  / /  | | |  \\| |
  \\   /| |  | | |  | |   \\ \\/  \\/ /   | | | . \` |
   | | | |__| | |__| |    \\  /\\  /   _| |_| |\\  |
   |_|  \\____/ \\____/      \\/  \\/   |_____|_| \\_|
`;

var you_lose = `
__     ______  _    _   _      ____   _____ ______ 
\\ \\   / / __ \\| |  | | | |    / __ \\ / ____|  ____|
 \\ \\_/ / |  | | |  | | | |   | |  | | (___ | |__   
  \\   /| |  | | |  | | | |   | |  | |\\___ \\|  __|  
   | | | |__| | |__| | | |___| |__| |____) | |____ 
   |_|  \\____/ \\____/  |______\\____/|_____/|______|
`;

var error = `
______ _____  _____   ____   _____  
|  ____|  __ \|  __ \ / __ \ / ____| 
| |__  | |__) | |__) | |  | | (___   
|  __| |  _  /|  _  /| |  | |\___ \  
| |____| | \ \| | \ \| |__| |____) | 
|______|_|  \_\_|  \_\\____/|_____/  
`;

function getConsoleTextCoverage(data, gameScore) {
	var valori_csv = extractThirdColumn(data);
	var consoleText = 
`============================== Results ===============================
Il tuo punteggio: ${gameScore}pt
----------------------------------------------------------------------
la tua coverage:  ${valori_csv[0]*100}% LOC
============================== Coverage ===============================
Il tuo punteggio EvoSuite: ${valori_csv[1]*100}% Branch
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[2]*100}% Exception
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[3]*100}% WeakMutation
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[4]*100}% Output
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[5]*100}% Method
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[6]*100}% MethodNoException
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[7]*100}% CBranch
======================================================================`;

	// Restituisce il testo generato
	return consoleText;
}

function getConsoleTextRun(data, punteggioJacoco, punteggioRobot, gameScore) {
	var valori_csv = extractThirdColumn(data);
	var consoleText2 = (valori_csv[0]*100) >= punteggioRobot ? you_win : you_lose;
	consoleText =
`===================================================================== \n` +
		consoleText2 +
		"\n" +
`============================== Results ===============================
Il tuo punteggio: ${gameScore}pt
----------------------------------------------------------------------
la tua coverage:  ${valori_csv[0]*100}% LOC
----------------------------------------------------------------------
Il tuo punteggio Jacoco:   ${punteggioJacoco}% LOC
----------------------------------------------------------------------
Il punteggio del robot:    ${punteggioRobot}% LOC
============================== Coverage ===============================
Il tuo punteggio EvoSuite: ${valori_csv[1]*100}% Branch
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[2]*100}% Exception
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[3]*100}% WeakMutation
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[4]*100}% Output
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[5]*100}% Method
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[6]*100}% MethodNoException
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[7]*100}% CBranch
======================================================================`;

	// Restituisce il testo generato
	return consoleText;
}

function getConsoleTextError(){
	return error + "\n Ci sono stati errori di compilazione, controlla la console !";
}

// Funzione per avviare il gioco utilizzando ajaxRequest
async function startGame(data) {
	try {
		// Utilizziamo la funzione ajaxRequest per la chiamata POST
		const response = await ajaxRequest(
			"/StartGame",
			"POST",
			data,
			true,
			"text"
		);

		console.log("Partita iniziata con successo:", response);
	} catch (error) {
		// Assicurati che error sia una stringa prima di usare includes
		const errorMessage = error.message || error.toString();

		if (errorMessage.includes("errore esiste già la partita")) {
			console.log("Il messaggio d'errore indica che esiste già la partita.");
			openModalWithText("Start Game", "Hai già un match in corso!");
		} 

		console.log("[start Game]", errorMessage);
	}
}

function toggleLoading(showSpinner, divId, buttonId) {
	const divElement = document.getElementById(divId);
	const button = document.getElementById(buttonId);

	if (!divElement) {
		console.error(`Elemento con ID "${divId}" non trovato.`);
		return;
	}

	const spinner = divElement.querySelector(".spinner-border");
	const statusText = divElement.querySelector('[role="status"]');
	const icon = divElement.querySelector("i");

	if (showSpinner) {
		spinner.style.display = "inline-block"; // Mostra lo spinner
		statusText.innerText = "Loading..."; // Mostra il testo "Loading..."
		icon.style.display = "none"; // Nascondi l'icona
		button.disabled = true;
	} else {
		spinner.style.display = "none"; // Nascondi lo spinner
		statusText.innerText = "Play"; // Nascondi il testo "Loading..."
		icon.style.display = "inline-block"; // Mostra l'icona
		button.disabled = false;
	}
}

function highlightCodeCoverage(reportContent) {
	// Analizza il contenuto del file di output di JaCoCo per individuare le righe coperte, non coperte e parzialmente coperte
	// Applica lo stile appropriato alle righe del tuo editor

	var coveredLines = [];
	var uncoveredLines = [];
	var partiallyCoveredLines = [];

	reportContent.querySelectorAll("line").forEach(function (line) {
		if (line.getAttribute("mi") == 0)
			coveredLines.push(line.getAttribute("nr"));
		else if (
			line.getAttribute("cb") /
				(line.getAttribute("mb") + line.getAttribute("cb")) ==
			2 / 4
		)
			partiallyCoveredLines.push(line.getAttribute("nr"));
		else uncoveredLines.push(line.getAttribute("nr"));
	});

	coveredLines.forEach(function (lineNumber) {
		editor_robot.removeLineClass(lineNumber - 2, "wrap", "uncovered-line");
		editor_robot.removeLineClass(
			lineNumber - 2,
			"wrap",
			"partially-covered-line"
		);
		editor_robot.addLineClass(lineNumber - 2, "wrap", "covered-line");
	});

	uncoveredLines.forEach(function (lineNumber) {
		editor_robot.removeLineClass(lineNumber - 2, "wrap", "covered-line");
		editor_robot.removeLineClass(
			lineNumber - 2,
			"wrap",
			"partially-covered-line"
		);
		editor_robot.addLineClass(lineNumber - 2, "wrap", "uncovered-line");
	});

	partiallyCoveredLines.forEach(function (lineNumber) {
		editor_robot.removeLineClass(lineNumber - 2, "wrap", "uncovered-line");
		editor_robot.removeLineClass(lineNumber - 2, "wrap", "covered-line");
		editor_robot.addLineClass(lineNumber - 2, "wrap", "partially-covered-line");
	});
}

// Funzione per ottenere i dati del form da inviare
function getFormData() {
	const formData = new FormData();
	const className = localStorage.getItem("underTestClassName");

	//formData.append("testingClassName", `Test${className}.java`);
	formData.append("testingClassCode", editor_utente.getValue());
	formData.append("underTestClassName", `${className}.java`);
	formData.append("underTestClassCode", editor_robot.getValue());
	formData.append("className", className);
	formData.append("playerId", String(parseJwt(getCookie("jwt")).userId));
	formData.append("turnId", localStorage.getItem("turnId"));
	formData.append("difficulty", localStorage.getItem("difficulty"));
	formData.append("type", localStorage.getItem("robot"));
	formData.append("order", orderTurno);
	formData.append("username", localStorage.getItem("username"));
	formData.append("testClassId", className);
	return formData;
}

async function ajaxRequest(
	url,
	method = "POST",
	data = null,
	isJson = true,
	dataType = "json"
) {
	try {
		const options = {
			url: url,
			type: method,
			dataType: dataType,
			processData: isJson, // Set to true to encode data properly
			contentType: isJson
				? "application/x-www-form-urlencoded; charset=UTF-8"
				: false,
			data: isJson && data ? $.param(data) : data, // Convert data to URL-encoded string
		};

		const response = await $.ajax(options);
		return response;
	} catch (error) {
		console.error("Si è verificato un errore:", error);
		throw error;
	}
}

function controlloScalata(
	iswin,
	current_round_scalata,
	total_rounds_scalata,
	displayRobotPoints
) {
	// Check if the player has won the round
	if (isWin) {
		/*The player has won the round, check if the player has 
        completed the Scalata (current_round_scalata == total_rounds_scalata)
        */
		if (current_round_scalata == total_rounds_scalata) {
			// alert("Hai completato la scalata!");
			calculateFinalScore(localStorage.getItem("scalataId"))
				.then((data) => {
					console.log("calculateFinalScore response: ", data.finalScore);
					closeScalata(
						localStorage.getItem("scalataId"),
						true,
						data.finalScore,
						current_round_scalata
					).then((data) => {
						swal(
							"Complimenti!",
							`Hai completato la scalata!\n${displayRobotPoints}\n A breve verrai reindirizzato alla classifica.`,
							"success"
						).then((value) => {
							window.location.href = "/leaderboardScalata";
						});
					});
				})
				.catch((error) => {
					console.log("Error:", error);
					swal(
						"Errore!",
						"Si è verificato un errore durante il recupero dei dati. Riprovare.",
						"error"
					);
				});
		} else {
			//The player has completed the round, not the Scalata
			swal(
				"Complimenti!",
				`Hai completato il round ${current_round_scalata}/${total_rounds_scalata}!\n${displayRobotPoints}`,
				"success"
			).then((value) => {
				current_round_scalata++;
				localStorage.setItem("current_round_scalata", current_round_scalata);
				classe = getScalataClasse(
					current_round_scalata - 1,
					localStorage.getItem("scalata_classes")
				);
				localStorage.setItem("classe", classe);
				console.log(
					"[editor.js] classes in scalata: " +
						localStorage.getItem("scalata_classes") +
						"\n\
                      selected class: " +
						classe
				);
				incrementScalataRound(
					localStorage.getItem("scalataId"),
					current_round_scalata
				)
					.then((data) => {
						console.log(
							"[editor.js] Creating new game for next round in scalata with parameters: \
                Robot: evosuite\n\
                Classe: " +
								classe +
								"\n\
                Difficulty: 1\n\
                ScalataId: " +
								localStorage.getItem("scalataId") +
								"\n\
                Username: " +
								localStorage.getItem("username") +
								"."
						);
						createGame(
							"evosuite",
							classe,
							1,
							localStorage.getItem("scalataId"),
							localStorage.getItem("username")
						).then((data) => {
							console.log(data);
							window.location.href = "/editor";
						});
					})
					.catch((error) => {
						console.log("Error:", error);
						swal(
							"Errore!",
							"Si è verificato un errore durante il recupero dei dati. Riprovare.",
							"error"
						);
					});
			});
		}
	} else {
		//The player has lost the round
		closeScalata(
			localStorage.getItem("scalataId"),
			false,
			0,
			current_round_scalata
		)
			.then((data) => {
				console.log("Close Scalata response: ", data);
				swal(
					"Peccato!",
					`Hai perso al round ${current_round_scalata}/${total_rounds_scalata} della scalata, la prossima volta andrà meglio!\n${displayRobotPoints}`,
					"error"
				).then((value) => {
					window.location.href = "/main";
				});
			})
			.catch((error) => {
				console.log("Error:", error);
				swal(
					"Errore!",
					"Si è verificato un errore durante il recupero dei dati. Riprovare.",
					"error"
				);
			});
	}
}
