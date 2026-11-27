"use client";

import { useEffect, useRef, useState } from "react";
import { getAuthConfig, login, loginWithGoogle, register } from "@/lib/api";

// Google's sign-in script adds window.google.
declare global {
  interface Window { google?: any }
}

type Mode = "login" | "register";

/**
 * Log in / create an account — email + password, or "Sign in with Google".
 * The Google button only appears when the api has a Google client ID configured.
 * On success the api sets the login cookie and we go to the dashboard.
 */
export default function LoginPage() {
  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [googleId, setGoogleId] = useState<string | null>(null);
  const googleBtn = useRef<HTMLDivElement>(null);

  const done = () => { window.location.href = "/"; };

  // Show the Google button if Google sign-in is set up on the api.
  useEffect(() => {
    getAuthConfig().then((c) => setGoogleId(c.googleClientId)).catch(() => setGoogleId(null));
  }, []);

  useEffect(() => {
    if (!googleId) return;
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.onload = () => {
      window.google?.accounts.id.initialize({
        client_id: googleId,
        callback: (r: { credential: string }) => {
          setError(null);
          loginWithGoogle(r.credential).then(done).catch((e) => setError(e.message));
        },
      });
      if (googleBtn.current) {
        window.google?.accounts.id.renderButton(googleBtn.current, {
          theme: "filled_black", size: "large", shape: "pill", width: 320, text: "continue_with",
        });
      }
    };
    document.body.appendChild(script);
    return () => { script.remove(); };
  }, [googleId]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await (mode === "login" ? login(email, password) : register(email, password));
      done();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <div className="card w-full max-w-sm p-6">
        <div className="mb-6 flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-white font-black text-black">E</span>
          <span className="text-xl font-semibold tracking-tight">EaseTrading</span>
        </div>

        <div className="mb-5 inline-flex w-full rounded-xl border border-slate-800 bg-slate-950/60 p-1">
          {(["login", "register"] as const).map((m) => (
            <button key={m} type="button" onClick={() => { setMode(m); setError(null); }}
              className={`flex-1 rounded-lg py-1.5 text-sm font-medium transition ${
                mode === m ? "bg-brand text-black" : "text-slate-400 hover:text-slate-100"}`}>
              {m === "login" ? "Log in" : "Create account"}
            </button>
          ))}
        </div>

        <form onSubmit={submit} className="space-y-3">
          <label className="block text-xs text-slate-400">
            Email
            <input type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand" />
          </label>
          <label className="block text-xs text-slate-400">
            Password {mode === "register" && <span className="text-slate-500">(at least 8 characters)</span>}
            <input type="password" required minLength={mode === "register" ? 8 : undefined}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              value={password} onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand" />
          </label>
          {error && <p className="rounded-lg border border-down/30 bg-down/10 px-3 py-2 text-xs text-down">{error}</p>}
          <button type="submit" disabled={busy}
            className="w-full rounded-lg bg-brand py-2 text-sm font-semibold text-black hover:bg-brand-dark disabled:opacity-50">
            {busy ? "Please wait…" : mode === "login" ? "Log in" : "Create account"}
          </button>
        </form>

        {googleId && (
          <>
            <div className="my-5 flex items-center gap-3 text-xs text-slate-500">
              <span className="h-px flex-1 bg-slate-800" /> or <span className="h-px flex-1 bg-slate-800" />
            </div>
            <div ref={googleBtn} className="flex justify-center" />
          </>
        )}
      </div>
    </main>
  );
}
