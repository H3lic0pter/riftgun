# Portal lifecycle verdict

Question: can replacement, animation, and bounce prevention stay independent from Tempad's energy, saved-location, permission, upgrade, UI, and compatibility systems?

Verdict: yes. Each pair owns a five-state lifecycle: `CHARGING`, `OPENING`, `OPEN`, `CLOSING`, `CLOSED`. Replacing a pair closes the old pair for five ticks while the new pair charges for six ticks. Only `OPEN` portals teleport. Each portal tracks entities currently inside its trigger; an arrival is blocked from returning until it fully leaves and enters again. Firing has no cooldown.

Primary source: Git branch `prototype/portal-state-machine`, commit `b73d4f6`.
