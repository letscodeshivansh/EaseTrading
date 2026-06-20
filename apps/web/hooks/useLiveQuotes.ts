"use client";

import { useEffect, useRef, useState } from "react";
import { WS_BASE } from "@/lib/api";
import type { Tick } from "@/lib/types";

/**
 * Opens ONE WebSocket to the backend and keeps a live map of token -> latest price.
 *
 * Every component that needs live prices can call this hook; React shares the state.
 * The socket auto-reconnects if the connection drops (e.g. the API restarts).
 *
 * Returns: { quotes, subscribe }
 *   quotes      = { [token]: ltp }
 *   subscribe() = ask the backend to start streaming a new token
 */
export function useLiveQuotes() {
  const [quotes, setQuotes] = useState<Record<string, number>>({});
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    let closed = false;
    let reconnectTimer: ReturnType<typeof setTimeout>;

    function connect() {
      const ws = new WebSocket(`${WS_BASE}/api/stream`);
      socketRef.current = ws;

      ws.onmessage = (event) => {
        try {
          const tick = JSON.parse(event.data) as Tick;
          if (tick.token) {
            setQuotes((prev) => ({ ...prev, [tick.token]: tick.ltp }));
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

    // Cleanup when the component using the hook unmounts.
    return () => {
      closed = true;
      clearTimeout(reconnectTimer);
      socketRef.current?.close();
    };
  }, []);

  function subscribe(token: string) {
    const ws = socketRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ action: "subscribe", token }));
    }
  }

  return { quotes, subscribe };
}
