import type { StorybookConfig } from "@storybook/react-vite";

/**
 * FE-09: Storybook configuration for @agentforge/ui.
 */
const config: StorybookConfig = {
  stories: ["../src/**/*.stories.@(ts|tsx)"],
  addons: ["@storybook/addon-essentials"],
  framework: {
    name: "@storybook/react-vite",
    options: {},
  },
};

export default config;
