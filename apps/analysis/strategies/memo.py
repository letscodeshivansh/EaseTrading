"""Deterministic, persona-flavoured memo writer (the fallback narrator).

When Claude is connected it writes this prose. When it isn't, this function produces
a grounded, readable memo from the SAME numbers so the product is fully usable in
development. Every sentence cites a real figure.
"""
from __future__ import annotations


def _fmt(v, suffix=""):
    return f"{v}{suffix}" if v is not None else "n/a"


def grounded_memo(verdict: dict, persona: dict) -> str:
    s = verdict["signals"]
    f = s["fundamentals"]
    sub = verdict["subScores"]
    sym = verdict["symbol"]

    rating = verdict["rating"].replace("_", " ").title()
    rr = verdict.get("rrRatio")

    lines = []
    lines.append(f"## {persona['name']} — {sym}")
    lines.append(f"**Verdict: {rating}**  ·  conviction {int(verdict['confidence']*100)}%  ·  "
                 f"lens: _{persona['focus']}_")
    lines.append("")

    # Technical read
    lines.append("### Technical picture")
    lines.append(
        f"Trend is **{_fmt(s.get('trend'))}** with the 50/200 relationship reading "
        f"_{_fmt((s.get('cross_50_200') or '').replace('_',' '))}_. "
        f"RSI(14) is {_fmt(s.get('rsi14'))} and MACD momentum is "
        f"_{_fmt(s.get('macd',{}).get('cross'))}_. "
        f"50-day SMA {_fmt(s.get('sma',{}).get('50'))}, 200-day SMA {_fmt(s.get('sma',{}).get('200'))}."
    )
    fib = s.get("fibonacci", {})
    if fib.get("active"):
        lines.append(f"Price is hugging the **{fib['active']}** Fibonacci level "
                     f"(swing {_fmt(fib.get('swingLow'))}–{_fmt(fib.get('swingHigh'))}), "
                     f"a key zone to watch.")
    lines.append("")

    # Fundamental read
    lines.append("### Fundamentals")
    lines.append(
        f"P/E **{_fmt(f.get('peRatio'))}**, ROE **{_fmt(f.get('roePct'),'%')}**, "
        f"EPS {_fmt(f.get('eps'))}, profit growth {_fmt(f.get('profitGrowthPct'),'%')}. "
        f"Institutional ownership — FII {_fmt(f.get('fiiHoldingPct'),'%')}, "
        f"DII {_fmt(f.get('diiHoldingPct'),'%')}."
    )
    lines.append("")

    # Trade plan
    lines.append("### Trade plan")
    lines.append(
        f"Entry near **{_fmt(verdict.get('entry'))}**, stop-loss **{_fmt(verdict.get('stopLoss'))}**, "
        f"target **{_fmt(verdict.get('target'))}** — risk-to-reward ≈ **{_fmt(rr)}**."
    )
    lines.append(
        f"Score breakdown — technical {sub['technical']}, valuation {sub['valuation']}, "
        f"quality {sub['quality']}, momentum {sub['momentum']}."
    )
    lines.append("")
    lines.append("_Decision support only — not SEBI-registered investment advice._")
    return "\n".join(lines)
