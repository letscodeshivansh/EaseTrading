"""Rule definitions and evaluation.

A rule looks like:  {"key": "pe_ratio", "op": "lt", "value": 30, "mandatory": True}

"key" must match a field name in the fundamentals dict. The default rule set below
is exactly the criteria you specified.
"""
from __future__ import annotations

from typing import Callable

# Comparison operators, kept tiny and explicit.
OPERATORS: dict[str, Callable[[float, float], bool]] = {
    "lt": lambda a, b: a < b,
    "lte": lambda a, b: a <= b,
    "gt": lambda a, b: a > b,
    "gte": lambda a, b: a >= b,
    "eq": lambda a, b: a == b,
}


def default_rules() -> list[dict]:
    """The criteria you asked for. Editable per request from the frontend."""
    return [
        {"key": "pe_ratio", "op": "lt", "value": 30, "mandatory": True},
        {"key": "roe_pct", "op": "gt", "value": 25, "mandatory": True},
        {"key": "eps", "op": "gt", "value": 0, "mandatory": True},
        {"key": "fii_holding_pct", "op": "gt", "value": 5, "mandatory": True},
        {"key": "dii_holding_pct", "op": "gt", "value": 5, "mandatory": True},
        {"key": "profit_growth_pct", "op": "gt", "value": 0, "mandatory": True},
        # Optional "nice to have" rules contribute to the score but don't exclude.
        {"key": "debt_to_equity", "op": "lt", "value": 1, "mandatory": False},
    ]


def evaluate(fundamentals: dict, rules: list[dict]) -> dict:
    """Apply every rule to one stock's fundamentals.

    Returns:
      passed / failed : lists of rule keys
      mandatory_pass  : True only if ALL mandatory rules pass (i.e. it's a candidate)
      score           : fraction of all rules passed (0..1), used for ranking
      values          : the actual numbers, so the result is auditable
    """
    passed, failed, values = [], [], {}
    mandatory_pass = True

    for rule in rules:
        key, op, target = rule["key"], rule["op"], rule["value"]
        actual = fundamentals.get(key)
        values[key] = actual

        ok = actual is not None and OPERATORS[op](actual, target)
        if ok:
            passed.append(key)
        else:
            failed.append(key)
            if rule.get("mandatory"):
                mandatory_pass = False

    total = len(rules) or 1
    return {
        "passed": passed,
        "failed": failed,
        "mandatory_pass": mandatory_pass,
        "score": round(len(passed) / total, 3),
        "values": values,
    }
