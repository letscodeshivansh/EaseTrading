"""Invokes Claude through the Agent SDK — on the Pro plan, with no paid API.

KEY POINTS:
  - We use the Claude Agent SDK, which can authenticate with your Claude Pro/Max
    subscription login (the same one Claude Code uses). It does NOT require an
    ANTHROPIC_API_KEY. In fact, if ANTHROPIC_API_KEY is set, billing switches to the
    paid API — so we explicitly warn if we detect it.
  - The SDK connects to our read-only MCP server so Claude can pull live numbers.
  - If the SDK isn't installed / authenticated (e.g. local dev), we raise
    ClaudeUnavailable and the caller falls back to the deterministic grounded analyst.

The exact SDK call surface may vary by version; everything is wrapped defensively so
a mismatch degrades gracefully to the fallback instead of crashing a request.
"""
from __future__ import annotations

import json
import os
import re


class ClaudeUnavailable(Exception):
    """Raised when we cannot (or should not) call Claude — triggers the fallback."""


def _warn_if_api_key():
    if os.getenv("ANTHROPIC_API_KEY"):
        print("[claude] WARNING: ANTHROPIC_API_KEY is set — this bills the paid API. "
              "Unset it to use the Pro-plan subscription instead.")


def run_claude(system_prompt: str, user_prompt: str) -> dict:
    """Ask Claude for a verdict and return the parsed JSON dict.

    Raises ClaudeUnavailable on any problem so the caller can fall back.
    """
    _warn_if_api_key()

    # Lazy import: the app must boot even without the SDK installed.
    try:
        from claude_agent_sdk import query, ClaudeAgentOptions  # type: ignore
    except Exception as e:  # ImportError or anything else
        raise ClaudeUnavailable(f"Agent SDK not available: {e}")

    try:
        import anyio

        async def _ask() -> str:
            # Attach our MCP server so Claude can call read-only data tools.
            options = ClaudeAgentOptions(
                system_prompt=system_prompt,
                # The MCP server is configured via .mcp.json / SDK options in deployment.
                # Tools are read-only; Claude cannot place orders.
                allowed_tools=[
                    "get_quote", "get_candles", "get_fundamentals",
                    "get_indicators", "run_screener", "get_portfolio",
                ],
            )
            chunks: list[str] = []
            async for message in query(prompt=user_prompt, options=options):
                # Collect text content from the streamed messages.
                text = getattr(message, "text", None) or getattr(message, "content", None)
                if isinstance(text, str):
                    chunks.append(text)
            return "".join(chunks)

        raw = anyio.run(_ask)
        return _extract_json(raw)
    except ClaudeUnavailable:
        raise
    except Exception as e:
        raise ClaudeUnavailable(f"Claude invocation failed: {e}")


def _extract_json(text: str) -> dict:
    """Pull the JSON object out of Claude's response (handles ```json fences)."""
    if not text:
        raise ClaudeUnavailable("Empty response from Claude")
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if not match:
        raise ClaudeUnavailable("No JSON found in Claude response")
    try:
        return json.loads(match.group(0))
    except json.JSONDecodeError as e:
        raise ClaudeUnavailable(f"Invalid JSON from Claude: {e}")
