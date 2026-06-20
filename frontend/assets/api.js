(function exposeApi(window) {
  "use strict";

  const config = {
    apiBase: "http://localhost:8080/api",
    getToken: () => ""
  };

  function configure(options = {}) {
    if (options.apiBase) {
      config.apiBase = String(options.apiBase).replace(/\/+$/, "");
    }
    if (typeof options.getToken === "function") {
      config.getToken = options.getToken;
    }
  }

  function normalizeErrors(data) {
    if (!data || !Array.isArray(data.errors)) {
      return [];
    }
    return data.errors.filter(Boolean).map(String);
  }

  async function request(path, options = {}) {
    const headers = {
      Accept: "application/json",
      ...(options.headers || {})
    };

    const token = config.getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    let body = options.body;
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = typeof body === "string" ? body : JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(`${config.apiBase}${path}`, {
        ...options,
        headers,
        body
      });
    } catch (error) {
      const networkError = new Error("Backend nicht erreichbar. Bitte Launcher oder Backend-Start prüfen.");
      networkError.cause = error;
      throw networkError;
    }

    const text = await response.text();
    let data = {};
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (error) {
        const parseError = new Error("Die API hat keine gültige JSON-Antwort geliefert.");
        parseError.status = response.status;
        throw parseError;
      }
    }

    const apiErrors = normalizeErrors(data);
    if (!response.ok || data.success === false) {
      const message = apiErrors[0] || data.message || `HTTP ${response.status}`;
      const apiError = new Error(message);
      apiError.status = response.status;
      apiError.errors = apiErrors;
      throw apiError;
    }

    return Object.prototype.hasOwnProperty.call(data, "data") ? data.data : data;
  }

  window.SkyTeamApi = {
    configure,
    request
  };
})(window);
