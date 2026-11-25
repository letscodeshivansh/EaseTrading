"use client";

import { useEffect, useRef, useState } from "react";
import { wsBase } from "@/lib/api";
import type { Tick } from "@/lib/types";

/**
 * Opens ONE WebSocket to the backend and keeps a live map of token -> latest price.
 *
 * Pass the tokens this screen cares about; the hook asks the backend to stream them
 * (and asks again after a reconnect). The socket auto-reconnects if it drops.
 *
 * Returns: { quotes, subscribe }
 *   quotes      = { [token]: ltp }
 *   subscribe() = ask the backend to start streaming one more token
 */
export function useLiveQuotes(tokens: string[] = []) {
  const [quotes, setQuotes] = useState<Record<string, number>>({});
  const socketRef = useRef<WebSocket | null>(null);
  const tokensRef = useRef<string[]>(tokens);
  tokensRef.current = tokens;

  useEffect(() => {
    let closed = false;
    let reconnectTimer: ReturnType<typeof setTimeout>;

    function connect() {
      const ws = new WebSocket(`${wsBase()}/api/stream`);
      socketRef.current = ws;

      ws.onopen = () => {
        for (const t of tokensRef.current) send(ws, t);
      };

      ws.onmessage = (event) => {
        try {
          const tick = JSON.parse(event.data) as Tick;
          if (tick.token) {
            setQuotes((prev) => (prev[tick.token] === tick.ltp ? prev : { ...prev, [tick.token]: tick.ltp }));
          }
        } catch {
          /* ignore malformed frames */
        }
      };

      // If the socket closes unexpectedly, retry after a short delay.
      ws.onclose = () => {
        if (!closed) reconnectTimer = setTimeout(connect, 1500);
      };
    }

    connect();

    return () => {
      closed = true;
      clearTimeout(reconnectTimer);
      socketRef.current?.close();
    };
  }, []);

  // When the screen's list of stocks changes, subscribe to the new ones.
  const key = tokens.join(",");
  useEffect(() => {
    const ws = socketRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) for (const t of tokensRef.current) send(ws, t);
  }, [key]);

  function subscribe(token: string) {
    const ws = socketRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) send(ws, token);
  }

  return { quotes, subscribe };
}

function send(ws: WebSocket, token: string) {
  ws.send(JSON.stringify({ action: "subscribe", token }));
}
