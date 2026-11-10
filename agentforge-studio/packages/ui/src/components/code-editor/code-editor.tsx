import { useEffect, useRef } from "react";
import type { Extension } from "@codemirror/state";
import { EditorView, basicSetup } from "codemirror";
import { EditorState } from "@codemirror/state";
import { json } from "@codemirror/lang-json";
import { yaml } from "@codemirror/lang-yaml";
import { javascript } from "@codemirror/lang-javascript";
import { oneDark } from "@codemirror/theme-one-dark";
import { keymap } from "@codemirror/view";
import { defaultKeymap } from "@codemirror/commands";
import { cn } from "../../lib/utils";

export type CodeEditorLanguage = "json" | "yaml" | "typescript" | "javascript" | "rego" | "jinja2";

export interface CodeEditorProps {
  value: string;
  language?: CodeEditorLanguage;
  onChange?: (value: string) => void;
  readOnly?: boolean;
  lineNumbers?: boolean;
  minHeight?: string;
  maxHeight?: string;
  className?: string;
}

const langExtensions: Record<string, () => Extension> = {
  json: () => json(),
  yaml: () => yaml(),
  typescript: () => javascript({ typescript: true }),
  javascript: () => javascript(),
};

export function CodeEditor({
  value,
  language = "json",
  onChange,
  readOnly = false,
  lineNumbers = true,
  minHeight = "120px",
  maxHeight = "600px",
  className,
}: CodeEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  // Create editor
  useEffect(() => {
    if (!containerRef.current) return;

    const langFactory = langExtensions[language];
    const extensions: Extension[] = [
      basicSetup,
      oneDark,
      keymap.of(defaultKeymap),
      EditorView.updateListener.of((update) => {
        if (update.docChanged && onChangeRef.current) {
          onChangeRef.current(update.state.doc.toString());
        }
      }),
      EditorView.theme({
        "&": {
          backgroundColor: "var(--surface)",
          fontSize: "13px",
          fontFamily: "var(--font-code)",
        },
        ".cm-content": {
          caretColor: "var(--color-primary)",
        },
        ".cm-gutters": {
          backgroundColor: "var(--surface-container-low)",
          border: "none",
          color: "var(--color-outline)",
        },
        ".cm-activeLine": {
          backgroundColor: "var(--surface-container-high)",
        },
        ".cm-activeLineGutter": {
          backgroundColor: "var(--surface-container-high)",
        },
        "&.cm-focused .cm-cursor": {
          borderLeftColor: "var(--color-primary)",
        },
        ".cm-selectionBackground": {
          backgroundColor: "rgba(10, 132, 255, 0.2) !important",
        },
      }),
      EditorView.editable.of(!readOnly),
    ];

    if (langFactory) {
      extensions.push(langFactory());
    }

    if (!lineNumbers) {
      // basicSetup includes lineNumbers; for simplicity we keep it and just hide via CSS
      extensions.push(
        EditorView.theme({
          ".cm-gutters": { display: "none" },
        }),
      );
    }

    const state = EditorState.create({
      doc: value,
      extensions,
    });

    const view = new EditorView({
      state,
      parent: containerRef.current,
    });

    viewRef.current = view;

    return () => {
      view.destroy();
      viewRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [language, readOnly, lineNumbers]);

  // Sync external value changes
  useEffect(() => {
    const view = viewRef.current;
    if (!view) return;
    const current = view.state.doc.toString();
    if (current !== value) {
      view.dispatch({
        changes: { from: 0, to: current.length, insert: value },
      });
    }
  }, [value]);

  return (
    <div
      ref={containerRef}
      className={cn(
        "rounded border border-outline-variant overflow-hidden",
        className,
      )}
      style={{ minHeight, maxHeight, overflow: "auto" }}
    />
  );
}
