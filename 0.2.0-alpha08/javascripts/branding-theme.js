(function () {
  "use strict";

  var LIGHT_LOGO = "devview-icon-light.svg";
  var DARK_LOGO = "devview-icon-dark.svg";
  var LIGHT_HERO_LOGO = "devview-logo-light.svg";
  var DARK_HERO_LOGO = "devview-logo-dark.svg";
  var scheduled = false;

  function isDarkScheme() {
    return document.body && document.body.getAttribute("data-md-color-scheme") === "slate";
  }

  function setHeaderLogo(darkMode) {
    var headerLogos = document.querySelectorAll('[data-md-component="logo"] img');

    headerLogos.forEach(function (img) {
      var prefix = getAssetPrefix(img.getAttribute("src") || "", /devview-icon-(?:light|dark|mono)\.svg(?:#.*)?$/);
      img.setAttribute("src", prefix + (darkMode ? DARK_LOGO : LIGHT_LOGO));
    });
  }

  function setHomeHeroLogo(darkMode) {
    var light = document.querySelectorAll(".devview-logo--light");
    var dark = document.querySelectorAll(".devview-logo--dark");

    light.forEach(function (img) {
      var prefix = getAssetPrefix(img.getAttribute("src") || "", /devview-logo-(?:light|dark|mono)\.svg(?:#.*)?$/);
      img.setAttribute("src", prefix + LIGHT_HERO_LOGO);
      img.style.display = darkMode ? "none" : "inline-block";
    });
    dark.forEach(function (img) {
      var prefix = getAssetPrefix(img.getAttribute("src") || "", /devview-logo-(?:light|dark|mono)\.svg(?:#.*)?$/);
      img.setAttribute("src", prefix + DARK_HERO_LOGO);
      img.style.display = darkMode ? "inline-block" : "none";
    });
  }

  function getAssetPrefix(src, pattern) {
    var cleanSrc = src.split("?")[0].split("#")[0];
    var match = cleanSrc.match(pattern);

    if (match) {
      return cleanSrc.slice(0, cleanSrc.length - match[0].length);
    }

    return "";
  }

  function applyBrandingTheme() {
    scheduled = false;
    var darkMode = isDarkScheme();
    setHeaderLogo(darkMode);
    setHomeHeroLogo(darkMode);
  }

  function scheduleApplyBrandingTheme() {
    if (scheduled) {
      return;
    }

    scheduled = true;
    window.requestAnimationFrame(applyBrandingTheme);
  }

  function observeColorScheme() {
    if (!document.body || !window.MutationObserver) {
      return;
    }

    var observer = new MutationObserver(function (mutations) {
      for (var i = 0; i < mutations.length; i++) {
        if (mutations[i].attributeName === "data-md-color-scheme") {
          applyBrandingTheme();
          break;
        }
      }
    });

    observer.observe(document.body, {
      attributes: true,
      attributeFilter: ["data-md-color-scheme"],
    });
  }

  function observePageChanges() {
    if (!document.body || !window.MutationObserver) {
      return;
    }

    var observer = new MutationObserver(function () {
      scheduleApplyBrandingTheme();
    });

    observer.observe(document.body, {
      childList: true,
      subtree: true,
    });
  }

  function init() {
    applyBrandingTheme();
    observeColorScheme();
    observePageChanges();
  }

  document.addEventListener("DOMContentLoaded", init);
  window.addEventListener("load", scheduleApplyBrandingTheme);
  window.addEventListener("pageshow", scheduleApplyBrandingTheme);
  window.addEventListener("popstate", scheduleApplyBrandingTheme);
  window.addEventListener("hashchange", scheduleApplyBrandingTheme);

  if (window.document$ && typeof window.document$.subscribe === "function") {
    window.document$.subscribe(function () {
      scheduleApplyBrandingTheme();
    });
  }
})();

