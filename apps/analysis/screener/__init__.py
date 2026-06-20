"""Fundamental screening engine.

Encodes your stock-picking rules (P/E < 30, ROE > 25%, EPS > 0, FII/DII > 5%,
profit growth > 0, ...) as DATA, not code. Each rule is {key, op, value, mandatory},
so thresholds can be edited from the UI without touching the engine.
"""
