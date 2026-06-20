"use client";

import { useEffect, useRef } from "react";
import {
  createChart,
  ColorType,
  type IChartApi,
  type ISeriesApi,
  type CandlestickData,
  type LineData,
  type IPriceLine,
  type Time,
} from "lightweight-charts";
import { getCandles } from "@/lib/api";
import type { Indicators } from "@/lib/types";

/**
 * A live candlestick chart for one instrument, with optional indicator overlays.
 *
 *  - Historical candles load from the API on mount / token change.
 *  - Live ticks (ltp) update the most recent candle in real time.
 *  - showSMA      : draws 50- and 200-period moving-average lines (computed here
 *                   from the candles, so no extra API data is needed).
 *  - indicators   : when provided AND showFib is on, the Fibonacci retracement
 *                   levels are drawn as horizontal price lines.
 */
export default function CandleChart({
  token,
  ltp,
  showSMA = false,
  showFib = false,
  indicators,
}: {
  token: string;
  ltp?: number;
  showSMA?: boolean;
  showFib?: boolean;
  indicators?: Indicators | null;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const sma50Ref = useRef<ISeriesApi<"Line"> | null>(null);
  const sma200Ref = useRef<ISeriesApi<"Line"> | null>(null);
  const lastBarRef = useRef<CandlestickData | null>(null);
  const closesRef = useRef<{ time: Time; close: number }[]>([]);
  const fibLinesRef = useRef<IPriceLine[]>([]);

  // ---- Build chart + load candles whenever the token changes. ----
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      layout: { background: { type: ColorType.Solid, color: "#020617" }, textColor: "#cbd5e1" },
      grid: { vertLines: { color: "#1e293b" }, horzLines: { color: "#1e293b" } },
      timeScale: { borderColor: "#334155" },
      rightPriceScale: { borderColor: "#334155" },
      autoSize: true, // responsive
    });

    const series = chart.addCandlestickSeries({
      upColor: "#16a34a", downColor: "#dc2626", borderVisible: false,
      wickUpColor: "#16a34a", wickDownColor: "#dc2626",
    });

    chartRef.current = chart;
    candleSeriesRef.current = series;

    getCandles(token, "1d", 250)
      .then((candles) => {
        const data: CandlestickData[] = candles.map((c) => ({
          time: c.ts.slice(0, 10) as Time,
          open: c.open, high: c.high, low: c.low, close: c.close,
        }));
        series.setData(data);
        lastBarRef.current = data[data.length - 1] ?? null;
        closesRef.current = data.map((d) => ({ time: d.time, close: d.close }));
        drawSMA();
        chart.timeScale().fitContent();
      })
      .catch((err) => console.error("Failed to load candles:", err));

    return () => chart.remove();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  // ---- Toggle SMA overlays on/off. ----
  useEffect(() => {
    drawSMA();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showSMA]);

  // ---- Draw / clear Fibonacci horizontal lines when indicators or toggle change. ----
  useEffect(() => {
    const series = candleSeriesRef.current;
    if (!series) return;

    // Remove any previous fib lines first.
    fibLinesRef.current.forEach((line) => series.removePriceLine(line));
    fibLinesRef.current = [];

    if (showFib && indicators?.fibonacci?.levels) {
      for (const [ratio, price] of Object.entries(indicators.fibonacci.levels)) {
        const line = series.createPriceLine({
          price,
          color: "#d97706", // amber
          lineWidth: 1,
          lineStyle: 2, // dashed
          axisLabelVisible: true,
          title: `Fib ${ratio}`,
        });
        fibLinesRef.current.push(line);
      }
    }
  }, [showFib, indicators, token]);

  // ---- Apply each live tick to the most recent candle. ----
  useEffect(() => {
    if (ltp == null || !candleSeriesRef.current || !lastBarRef.current) return;
    const bar = lastBarRef.current;
    const updated: CandlestickData = {
      time: bar.time, open: bar.open,
      high: Math.max(bar.high, ltp), low: Math.min(bar.low, ltp), close: ltp,
    };
    lastBarRef.current = updated;
    candleSeriesRef.current.update(updated);
  }, [ltp]);

  /** Compute simple moving averages from loaded closes and (re)draw the lines. */
  function drawSMA() {
    const chart = chartRef.current;
    if (!chart) return;

    // Clear existing SMA series.
    if (sma50Ref.current) { chart.removeSeries(sma50Ref.current); sma50Ref.current = null; }
    if (sma200Ref.current) { chart.removeSeries(sma200Ref.current); sma200Ref.current = null; }

    if (!showSMA || closesRef.current.length === 0) return;

    sma50Ref.current = chart.addLineSeries({ color: "#38bdf8", lineWidth: 1 });  // blue
    sma200Ref.current = chart.addLineSeries({ color: "#a78bfa", lineWidth: 1 }); // purple
    sma50Ref.current.setData(movingAverage(closesRef.current, 50));
    sma200Ref.current.setData(movingAverage(closesRef.current, 200));
  }

  return <div ref={containerRef} className="h-full w-full" />;
}

/** Plain simple moving average over a list of {time, close}. */
function movingAverage(points: { time: Time; close: number }[], period: number): LineData[] {
  const out: LineData[] = [];
  let sum = 0;
  for (let i = 0; i < points.length; i++) {
    sum += points[i].close;
    if (i >= period) sum -= points[i - period].close;
    if (i >= period - 1) out.push({ time: points[i].time, value: sum / period });
  }
  return out;
}
