// Fills [data-i18n] elements from window.STRINGS[lang][key], and wires the
// [data-lang] buttons. The choice is remembered when storage is available.
(function () {
  const LANGUAGES = ["en", "fr"];
  const STORAGE_KEY = "debout.lang";

  function storedLanguage() {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch (e) {
      return null;
    }
  }

  function preferredLanguage() {
    const stored = storedLanguage();
    if (LANGUAGES.includes(stored)) return stored;
    const browser = navigator.languages || [navigator.language || ""];
    for (const tag of browser) {
      const lang = tag.toLowerCase().split("-")[0];
      if (LANGUAGES.includes(lang)) return lang;
    }
    return "en";
  }

  function apply(lang) {
    const strings = window.STRINGS[lang];
    document.documentElement.lang = lang;
    document.querySelectorAll("[data-i18n]").forEach((element) => {
      element.textContent = strings[element.dataset.i18n];
    });
    if (strings.title) document.title = strings.title;
    document.querySelectorAll("[data-lang]").forEach((button) => {
      button.setAttribute("aria-pressed", String(button.dataset.lang === lang));
    });
    document.dispatchEvent(new CustomEvent("languagechange", { detail: lang }));
  }

  window.currentStrings = () => window.STRINGS[document.documentElement.lang] || window.STRINGS.en;

  document.querySelectorAll("[data-lang]").forEach((button) => {
    button.addEventListener("click", () => {
      try {
        localStorage.setItem(STORAGE_KEY, button.dataset.lang);
      } catch (e) {
        // Private mode: the choice lasts for this page only.
      }
      apply(button.dataset.lang);
    });
  });

  apply(preferredLanguage());
})();
