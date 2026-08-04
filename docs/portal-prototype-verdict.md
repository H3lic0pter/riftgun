# Portal lifecycle verdict

Question: can replacement, animation, and bounce prevention stay independent from Tempad's energy, saved-location, permission, upgrade, UI, and compatibility systems?

Verdict: yes. Each pair owns a five-state lifecycle: `CHARGING`, `OPENING`, `OPEN`, `CLOSING`, `CLOSED`. Replacing a pair closes the old pair for five ticks while the new pair charges for 26 ticks. Only `OPEN` portals teleport. Cooldown belongs to the travelling entity for 20 ticks; firing has no cooldown.

Primary source: Git branch `prototype/portal-state-machine`, commit `b73d4f6`.

