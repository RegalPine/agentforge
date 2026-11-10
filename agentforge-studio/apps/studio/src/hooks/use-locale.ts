/**
 * FE-07: React hook for i18n locale management.
 *
 * Usage:
 *   const { locale, setLocale, t } = useLocale();
 *   <button onClick={() => setLocale(locale === "en-US" ? "zh-CN" : "en-US")}>
 *     {t("nav.dashboard")}
 *   </button>
 */

import { useState, useCallback } from "react";
import { t, getLocale, setLocale as setI18nLocale, initLocale, type Locale } from "../i18n/i18n";

export { initLocale };
export type { Locale };

/**
 * Hook providing reactive locale state and translation function.
 */
export function useLocale() {
  const [locale, setLocaleState] = useState(getLocale());

  const setLocale = useCallback((newLocale: Locale) => {
    setI18nLocale(newLocale);
    setLocaleState(newLocale);
  }, []);

  return { locale, setLocale, t };
}
