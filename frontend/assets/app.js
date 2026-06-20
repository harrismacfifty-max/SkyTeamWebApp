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
  { id: "P001", label: "P001 - Max Mueller" },
  { id: "P002", label: "P002 - Erika Schmidt" },
  { id: "P003", label: "P003 - kein Fluglehrer" },
  { id: "P004", label: "P004 - nicht verfuegbar" }
];

const AIRCRAFT_OPTIONS = [
  { id: "FZ002", label: "FZ002 - einsatzbereit" },
  { id: "FZ003", label: "FZ003 - einsatzbereit" },
  { id: "FZ001", label: "FZ001 - in Wartung" },
  { id: "FZ004", label: "FZ004 - gesperrt" }
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

function resetSelectedData() {
  state.selectedStudent = null;
  state.status = null;
  state.theorie = null;
  state.praxis = null;
  state.pruefungen = [];
}

function clearAuth(message = "") {
  localStorage.removeItem(STORAGE.token);
  localStorage.removeItem(STORAGE.displayName);
  state.token = "";
  state.displayName = "";
  state.loading = false;
  state.action = false;
  state.students = [];
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
  setMessage("error", error.message || "Unerwarteter Fehler.");
  return false;
}

async function api(path, options) {
  return SkyTeamApi.request(path, options);
}

async function loadStudents() {
  state.students = await api("/schueler");
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

  const id = encodeURIComponent(state.selectedStudentId);
  const [student, status, theorie, praxis, pruefungen] = await Promise.all([
    api(`/schueler/${id}`),
    api(`/status/${id}/gesamt`),
    api(`/theorie/${id}`),
    api(`/praxis/${id}`),
    api(`/pruefung/${id}`)
  ]);

  state.selectedStudent = student;
  state.status = status;
  state.theorie = theorie;
  state.praxis = praxis;
  state.pruefungen = pruefungen;
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
          <div class="brand-mark">ST</div>
          <div>
            <p class="eyebrow">SkyTeam Flight School</p>
            <h1>Flugschulprozess</h1>
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
        ${state.loading ? renderLoading() : renderCurrentView()}
      </main>
    </div>
  `;
}

function renderStudentContext() {
  return `
    <section class="student-context">
      <div>
        <p class="eyebrow">Ausgewählter Schüler</p>
        <h2>${escapeHtml(studentFullName(selectedStudent()))}</h2>
      </div>
      <label class="compact-label">
        Schüler auswählen
        <select id="studentPicker" ${state.loading || state.action ? "disabled" : ""}>
          ${state.students.length ? state.students.map((student) => `
            <option value="${escapeHtml(student.id)}" ${student.id === state.selectedStudentId ? "selected" : ""}>
              ${escapeHtml(`${student.vorname} ${student.name} (${student.id})`)}
            </option>
          `).join("") : `<option value="">Keine Schüler vorhanden</option>`}
        </select>
      </label>
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

function renderDashboardView() {
  const student = selectedStudent();
  const status = state.status || {};
  const theorie = state.theorie?.fortschritt || {};
  const praxis = state.praxis?.fortschritt || {};
  const canComplete = Boolean(status.theorieBestanden && status.praxisBestanden);

  return `
    <section class="dashboard-grid">
      <article class="panel hero-panel">
        <div>
          <p class="eyebrow">Dashboard</p>
          <h2>${escapeHtml(studentFullName(student))}</h2>
          <p class="muted">Vertrag ${escapeHtml(student?.ausbildungsVertragId || "-")} · ${escapeHtml(status.vertragsStatus || "Status unbekannt")}</p>
        </div>
        ${renderBadge(statusLabel(status.status), status.status === "ABGESCHLOSSEN" ? "success" : "info")}
      </article>

      <article class="metric-card">
        <span>Ausbildungsstatus</span>
        <strong>${escapeHtml(statusLabel(status.status))}</strong>
        <p>${escapeHtml(student?.notiz || "Keine Notiz hinterlegt.")}</p>
      </article>

      <article class="metric-card">
        <span>Theorie-Fortschritt</span>
        <strong>${formatHours(theorie.theorieStunden)} h</strong>
        ${renderProgress(theorie.theorieStunden, theorie.mindestTheorieStunden)}
        ${renderBooleanBadge(theorie.theoriePruefungFreigeschaltet, "Prüfung freigeschaltet", "Noch nicht freigeschaltet")}
      </article>

      <article class="metric-card">
        <span>Praxis-Fortschritt</span>
        <strong>${formatHours(praxis.flugStunden)} h</strong>
        ${renderProgress(praxis.flugStunden, praxis.mindestFlugStunden)}
        ${renderBooleanBadge(praxis.praxisPruefungFreigeschaltet, "Prüfung freigeschaltet", "Noch nicht freigeschaltet")}
      </article>

      <article class="metric-card">
        <span>Prüfungsstatus</span>
        <strong>${state.pruefungen.length}</strong>
        <p>Prüfungen erfasst</p>
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

function renderStudentsView() {
  const student = selectedStudent();
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
          <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>Schüler anlegen</button>
        </form>
      </div>

      <section class="two-column">
        <div class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Schüler</p>
              <h3>Liste</h3>
            </div>
            ${renderBadge(`${state.students.length} geladen`, "neutral")}
          </div>
          ${state.students.length ? renderStudentsTable() : `<p class="muted">Keine Schüler vorhanden.</p>`}
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

function renderStudentsTable() {
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Theorie</th>
            <th>Praxis</th>
            <th>Vertrag</th>
            <th>Optionen</th>
          </tr>
        </thead>
        <tbody>
          ${state.students.map((student) => `
            <tr class="${student.id === state.selectedStudentId ? "selected-row" : ""}">
              <td><strong>${escapeHtml(student.id)}</strong></td>
              <td>${escapeHtml(studentFullName(student))}</td>
              <td>${formatHours(student.theorieStunden)} h</td>
              <td>${formatHours(student.flugStunden)} h</td>
              <td>${escapeHtml(student.ausbildungsVertragId || "-")}</td>
              <td>
                <div class="row-actions">
                  <button class="button small" type="button" data-select-student="${escapeHtml(student.id)}" ${state.action ? "disabled" : ""}>
                    Auswählen
                  </button>
                  <button class="button small danger" type="button" data-delete-student="${escapeHtml(student.id)}" ${state.action ? "disabled" : ""}>
                    Löschen
                  </button>
                </div>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderStudentDetails(student) {
  return `
    <dl class="details">
      <div><dt>Schüler-ID</dt><dd>${escapeHtml(student.id)}</dd></div>
      <div><dt>Name</dt><dd>${escapeHtml(studentFullName(student))}</dd></div>
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
  const unlocked = Boolean(progress.theoriePruefungFreigeschaltet);

  return `
    <section class="stack-layout">
      <div class="status-strip">
        <article>
          <span>Theoriestunden</span>
          <strong>${formatHours(progress.theorieStunden)} / ${formatHours(progress.mindestTheorieStunden)} h</strong>
          ${renderProgress(progress.theorieStunden, progress.mindestTheorieStunden)}
        </article>
        <article>
          <span>Theorieprüfung</span>
          ${renderBooleanBadge(unlocked, "Freigeschaltet", "Noch gesperrt")}
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
          ${courses.length ? renderCoursesTable(courses) : `<p class="muted">Noch keine Theoriekurse vorhanden.</p>`}
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
            <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>Theoriekurs buchen</button>
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
          <button class="button primary full" type="submit" ${!unlocked || state.action ? "disabled" : ""}>Theorieprüfung anmelden</button>
        </form>
      </section>
    </section>
  `;
}

function renderCoursesTable(courses) {
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Datum</th>
            <th>Thema</th>
            <th>Lehrer</th>
          </tr>
        </thead>
        <tbody>
          ${courses.map((course) => `
            <tr>
              <td><strong>${escapeHtml(course.id)}</strong></td>
              <td>${formatDate(course.tag)}</td>
              <td>${escapeHtml(course.typ)}</td>
              <td>${escapeHtml(course.lehrer)}</td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderPracticeView() {
  const progress = state.praxis?.fortschritt || {};
  const flights = state.praxis?.fluege || [];
  const unlocked = Boolean(progress.praxisPruefungFreigeschaltet);

  return `
    <datalist id="pilotOptions">
      ${PILOT_OPTIONS.map((pilot) => `<option value="${escapeHtml(pilot.id)}">${escapeHtml(pilot.label)}</option>`).join("")}
    </datalist>
    <datalist id="aircraftOptions">
      ${AIRCRAFT_OPTIONS.map((aircraft) => `<option value="${escapeHtml(aircraft.id)}">${escapeHtml(aircraft.label)}</option>`).join("")}
    </datalist>

    <section class="stack-layout">
      <div class="status-strip">
        <article>
          <span>Flugstunden</span>
          <strong>${formatHours(progress.flugStunden)} / ${formatHours(progress.mindestFlugStunden)} h</strong>
          ${renderProgress(progress.flugStunden, progress.mindestFlugStunden)}
        </article>
        <article>
          <span>Praxisprüfung</span>
          ${renderBooleanBadge(unlocked, "Freigeschaltet", "Noch gesperrt")}
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
            <label>Fluglehrer/Pilot<input name="fluglehrer" list="pilotOptions" value="P001" required></label>
            <label>Flugzeug<input name="flugzeugId" list="aircraftOptions" value="FZ002" required></label>
            <label>Startflughafen<input name="startFlughafen" value="EDDV" required></label>
            <label>Zielflughafen<input name="zielFlughafen" value="EDDV" required></label>
            <label>Ausbildungsinhalt<input name="ausbildungsinhalt" placeholder="Platzrunde" required></label>
            <label class="full">Notizen<textarea name="notizen" rows="3" placeholder="Optional"></textarea></label>
            <button class="button primary full" type="submit" ${state.action ? "disabled" : ""}>Flugstunde buchen</button>
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
          <button class="button primary full" type="submit" ${!unlocked || state.action ? "disabled" : ""}>Praxisprüfung anmelden</button>
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
            <tr>
              <td><strong>${escapeHtml(flight.id)}</strong></td>
              <td>${formatDateTime(flight.startzeit)}<br><span class="muted">bis ${formatDateTime(flight.endzeit)}</span></td>
              <td>${escapeHtml(flight.flugzeugId)}</td>
              <td>${escapeHtml(flight.startFlughafen)} → ${escapeHtml(flight.zielFlughafen)}</td>
              <td>${escapeHtml(flight.flugArt)}</td>
              <td>
                <button class="button small danger" type="button" data-cancel-flight="${escapeHtml(flight.id)}" ${state.action ? "disabled" : ""}>
                  Stornieren
                </button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderExamsView() {
  return `
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Prüfungen</p>
            <h3>Angemeldete Prüfungen</h3>
          </div>
          ${renderBadge(`${state.pruefungen.length} Prüfungen`, "neutral")}
        </div>
        ${state.pruefungen.length ? renderExamsTable() : `<p class="muted">Noch keine Prüfungen vorhanden.</p>`}
      </div>

      <aside class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Ergebnis</p>
            <h3>Prüfungsergebnis speichern</h3>
          </div>
        </div>
        <form id="examResultForm" class="form-grid single">
          <label>
            Prüfung
            <select name="pruefungId" required ${state.pruefungen.length ? "" : "disabled"}>
              ${state.pruefungen.map((exam) => `
                <option value="${escapeHtml(exam.id)}">${escapeHtml(`${exam.id} - ${exam.typ}`)}</option>
              `).join("")}
            </select>
          </label>
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
          <button class="button primary" type="submit" ${!state.pruefungen.length || state.action ? "disabled" : ""}>Ergebnis speichern</button>
        </form>
      </aside>
    </section>
  `;
}

function renderExamsTable() {
  return `
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Datum</th>
            <th>Typ / Status</th>
          </tr>
        </thead>
        <tbody>
          ${state.pruefungen.map((exam) => `
            <tr>
              <td><strong>${escapeHtml(exam.id)}</strong></td>
              <td>${formatDate(exam.datum)}</td>
              <td>${escapeHtml(exam.typ)}</td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderCompletionView() {
  const status = state.status || {};
  const canComplete = Boolean(status.theorieBestanden && status.praxisBestanden);
  const completed = status.status === "ABGESCHLOSSEN";

  return `
    <section class="two-column">
      <div class="panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Gesamtstatus</p>
            <h3>${escapeHtml(statusLabel(status.status))}</h3>
          </div>
          ${renderBadge(status.vertragsStatus || "Unbekannt", completed ? "success" : "neutral")}
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
          ${completed ? "Bereits abgeschlossen" : "Ausbildung abschließen"}
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
    setMessage("error", error.message || "Eingaben konnten nicht verarbeitet werden.");
    render();
  }
});

app.addEventListener("click", async (event) => {
  const tab = event.target.closest("[data-tab]");
  if (tab) {
    state.view = tab.dataset.tab;
    setMessage("notice", "");
    render();
    return;
  }

  const selectButton = event.target.closest("[data-select-student]");
  if (selectButton) {
    state.selectedStudentId = selectButton.dataset.selectStudent;
    localStorage.setItem(STORAGE.selectedStudentId, state.selectedStudentId);
    await refreshAll();
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
    const flightId = cancelButton.dataset.cancelFlight;
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

app.addEventListener("change", async (event) => {
  if (event.target.id !== "studentPicker") {
    return;
  }
  state.selectedStudentId = event.target.value;
  localStorage.setItem(STORAGE.selectedStudentId, state.selectedStudentId);
  await refreshAll();
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
