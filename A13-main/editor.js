const getCookie = (name) => {
	const value = `; ${document.cookie}`;
	const parts = value.split(`; ${name}=`);
	if (parts.length === 2) return parts.pop().split(";").shift();
	return null;
};

const parseJwt = (token) => {
	try {
		return JSON.parse(atob(token.split(".")[1]));
	} catch (e) {
		return null;
	}
};

//Variabili varie
var turno = 0; // numero di turni giocati fino ad ora
var current_round_scalata = 0; // round corrente
var total_rounds_scalata = 0; // numero totale di rounds
var selectedScalata = "";
var isWin = false; // flag per indicare se il giocatore ha vinto o perso
var orderTurno = 0;
var perc_robot = "0"; // percentuale di copertura del robot scelto
var gameScore = 0;
var locGiocatore = 0;

//chiamata a /StartGame
const data = {
	playerID: String(parseJwt(getCookie("jwt")).userId),
	type_robot: localStorage.getItem("robot"),
	difficulty: localStorage.getItem("difficulty"),
	mode: localStorage.getItem("modalita"),
	underTestClassName: localStorage.getItem("underTestClassName")
};
await startGame(data);

// Elemento per il tasto storico
const storico = document.getElementById("storico");

// Aggiungere l'event listener per il tasto "storico"
storico.addEventListener("click", function () {
	toggleLoading(true);

	// Verificare se esiste uno storico
	if (orderTurno === 0) {
		showAlert("Non esiste ancora uno storico dei test");
	} else if (localStorage.getItem("gameId") === "null") {
		showAlert("Impossibile accedere allo storico. La partita è terminata");
	} else {
		toggleLoading(false);
		fetchTurns();
	}
});

// Elemento del pulsante "Play/Submit"
const runButton = document.getElementById("runButton");

// Funzione principale per la gestione del click del pulsante
runButton.addEventListener("click", async function () {
	toggleLoading(true);

	try {
		const formData = getFormData();

		// Prima richiesta AJAX per eseguire il test
		const response = await ajaxRequest("/run", "POST",formData, false, "json");

		const { robotScore, gameScore, outCompile, coverage, win } = response;
		consoleArea.setValue(outCompile);
		highlightCodeCoverage($.parseXML(coverage));

		showGameResult(win, gameScore);
		orderTurno++;

		const url = createApiUrl(formData, orderTurno);
		console.log("URL post on: " + url);

		// Seconda richiesta AJAX per inviare il codice di test
		toggleLoading(false);
		const javaCode = editor.getValue();
		const csvContent = await ajaxRequest(url, "POST",javaCode, false, "json");

		consoleArea2.setValue(getConsoleTextRun(csvContent, gameScore, robotScore));
		handleScalataMode();
	} catch (error) {
		swal(
			"Errore",
			"Si è verificato un errore durante l'esecuzione. Riprovare.",
			"error"
		);
	} finally {
		toggleLoading(false);
	}
});

// TASTO RUN (COVERAGE)
var coverageButton = document.getElementById("coverageButton");
coverageButton.addEventListener("click", async function () {
	const formData = getFormData();
	toggleLoading(true);

	try {
		urlJacoco = "http://remoteccc-app-1:1234/compile-and-codecoverage";
		// Richiesta per ottenere il report di JaCoCo
		const reportContent = await ajaxRequest(urlJacoco, "POST", formData, false, "xml");
		console.log("(POST /getJaCoCoReport)", reportContent);
		highlightCodeCoverage(reportContent);

		orderTurno++;
		const url = createApiUrl(formData, orderTurno);
		const javaCode = editor.getValue();

		// Richiesta per inviare il codice di test
		toggleLoading(false);
		const csvContent = await ajaxRequest(url, "POST",javaCode, false, "text");
		consoleArea.setValue(getConsoleTextCoverage(csvContent));

		const turnId = localStorage.getItem("turnId");
		await updateOrCreateTurn(turnId, locGiocatore, orderTurno);
	} catch (error) {
		toggleLoading(false);
		alert(
			"Si è verificato un errore. Assicurati prima che la compilazione vada a buon fine!"
		);
		console.error(
			"Errore durante il recupero del file di output di JaCoCo o la gestione del turno:",
			error
		);
	} finally {
		toggleLoading(false);
	}
});

// GAME INFO BUTTON
function openInfoModal() {
	// Open the modal
	var infoModal = document.getElementById("infoModal");

	/* Set the display property of the modal to "block" to make it visible
  if it was previously hidden
  */
	infoModal.style.display = "block";

	//Get a reference to the modal2-content element
	var modal2Content = document.querySelector("#infoModal .modal2-content");

	//Retrieve the gameID from the local storage
	var gameIDj = localStorage.getItem("gameId");

	// Seleziona tutti gli elementi figli tranne il primo (che è il pulsante per chiudere il modale)
	var childrenToRemove = Array.from(modal2Content.children).slice(1);

	// Rimuovi tutti gli elementi figli eccetto il primo
	childrenToRemove.forEach((child) => {
		modal2Content.removeChild(child);
	});

	// Aggiungi il titolo
	var titleElement = document.createElement("h2");
	titleElement.classList.add("modal2-title");
	titleElement.textContent = "GAME INFO";
	modal2Content.appendChild(titleElement);

	if (gameIDj != "null") {
		// Aggiungi i campi aggiornati
		var idUtenteElement = document.createElement("p");
		var usernamej = parseJwt(getCookie("jwt")).userId;
		idUtenteElement.textContent = "UserID: " + usernamej;
		modal2Content.appendChild(idUtenteElement);

		var usernameElement = document.createElement("p");
		var username = parseJwt(getCookie("jwt")).sub.toString();
		usernameElement.textContent = "Username: " + username;
		modal2Content.appendChild(usernameElement);

		var idPartitaElement = document.createElement("p");
		idPartitaElement.textContent = "GameID: " + gameIDj;
		modal2Content.appendChild(idPartitaElement);

		// (MODIFICA 23/04/2024): Add the name of the CUT
		var CUTName = document.createElement("p");
		CUTName.textContent = "Class Under Test: " + localStorage.getItem("classe");
		modal2Content.appendChild(CUTName);

		var idTurnoElement = document.createElement("p");
		var turnoIDj = orderTurno + 1;
		idTurnoElement.textContent = "Turno: " + turnoIDj;
		modal2Content.appendChild(idTurnoElement);

		var robotSceltoElement = document.createElement("p");
		var robotj = localStorage.getItem("robot");
		robotSceltoElement.textContent = "Robot:" + robotj;
		modal2Content.appendChild(robotSceltoElement);

		var difficoltaElement = document.createElement("p");
		difficoltaElement.textContent =
			"Livello:" + localStorage.getItem("difficulty");
		modal2Content.appendChild(difficoltaElement);
	}
}
function closeInfoModal() {
	var infoModal = document.getElementById("infoModal");
	infoModal.style.display = "none";
}

window.onbeforeunload = function () {
	if (localStorage.getItem("modalita") !== "Scalata") {
		localStorage.setItem("gameId", null);
		localStorage.setItem("turnId", null);
		localStorage.setItem("classe", null);
		localStorage.setItem("robot", null);
		localStorage.setItem("difficulty", null);
	}
};

//codice custom per l'integrabilità con thymeleaf
var robot = "[[${robot}]]";
var username = "[[${username}]]";
var gameIDJ = "[[${gameIDj}]]";
