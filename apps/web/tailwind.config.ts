import type { Config } from "tailwindcss";

// Tailwind scans these files for class names. The theme adds our brand greens.
const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#1F6F4C",
          dark: "#0B3D2E",
          light: "#EAF3EF",
        },
        up: "#16a34a",   // green for rising prices
        down: "#dc2626", // red for falling prices
      },
    },
  },
  plugins: [],
};
export default config;
