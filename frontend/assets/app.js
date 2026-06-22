const app = document.getElementById("app");
const apiBase = document.body.dataset.apiBase || "http://localhost:8080/api";

const STORAGE = {
  token: "skyteam.token",
  displayName: "skyteam.displayName",
  selectedStudentId: "skyteam.selectedStudentId"
};

const TABS = [
  { id: "dashboard", label: "Dashboard" },
  { id: "students", label: "Schüler" },
  { id: "theory", label: "Theorie" },
  { id: "practice", label: "Praxis" },
  { id: "exams", label: "Prüfungen" },
  { id: "planning", label: "Planung" },
  { id: "completion", label: "Abschluss" }
];

const STATUS_LABELS = {
  NICHT_GESTARTET: "Nicht gestartet",
  AKTIV: "Aktiv",
  THEORIE_BEREIT: "Theorie bereit",
  PRAXIS_BEREIT: "Praxis bereit",
  PRUEFUNGEN_OFFEN: "Prüfungen offen",
  ABGESCHLOSSEN: "Abgeschlossen",
  ABGEBROCHEN: "Abgebrochen"
};

const FIELD_LABELS = {
  username: "Benutzername",
  password: "Passwort",
  name: "Nachname",
  vorname: "Vorname",
  thema: "Thema",
  termin: "Termin",
  dauerMinuten: "Dauer",
  dozent: "Dozent",
  wunschtermin: "Wunschtermin",
  datum: "Datum",
  startzeit: "Startzeit",
  endzeit: "Endzeit",
  fluglehrer: "Fluglehrer/Pilot",
  flugzeugId: "Flugzeug",
  startFlughafen: "Startflughafen",
  zielFlughafen: "Zielflughafen",
  ausbildungsinhalt: "Ausbildungsinhalt",
  pruefungId: "Prüfung",
  pruefungsart: "Prüfungsart",
  bestanden: "Ergebnis"
};

const PILOT_OPTIONS = [
  { id: "P001", name: "Max Mueller", license: "FI(A)", available: true, note: "verfuegbar" },
  { id: "P002", name: "Erika Schmidt", license: "FI(A)", available: true, note: "verfuegbar" },
  { id: "P003", name: "Lina Weber", license: "PPL(A)", available: false, note: "kein Fluglehrer" },
  { id: "P004", name: "Tom Neumann", license: "FI(A)", available: false, note: "nicht verfuegbar" }
];

const AIRCRAFT_OPTIONS = [
  { id: "FZ002", type: "Cessna 172", status: "einsatzbereit", maintenance: "keine offene Wartung", available: true },
  { id: "FZ003", type: "Piper PA-28", status: "einsatzbereit", maintenance: "keine offene Wartung", available: true },
  { id: "FZ001", type: "Cessna 152", status: "in_wartung", maintenance: "100h Kontrolle faellig", available: false },
  { id: "FZ004", type: "Diamond DA20", status: "gesperrt", maintenance: "gesperrt", available: false }
];

const PLANNING_COLUMNS = [
  { id: "open-theory", title: "Offene Theorieanfragen", accepts: ["theory-planned"] },
  { id: "planned-theory", title: "Geplante Theoriestunden", accepts: ["theory-request"] },
  { id: "open-practice", title: "Offene Praxisanfragen", accepts: [] },
  { id: "planned-flights", title: "Geplante Flugstunden", accepts: ["practice-request"] },
  { id: "exams", title: "Prüfungen", accepts: [] }
];

const state = {
  token: localStorage.getItem(STORAGE.token) || "",
  displayName: localStorage.getItem(STORAGE.displayName) || "",
  selectedStudentId: localStorage.getItem(STORAGE.selectedStudentId) || "",
  view: "dashboard",
  loading: false,
  action: false,
  error: "",
  notice: "",
  students: [],
  studentStatuses: {},
  studentFilters: {
    name: "",
    vorname: "",
    status: "",
    contract: ""
  },
  tableSearch: {
    courses: "",
    exams: ""
  },
  practiceSelection: {
    pilotId: "P001",
    aircraftId: "FZ002"
  },
  examResultSelection: "",
  repeatMarkers: {},
  modal: null,
  contextMenu: null,
  planningDrag: null,
  selectedStudent: null,
  status: null,
  theorie: null,
  praxis: null,
  pruefungen: []
};

SkyTeamApi.configure({
  apiBase,
  getToken: () => state.token
});

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(String(value).length === 10 ? `${value}T00:00` : value);
  if (Number.isNaN(date.getTime())) {
    return escapeHtml(value);
  }
  return new Intl.DateTimeFormat("de-DE", { dateStyle: "medium" }).format(date);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return escapeHtml(value);
  }
  return new Intl.DateTimeFormat("de-DE", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(date);
}

function formatHours(value) {
  const number = Number(value || 0);
  return number.toLocaleString("de-DE", {
    minimumFractionDigits: number % 1 === 0 ? 0 : 1,
    maximumFractionDigits: 1
  });
}

function todayDate(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function studentFullName(student) {
  if (!student) {
    return "Kein Schüler ausgewählt";
  }
  return `${student.vorname || ""} ${student.name || ""}`.trim() || student.id;
}

function selectedStudent() {
  return state.selectedStudent || state.students.find((student) => student.id === state.selectedStudentId) || null;
}

function statusLabel(code) {
  return STATUS_LABELS[code] || code || "Unbekannt";
}

function yesNo(value) {
  return value ? "Ja" : "Nein";
}

function normalizeText(value) {
  return String(value ?? "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function includesText(value, query) {
  return normalizeText(value).includes(normalizeText(query));
}

function pilotById(id) {
  return PILOT_OPTIONS.find((pilot) => pilot.id === id) || null;
}

function aircraftById(id) {
  return AIRCRAFT_OPTIONS.find((aircraft) => aircraft.id === id) || null;
}

function studentStatus(studentId) {
  return state.studentStatuses[studentId] || {};
}

function contractStatus(student) {
  return studentStatus(student.id).vertragsStatus || "-";
}

function examStatus(exam) {
  const type = normalizeText(exam?.typ || "");
  if (type.includes("nicht bestanden")) {
    return { label: "Nicht bestanden", tone: "warning" };
  }
  if (type.includes("bestanden")) {
    return { label: "Bestanden", tone: "success" };
  }
  return { label: "Angemeldet", tone: "info" };
}

function theoryWorkflowStatus(status = {}) {
  if (status.theorieBestanden) {
    return { label: "Bestanden", tone: "success", detail: "Theorieprüfung bestanden." };
  }
  if (status.theoriePruefungFreigeschaltet) {
    return { label: "Bereit", tone: "info", detail: "Mindeststunden erreicht, Prüfung kann angemeldet werden." };
  }
  if (Number(status.theorieStunden || 0) > 0) {
    return { label: "Offen", tone: "warning", detail: "Theorieausbildung läuft, Mindeststunden fehlen noch." };
  }
  return { label: "Offen", tone: "neutral", detail: "Noch keine Theoriestunden erfasst." };
}

function practiceWorkflowStatus(status = {}) {
  if (status.praxisBestanden) {
    return { label: "Bestanden", tone: "success", detail: "Praxisprüfung bestanden." };
  }
  if (status.praxisPruefungFreigeschaltet) {
    return { label: "Bereit", tone: "info", detail: "Mindestflugstunden erreicht, Prüfung kann angemeldet werden." };
  }
  if (Number(status.flugStunden || 0) > 0) {
    return { label: "Offen", tone: "warning", detail: "Praxisausbildung läuft, Mindestflugstunden fehlen noch." };
  }
  return { label: "Offen", tone: "neutral", detail: "Noch keine Flugstunden erfasst." };
}

function examWorkflowStatus(status = {}, exams = []) {
  if (exams.some(isFailedExam)) {
    return { label: "Nicht bestanden", tone: "warning", detail: "Mindestens eine Prüfung muss wiederholt werden." };
  }
  if (status.theorieBestanden && status.praxisBestanden) {
    return { label: "Bestanden", tone: "success", detail: "Theorie und Praxis sind bestanden." };
  }
  if (exams.length > 0) {
    return { label: "Angemeldet", tone: "info", detail: `${exams.length} Prüfung(en) erfasst.` };
  }
  return { label: "Offen", tone: "neutral", detail: "Noch keine Prüfung angemeldet." };
}

function trainingWorkflowStatus(status = {}) {
  if (status.status === "ABGESCHLOSSEN") {
    return { label: "Abgeschlossen", tone: "success", detail: "Ausbildung ist abgeschlossen." };
  }
  if (status.status === "ABGEBROCHEN") {
    return { label: "Abgebrochen", tone: "warning", detail: "Ausbildung wurde abgebrochen." };
  }
  if (status.status === "NICHT_GESTARTET") {
    return { label: "Nicht gestartet", tone: "neutral", detail: "Ausbildung wurde noch nicht begonnen." };
  }
  return { label: statusLabel(status.status || "AKTIV"), tone: "info", detail: "Ausbildung ist aktiv." };
}

function isFailedExam(exam) {
  return examStatus(exam).label === "Nicht bestanden";
}

function contextButton(type, id, label = "Weitere Aktionen") {
  return `
    <button class="icon-button" type="button" aria-label="${escapeHtml(label)}" title="${escapeHtml(label)}"
      data-context-trigger="${escapeHtml(type)}" data-context-id="${escapeHtml(id)}">
      ...
    </button>
  `;
}

function contextMenuPosition(x, y) {
  const width = 240;
  const height = 260;
  const margin = 10;
  return {
    x: Math.max(margin, Math.min(x, window.innerWidth - width - margin)),
    y: Math.max(margin, Math.min(y, window.innerHeight - height - margin))
  };
}

function openContextMenu(type, id, x, y) {
  state.contextMenu = {
    type,
    id,
    ...contextMenuPosition(x, y)
  };
  render();
}

function closeContextMenu() {
  if (state.contextMenu) {
    state.contextMenu = null;
  }
}

function progressPercent(current, target) {
  if (!target || target <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round((Number(current || 0) / Number(target)) * 100)));
}

function renderProgress(current, target) {
  const percent = progressPercent(current, target);
  return `
    <div class="progress" aria-label="${percent} Prozent">
      <span style="width: ${percent}%"></span>
    </div>
    <p class="metric-detail">${formatHours(current)} von ${formatHours(target)} Stunden</p>
  `;
}

function renderBadge(text, tone = "neutral") {
  return `<span class="badge ${tone}">${escapeHtml(text)}</span>`;
}

function renderBooleanBadge(value, positiveText, negativeText) {
  return renderBadge(value ? positiveText : negativeText, value ? "success" : "warning");
}

function isAborted(status) {
  return status?.status === "ABGEBROCHEN";
}

function isCompleted(status) {
  return status?.status === "ABGESCHLOSSEN";
}

function blockedWhenAborted(status, fallback) {
  return isAborted(status) ? "blocked" : fallback;
}

function stepDone(condition, status) {
  return blockedWhenAborted(status, condition ? "done" : "open");
}

function stepActive(condition, status) {
  return blockedWhenAborted(status, condition ? "active" : "open");
}

function processStepState({ done = false, active = false, blocked = false }, status) {
  if (isAborted(status) || blocked) {
    return "blocked";
  }
  if (done) {
    return "done";
  }
  if (active) {
    return "active";
  }
  return "open";
}

function processStateLabel(stateName) {
  return {
    open: "offen",
    active: "aktiv",
    done: "erledigt",
    blocked: "blockiert"
  }[stateName] || stateName;
}

function processTone(stateName) {
  return {
    open: "neutral",
    active: "info",
    done: "success",
    blocked: "warning"
  }[stateName] || "neutral";
}

function buildProcessAreas(status = {}) {
  const hasStatus = Boolean(status.schuelerId);
  const hasContract = Boolean(status.vertragsStatus);
  const theoryHours = Number(status.theorieStunden || 0);
  const flightHours = Number(status.flugStunden || 0);
  const hasTheoryStarted = theoryHours > 0;
  const hasPracticeStarted = flightHours > 0;
  const theoryReady = Boolean(status.theoriePruefungFreigeschaltet);
  const practiceReady = Boolean(status.praxisPruefungFreigeschaltet);
  const theoryPassed = Boolean(status.theorieBestanden);
  const practicePassed = Boolean(status.praxisBestanden);
  const bothPassed = theoryPassed && practicePassed;
  const complete = isCompleted(status);
  const aborted = isAborted(status);

  const theoryOpen = hasStatus && !theoryPassed && hasTheoryStarted && !theoryReady;
  const theoryExamOpen = theoryReady && !theoryPassed;
  const theoryRepeat = theoryReady && !theoryPassed;
  const practiceOpen = hasStatus && !practicePassed && hasPracticeStarted && !practiceReady;
  const practiceExamOpen = practiceReady && !practicePassed;
  const practiceRepeat = practiceReady && !practicePassed;

  return [
    {
      title: "Ausbildungsverwaltung",
      summary: "Vom Ausbildungsstart bis zur Abschlussdokumentation.",
      steps: [
        {
          label: "Ausbildungsanfrage / Ausbildung starten",
          detail: hasStatus ? `Schüler ${status.schuelerId}` : "Kein Schülerstatus geladen",
          state: processStepState({ done: hasStatus, active: !hasStatus }, status)
        },
        {
          label: "Schülerdaten prüfen",
          detail: hasStatus ? "Schülerakte vorhanden" : "Schülerauswahl fehlt",
          state: processStepState({ done: hasStatus }, status)
        },
        {
          label: "Ausbildungsvertrag prüfen",
          detail: hasContract ? status.vertragsStatus : "Vertragsstatus fehlt",
          state: processStepState({ done: hasContract && !aborted, active: hasStatus && !hasContract }, status)
        },
        {
          label: "Ausbildungsplan anlegen",
          detail: hasTheoryStarted || hasPracticeStarted ? "Theorie/Praxis im Plan sichtbar" : "Noch keine Ausbildungsstunden",
          state: processStepState({
            done: hasTheoryStarted || hasPracticeStarted,
            active: hasStatus && hasContract && !hasTheoryStarted && !hasPracticeStarted
          }, status)
        },
        {
          label: "Ausbildungsabschluss dokumentieren",
          detail: complete ? "Abschluss dokumentiert" : bothPassed ? "Bereit zur Dokumentation" : "Theorie und Praxis müssen bestanden sein",
          state: processStepState({
            done: complete,
            active: bothPassed && !complete,
            blocked: hasStatus && !bothPassed && !aborted
          }, status)
        }
      ]
    },
    {
      title: "Theorieausbildung",
      summary: "Buchung, Stundenfortschritt, Prüfung und Wiederholung.",
      steps: [
        {
          label: "Theoriekurs buchen",
          detail: hasTheoryStarted ? "Theoriedaten vorhanden" : "Nächster Schritt: Kurs buchen",
          state: processStepState({ done: hasTheoryStarted, active: hasStatus && !hasTheoryStarted }, status)
        },
        {
          label: "Theoriekurs durchführen",
          detail: hasTheoryStarted ? "Kursdurchführung im MVP über Buchung abgebildet" : "Wartet auf Buchung",
          state: processStepState({ done: hasTheoryStarted, active: hasStatus && !hasTheoryStarted }, status)
        },
        {
          label: "Theoriestunden erfassen",
          detail: `${formatHours(theoryHours)} h erfasst`,
          state: processStepState({ done: hasTheoryStarted, active: hasStatus && !hasTheoryStarted }, status)
        },
        {
          label: "Restliche Theoriestunden prüfen",
          detail: theoryReady ? "Mindeststunden erreicht" : `${formatHours(10 - Math.min(theoryHours, 10))} h fehlen`,
          state: processStepState({ done: theoryReady, active: theoryOpen }, status)
        },
        {
          label: "Theorieprüfung durchführen",
          detail: theoryPassed ? "Bestanden" : theoryReady ? "Freigeschaltet" : "Mindeststunden fehlen",
          state: processStepState({ done: theoryPassed, active: theoryExamOpen, blocked: hasStatus && !theoryReady && !theoryPassed }, status)
        },
        {
          label: "Theorieergebnis speichern",
          detail: theoryPassed ? "Ergebnis gespeichert" : "Noch kein bestandenes Ergebnis",
          state: processStepState({ done: theoryPassed, active: theoryExamOpen, blocked: hasStatus && !theoryReady && !theoryPassed }, status)
        },
        {
          label: "Theorie wiederholen oder abschließen",
          detail: theoryPassed ? "Theoriezweig abgeschlossen" : theoryRepeat ? "Wiederholung möglich, falls nicht bestanden" : "Prüfung noch nicht erreichbar",
          state: processStepState({ done: theoryPassed, active: theoryRepeat, blocked: hasStatus && !theoryReady && !theoryPassed }, status)
        }
      ]
    },
    {
      title: "Praxisausbildung",
      summary: "Buchung, Verfügbarkeitschecks, Flug, Stunden und Praxisprüfung.",
      steps: [
        {
          label: "Neue Flugstunde buchen",
          detail: hasPracticeStarted ? "Flugstunden vorhanden" : "Nächster Schritt: Flugstunde buchen",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Fluglehrer prüfen",
          detail: hasPracticeStarted ? "Über Praxisbuchung geprüft" : "Blockiert Buchung bei fehlender Verfügbarkeit",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Flugzeug prüfen",
          detail: hasPracticeStarted ? "Über Praxisbuchung geprüft" : "Blockiert Buchung bei fehlender Verfügbarkeit",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Wartungsstatus prüfen",
          detail: hasPracticeStarted ? "Wartungsregel in Buchung angewendet" : "Wartungsrelevante Flugzeuge werden abgelehnt",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Check-in durchführen",
          detail: hasPracticeStarted ? "Im MVP als Teil der Flugstunde sichtbar" : "Wartet auf Flugstunde",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Ausbildungsflug durchführen",
          detail: hasPracticeStarted ? "Ausbildungsflug erfasst" : "Noch kein Ausbildungsflug",
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Flugstunden erfassen",
          detail: `${formatHours(flightHours)} h erfasst`,
          state: processStepState({ done: hasPracticeStarted, active: hasStatus && !hasPracticeStarted }, status)
        },
        {
          label: "Praxisprüfung durchführen",
          detail: practicePassed ? "Bestanden" : practiceReady ? "Freigeschaltet" : "Mindeststunden fehlen",
          state: processStepState({ done: practicePassed, active: practiceExamOpen, blocked: hasStatus && !practiceReady && !practicePassed }, status)
        },
        {
          label: "Praxisergebnis speichern",
          detail: practicePassed ? "Ergebnis gespeichert" : "Noch kein bestandenes Ergebnis",
          state: processStepState({ done: practicePassed, active: practiceExamOpen, blocked: hasStatus && !practiceReady && !practicePassed }, status)
        },
        {
          label: "Praxis wiederholen oder abschließen",
          detail: practicePassed ? "Praxiszweig abgeschlossen" : practiceRepeat ? "Wiederholung möglich, falls nicht bestanden" : "Prüfung noch nicht erreichbar",
          state: processStepState({ done: practicePassed, active: practiceRepeat, blocked: hasStatus && !practiceReady && !practicePassed }, status)
        }
      ]
    }
  ];
}

function processCounts(areas) {
  return areas.flatMap((area) => area.steps).reduce((counts, step) => {
    counts[step.state] = (counts[step.state] || 0) + 1;
    return counts;
  }, { open: 0, active: 0, done: 0, blocked: 0 });
}

function renderProcessStep(step, index) {
  return `
    <li class="process-step ${step.state}">
      <span class="step-index">${index + 1}</span>
      <div>
        <div class="step-head">
          <strong>${escapeHtml(step.label)}</strong>
          ${renderBadge(processStateLabel(step.state), processTone(step.state))}
        </div>
        <p>${escapeHtml(step.detail)}</p>
      </div>
    </li>
  `;
}

function renderProcessArea(area) {
  return `
    <article class="process-lane">
      <div class="process-lane-head">
        <div>
          <h4>${escapeHtml(area.title)}</h4>
          <p>${escapeHtml(area.summary)}</p>
        </div>
      </div>
      <ol class="process-steps">
        ${area.steps.map((step, index) => renderProcessStep(step, index)).join("")}
      </ol>
    </article>
  `;
}

function renderProcessOverview(status) {
  const areas = buildProcessAreas(status);
  const counts = processCounts(areas);
  return `
    <section class="panel process-panel">
      <div class="section-head">
        <div>
          <p class="eyebrow">BPMN-Prozess im MVP</p>
          <h3>Prozess-Fortschritt</h3>
        </div>
        <div class="process-legend">
          ${renderBadge(`${counts.done} erledigt`, "success")}
          ${renderBadge(`${counts.active} aktiv`, "info")}
          ${renderBadge(`${counts.open} offen`, "neutral")}
          ${renderBadge(`${counts.blocked} blockiert`, "warning")}
        </div>
      </div>
      <p class="process-note">
        Die Schrittzustände werden aus <code>GET /api/status/${escapeHtml(status?.schuelerId || "{schuelerId}")}/gesamt</code> abgeleitet.
      </p>
      <div class="process-lanes">
        ${areas.map(renderProcessArea).join("")}
      </div>
    </section>
  `;
}

function readForm(form) {
  const values = Object.fromEntries(new FormData(form).entries());
  Object.keys(values).forEach((key) => {
    if (typeof values[key] === "string") {
      values[key] = values[key].trim();
    }
  });
  return values;
}

function requireFields(values, fields) {
  const missing = fields.find((field) => !values[field]);
  if (missing) {
    throw new Error(`${FIELD_LABELS[missing] || missing} ist erforderlich.`);
  }
}

function positiveInteger(value, field) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${FIELD_LABELS[field] || field} muss eine positive Zahl sein.`);
  }
  return parsed;
}

function ensureStudentId() {
  if (!state.selectedStudentId) {
    throw new Error("Bitte zuerst einen Schüler auswählen.");
  }
  return state.selectedStudentId;
}

function setMessage(type, message) {
  state.error = type === "error" ? message : "";
  state.notice = type === "notice" ? message : "";
}

function sanitizeErrorMessage(message) {
  const text = String(message || "").split(/\r?\n/)[0].trim();
  if (!text) {
    return "Aktion konnte nicht ausgeführt werden.";
  }
  if (/exception|stacktrace|\bat\s+|java\.|nullpointer|sqlexception|runtimeexception/i.test(text)) {
    return "Die Aktion konnte nicht verarbeitet werden. Bitte Eingaben prüfen oder Backend-Status kontrollieren.";
  }
  return text.length > 180 ? `${text.slice(0, 177)}...` : text;
}

function actionMessage() {
  if (!state.action) {
    return "";
  }
  if (state.view === "theory") {
    return "Theorieaktion wird verarbeitet...";
  }
  if (state.view === "practice") {
    return "Praxisaktion wird verarbeitet...";
  }
  if (state.view === "exams") {
    return "Prüfungsergebnis wird gespeichert...";
  }
  if (state.view === "completion") {
    return "Ausbildungsstatus wird aktualisiert...";
  }
  if (state.view === "students") {
    return "Schülerdaten werden aktualisiert...";
  }
  if (state.view === "planning") {
    return "Planungsänderung wird gespeichert...";
  }
  return "Aktion wird verarbeitet...";
}

function resetSelectedData() {
  state.selectedStudent = null;
  state.status = null;
  state.theorie = null;
  state.praxis = null;
  state.pruefungen = [];
  state.examResultSelection = "";
  state.repeatMarkers = {};
  state.planningDrag = null;
}

function clearAuth(message = "") {
  localStorage.removeItem(STORAGE.token);
  localStorage.removeItem(STORAGE.displayName);
  state.token = "";
  state.displayName = "";
  state.loading = false;
  state.action = false;
  state.students = [];
  state.studentStatuses = {};
  state.contextMenu = null;
  state.repeatMarkers = {};
  state.planningDrag = null;
  state.selectedStudentId = "";
  resetSelectedData();
  setMessage(message ? "error" : "notice", message);
  render();
}

function handleApiError(error) {
  if (error && error.status === 401) {
    clearAuth("Bitte erneut anmelden.");
    return true;
  }
  setMessage("error", sanitizeErrorMessage(error?.message));
  return false;
}

async function api(path, options) {
  return SkyTeamApi.request(path, options);
}

async function loadStudents() {
  state.students = await api("/schueler");
  const statuses = await Promise.all(state.students.map(async (student) => {
    try {
      return [student.id, await api(`/status/${encodeURIComponent(student.id)}/gesamt`)];
    } catch (error) {
      return [student.id, null];
    }
  }));
  state.studentStatuses = Object.fromEntries(statuses.filter((entry) => entry[1]));
  if (!state.students.some((student) => student.id === state.selectedStudentId)) {
    state.selectedStudentId = state.students[0]?.id || "";
  }
  if (state.selectedStudentId) {
    localStorage.setItem(STORAGE.selectedStudentId, state.selectedStudentId);
  } else {
    localStorage.removeItem(STORAGE.selectedStudentId);
  }
}

async function loadSelectedStudentData() {
  if (!state.selectedStudentId) {
    resetSelectedData();
    return;
  }

  const previousStudentId = state.selectedStudent?.id;
  const id = encodeURIComponent(state.selectedStudentId);
  const [student, status, theorie, praxis, pruefungen] = await Promise.all([
    api(`/schueler/${id}`),
    api(`/status/${id}/gesamt`),
    api(`/theorie/${id}`),
    api(`/praxis/${id}`),
    api(`/pruefung/${id}`)
  ]);

  if (previousStudentId !== student.id) {
    state.repeatMarkers = {};
  }
  state.selectedStudent = student;
  state.status = status;
  state.studentStatuses[student.id] = status;
  state.theorie = theorie;
  state.praxis = praxis;
  state.pruefungen = pruefungen;
  if (!pruefungen.some((exam) => exam.id === state.examResultSelection)) {
    state.examResultSelection = pruefungen[0]?.id || "";
  }
}

async function refreshAll({ keepNotice = false } = {}) {
  state.loading = true;
  if (!keepNotice) {
    setMessage("notice", "");
  }
  render();

  try {
    await loadStudents();
    await loadSelectedStudentData();
    state.error = "";
  } catch (error) {
    if (!handleApiError(error)) {
      resetSelectedData();
    }
  } finally {
    state.loading = false;
    render();
  }
}

async function runAction(operation, successMessage) {
  state.action = true;
  setMessage("notice", "");
  render();

  try {
    await operation();
    setMessage("notice", successMessage);
    await refreshAll({ keepNotice: true });
  } catch (error) {
    if (!handleApiError(error)) {
      render();
    }
  } finally {
    state.action = false;
    render();
  }
}

function render() {
  if (!state.token) {
    renderLogin();
    return;
  }
  renderApplication();
}

function renderLogin() {
  app.innerHTML = `
    <main class="login-page">
      <section class="login-panel">
        <p class="eyebrow">SkyTeam Flight School</p>
        <h1>Anmelden</h1>
        <p class="muted">Demo-Zugang: <strong>demo</strong> / <strong>demo</strong></p>
        ${state.error ? `<p class="alert error">${escapeHtml(state.error)}</p>` : ""}
        <form id="loginForm" class="form-grid single">
          <label>
            Benutzername
            <input name="username" autocomplete="username" value="demo" required>
          </label>
          <label>
            Passwort
            <input name="password" type="password" autocomplete="current-password" value="demo" required>
          </label>
          <button class="button primary" type="submit" ${state.action ? "disabled" : ""}>
            ${state.action ? "Anmeldung läuft..." : "Anmelden"}
          </button>
        </form>
      </section>
    </main>
  `;
}

function renderApplication() {
  app.innerHTML = `
    <div class="app-shell">
      <header class="app-header">
        <div class="brand">
          <div class="brand-mark">
            <img src="assets/logo.png" alt="SkyTeam Logo" onerror="this.closest('.brand-mark').classList.add('logo-missing'); this.remove();">
            <span aria-hidden="true">ST</span>
          </div>
          <div>
            <p class="eyebrow">Flight-School-WebApp</p>
            <h1>SkyTeam Flight School</h1>
          </div>
        </div>
        <div class="session">
          <span>${renderBadge(`Angemeldet: ${state.displayName || "demo"}`, "success")}</span>
          <button class="button secondary" id="refreshButton" type="button" ${state.loading || state.action ? "disabled" : ""}>Aktualisieren</button>
          <button class="button ghost" id="logoutButton" type="button">Logout</button>
        </div>
      </header>

      <nav class="tabs" aria-label="Hauptnavigation">
        ${TABS.map((tab) => `
          <button class="tab ${state.view === tab.id ? "active" : ""}" type="button" data-tab="${tab.id}">
            ${escapeHtml(tab.label)}
          </button>
        `).join("")}
      </nav>

      <main class="content">
        ${renderStudentContext()}
        ${state.error ? `<p class="alert error">${escapeHtml(state.error)}</p>` : ""}
        ${state.notice ? `<p class="alert success">${escapeHtml(state.notice)}</p>` : ""}
        ${state.action ? `<p class="alert info"><span class="mini-spinner"></span>${escapeHtml(actionMessage())}</p>` : ""}
        ${state.loading ? renderLoading() : renderCurrentView()}
      </main>
      ${renderSelectionModal()}
      ${renderContextMenu()}
    </div>
  `;
}

function renderContextMenu() {
  if (!state.contextMenu) {
    return "";
  }

  const items = contextMenuItems(state.contextMenu);
  if (!items.length) {
    return "";
  }

  return `
    <nav class="context-menu" role="menu" style="left: ${state.contextMenu.x}px; top: ${state.contextMenu.y}px;">
      ${items.map((item) => `
        <button type="button" role="menuitem" data-context-action="${escapeHtml(item.action)}"
          ${item.disabled ? "disabled" : ""} title="${escapeHtml(item.disabled ? item.hint || "" : "")}">
          <span>${escapeHtml(item.label)}</span>
          ${item.disabled ? `<small>${escapeHtml(item.hint || "Nicht verfügbar")}</small>` : ""}
        </button>
      `).join("")}
    </nav>
  `;
}

function contextMenuItems(menu) {
  if (menu.type === "student") {
    const student = state.students.find((entry) => entry.id === menu.id);
    if (!student) {
      return [];
    }
    return [
      { action: "student-details", label: "Details anzeigen" },
      { action: "student-status", label: "Status anzeigen" },
      { action: "student-theory", label: "Theorie öffnen" },
      { action: "student-practice", label: "Praxis öffnen" },
      { action: "student-exams", label: "Prüfungen öffnen" }
    ];
  }

  if (menu.type === "course") {
    const course = state.theorie?.kurse?.find((entry) => entry.id === menu.id);
    if (!course) {
      return [];
    }
    const unlocked = Boolean(state.theorie?.fortschritt?.theoriePruefungFreigeschaltet || state.status?.theoriePruefungFreigeschaltet);
    return [
      { action: "course-details", label: "Details anzeigen" },
      { action: "course-theory-exam", label: "Prüfung anmelden", disabled: !unlocked || state.action, hint: "Mindeststunden fehlen" },
      { action: "course-cancel", label: "Stornieren", disabled: state.action, hint: "Aktion läuft" }
    ];
  }

  if (menu.type === "flight") {
    const flight = state.praxis?.fluege?.find((entry) => entry.id === menu.id);
    if (!flight) {
      return [];
    }
    const unlocked = Boolean(state.praxis?.fortschritt?.praxisPruefungFreigeschaltet || state.status?.praxisPruefungFreigeschaltet);
    return [
      { action: "flight-details", label: "Details anzeigen" },
      { action: "flight-practice-exam", label: "Praxisprüfung anmelden", disabled: !unlocked || state.action, hint: "Mindestflugstunden fehlen" },
      { action: "flight-cancel", label: "Stornieren", disabled: state.action, hint: "Aktion läuft" }
    ];
  }

  if (menu.type === "exam") {
    const exam = state.pruefungen.find((entry) => entry.id === menu.id);
    return [
      { action: "exam-result", label: "Ergebnis speichern", disabled: state.action, hint: "Aktion läuft" },
      { action: "exam-repeat", label: "Wiederholung markieren", disabled: !exam || !isFailedExam(exam), hint: "Nur bei nicht bestandener Prüfung" }
    ];
  }

  if (menu.type === "aircraft") {
    const aircraft = aircraftById(menu.id);
    if (!aircraft) {
      return [];
    }
    return [
      { action: "aircraft-details", label: "Details anzeigen" },
      { action: "aircraft-maintenance", label: "Wartungsstatus anzeigen" },
      { action: "aircraft-select", label: "Für Buchung auswählen", disabled: !aircraft?.available || state.action, hint: "Flugzeug nicht verfügbar" }
    ];
  }

  return [];
}

function renderStudentContext() {
  const student = selectedStudent();
  const status = student ? studentStatus(student.id) : {};
  const training = student ? trainingWorkflowStatus(status) : null;
  return `
    <section class="student-context">
      <div>
        <p class="eyebrow">Ausgewählter Schüler</p>
        <h2>${escapeHtml(studentFullName(student))}</h2>
        <div class="badge-row context-badges">
          ${student ? renderBadge(student.id, "neutral") : ""}
          ${training ? renderBadge(training.label, training.tone) : ""}
          ${student ? renderBadge(contractStatus(student), "neutral") : ""}
        </div>
      </div>
      <button class="button secondary" type="button" data-tab="students">Schueler suchen</button>
    </section>
  `;
}

function renderLoading() {
  return `
    <section class="panel loading-panel">
      <div class="spinner"></div>
      <span>Daten werden geladen...</span>
    </section>
  `;
}

function renderCurrentView() {
  if (!state.selectedStudentId && state.view !== "students") {
    return renderEmptyState("Bitte zuerst einen Schüler in der Schüleransicht auswählen.");
  }

  switch (state.view) {
    case "students":
      return renderStudentsView();
    case "theory":
      return renderTheoryView();
    case "practice":
      return renderPracticeView();
    case "exams":
      return renderExamsView();
    case "planning":
      return renderPlanningView();
    case "completion":
      return renderCompletionView();
    default:
      return renderDashboardView();
  }
}

function renderEmptyState(message) {
  return `
    <section class="panel empty-state">
      <h3>Keine Daten</h3>
      <p>${escapeHtml(message)}</p>
    </section>
  `;
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b, "de"));
}

function studentFilterOptions(field) {
  if (field === "status") {
    return uniqueValues(state.students.map((student) => studentStatus(student.id).status || ""));
  }
  if (field === "contract") {
    return uniqueValues(state.students.map((student) => contractStatus(student)).filter((value) => value !== "-"));
  }
  return [];
}

function filteredStudents() {
  const filters = state.studentFilters;
  return state.students.filter((student) => {
    const status = studentStatus(student.id);
    return includesText(student.name, filters.name)
      && includesText(student.vorname, filters.vorname)
      && (!filters.status || status.status === filters.status)
      && (!filters.contract || contractStatus(student) === filters.contract);
  });
}

function renderStudentFilters() {
  return `
    <div class="filter-grid">
      <label>Name<input data-student-filter="name" value="${escapeHtml(state.studentFilters.name)}" placeholder="Nachname suchen"></label>
      <label>Vorname<input data-student-filter="vorname" value="${escapeHtml(state.studentFilters.vorname)}" placeholder="Vorname suchen"></label>
      <label>Ausbildungsstatus
        <select data-student-filter="status">
          <option value="">Alle Status</option>
          ${studentFilterOptions("status").map((status) => `
            <option value="${escapeHtml(status)}" ${state.studentFilters.status === status ? "selected" : ""}>${escapeHtml(statusLabel(status))}</option>
          `).join("")}
        </select>
      </label>
      <label>Vertragsstatus
        <select data-student-filter="contract">
          <option value="">Alle Vertragsstatus</option>
          ${studentFilterOptions("contract").map((status) => `
            <option value="${escapeHtml(status)}" ${state.studentFilters.contract === status ? "selected" : ""}>${escapeHtml(status)}</option>
          `).join("")}
        </select>
      </label>
    </div>
  `;
}

function filteredByTableSearch(items, key, toText) {
  const query = state.tableSearch[key] || "";
  if (!query) {
    return items;
  }
  return items.filter((item) => includesText(toText(item), query));
}

function renderTableSearch(key, label, placeholder) {
  return `
    <label class="table-search">
      ${escapeHtml(label)}
      <input data-table-search="${escapeHtml(key)}" value="${escapeHtml(state.tableSearch[key] || "")}" placeholder="${escapeHtml(placeholder)}">
    </label>
  `;
}

function renderStatusSummary(status = {}) {
  const theory = theoryWorkflowStatus(status);
  const practice = practiceWorkflowStatus(status);
  const exams = examWorkflowStatus(status, state.pruefungen);
  const training = trainingWorkflowStatus(status);
  const items = [
    { title: "Theorie", info: theory },
    { title: "Praxis", info: practice },
    { title: "Prüfung", info: exams },
    { title: "Ausbildung", info: training }
  ];
  return `
    <section class="panel status-summary" aria-label="Statusübersicht">
      ${items.map((item) => `
        <article>
          <span>${escapeHtml(item.title)}</span>
          ${renderBadge(item.info.label, item.info.tone)}
          <p>${escapeHtml(item.info.detail)}</p>
        </article>
      `).join("")}
    </section>
  `;
}

function renderDashboardView() {
  const student = selectedStudent();
  const status = state.status || {};
  const theorie = state.theorie?.fortschritt || {};
  const praxis = state.praxis?.fortschritt || {};
  const canComplete = Boolean(status.theorieBestanden && status.praxisBestanden);
  const theoryInfo = theoryWorkflowStatus(status);
  const practiceInfo = practiceWorkflowStatus(status);
  const examInfo = examWorkflowStatus(status, state.pruefungen);
  const trainingInfo = trainingWorkflowStatus(status);

  return `
    <section class="dashboard-grid">
      <article class="panel hero-panel">
        <div>
          <p class="eyebrow">Dashboard</p>
          <h2>${escapeHtml(studentFullName(student))}</h2>
          <p class="muted">Vertrag ${escapeHtml(student?.ausbildungsVertragId || "-")} · ${escapeHtml(status.vertragsStatus || "Status unbekannt")}</p>
        </div>
        ${renderBadge(trainingInfo.label, trainingInfo.tone)}
      </article>

      ${renderStatusSummary(status)}

      <article class="metric-card">
        <span>Ausbildungsstatus</span>
        <strong>${escapeHtml(trainingInfo.label)}</strong>
        <p>${escapeHtml(trainingInfo.detail)}</p>
      </article>

      <article class="metric-card">
        <span>Theorie-Fortschritt</span>
        <strong>${formatHours(theorie.theorieStunden)} h</strong>
        ${renderProgress(theorie.theorieStunden, theorie.mindestTheorieStunden)}
        ${renderBadge(theoryInfo.label, theoryInfo.tone)}
      </article>

      <article class="metric-card">
        <span>Praxis-Fortschritt</span>
        <strong>${formatHours(praxis.flugStunden)} h</strong>
        ${renderProgress(praxis.flugStunden, praxis.mindestFlugStunden)}
        ${renderBadge(practiceInfo.label, practiceInfo.tone)}
      </article>

      <article class="metric-card">
        <span>Prüfungsstatus</span>
        <strong>${escapeHtml(examInfo.label)}</strong>
        <p>${escapeHtml(examInfo.detail)}</p>
        <div class="badge-row">
          ${renderBooleanBadge(status.theorieBestanden, "Theorie bestanden", "Theorie offen")}
          ${renderBooleanBadge(status.praxisBestanden, "Praxis bestanden", "Praxis offen")}
        </div>
      </article>

      <article class="metric-card">
        <span>Abschlussstatus</span>
        <strong>${canComplete ? "Möglich" : "Offen"}</strong>
        <p>${canComplete ? "Theorie und Praxis sind bestanden." : "Abschluss erst nach bestandener Theorie- und Praxisprüfung."}</p>
        ${renderBooleanBadge(canComplete, "Abschluss bereit", "Noch nicht bereit")}
      </article>

      ${renderProcessOverview(status)}
    </section>
  `;
}

function planningColumn(id) {
  return PLANNING_COLUMNS.find((column) => column.id === id) || null;
}

function planningDropAllowed(cardType, targetColumnId) {
  const column = planningColumn(targetColumnId);
  return Boolean(column?.accepts.includes(cardType));
}

function formatPlanningDate(value) {
  if (!value) {
    return "-";
  }
  return String(value).includes("T") ? formatDateTime(value) : formatDate(value);
}

function planningCards() {
  const student = selectedStudent();
  if (!student) {
    return [];
  }

  const status = state.status || {};
  const theoryProgress = state.theorie?.fortschritt || {};
  const practiceProgress = state.praxis?.fortschritt || {};
  const theoryHours = Number(theoryProgress.theorieStunden ?? status.theorieStunden ?? student.theorieStunden ?? 0);
  const minTheoryHours = Number(theoryProgress.mindestTheorieStunden ?? 10);
  const flightHours = Number(practiceProgress.flugStunden ?? status.flugStunden ?? student.flugStunden ?? 0);
  const minFlightHours = Number(practiceProgress.mindestFlugStunden ?? 10);
  const blockedByStatus = isCompleted(status) || isAborted(status);
  const studentName = studentFullName(student);
  const cards = [];

  if (!status.theorieBestanden) {
    const missingTheory = Math.max(0, minTheoryHours - theoryHours);
    cards.push({
      id: `theory-request-${student.id}`,
      type: "theory-request",
      column: "open-theory",
      studentName,
      title: missingTheory > 0 ? "Theoriestunde anfragen" : "Theorie optional vertiefen",
      date: todayDate(1),
      status: missingTheory > 0 ? `${formatHours(missingTheory)} h bis Mindestumfang` : "Prüfungsvoraussetzung erfüllt",
      badge: "Theorie",
      tone: missingTheory > 0 ? "warning" : "success",
      blocked: blockedByStatus,
      blockedReason: blockedByStatus ? "Ausbildung nicht mehr aktiv" : "",
      draggable: !blockedByStatus
    });
  }

  (state.theorie?.kurse || []).forEach((course) => {
    cards.push({
      id: course.id,
      type: "theory-planned",
      column: "planned-theory",
      studentName,
      title: course.typ || "Theoriestunde",
      date: course.tag,
      status: course.lehrer ? `Dozent: ${course.lehrer}` : "Geplant",
      badge: "Theorie",
      tone: "info",
      blocked: false,
      draggable: true
    });
  });

  if (!status.praxisBestanden) {
    const missingPractice = Math.max(0, minFlightHours - flightHours);
    const hasAvailablePilot = PILOT_OPTIONS.some((pilot) => pilot.available);
    const hasAvailableAircraft = AIRCRAFT_OPTIONS.some((aircraft) => aircraft.available);
    const blocked = blockedByStatus || !hasAvailablePilot || !hasAvailableAircraft;
    cards.push({
      id: `practice-request-${student.id}`,
      type: "practice-request",
      column: "open-practice",
      studentName,
      title: missingPractice > 0 ? "Flugstunde anfragen" : "Praxis optional vertiefen",
      date: todayDate(1),
      status: missingPractice > 0 ? `${formatHours(missingPractice)} h bis Mindestumfang` : "Prüfungsvoraussetzung erfüllt",
      badge: "Praxis",
      tone: missingPractice > 0 ? "warning" : "success",
      blocked,
      blockedReason: blockedByStatus ? "Ausbildung nicht mehr aktiv" : "Kein verfügbarer Pilot oder kein verfügbares Flugzeug",
      draggable: !blocked
    });
  }

  (state.praxis?.fluege || []).forEach((flight) => {
    cards.push({
      id: flight.id,
      type: "practice-planned",
      column: "planned-flights",
      studentName,
      title: flight.flugArt || "Flugstunde",
      date: flight.startzeit,
      status: `${flight.flugzeugId || "-"} · ${flight.startFlughafen || "-"} -> ${flight.zielFlughafen || "-"}`,
      badge: "Praxis",
      tone: "info",
      blocked: false,
      draggable: false
    });
  });

  (state.pruefungen || []).forEach((exam) => {
    const statusInfo = examStatus(exam);
    cards.push({
      id: exam.id,
      type: "exam",
      column: "exams",
      studentName,
      title: exam.typ || "Prüfung",
      date: exam.datum,
      status: statusInfo.label,
      badge: "Prüfung",
      tone: statusInfo.tone,
      blocked: false,
      draggable: false
    });
  });

  const hasTheoryExam = (state.pruefungen || []).some((exam) => normalizeText(exam.typ).includes("theorie"));
  const hasPracticeExam = (state.pruefungen || []).some((exam) => normalizeText(exam.typ).includes("praxis"));
  if (!status.theorieBestanden && !hasTheoryExam) {
    const ready = Boolean(status.theoriePruefungFreigeschaltet);
    cards.push({
      id: `theory-exam-placeholder-${student.id}`,
      type: "exam-placeholder",
      column: "exams",
      studentName,
      title: "Theorieprüfung",
      date: ready ? "Anmeldung möglich" : "",
      status: ready ? "Bereit zur Anmeldung" : "Voraussetzungen fehlen",
      badge: "Prüfung",
      tone: ready ? "success" : "warning",
      blocked: !ready,
      blockedReason: ready ? "" : "Mindest-Theoriestunden fehlen",
      draggable: false
    });
  }

  if (!status.praxisBestanden && !hasPracticeExam) {
    const ready = Boolean(status.praxisPruefungFreigeschaltet);
    cards.push({
      id: `practice-exam-placeholder-${student.id}`,
      type: "exam-placeholder",
      column: "exams",
      studentName,
      title: "Praxisprüfung",
      date: ready ? "Anmeldung möglich" : "",
      status: ready ? "Bereit zur Anmeldung" : "Voraussetzungen fehlen",
      badge: "Prüfung",
      tone: ready ? "success" : "warning",
      blocked: !ready,
      blockedReason: ready ? "" : "Mindest-Flugstunden fehlen",
      draggable: false
    });
  }

  return cards;
}

function renderPlanningView() {
  const student = selectedStudent();
  const cards = planningCards();

  return `
    <section class="stack-layout">
      <div class="panel planning-intro">
        <div class="section-head">
          <div>
            <p class="eyebrow">Planung</p>
            <h3>Drag-and-Drop-Planungsboard</h3>
          </div>
          ${student ? renderBadge(studentFullName(student), "info") : ""}
        </div>
        <p class="muted">
          Ziehe offene Theorieanfragen in die geplanten Theoriestunden oder offene Praxisanfragen in die geplanten Flugstunden.
          Vor dem Speichern wird eine Bestätigung angezeigt.
        </p>
      </div>

      <section class="planning-board" aria-label="Planungsboard">
        ${PLANNING_COLUMNS.map((column) => renderPlanningColumn(column, cards.filter((card) => card.column === column.id))).join("")}
      </section>
    </section>
  `;
}

function renderPlanningColumn(column, cards) {
  return `
    <section class="planning-column" data-planning-column="${escapeHtml(column.id)}">
      <div class="planning-column-head">
        <h3>${escapeHtml(column.title)}</h3>
        ${renderBadge(String(cards.length), "neutral")}
      </div>
      <div class="planning-card-list">
        ${cards.length ? cards.map(renderPlanningCard).join("") : `<p class="muted empty-inline">Keine Karten.</p>`}
      </div>
    </section>
  `;
}

function renderPlanningCard(card) {
  const className = `planning-card ${card.blocked ? "blocked" : ""}`;
  return `
    <article class="${className}" draggable="${card.draggable ? "true" : "false"}"
      data-planning-card="${escapeHtml(card.id)}" data-planning-card-type="${escapeHtml(card.type)}">
      <div class="planning-card-top">
        ${renderBadge(card.badge, card.tone)}
        ${card.blocked ? renderBadge("Blockiert", "warning") : ""}
      </div>
      <h4>${escapeHtml(card.title)}</h4>
      <p>${escapeHtml(card.studentName)}</p>
      <dl>
        <div><dt>Termin</dt><dd>${escapeHtml(formatPlanningDate(card.date))}</dd></div>
        <div><dt>Status</dt><dd>${escapeHtml(card.status)}</dd></div>
      </dl>
      ${card.blockedReason ? `<p class="blocked-reason">${escapeHtml(card.blockedReason)}</p>` : ""}
    </article>
  `;
}

function renderStudentsView() {
  const student = selectedStudent();
  const results = filteredStudents();
  return `
    <section class="stack-layout">
      <div class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Globale Optionen</p>
            <h3>Demo-Schüler anlegen</h3>
          </div>
        </div>
        <form id="studentCreateForm" class="form-grid">
          <label>Vorname<input name="vorname" placeholder="Alex" required></label>
          <label>Nachname<input name="name" placeholder="Muster" required></label>
          <label>Ausbildungsbeginn<input name="startzeit" type="date" value="${todayDate()}"></label>
          <label>Theoriestunden<input name="theorieStunden" type="number" min="0" step="0.5" value="0"></label>
          <label>Flugstunden<input name="flugStunden" type="number" min="0" step="0.5" value="0"></label>
          <label>Vertragsstatus
            <select name="vertragsStatus">
              <option>Unterschrieben</option>
              <option>Abgeschlossen</option>
              <option>Abgebrochen</option>
            </select>
          </label>
          <label class="full">Notiz<textarea name="notiz" rows="2" placeholder="Manuell angelegter Demo-Schüler"></textarea></label>
          <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>
            ${state.action ? "Anlegen läuft..." : "Schüler anlegen"}
          </button>
        </form>
      </div>

      <section class="two-column">
        <div class="panel">
          <div class="section-head">
            <div>
            <p class="eyebrow">Schüler</p>
              <h3>Suche und Liste</h3>
            </div>
            <div class="badge-row">
              ${renderBadge(`${results.length} Treffer`, "info")}
              ${renderBadge(`${state.students.length} geladen`, "neutral")}
            </div>
          </div>
          ${renderStudentFilters()}
          ${state.students.length ? renderStudentsTable(results) : `<p class="muted">Keine Schüler vorhanden.</p>`}
        </div>

        <aside class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Detaildaten</p>
              <h3>${escapeHtml(studentFullName(student))}</h3>
            </div>
          </div>
          ${student ? renderStudentDetails(student) : `<p class="muted">Kein Schüler ausgewählt.</p>`}
        </aside>
      </section>
    </section>
  `;
}

function renderStudentsTable(students = state.students) {
  if (!students.length) {
    return `<p class="muted empty-inline">Keine Schüler entsprechen den aktuellen Filtern.</p>`;
  }
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Theorie</th>
            <th>Praxis</th>
            <th>Status</th>
            <th>Vertrag</th>
            <th>Optionen</th>
          </tr>
        </thead>
        <tbody>
          ${students.map((student) => {
            const status = studentStatus(student.id);
            const training = trainingWorkflowStatus(status);
            return `
            <tr class="${student.id === state.selectedStudentId ? "selected-row" : ""}"
              data-context-type="student" data-context-id="${escapeHtml(student.id)}">
              <td><strong>${escapeHtml(student.id)}</strong></td>
              <td>${escapeHtml(studentFullName(student))}</td>
              <td>${formatHours(student.theorieStunden)} h</td>
              <td>${formatHours(student.flugStunden)} h</td>
              <td>${renderBadge(training.label, training.tone)}</td>
              <td>${escapeHtml(contractStatus(student))}</td>
              <td>
                <div class="row-actions">
                  <button class="button small" type="button" data-select-student="${escapeHtml(student.id)}" ${state.action ? "disabled" : ""}>
                    Auswählen
                  </button>
                  <button class="button small danger" type="button" data-delete-student="${escapeHtml(student.id)}" ${state.action ? "disabled" : ""}>
                    Löschen
                  </button>
                  ${contextButton("student", student.id, "Schüleraktionen")}
                </div>
              </td>
            </tr>
          `;}).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderStudentDetails(student) {
  const status = studentStatus(student.id);
  const theory = theoryWorkflowStatus(status);
  const practice = practiceWorkflowStatus(status);
  const training = trainingWorkflowStatus(status);
  return `
    <dl class="details">
      <div><dt>Schüler-ID</dt><dd>${escapeHtml(student.id)}</dd></div>
      <div><dt>Name</dt><dd>${escapeHtml(studentFullName(student))}</dd></div>
      <div><dt>Status</dt><dd>${renderBadge(training.label, training.tone)}</dd></div>
      <div><dt>Theorie</dt><dd>${renderBadge(theory.label, theory.tone)}</dd></div>
      <div><dt>Praxis</dt><dd>${renderBadge(practice.label, practice.tone)}</dd></div>
      <div><dt>Ausbildungsvertrag</dt><dd>${escapeHtml(student.ausbildungsVertragId || "-")}</dd></div>
      <div><dt>Beginn</dt><dd>${formatDate(student.startzeit)}</dd></div>
      <div><dt>Geplantes Ende</dt><dd>${formatDate(student.endzeit)}</dd></div>
      <div><dt>Theoriestunden</dt><dd>${formatHours(student.theorieStunden)} h</dd></div>
      <div><dt>Flugstunden</dt><dd>${formatHours(student.flugStunden)} h</dd></div>
      <div><dt>Notiz</dt><dd>${escapeHtml(student.notiz || "-")}</dd></div>
    </dl>
  `;
}

function renderTheoryView() {
  const progress = state.theorie?.fortschritt || {};
  const courses = state.theorie?.kurse || [];
  const filteredCourses = filteredByTableSearch(courses, "courses", (course) => `${course.id} ${course.tag} ${course.typ} ${course.lehrer}`);
  const unlocked = Boolean(progress.theoriePruefungFreigeschaltet);
  const theory = theoryWorkflowStatus(state.status || {});

  return `
    <section class="stack-layout">
      <div class="status-strip">
        <article>
          <span>Theoriestunden</span>
          <strong>${formatHours(progress.theorieStunden)} / ${formatHours(progress.mindestTheorieStunden)} h</strong>
          ${renderProgress(progress.theorieStunden, progress.mindestTheorieStunden)}
        </article>
        <article>
          <span>Theoriestatus</span>
          ${renderBadge(theory.label, theory.tone)}
          <p class="metric-detail">${escapeHtml(theory.detail)}</p>
        </article>
      </div>

      <section class="two-column">
        <div class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Theorie</p>
              <h3>Vorhandene Kurse</h3>
            </div>
            ${renderBadge(`${courses.length} Kurse`, "neutral")}
          </div>
          ${renderTableSearch("courses", "Kurse durchsuchen", "ID, Thema, Lehrer oder Datum")}
          ${courses.length ? renderCoursesTable(filteredCourses) : `<p class="muted">Noch keine Theoriekurse vorhanden.</p>`}
        </div>

        <div class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Buchen</p>
              <h3>Neuer Theoriekurs</h3>
            </div>
          </div>
          <form id="theoryBookingForm" class="form-grid">
            <label>Thema<input name="thema" placeholder="Theorie - Navigation" required></label>
            <label>Termin<input name="termin" type="date" value="${todayDate(1)}" required></label>
            <label>Dauer Minuten<input name="dauerMinuten" type="number" min="15" step="15" value="90" required></label>
            <label>Dozent<input name="dozent" placeholder="Elias Schulz" required></label>
            <label class="full">Notizen<textarea name="notizen" rows="3" placeholder="Optionale Hinweise"></textarea></label>
            <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>
              ${state.action ? "Buchung läuft..." : "Theoriekurs buchen"}
            </button>
          </form>
        </div>
      </section>

      <section class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Prüfung</p>
            <h3>Theorieprüfung anmelden</h3>
          </div>
          ${renderBooleanBadge(unlocked, "Anmeldung möglich", "Mindeststunden fehlen")}
        </div>
        <form id="theoryExamForm" class="form-grid">
          <label>Wunschtermin<input name="wunschtermin" type="date" value="${todayDate(7)}" required></label>
          <label>Prüfer<input name="pruefer" placeholder="P001"></label>
          <label class="full">Bemerkung<textarea name="bemerkung" rows="2" placeholder="Erstanmeldung"></textarea></label>
          <button class="button primary full" type="submit" ${!unlocked || state.action ? "disabled" : ""}>
            ${state.action ? "Anmeldung läuft..." : "Theorieprüfung anmelden"}
          </button>
        </form>
      </section>
    </section>
  `;
}

function renderCoursesTable(courses) {
  if (!courses.length) {
    return `<p class="muted empty-inline">Keine Kurse entsprechen der Suche.</p>`;
  }
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Datum</th>
            <th>Thema</th>
            <th>Lehrer</th>
            <th>Dauer</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${courses.map((course) => `
            <tr data-context-type="course" data-context-id="${escapeHtml(course.id)}">
              <td><strong>${escapeHtml(course.id)}</strong></td>
              <td>${formatDate(course.tag)}</td>
              <td>${escapeHtml(course.typ)}</td>
              <td>${escapeHtml(course.lehrer)}</td>
              <td>${Number(course.dauerMinuten || 60)} min</td>
              <td>
                <div class="row-actions">
                  <button class="button small danger" type="button" data-cancel-course="${escapeHtml(course.id)}" ${state.action ? "disabled" : ""}>
                    Stornieren
                  </button>
                  ${contextButton("course", course.id, "Kursaktionen")}
                </div>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderPilotSelection() {
  const pilot = pilotById(state.practiceSelection.pilotId) || PILOT_OPTIONS[0];
  return `
    <div class="entity-picker full">
      <input type="hidden" name="fluglehrer" value="${escapeHtml(pilot?.id || "")}" required>
      <div>
        <span class="field-label">Fluglehrer/Pilot</span>
        <strong>${escapeHtml(pilot ? `${pilot.id} - ${pilot.name}` : "Kein Pilot ausgewaehlt")}</strong>
        <p>${pilot ? `${pilot.license} - ${pilot.note}` : "Bitte Pilot auswaehlen."}</p>
      </div>
      <button class="button secondary" type="button" data-open-modal="pilot" ${state.action ? "disabled" : ""}>Pilot suchen</button>
    </div>
  `;
}

function renderAircraftSelection() {
  const aircraft = aircraftById(state.practiceSelection.aircraftId) || AIRCRAFT_OPTIONS.find((entry) => entry.available);
  return `
    <div class="entity-picker full">
      <input type="hidden" name="flugzeugId" value="${escapeHtml(aircraft?.id || "")}" required>
      <div>
        <span class="field-label">Flugzeug</span>
        <strong>${escapeHtml(aircraft ? `${aircraft.id} - ${aircraft.type}` : "Kein Flugzeug ausgewaehlt")}</strong>
        <p>${aircraft ? `${aircraft.status} - ${aircraft.maintenance}` : "Bitte Flugzeug auswaehlen."}</p>
      </div>
      <button class="button secondary" type="button" data-open-modal="aircraft" ${state.action ? "disabled" : ""}>Flugzeug suchen</button>
    </div>
  `;
}

function renderSelectionModal() {
  if (!state.modal) {
    return "";
  }
  const type = state.modal;
  const isPilot = type === "pilot";
  const title = isPilot ? "Fluglehrer auswaehlen" : "Flugzeug auswaehlen";
  const rows = isPilot ? renderPilotModalRows() : renderAircraftModalRows();
  return `
    <div class="modal-backdrop" data-close-modal>
      <section class="modal-panel" role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}">
        <div class="modal-head">
          <div>
            <p class="eyebrow">Auswahl</p>
            <h3>${escapeHtml(title)}</h3>
          </div>
          <button class="button ghost" type="button" data-close-modal>Schliessen</button>
        </div>
        <label class="table-search">
          Suchen
          <input data-modal-search value="" placeholder="${isPilot ? "Name, ID oder Lizenz" : "ID, Typ, Status oder Wartung"}" autofocus>
        </label>
        <div class="table-wrap modal-table">
          <table>
            <thead>
              ${isPilot ? `
                <tr><th>ID</th><th>Name</th><th>Lizenz</th><th>Verfuegbarkeit</th><th></th></tr>
              ` : `
                <tr><th>ID</th><th>Typ</th><th>Status</th><th>Wartung</th><th></th></tr>
              `}
            </thead>
            <tbody>
              ${rows}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `;
}

function renderPilotModalRows() {
  return PILOT_OPTIONS.map((pilot) => `
    <tr class="${pilot.available ? "" : "disabled-row"}" data-modal-row data-search="${escapeHtml(`${pilot.id} ${pilot.name} ${pilot.license} ${pilot.note}`)}">
      <td><strong>${escapeHtml(pilot.id)}</strong></td>
      <td>${escapeHtml(pilot.name)}</td>
      <td>${escapeHtml(pilot.license)}</td>
      <td>${renderBadge(pilot.note, pilot.available ? "success" : "warning")}</td>
      <td>
        <button class="button small" type="button" data-select-pilot="${escapeHtml(pilot.id)}" ${pilot.available || state.practiceSelection.pilotId === pilot.id ? "" : "disabled"}>
          ${state.practiceSelection.pilotId === pilot.id ? "Ausgewaehlt" : "Waehlen"}
        </button>
      </td>
    </tr>
  `).join("");
}

function renderAircraftModalRows() {
  return AIRCRAFT_OPTIONS.map((aircraft) => `
    <tr class="${aircraft.available ? "" : "disabled-row"}" data-modal-row data-search="${escapeHtml(`${aircraft.id} ${aircraft.type} ${aircraft.status} ${aircraft.maintenance}`)}"
      data-context-type="aircraft" data-context-id="${escapeHtml(aircraft.id)}">
      <td><strong>${escapeHtml(aircraft.id)}</strong></td>
      <td>${escapeHtml(aircraft.type)}</td>
      <td>${renderBadge(aircraft.status, aircraft.available ? "success" : "warning")}</td>
      <td>${escapeHtml(aircraft.maintenance)}</td>
      <td>
        <button class="button small" type="button" data-select-aircraft="${escapeHtml(aircraft.id)}" ${aircraft.available || state.practiceSelection.aircraftId === aircraft.id ? "" : "disabled"}>
          ${state.practiceSelection.aircraftId === aircraft.id ? "Ausgewaehlt" : "Waehlen"}
        </button>
        ${contextButton("aircraft", aircraft.id, "Flugzeugaktionen")}
      </td>
    </tr>
  `).join("");
}

function renderPracticeView() {
  const progress = state.praxis?.fortschritt || {};
  const flights = state.praxis?.fluege || [];
  const unlocked = Boolean(progress.praxisPruefungFreigeschaltet);
  const practice = practiceWorkflowStatus(state.status || {});

  return `
    <section class="stack-layout">
      <div class="status-strip">
        <article>
          <span>Flugstunden</span>
          <strong>${formatHours(progress.flugStunden)} / ${formatHours(progress.mindestFlugStunden)} h</strong>
          ${renderProgress(progress.flugStunden, progress.mindestFlugStunden)}
        </article>
        <article>
          <span>Praxisstatus</span>
          ${renderBadge(practice.label, practice.tone)}
          <p class="metric-detail">${escapeHtml(practice.detail)}</p>
        </article>
      </div>

      <section class="two-column">
        <div class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Praxis</p>
              <h3>Vorhandene Flugstunden</h3>
            </div>
            ${renderBadge(`${flights.length} Flüge`, "neutral")}
          </div>
          ${flights.length ? renderFlightsTable(flights) : `<p class="muted">Noch keine Flugstunden vorhanden.</p>`}
        </div>

        <div class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Buchen</p>
              <h3>Neue Flugstunde</h3>
            </div>
          </div>
          <form id="practiceBookingForm" class="form-grid">
            <label>Datum<input name="datum" type="date" value="${todayDate(1)}" required></label>
            <label>Startzeit<input name="startzeit" type="time" value="10:00" required></label>
            <label>Endzeit<input name="endzeit" type="time" value="11:00" required></label>
            ${renderPilotSelection()}
            ${renderAircraftSelection()}
            <label>Startflughafen<input name="startFlughafen" value="EDDV" required></label>
            <label>Zielflughafen<input name="zielFlughafen" value="EDDV" required></label>
            <label>Ausbildungsinhalt<input name="ausbildungsinhalt" placeholder="Platzrunde" required></label>
            <label class="full">Notizen<textarea name="notizen" rows="3" placeholder="Optional"></textarea></label>
            <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>
              ${state.action ? "Buchung läuft..." : "Flugstunde buchen"}
            </button>
          </form>
        </div>
      </section>

      <section class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Prüfung</p>
            <h3>Praxisprüfung anmelden</h3>
          </div>
          ${renderBooleanBadge(unlocked, "Anmeldung möglich", "Mindeststunden fehlen")}
        </div>
        <form id="practiceExamForm" class="form-grid">
          <label>Wunschtermin<input name="wunschtermin" type="date" value="${todayDate(10)}" required></label>
          <label>Prüfer<input name="pruefer" placeholder="P002"></label>
          <label class="full">Bemerkung<textarea name="bemerkung" rows="2" placeholder="Praxisprüfung"></textarea></label>
          <button class="button primary full" type="submit" ${!unlocked || state.action ? "disabled" : ""}>
            ${state.action ? "Anmeldung läuft..." : "Praxisprüfung anmelden"}
          </button>
        </form>
      </section>
    </section>
  `;
}

function renderFlightsTable(flights) {
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Datum</th>
            <th>Flugzeug</th>
            <th>Route</th>
            <th>Inhalt</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${flights.map((flight) => `
            <tr data-context-type="flight" data-context-id="${escapeHtml(flight.id)}">
              <td><strong>${escapeHtml(flight.id)}</strong></td>
              <td>${formatDateTime(flight.startzeit)}<br><span class="muted">bis ${formatDateTime(flight.endzeit)}</span></td>
              <td>${escapeHtml(flight.flugzeugId)}</td>
              <td>${escapeHtml(flight.startFlughafen)} → ${escapeHtml(flight.zielFlughafen)}</td>
              <td>${escapeHtml(flight.flugArt)}</td>
              <td>
                <button class="button small danger" type="button" data-cancel-flight="${escapeHtml(flight.id)}" ${state.action ? "disabled" : ""}>
                  Stornieren
                </button>
                ${contextButton("flight", flight.id, "Flugstundenaktionen")}
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderExamsView() {
  const filteredExams = filteredByTableSearch(state.pruefungen, "exams", (exam) => `${exam.id} ${exam.datum} ${exam.typ}`);
  const selectedExam = state.pruefungen.find((exam) => exam.id === state.examResultSelection) || state.pruefungen[0] || null;
  const examInfo = examWorkflowStatus(state.status || {}, state.pruefungen);
  return `
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Prüfungen</p>
            <h3>Angemeldete Prüfungen</h3>
            <p class="muted">${escapeHtml(examInfo.detail)}</p>
          </div>
          <div class="badge-row">
            ${renderBadge(`${state.pruefungen.length} Prüfungen`, "neutral")}
            ${renderBadge(examInfo.label, examInfo.tone)}
          </div>
        </div>
        ${renderTableSearch("exams", "Pruefungen durchsuchen", "ID, Typ, Status oder Datum")}
        ${state.pruefungen.length ? renderExamsTable(filteredExams) : `<p class="muted">Noch keine Prüfungen vorhanden.</p>`}
      </div>

      <aside class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Ergebnis</p>
            <h3>Prüfungsergebnis speichern</h3>
          </div>
        </div>
        <form id="examResultForm" class="form-grid single">
          <input type="hidden" name="pruefungId" value="${escapeHtml(selectedExam?.id || "")}" required>
          <div class="selected-summary">
            <span class="field-label">Ausgewaehlte Pruefung</span>
            <strong>${selectedExam ? escapeHtml(`${selectedExam.id} - ${selectedExam.typ}`) : "Keine Pruefung ausgewaehlt"}</strong>
            <p>${selectedExam ? formatDate(selectedExam.datum) : "Bitte in der Tabelle eine Pruefung waehlen."}</p>
          </div>
          <label>
            Prüfungsart
            <select name="pruefungsart" required>
              <option value="Theoriepruefung">Theorie</option>
              <option value="Praxispruefung">Praxis</option>
            </select>
          </label>
          <label>Datum<input name="datum" type="date" value="${todayDate()}" required></label>
          <label>
            Ergebnis
            <select name="bestanden" required>
              <option value="true">Bestanden</option>
              <option value="false">Nicht bestanden</option>
            </select>
          </label>
          <label>Ergebnistext<input name="ergebnisText" placeholder="Prüfung bestanden"></label>
          <label>Notizen<textarea name="notizen" rows="3" placeholder="Optionale Details"></textarea></label>
          <button class="button primary" type="submit" ${!state.pruefungen.length || state.action ? "disabled" : ""}>
            ${state.action ? "Speichern läuft..." : "Ergebnis speichern"}
          </button>
        </form>
      </aside>
    </section>
  `;
}

function renderExamsTable(exams = state.pruefungen) {
  if (!exams.length) {
    return `<p class="muted empty-inline">Keine Pruefungen entsprechen der Suche.</p>`;
  }
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Datum</th>
            <th>Typ / Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${exams.map((exam) => {
            const status = examStatus(exam);
            return `
            <tr class="${exam.id === state.examResultSelection ? "selected-row" : ""}"
              data-context-type="exam" data-context-id="${escapeHtml(exam.id)}">
              <td><strong>${escapeHtml(exam.id)}</strong></td>
              <td>${formatDate(exam.datum)}</td>
              <td>
                ${escapeHtml(exam.typ)}<br>
                ${renderBadge(status.label, status.tone)}
                ${state.repeatMarkers[exam.id] ? renderBadge("Wiederholung", "warning") : ""}
              </td>
              <td>
                <button class="button small" type="button" data-select-exam="${escapeHtml(exam.id)}" ${state.action ? "disabled" : ""}>
                  ${exam.id === state.examResultSelection ? "Ausgewaehlt" : "Waehlen"}
                </button>
                ${contextButton("exam", exam.id, "Prüfungsaktionen")}
              </td>
            </tr>
          `;}).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderCompletionView() {
  const status = state.status || {};
  const canComplete = Boolean(status.theorieBestanden && status.praxisBestanden);
  const completed = status.status === "ABGESCHLOSSEN";
  const training = trainingWorkflowStatus(status);

  return `
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Gesamtstatus</p>
            <h3>${escapeHtml(training.label)}</h3>
          </div>
          ${renderBadge(status.vertragsStatus || "Unbekannt", training.tone)}
        </div>
        <dl class="details">
          <div><dt>Theorieprüfung bestanden</dt><dd>${renderBooleanBadge(status.theorieBestanden, "Ja", "Nein")}</dd></div>
          <div><dt>Praxisprüfung bestanden</dt><dd>${renderBooleanBadge(status.praxisBestanden, "Ja", "Nein")}</dd></div>
          <div><dt>Theorie freigeschaltet</dt><dd>${yesNo(status.theoriePruefungFreigeschaltet)}</dd></div>
          <div><dt>Praxis freigeschaltet</dt><dd>${yesNo(status.praxisPruefungFreigeschaltet)}</dd></div>
          <div><dt>Prüfungsreif</dt><dd>${yesNo(status.pruefungsreif)}</dd></div>
          <div><dt>Theoriestunden</dt><dd>${formatHours(status.theorieStunden)} h</dd></div>
          <div><dt>Flugstunden</dt><dd>${formatHours(status.flugStunden)} h</dd></div>
        </dl>
      </div>

      <aside class="panel action-panel">
        <p class="eyebrow">Abschluss</p>
        <h3>Ausbildung abschließen</h3>
        <p class="muted">
          Der Abschluss ist nur möglich, wenn Theorie- und Praxisprüfung bestanden sind.
        </p>
        <button class="button primary" type="button" id="completeTrainingButton" ${!canComplete || completed || state.action ? "disabled" : ""}>
          ${state.action ? "Abschluss läuft..." : completed ? "Bereits abgeschlossen" : "Ausbildung abschließen"}
        </button>
      </aside>
    </section>
  `;
}

function practicePayload(values) {
  requireFields(values, [
    "datum",
    "startzeit",
    "endzeit",
    "fluglehrer",
    "flugzeugId",
    "startFlughafen",
    "zielFlughafen",
    "ausbildungsinhalt"
  ]);

  const start = new Date(`${values.datum}T${values.startzeit}`);
  const end = new Date(`${values.datum}T${values.endzeit}`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    throw new Error("Datum, Startzeit und Endzeit müssen gültig sein.");
  }
  const durationMinutes = Math.round((end.getTime() - start.getTime()) / 60000);
  if (durationMinutes <= 0) {
    throw new Error("Die Endzeit muss nach der Startzeit liegen.");
  }

  const pilot = pilotById(values.fluglehrer);
  if (!pilot || !pilot.available) {
    throw new Error("Bitte einen verfuegbaren Fluglehrer auswaehlen.");
  }

  const aircraft = aircraftById(values.flugzeugId);
  if (!aircraft || !aircraft.available) {
    throw new Error("Bitte ein einsatzbereites Flugzeug ohne Wartungshinweis auswaehlen.");
  }

  const route = `${values.startFlughafen} → ${values.zielFlughafen}`;
  const notes = values.notizen ? `${values.notizen} | Route: ${route}` : `Route: ${route}`;

  return {
    schuelerId: ensureStudentId(),
    flugzeugId: values.flugzeugId,
    fluglehrer: values.fluglehrer,
    termin: `${values.datum}T${values.startzeit}`,
    dauerMinuten: durationMinutes,
    ausbildungsinhalt: `${values.ausbildungsinhalt} (${route})`,
    startFlughafen: values.startFlughafen,
    zielFlughafen: values.zielFlughafen,
    notizen: notes
  };
}

async function handleLogin(form) {
  const values = readForm(form);
  requireFields(values, ["username", "password"]);

  await runAction(async () => {
    const response = await api("/auth/login", {
      method: "POST",
      body: values
    });
    state.token = response.token;
    state.displayName = response.displayName || values.username;
    localStorage.setItem(STORAGE.token, state.token);
    localStorage.setItem(STORAGE.displayName, state.displayName);
    await loadStudents();
    await loadSelectedStudentData();
  }, "Login erfolgreich.");
}

async function handleTheoryBooking(form) {
  const values = readForm(form);
  requireFields(values, ["thema", "termin", "dauerMinuten", "dozent"]);
  const body = {
    schuelerId: ensureStudentId(),
    thema: values.thema,
    termin: values.termin,
    dauerMinuten: positiveInteger(values.dauerMinuten, "dauerMinuten"),
    dozent: values.dozent,
    notizen: values.notizen || ""
  };

  await runAction(() => api("/theorie/buchen", { method: "POST", body }), "Theoriekurs wurde gebucht.");
}

async function handleStudentCreate(form) {
  const values = readForm(form);
  requireFields(values, ["vorname", "name"]);
  const body = {
    vorname: values.vorname,
    name: values.name,
    startzeit: values.startzeit || todayDate(),
    theorieStunden: values.theorieStunden || "0",
    flugStunden: values.flugStunden || "0",
    vertragsStatus: values.vertragsStatus || "Unterschrieben",
    notiz: values.notiz || "Manuell angelegter Demo-Schüler"
  };

  await runAction(async () => {
    const created = await api("/schueler", { method: "POST", body });
    state.selectedStudentId = created.id;
    localStorage.setItem(STORAGE.selectedStudentId, created.id);
  }, "Schüler wurde angelegt.");
}

async function handleTheoryExam(form) {
  const values = readForm(form);
  requireFields(values, ["wunschtermin"]);
  const body = {
    schuelerId: ensureStudentId(),
    pruefungsart: "Theoriepruefung",
    wunschtermin: values.wunschtermin,
    pruefer: values.pruefer || "",
    bemerkung: values.bemerkung || ""
  };

  await runAction(() => api("/pruefung/theorie/anmelden", { method: "POST", body }), "Theorieprüfung wurde angemeldet.");
}

async function handlePracticeBooking(form) {
  const body = practicePayload(readForm(form));
  await runAction(() => api("/praxis/buchen", { method: "POST", body }), "Flugstunde wurde gebucht.");
}

async function handlePracticeExam(form) {
  const values = readForm(form);
  requireFields(values, ["wunschtermin"]);
  const body = {
    schuelerId: ensureStudentId(),
    pruefungsart: "Praxispruefung",
    wunschtermin: values.wunschtermin,
    pruefer: values.pruefer || "",
    bemerkung: values.bemerkung || ""
  };

  await runAction(() => api("/pruefung/praxis/anmelden", { method: "POST", body }), "Praxisprüfung wurde angemeldet.");
}

async function handleExamResult(form) {
  const values = readForm(form);
  requireFields(values, ["pruefungId", "pruefungsart", "datum", "bestanden"]);
  const body = {
    pruefungId: values.pruefungId,
    schuelerId: ensureStudentId(),
    pruefungsart: values.pruefungsart,
    datum: values.datum,
    bestanden: values.bestanden === "true",
    ergebnisText: values.ergebnisText || "",
    notizen: values.notizen || ""
  };

  await runAction(() => api("/pruefung/ergebnis", { method: "POST", body }), "Prüfungsergebnis wurde gespeichert.");
}

async function logout() {
  try {
    await api("/auth/logout", {
      method: "POST",
      body: {}
    });
  } catch (error) {
    // The local session is cleared even if the server token is already invalid.
  }
  clearAuth("");
}

async function selectStudentForView(studentId, view) {
  state.selectedStudentId = studentId;
  if (view) {
    state.view = view;
  }
  localStorage.setItem(STORAGE.selectedStudentId, state.selectedStudentId);
  await refreshAll();
}

function showDetails(title, entries) {
  const lines = entries
    .filter((entry) => entry.value !== undefined && entry.value !== null && entry.value !== "")
    .map((entry) => `${entry.label}: ${entry.value}`);
  window.alert(`${title}\n\n${lines.join("\n")}`);
}

function showCourseDetails(courseId) {
  const course = state.theorie?.kurse?.find((entry) => entry.id === courseId);
  if (!course) {
    return;
  }
  showDetails("Theoriekurs", [
    { label: "ID", value: course.id },
    { label: "Datum", value: formatDate(course.tag) },
    { label: "Thema", value: course.typ },
    { label: "Lehrer", value: course.lehrer },
    { label: "Dauer", value: `${Number(course.dauerMinuten || 60)} min` }
  ]);
}

function showFlightDetails(flightId) {
  const flight = state.praxis?.fluege?.find((entry) => entry.id === flightId);
  if (!flight) {
    return;
  }
  showDetails("Flugstunde", [
    { label: "ID", value: flight.id },
    { label: "Beginn", value: formatDateTime(flight.startzeit) },
    { label: "Ende", value: formatDateTime(flight.endzeit) },
    { label: "Flugzeug", value: flight.flugzeugId },
    { label: "Route", value: `${flight.startFlughafen} -> ${flight.zielFlughafen}` },
    { label: "Inhalt", value: flight.flugArt }
  ]);
}

function showAircraftDetails(aircraftId, maintenanceOnly = false) {
  const aircraft = aircraftById(aircraftId);
  if (!aircraft) {
    return;
  }
  if (maintenanceOnly) {
    showDetails("Wartungsstatus", [
      { label: "Flugzeug", value: aircraft.id },
      { label: "Status", value: aircraft.status },
      { label: "Hinweis", value: aircraft.maintenance },
      { label: "Buchbar", value: aircraft.available ? "Ja" : "Nein" }
    ]);
    return;
  }
  showDetails("Flugzeug", [
    { label: "ID", value: aircraft.id },
    { label: "Typ", value: aircraft.type },
    { label: "Status", value: aircraft.status },
    { label: "Wartung", value: aircraft.maintenance },
    { label: "Buchbar", value: aircraft.available ? "Ja" : "Nein" }
  ]);
}

async function registerTheoryExamFromContext() {
  if (!state.theorie?.fortschritt?.theoriePruefungFreigeschaltet && !state.status?.theoriePruefungFreigeschaltet) {
    setMessage("error", "Theorieprüfung ist noch nicht freigeschaltet.");
    render();
    return;
  }
  state.view = "theory";
  const body = {
    schuelerId: ensureStudentId(),
    pruefungsart: "Theoriepruefung",
    wunschtermin: todayDate(7),
    pruefer: "",
    bemerkung: "Anmeldung per Kontextmenü"
  };
  await runAction(() => api("/pruefung/theorie/anmelden", { method: "POST", body }), "Theorieprüfung wurde angemeldet.");
}

async function registerPracticeExamFromContext() {
  if (!state.praxis?.fortschritt?.praxisPruefungFreigeschaltet && !state.status?.praxisPruefungFreigeschaltet) {
    setMessage("error", "Praxisprüfung ist noch nicht freigeschaltet.");
    render();
    return;
  }
  state.view = "practice";
  const body = {
    schuelerId: ensureStudentId(),
    pruefungsart: "Praxispruefung",
    wunschtermin: todayDate(10),
    pruefer: "",
    bemerkung: "Anmeldung per Kontextmenü"
  };
  await runAction(() => api("/pruefung/praxis/anmelden", { method: "POST", body }), "Praxisprüfung wurde angemeldet.");
}

async function cancelFlight(flightId) {
  const reason = window.prompt("Grund für die Stornierung:", "Termin verschoben");
  if (reason === null) {
    return;
  }
  const body = {
    schuelerId: ensureStudentId(),
    flugId: flightId,
    grund: reason
  };
  await runAction(() => api("/praxis/stornieren", { method: "POST", body }), "Flugstunde wurde storniert.");
}

async function cancelCourse(courseId) {
  const reason = window.prompt("Grund für die Stornierung:", "Termin verschoben");
  if (reason === null) {
    return;
  }
  await cancelCourseWithReason(courseId, reason, "Theoriestunde wurde storniert.");
}

async function cancelCourseFromPlanning(courseId) {
  const student = selectedStudent();
  if (!window.confirm(`Theoriestunde ${courseId} wieder aus der Planung herausziehen und stornieren?`)) {
    return;
  }
  const reason = student
    ? `Per Planungsboard fuer ${studentFullName(student)} zurueckgezogen`
    : "Per Planungsboard zurueckgezogen";
  await cancelCourseWithReason(courseId, reason, "Theoriestunde wurde aus der Planung entfernt.");
}

async function cancelCourseWithReason(courseId, reason, successMessage) {
  const body = {
    schuelerId: ensureStudentId(),
    kursId: courseId,
    grund: reason
  };
  await runAction(() => api("/theorie/stornieren", { method: "POST", body }), successMessage);
}

function selectExamForResult(examId) {
  state.examResultSelection = examId;
  state.view = "exams";
  setMessage("notice", "Prüfung ausgewählt. Ergebnis kann rechts gespeichert werden.");
  render();
}

function markExamRepeat(examId) {
  const exam = state.pruefungen.find((entry) => entry.id === examId);
  state.examResultSelection = examId;
  state.view = "exams";
  if (exam && isFailedExam(exam)) {
    state.repeatMarkers[examId] = true;
    setMessage("notice", "Wiederholungsbedarf ist durch das nicht bestandene Ergebnis markiert.");
  } else {
    setMessage("error", "Wiederholung kann nur bei nicht bestandenen Prüfungen markiert werden.");
  }
  render();
}

function selectAircraftForBooking(aircraftId) {
  const aircraft = aircraftById(aircraftId);
  if (!aircraft?.available) {
    setMessage("error", "Dieses Flugzeug ist nicht für Buchungen verfügbar.");
    render();
    return;
  }
  state.practiceSelection.aircraftId = aircraft.id;
  state.view = "practice";
  state.modal = null;
  setMessage("notice", `${aircraft.id} wurde für die nächste Praxisbuchung ausgewählt.`);
  render();
}

function availablePilot() {
  return pilotById(state.practiceSelection.pilotId)?.available
    ? pilotById(state.practiceSelection.pilotId)
    : PILOT_OPTIONS.find((pilot) => pilot.available);
}

function availableAircraft() {
  return aircraftById(state.practiceSelection.aircraftId)?.available
    ? aircraftById(state.practiceSelection.aircraftId)
    : AIRCRAFT_OPTIONS.find((aircraft) => aircraft.available);
}

function planningDropError(cardType, targetColumnId) {
  if (targetColumnId === "open-theory") {
    return cardType === "theory-planned"
      ? ""
      : "In offene Theorieanfragen koennen nur geplante Theoriestunden zurueckgezogen werden.";
  }
  if (targetColumnId === "planned-theory") {
    return cardType === "theory-request"
      ? ""
      : "In geplante Theoriestunden dürfen nur Theorieanfragen verschoben werden.";
  }
  if (targetColumnId === "planned-flights") {
    return cardType === "practice-request"
      ? ""
      : "In geplante Flugstunden dürfen nur Praxisanfragen verschoben werden.";
  }
  if (targetColumnId === "exams") {
    return "Prüfungen werden über die Prüfungsfunktionen angemeldet und können nicht frei verschoben werden.";
  }
  return "Diese Verschiebung ist fachlich nicht erlaubt.";
}

async function planTheoryFromDrop() {
  const student = selectedStudent();
  if (!student) {
    throw new Error("Bitte zuerst einen Schüler auswählen.");
  }
  if (!window.confirm(`Statusupdate vorbereiten: Theoriestunde für ${studentFullName(student)} planen und speichern?`)) {
    return;
  }
  const body = {
    schuelerId: ensureStudentId(),
    thema: "Planung: Theoriestunde",
    termin: todayDate(1),
    dauerMinuten: 60,
    dozent: "Planungsboard",
    notizen: "Aus der Planungsansicht erstellt."
  };
  await runAction(() => api("/theorie/buchen", { method: "POST", body }), "Theoriestunde wurde aus der Planung erstellt.");
}

async function planPracticeFromDrop() {
  const student = selectedStudent();
  const pilot = availablePilot();
  const aircraft = availableAircraft();
  if (!student) {
    throw new Error("Bitte zuerst einen Schüler auswählen.");
  }
  if (!pilot || !aircraft) {
    throw new Error("Praxisstunde kann nicht geplant werden, weil kein verfügbarer Pilot oder kein verfügbares Flugzeug vorhanden ist.");
  }
  if (!window.confirm(`Statusupdate vorbereiten: Flugstunde für ${studentFullName(student)} mit ${pilot.id} und ${aircraft.id} planen und speichern?`)) {
    return;
  }
  const body = {
    schuelerId: ensureStudentId(),
    flugzeugId: aircraft.id,
    fluglehrer: pilot.id,
    termin: `${todayDate(1)}T10:00`,
    dauerMinuten: 60,
    ausbildungsinhalt: "Planung: Flugstunde",
    startFlughafen: "EDDV",
    zielFlughafen: "EDDV",
    notizen: "Aus der Planungsansicht erstellt."
  };
  await runAction(() => api("/praxis/buchen", { method: "POST", body }), "Flugstunde wurde aus der Planung erstellt.");
}

async function handlePlanningDrop(cardType, targetColumnId, cardId) {
  if (!cardType || !targetColumnId) {
    return;
  }
  if (!planningDropAllowed(cardType, targetColumnId)) {
    setMessage("error", planningDropError(cardType, targetColumnId));
    render();
    return;
  }
  if (cardType === "theory-request" && targetColumnId === "planned-theory") {
    await planTheoryFromDrop();
    return;
  }
  if (cardType === "theory-planned" && targetColumnId === "open-theory") {
    await cancelCourseFromPlanning(cardId);
    return;
  }
  if (cardType === "practice-request" && targetColumnId === "planned-flights") {
    await planPracticeFromDrop();
  }
}

function clearPlanningDropClasses() {
  document.querySelectorAll(".planning-column.drag-allowed, .planning-column.drag-blocked").forEach((column) => {
    column.classList.remove("drag-allowed", "drag-blocked");
  });
}

async function handleContextMenuAction(action) {
  const menu = state.contextMenu;
  closeContextMenu();
  if (!menu) {
    render();
    return;
  }

  try {
    if (action === "student-details") {
      await selectStudentForView(menu.id, "students");
    } else if (action === "student-status") {
      await selectStudentForView(menu.id, "dashboard");
    } else if (action === "student-theory") {
      await selectStudentForView(menu.id, "theory");
    } else if (action === "student-practice") {
      await selectStudentForView(menu.id, "practice");
    } else if (action === "student-exams") {
      await selectStudentForView(menu.id, "exams");
    } else if (action === "course-details") {
      showCourseDetails(menu.id);
      render();
    } else if (action === "course-theory-exam") {
      await registerTheoryExamFromContext();
    } else if (action === "course-cancel") {
      await cancelCourse(menu.id);
    } else if (action === "flight-details") {
      showFlightDetails(menu.id);
      render();
    } else if (action === "flight-practice-exam") {
      await registerPracticeExamFromContext();
    } else if (action === "flight-cancel") {
      await cancelFlight(menu.id);
    } else if (action === "exam-result") {
      selectExamForResult(menu.id);
    } else if (action === "exam-repeat") {
      markExamRepeat(menu.id);
    } else if (action === "aircraft-details") {
      showAircraftDetails(menu.id);
      render();
    } else if (action === "aircraft-maintenance") {
      showAircraftDetails(menu.id, true);
      render();
    } else if (action === "aircraft-select") {
      selectAircraftForBooking(menu.id);
    }
  } catch (error) {
    setMessage("error", sanitizeErrorMessage(error?.message || "Kontextaktion konnte nicht ausgeführt werden."));
    render();
  }
}

app.addEventListener("submit", async (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) {
    return;
  }

  event.preventDefault();

  try {
    if (form.id === "loginForm") {
      await handleLogin(form);
    } else if (form.id === "studentCreateForm") {
      await handleStudentCreate(form);
    } else if (form.id === "theoryBookingForm") {
      await handleTheoryBooking(form);
    } else if (form.id === "theoryExamForm") {
      await handleTheoryExam(form);
    } else if (form.id === "practiceBookingForm") {
      await handlePracticeBooking(form);
    } else if (form.id === "practiceExamForm") {
      await handlePracticeExam(form);
    } else if (form.id === "examResultForm") {
      await handleExamResult(form);
    }
  } catch (error) {
    setMessage("error", sanitizeErrorMessage(error?.message || "Eingaben konnten nicht verarbeitet werden."));
    render();
  }
});

app.addEventListener("click", async (event) => {
  const contextAction = event.target.closest("[data-context-action]");
  if (contextAction) {
    await handleContextMenuAction(contextAction.dataset.contextAction);
    return;
  }

  const contextTrigger = event.target.closest("[data-context-trigger]");
  if (contextTrigger) {
    const rect = contextTrigger.getBoundingClientRect();
    openContextMenu(
      contextTrigger.dataset.contextTrigger,
      contextTrigger.dataset.contextId,
      rect.left,
      rect.bottom + 4
    );
    return;
  }

  if (state.contextMenu && !event.target.closest(".context-menu")) {
    closeContextMenu();
    render();
    return;
  }

  const closeModal = event.target.closest("[data-close-modal]");
  if (closeModal && (!event.target.closest(".modal-panel") || closeModal.tagName === "BUTTON")) {
    state.modal = null;
    render();
    return;
  }

  const modalOpenButton = event.target.closest("[data-open-modal]");
  if (modalOpenButton) {
    state.modal = modalOpenButton.dataset.openModal;
    render();
    return;
  }

  const pilotButton = event.target.closest("[data-select-pilot]");
  if (pilotButton) {
    const pilot = pilotById(pilotButton.dataset.selectPilot);
    if (pilot?.available) {
      state.practiceSelection.pilotId = pilot.id;
      state.modal = null;
      render();
    }
    return;
  }

  const aircraftButton = event.target.closest("[data-select-aircraft]");
  if (aircraftButton) {
    const aircraft = aircraftById(aircraftButton.dataset.selectAircraft);
    if (aircraft?.available) {
      state.practiceSelection.aircraftId = aircraft.id;
      state.modal = null;
      render();
    }
    return;
  }

  const examButton = event.target.closest("[data-select-exam]");
  if (examButton) {
    state.examResultSelection = examButton.dataset.selectExam;
    render();
    return;
  }

  const tab = event.target.closest("[data-tab]");
  if (tab) {
    state.view = tab.dataset.tab;
    setMessage("notice", "");
    render();
    return;
  }

  const selectButton = event.target.closest("[data-select-student]");
  if (selectButton) {
    await selectStudentForView(selectButton.dataset.selectStudent);
    return;
  }

  const deleteStudentButton = event.target.closest("[data-delete-student]");
  if (deleteStudentButton) {
    const id = deleteStudentButton.dataset.deleteStudent;
    const student = state.students.find((entry) => entry.id === id);
    const name = student ? studentFullName(student) : id;
    if (!window.confirm(`Schüler ${name} wirklich löschen? Zugehörige Kurse, Flüge und Prüfungen werden im Demo-Modus ebenfalls entfernt.`)) {
      return;
    }
    await runAction(async () => {
      await api(`/schueler/${encodeURIComponent(id)}`, { method: "DELETE" });
      if (state.selectedStudentId === id) {
        state.selectedStudentId = "";
        localStorage.removeItem(STORAGE.selectedStudentId);
      }
    }, "Schüler wurde gelöscht.");
    return;
  }

  const cancelButton = event.target.closest("[data-cancel-flight]");
  if (cancelButton) {
    await cancelFlight(cancelButton.dataset.cancelFlight);
    return;
  }

  const cancelCourseButton = event.target.closest("[data-cancel-course]");
  if (cancelCourseButton) {
    await cancelCourse(cancelCourseButton.dataset.cancelCourse);
    return;
  }

  if (event.target.closest("#completeTrainingButton")) {
    await runAction(
      () => api(`/ausbildung/${encodeURIComponent(ensureStudentId())}/abschliessen`, { method: "POST", body: {} }),
      "Ausbildung wurde abgeschlossen."
    );
    return;
  }

  if (event.target.closest("#refreshButton")) {
    await refreshAll();
    return;
  }

  if (event.target.closest("#logoutButton")) {
    await logout();
  }
});

app.addEventListener("dragstart", (event) => {
  const card = event.target.closest("[data-planning-card]");
  if (!card || card.getAttribute("draggable") !== "true") {
    event.preventDefault();
    return;
  }
  state.planningDrag = {
    id: card.dataset.planningCard,
    type: card.dataset.planningCardType
  };
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", card.dataset.planningCardType || "");
  card.classList.add("dragging");
});

app.addEventListener("dragover", (event) => {
  const column = event.target.closest("[data-planning-column]");
  if (!column || !state.planningDrag) {
    return;
  }
  event.preventDefault();
  const allowed = planningDropAllowed(state.planningDrag.type, column.dataset.planningColumn);
  event.dataTransfer.dropEffect = allowed ? "move" : "none";
  column.classList.toggle("drag-allowed", allowed);
  column.classList.toggle("drag-blocked", !allowed);
});

app.addEventListener("dragleave", (event) => {
  const column = event.target.closest("[data-planning-column]");
  const relatedTarget = event.relatedTarget instanceof Node ? event.relatedTarget : null;
  if (!column || (relatedTarget && column.contains(relatedTarget))) {
    return;
  }
  column.classList.remove("drag-allowed", "drag-blocked");
});

app.addEventListener("drop", async (event) => {
  const column = event.target.closest("[data-planning-column]");
  if (!column || !state.planningDrag) {
    return;
  }
  event.preventDefault();
  const cardType = state.planningDrag.type;
  const cardId = state.planningDrag.id;
  const targetColumnId = column.dataset.planningColumn;
  clearPlanningDropClasses();
  state.planningDrag = null;
  await handlePlanningDrop(cardType, targetColumnId, cardId);
});

app.addEventListener("dragend", (event) => {
  const card = event.target.closest("[data-planning-card]");
  if (card) {
    card.classList.remove("dragging");
  }
  state.planningDrag = null;
  clearPlanningDropClasses();
});

function applyModalSearch(input) {
  const query = input.value;
  document.querySelectorAll("[data-modal-row]").forEach((row) => {
    row.hidden = !includesText(row.dataset.search || "", query);
  });
}

function renderPreservingControl(control, selector) {
  const selectionStart = typeof control.selectionStart === "number" ? control.selectionStart : null;
  const selectionEnd = typeof control.selectionEnd === "number" ? control.selectionEnd : null;
  render();
  requestAnimationFrame(() => {
    const next = document.querySelector(selector);
    if (!next) {
      return;
    }
    next.focus();
    if (selectionStart !== null && typeof next.setSelectionRange === "function") {
      next.setSelectionRange(selectionStart, selectionEnd);
    }
  });
}

app.addEventListener("input", (event) => {
  const studentFilter = event.target.closest("[data-student-filter]");
  if (studentFilter) {
    state.studentFilters[studentFilter.dataset.studentFilter] = studentFilter.value;
    renderPreservingControl(studentFilter, `[data-student-filter="${studentFilter.dataset.studentFilter}"]`);
    return;
  }

  const tableSearch = event.target.closest("[data-table-search]");
  if (tableSearch) {
    state.tableSearch[tableSearch.dataset.tableSearch] = tableSearch.value;
    renderPreservingControl(tableSearch, `[data-table-search="${tableSearch.dataset.tableSearch}"]`);
    return;
  }

  const modalSearch = event.target.closest("[data-modal-search]");
  if (modalSearch) {
    applyModalSearch(modalSearch);
  }
});

app.addEventListener("contextmenu", (event) => {
  const contextSource = event.target.closest("[data-context-type]");
  if (!contextSource || !app.contains(contextSource)) {
    return;
  }
  event.preventDefault();
  openContextMenu(
    contextSource.dataset.contextType,
    contextSource.dataset.contextId,
    event.clientX,
    event.clientY
  );
});

app.addEventListener("change", async (event) => {
  const studentFilter = event.target.closest("[data-student-filter]");
  if (studentFilter) {
    state.studentFilters[studentFilter.dataset.studentFilter] = studentFilter.value;
    renderPreservingControl(studentFilter, `[data-student-filter="${studentFilter.dataset.studentFilter}"]`);
  }
});

document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") {
    return;
  }
  if (state.contextMenu) {
    closeContextMenu();
    render();
    return;
  }
  if (state.modal) {
    state.modal = null;
    render();
  }
});

async function init() {
  if (!state.token) {
    render();
    return;
  }

  state.loading = true;
  render();
  try {
    const user = await api("/auth/me");
    state.displayName = user.displayName || user.username || state.displayName || "demo";
    localStorage.setItem(STORAGE.displayName, state.displayName);
    await loadStudents();
    await loadSelectedStudentData();
    state.error = "";
  } catch (error) {
    clearAuth("Bitte anmelden.");
    return;
  } finally {
    state.loading = false;
    render();
  }
}

init();
