"""Company fundamentals (P/E, ROE, EPS, profit growth, FII/DII holdings).

Angel One's API does not provide these, so we source them separately and for FREE:
  - Yahoo Finance (yfinance) : P/E, EPS, ROE, margins, profit growth
  - BSE / NSE filings        : per-company FII / DII / promoter holding %

Following the same idea as the market feed, there is also a MockProvider so the app
works offline during development without hitting any external site.
"""
