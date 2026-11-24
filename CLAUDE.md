# EaseTrading — notes for Claude

Personal project: AI-assisted NSE/BSE trading app with an MCP server so Claude can use it.
Services: `apps/web` (Next.js), `apps/api` (Spring Boot, system of record), `apps/analysis` (FastAPI + `mcp_server.py`).

## Always keep the flow report current

`docs/PROJECT_FLOW.md` is the living flow report. After **any** code change in this repo, update it in the same change:

1. The affected flow in §5 (diagram + step table + file:line links).
2. The Status Board in §2.
3. The Issues table in §6 — mark fixed items `✅ Fixed (<commit>)`, add new ones with the next B-number.
4. The roadmap checkboxes in §7.
5. A new Changelog row in §9 (date, commit, change, sections touched), and the "Last updated" / "Code version" header.

Keep the language plain and simple — the report is for tracking progress, not for engineers only.
