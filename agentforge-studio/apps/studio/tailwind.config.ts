import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/**/*.{ts,tsx}",
    "./index.html",
    "../../packages/ui/src/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        /* ── New IDE surface hierarchy ── */
        surface: {
          DEFAULT: "rgb(var(--surface) / <alpha-value>)",
          dim: "rgb(var(--surface-dim) / <alpha-value>)",
          bright: "rgb(var(--surface-bright) / <alpha-value>)",
          "container-lowest": "rgb(var(--surface-container-lowest) / <alpha-value>)",
          "container-low": "rgb(var(--surface-container-low) / <alpha-value>)",
          container: "rgb(var(--surface-container) / <alpha-value>)",
          "container-high": "rgb(var(--surface-container-high) / <alpha-value>)",
          "container-highest": "rgb(var(--surface-container-highest) / <alpha-value>)",
        },
        "on-surface": "rgb(var(--on-surface) / <alpha-value>)",
        "on-surface-variant": "rgb(var(--on-surface-variant) / <alpha-value>)",
        outline: {
          DEFAULT: "rgb(var(--outline) / <alpha-value>)",
          variant: "rgb(var(--outline-variant) / <alpha-value>)",
        },
        primary: {
          DEFAULT: "rgb(var(--primary) / <alpha-value>)",
          container: "rgb(var(--primary-container) / <alpha-value>)",
          fixed: "rgb(var(--primary) / <alpha-value>)",
          "fixed-dim": "rgb(var(--primary) / <alpha-value>)",
          hover: "rgb(var(--color-primary-hover) / <alpha-value>)",
          active: "rgb(var(--color-primary-active) / <alpha-value>)",
          foreground: "rgb(var(--color-primary-foreground) / <alpha-value>)",
        },
        "on-primary": "rgb(var(--on-primary) / <alpha-value>)",
        "accent-primary": "rgb(var(--accent-primary) / <alpha-value>)",
        secondary: "rgb(var(--accent-secondary) / <alpha-value>)",
        "secondary-container": "rgb(111 0 190 / <alpha-value>)",
        "on-secondary-container": "rgb(214 169 255 / <alpha-value>)",
        tertiary: "rgb(var(--accent-tertiary) / <alpha-value>)",
        "tertiary-container": "rgb(0 158 185 / <alpha-value>)",
        "tertiary-fixed-dim": "rgb(var(--accent-tertiary) / <alpha-value>)",
        "accent-tool": "rgb(var(--accent-tool) / <alpha-value>)",
        error: "rgb(var(--color-error) / <alpha-value>)",
        success: "rgb(var(--color-success) / <alpha-value>)",
        warning: "rgb(var(--accent-tool) / <alpha-value>)",

        /* ── Backward-compatible aliases (old class names still work) ── */
        background: "rgb(var(--surface) / <alpha-value>)",
        foreground: "rgb(var(--on-surface) / <alpha-value>)",
        muted: {
          DEFAULT: "rgb(var(--surface-container-low) / <alpha-value>)",
          foreground: "rgb(var(--outline) / <alpha-value>)",
        },
        border: "rgb(var(--outline-variant) / <alpha-value>)",
        destructive: {
          DEFAULT: "rgb(var(--color-error) / <alpha-value>)",
          foreground: "rgb(255 255 255 / <alpha-value>)",
        },
        ring: "rgb(var(--accent-primary) / <alpha-value>)",
      },
      borderRadius: {
        sm: "var(--radius-sm)",
        DEFAULT: "var(--radius-default)",
        md: "var(--radius-md)",
        lg: "var(--radius-lg)",
        xl: "var(--radius-xl)",
        full: "9999px",
      },
      spacing: {
        "space-xs": "var(--space-xs)",
        "space-sm": "var(--space-sm)",
        "space-md": "var(--space-md)",
        "space-lg": "var(--space-lg)",
        "space-xl": "var(--space-xl)",
        gutter: "var(--space-md)",
        "gutter-desktop": "var(--space-lg)",
        margin: "var(--space-md)",
        "margin-desktop": "var(--space-lg)",
      },
      fontFamily: {
        sans: ["var(--font-sans)"],
        mono: ["var(--font-mono)"],
        "body-md": ["Inter"],
        "body-lg": ["Inter"],
        "body-sm": ["Inter"],
        "headline-xl": ["Inter"],
        "headline-lg": ["Inter"],
        "headline-md": ["Inter"],
        "label-md": ["Inter"],
        "label-sm": ["Inter"],
        "code-sm": ["JetBrains Mono"],
      },
      fontSize: {
        "body-md": ["13px", { lineHeight: "18px", letterSpacing: "0em", fontWeight: "400" }],
        "body-lg": ["14px", { lineHeight: "20px", letterSpacing: "-0.005em", fontWeight: "400" }],
        "body-sm": ["11px", { lineHeight: "15px", letterSpacing: "0.01em", fontWeight: "400" }],
        "headline-xl": ["28px", { lineHeight: "34px", letterSpacing: "-0.02em", fontWeight: "600" }],
        "headline-lg": ["20px", { lineHeight: "26px", letterSpacing: "-0.015em", fontWeight: "600" }],
        "headline-md": ["16px", { lineHeight: "22px", letterSpacing: "-0.01em", fontWeight: "600" }],
        "label-md": ["12px", { lineHeight: "16px", letterSpacing: "0.01em", fontWeight: "500" }],
        "label-sm": ["10px", { lineHeight: "13px", letterSpacing: "0.04em", fontWeight: "600" }],
        "code-sm": ["11px", { lineHeight: "16px", letterSpacing: "0em", fontWeight: "400" }],
        /* backward-compat aliases */
        xs: "var(--text-xs)",
        sm: "var(--text-sm)",
        base: "var(--text-base)",
        lg: "var(--text-lg)",
        xl: "var(--text-xl)",
        "2xl": "var(--text-2xl)",
      },
      boxShadow: {
        sm: "var(--shadow-sm)",
        md: "var(--shadow-md)",
        lg: "var(--shadow-lg)",
        xl: "var(--shadow-xl)",
      },
      transitionDuration: {
        fast: "var(--duration-fast)",
        normal: "var(--duration-normal)",
        slow: "var(--duration-slow)",
      },
      transitionTimingFunction: {
        mac: "var(--ease-mac)",
        "mac-bounce": "var(--ease-mac-bounce)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
        "progress-indeterminate": {
          "0%": { transform: "translateX(-100%)" },
          "100%": { transform: "translateX(400%)" },
        },
        "ping-slow": {
          "0%": { transform: "scale(1)", opacity: "1" },
          "75%, 100%": { transform: "scale(2)", opacity: "0" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
        "progress-indeterminate": "progress-indeterminate 1.5s ease-in-out infinite",
        "ping-slow": "ping-slow 2s cubic-bezier(0, 0, 0.2, 1) infinite",
      },
    },
  },
  plugins: [],
};

export default config;
