/// <reference types="vitest/globals" />
/**
 * A11y test utilities for AgentForge Studio.
 *
 * FE-06: Provides helpers for accessibility testing.
 *
 * Usage in test files:
 *   import { assertAccessible } from "../test-utils/a11y";
 *   await assertAccessible(<Button>Click me</Button>);
 *
 * NOTE: This file is intended for use in vitest test files only.
 */

import type { ReactElement } from "react";

type RenderResult = { container: HTMLElement };

async function renderInTest(element: ReactElement): Promise<RenderResult> {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const mod: { render: (el: ReactElement) => RenderResult } =
    // eslint-disable-next-line no-new-func
    await (Function("return import('@testing-library/react')")() as Promise<{ render: (el: ReactElement) => RenderResult }>);
  return mod.render(element);
}

/**
 * Render and verify basic accessibility attributes.
 */
export async function assertAccessible(element: ReactElement) {
  const { container } = await renderInTest(element);

  // Check that all images have alt text
  const images = container.querySelectorAll("img");
  images.forEach((img: Element) => {
    expect(img.getAttribute("alt")).not.toBeNull();
  });

  // Check that all inputs have associated labels
  const inputs = container.querySelectorAll("input, textarea, select");
  inputs.forEach((input: Element) => {
    const id = input.getAttribute("id");
    const ariaLabel = input.getAttribute("aria-label");
    const ariaLabelledBy = input.getAttribute("aria-labelledby");
    const hasLabel =
      id != null ||
      ariaLabel != null ||
      ariaLabelledBy != null ||
      input.closest("label") != null;
    expect(hasLabel).toBe(true);
  });

  // Check that buttons have accessible names
  const buttons = container.querySelectorAll("button, [role='button']");
  buttons.forEach((btn: Element) => {
    const text = btn.textContent?.trim();
    const ariaLabel = btn.getAttribute("aria-label");
    expect((text != null && text.length > 0) || ariaLabel != null).toBe(true);
  });

  return { container };
}

/**
 * Verify keyboard focusability of interactive elements.
 */
export async function assertKeyboardFocusable(element: ReactElement) {
  const { container } = await renderInTest(element);

  const interactiveSelectors = [
    "button:not([disabled])",
    "a[href]",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    "[tabindex]:not([tabindex='-1'])",
  ];

  for (const selector of interactiveSelectors) {
    const elements = container.querySelectorAll(selector);
    elements.forEach((el: Element) => {
      expect(el.getAttribute("aria-hidden")).not.toBe("true");
    });
  }

  return { container };
}
