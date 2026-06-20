"""Technical-indicator library.

Every function here is PURE: you give it price data, it returns numbers. No network,
no database, no global state. That makes the math fully reproducible and easy to
unit-test against known fixtures (see ../tests/test_indicators.py).

The public entry point is `engine.compute_all(candles)`, which bundles all
indicators into one dictionary for the API to return.
"""
