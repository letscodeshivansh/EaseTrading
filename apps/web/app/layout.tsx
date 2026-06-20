import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "EaseTrading",
  description: "AI-assisted stock analysis & trading for NSE & BSE",
};

// Root layout wraps every page. The viewport meta makes the app responsive on phones.
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </head>
      <body>{children}</body>
    </html>
  );
}
