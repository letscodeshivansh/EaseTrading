import type { Config } from "tailwindcss";

/**
 * EaseTrading design tokens.
 *
 *  - slate   : re-tuned to a deep navy so every existing bg-/border-/text-slate-* class
 *              picks up the new look (950 = page, 900 = card, 800 = line, 700 = raised line).
 *  - brand   : indigo accent — used ONLY for navigation, actions and highlights.
 *  - up/down : emerald / rose — used ONLY for profit/loss and price direction, so
 *              green and red always mean money, never decoration.
 */
const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        slate: {
          700: "#2E3852",
          800: "#1F283C",
          900: "#121826",
          950: "#0A0E17",
        },
        brand: {
          DEFAULT: "#6366F1", // buttons (white text passes contrast)
          dark: "#4F46E5",    // button hover
          light: "#A5B4FC",   // accent text on dark surfaces
        },
        up: "#34D399",   // gains
        down: "#FB7185", // losses
      },
      fontFamily: {
        sans: ["var(--font-inter)", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 1px 0 0 rgba(255,255,255,0.03) inset, 0 8px 24px -12px rgba(0,0,0,0.6)",
      },
    },
  },
  plugins: [],
};
export default config;
