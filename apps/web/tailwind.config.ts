import type { Config } from "tailwindcss";

/**
 * EaseTrading design tokens.
 *
 *  - slate   : re-tuned to pure neutral greys on black, so every existing
 *              bg-/border-/text-slate-* class picks up the look
 *              (950 = page, 900 = card, 800 = line, 700 = raised line).
 *  - brand   : white accent — buttons are white with black text; used ONLY for
 *              navigation, actions and highlights.
 *  - up/down : green / red — used ONLY for profit/loss and price direction, so
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
          50: "#FAFAFA",
          100: "#F5F5F5",
          200: "#E5E5E5",
          300: "#D4D4D4",
          400: "#A3A3A3",
          500: "#737373",
          600: "#525252",
          700: "#2E2E2E",
          800: "#1F1F1F",
          900: "#111111",
          950: "#000000",
        },
        brand: {
          DEFAULT: "#FFFFFF", // buttons (black text)
          dark: "#D4D4D4",    // button hover
          light: "#FFFFFF",   // accent text on dark surfaces
        },
        up: "#22C55E",   // gains
        down: "#EF4444", // losses
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
