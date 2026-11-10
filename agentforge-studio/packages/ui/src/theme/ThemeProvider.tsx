import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from "react";

/**
 * Theme modes supported by the design system.
 */
export type ThemeMode = "light" | "dark" | "system";

interface ThemeContextValue {
  mode: ThemeMode;
  resolved: "light" | "dark";
  setMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * ThemeProvider - manages light/dark/system theme.
 * Applies CSS class to document root for CSS variable switching.
 */
export function ThemeProvider({
  children,
  defaultMode = "system",
}: {
  children: ReactNode;
  defaultMode?: ThemeMode;
}) {
  const [mode, setMode] = useState<ThemeMode>(defaultMode);
  const [resolved, setResolved] = useState<"light" | "dark">("light");

  useEffect(() => {
    const root = document.documentElement;

    const applyTheme = (isDark: boolean) => {
      const theme = isDark ? "dark" : "light";
      setResolved(theme);
      root.classList.remove("light", "dark");
      root.classList.add(theme);
    };

    if (mode === "system") {
      const mq = window.matchMedia("(prefers-color-scheme: dark)");
      applyTheme(mq.matches);
      const handler = (e: MediaQueryListEvent) => applyTheme(e.matches);
      mq.addEventListener("change", handler);
      return () => mq.removeEventListener("change", handler);
    }

    applyTheme(mode === "dark");
  }, [mode]);

  return (
    <ThemeContext.Provider value={{ mode, resolved, setMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used within ThemeProvider");
  return ctx;
}
