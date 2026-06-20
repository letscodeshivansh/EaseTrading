"""The analyst layer: 10 strategy personas + the verdict engine.

Each strategy is a profile = (persona prompt) + (which signals it weighs most) +
(a strict JSON output contract). The verdict is produced by Claude (via the Agent
SDK on the Pro plan) when available, or by a deterministic grounded analyst as a
fallback so the app works offline in development.

Either way the NUMBERS come from the deterministic indicator/fundamental engine, so
verdicts are always grounded and auditable — the LLM only writes the narrative.
"""
