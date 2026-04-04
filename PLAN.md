# Clan Wars Implementation Plan v6: Public Season Ladder, Join-to-War First, Milestone-Gated Delivery

## Summary
- Rebuild the app so **clans are the only progression system**. Remove ranked entirely from client, backend, routing, tasks, and UI. There is no migration and no backward compatibility.
- Keep the **solo core intact**: genre, lightning, grand master, verified daily challenge, daily board, coins, non-competitive abilities, and the premium shop stay playable without a clan.
- Add a **public Season ladder** visible to everyone. Solo players can see their row and progress, but their Season CP is capped. Clan members keep climbing through war contributions and clan tasks.
- Make the **join-to-war moment** the first product-quality slice: capped ladder row -> clan discovery -> join clan -> land on war board -> play one contributing quiz -> return to visibly updated war board.
- Deliver strictly by milestone. **Do not begin the next milestone until the current milestone passes its full acceptance checklist.** Each milestone is a shippable checkpoint.

## Non-Negotiable Product Rules
- No ranked system anywhere in the shipped product.
- Maximum clan size: **30**.
- Active member: at least **one eligible quiz completion in the last 7 days**.
- Matchmaking uses active-member bands only: `3-8`, `9-18`, `19-30`.
- War cycle: `24h prep`, `48h battle`, `12h settlement`.
- Season length: **8 wars**.
- Contributor cap for a war is frozen at pairing time as `min(leftActiveCount, rightActiveCount)`, clamped to `3-20`.
- Solo players never earn war contribution, never get clan tasks, and never get war chest.
- Solo players do earn **Season CP from verified daily only**, capped at **1799**.
- The cap label on the solo row is the join prompt. No forced modal.
- `Today` and `Season` are separate tabs:
  - `Today`: verified daily challenge leaderboard, resets every 24 hours
  - `Season`: public contribution ladder, resets every 8 wars
- All paid purchases are personal, permanent, portable, and never clan-owned.
- No purchase can affect quiz scoring, war score, ladder score, matchmaking, or reward eligibility.

## Milestone Sequence With Hard Verification Gates

### Milestone 1: Remove Ranked and Reframe Navigation
**Build**
- Delete ranked client logic, ranked state, rank UI, rank badges, rank progression, ranked tasks, ranked screen, and ranked routes.
- Remove ranked session type and ranked-specific backend handling.
- Replace app-shell entry points:
  - bottom nav becomes `Home`, `Clans`, `Profile`
  - home CTA becomes `Clan War`
- Replace profile’s rank surfaces with neutral progression placeholders to be filled later by clan/season identity.
- Keep daily challenge, genre, lightning, grand master, wallet, and tasks working.

**Acceptance gate before Milestone 2**
- App builds and runs with zero ranked references.
- No ranked route resolves.
- No ranked UI renders anywhere.
- No ranked session type exists in backend processing.
- Navigation to `Clans` works.
- Home CTA shows `Clan War`.

---

### Milestone 2: Public Season Ladder and Clan Discovery
**Build**
- Add `Clans` area for solo players with two tabs only:
  - `Season`
  - `Today`
- `Season` is the default landing tab for solo players.
- Show public ladder rows with:
  - display name
  - clan tag if present
  - equipped cosmetics
  - total CP
  - current user row pinned/visible
- Solo player row behavior:
  - visible row
  - lock icon
  - `Join a clan to climb`
  - capped status clearly shown
  - tap on label opens clan discovery
- Build clan discovery and search:
  - `Join Existing Clan` is the primary action
  - `Create Clan` is secondary
  - results sort battle-phase clans first, then suitable active band, then open slots
- Clan create flow requires:
  - name
  - tag
  - description
  - emblem

**Acceptance gate before Milestone 3**
- Solo player opens `Clans`.
- Solo player lands on `Season`.
- Solo player sees their own row capped at `1799`.
- Tapping the join label opens clan discovery.
- Discovery shows clans sorted by battle phase first.

---

### Milestone 3: Join-to-War Vertical Slice
**Build**
- On successful clan create or join, route directly to `War`, never to a generic clan home.
- Implement war board above-the-fold payload:
  - clan name
  - opponent clan name
  - current phase
  - countdown
  - current clan score
  - current opponent score
  - contributor cap
  - top clanmate rows sorted descending
  - player’s own row even at `0`
  - player counted contribution state `0/3`
  - one primary CTA: `Play First Contributing Quiz`
- Primary CTA launches a direct autostart genre quiz contribution run.
- On eligible completion, route back to `War` and animate:
  - player row update
  - `x/3` state update
  - clan total update
  - rank position change if applicable

**Acceptance gate before Milestone 4**
- Joining a clan routes immediately to `War`.
- War board shows opponent, phase, countdown, clanmate rows, and player `0/3` row.
- Primary CTA launches genre quiz.
- Completing that quiz returns to war board with animated update.

---

### Milestone 4: Server-Authoritative CP, War Scoring, and Active Member State
**Build**
- Add `Contribution Points (CP)` as the central progression metric.
- Extend eligible gameplay session summaries with `bestCorrectStreak`.
- Fixed `sessionCP` formula:
  - `accuracyPoints = round((correctAnswers / questionsAnswered) * 100)`
  - `speedPoints = round(max(0, 1 - durationSeconds / modeDurationCap) * 40)`
  - `streakPoints = min(bestCorrectStreak, 10) * 3`
  - `modeBonus = genre 0, lightning 15, grand master 25, verified daily 35`
  - `sessionCP = accuracyPoints + speedPoints + streakPoints + modeBonus`
- `modeDurationCap`:
  - genre `300`
  - lightning `240`
  - grand master `300`
  - verified daily `180`
- Solo `Season CP`:
  - only from verified daily
  - capped at `1799`
- Clan `Season CP`:
  - verified daily CP
  - war CP from counted eligible runs
  - task CP from clan tasks
- War contribution rules:
  - each player gets `3` counted contributions
  - only top `3` personal runs count
  - clan total uses the top contributors up to the frozen contributor cap
- Active member handling:
  - every eligible session updates member `activeUntil = now + 7 days`
  - clan document stores computed `activeMemberCount7d`
  - clan document stores `activeBand`
  - hourly expiry sweep decrements expired active counts
- Pairing reads only clan-level computed fields, not member scans.

**Acceptance gate before Milestone 5**
- Session summary from each eligible mode produces correct CP using the defined formula.
- Solo verified daily caps at `1799`.
- Clan member war CP updates war score.
- Active member count updates and expires correctly.

---

### Milestone 5: Clan Tasks, War Chest, Streaks, and Milestone Badges
**Build**
- Add 3 fixed daily clan tasks:
  - `Opening Move`: complete 1 eligible contributing quiz -> `50 coins + 10 CP`
  - `Triple Pressure`: complete 3 eligible contributing quizzes -> `75 coins + 10 CP`
  - `High Stakes`: complete 1 verified daily or grand master -> `100 coins + 10 CP`
- Add settlement chest:
  - personal, never shared
  - requires at least 1 counted contribution in battle window
  - loss chest: `75 coins + 40 warFragments`
  - win chest: `125 coins + 60 warFragments`
  - MVP bonus: `+20 warFragments`
- Add personal clan meta:
  - contribution streak
  - lifetime contribution total
  - war participation count
  - war win count
  - MVP count
- Add personal milestone badges:
  - `Warborn` after 5 chest claims
  - `Siegebreaker` after 15 chest claims
  - `Clan Victor` after 10 war wins
  - `MVP Ace` after 3 MVP awards
- Add fragment redemption catalog for clan-earned cosmetics.

**Acceptance gate before Milestone 6**
- Three daily clan tasks progress and claim correctly.
- War chest pays correct win/loss/MVP rewards.
- Contribution streak increments and resets correctly.
- Milestone badges unlock and persist after clan switch.

---

### Milestone 6: Crown Shop, Premium Cosmetics, and Visibility Surfaces
**Build**
- Add `Crowns` as the only paid currency.
- Keep `coins` for free progression and non-competitive ability usage.
- Split shop into:
  - `Style`
  - `Abilities`
  - `Earned`
- Crown packs:
  - `80 = ₹79`
  - `220 = ₹199`
  - `600 = ₹499`
  - `1300 = ₹999`
- Premium catalog:
  - `Nameplates` at `120`
  - `Badge Frames` at `150`
  - `War Titles` at `90`
  - `Streak Effects` at `180`
  - `MVP Result Cards` at `220`
  - `Starter Bundle` at `300`
  - `Founder Social Pack` at `420`
- Premium cosmetics visible on:
  - clan roster rows
  - war leaderboard rows
  - post-quiz contribution cards
  - post-war MVP cards
  - profile header
  - public daily board rows
  - season ladder rows
- Solo players can buy and equip before joining.
- Remove performance-affecting competitive abilities:
  - delete RP-related ability
  - remove any effect that changes war, Today, Season, or quiz scoring
- Keep only non-competitive abilities:
  - `XP Overdrive`
  - `Streak Shield`

**Acceptance gate before Milestone 7**
- Crown purchase is server-verified before balance updates.
- Every cosmetic type is purchasable, equippable, and visible on all required surfaces.
- Solo player can purchase before joining a clan.
- Leaving a clan does not remove owned cosmetics.

---

### Milestone 7: Season Rollover, Analytics, and Final Support
**Build**
- Add global season rollover after 8 wars.
- Grant permanent season badges:
  - rank `1`: `Season Champion`
  - ranks `2-10`: `Season Elite`
  - ranks `11-100`: `Season Veteran`
- Reset season ladder totals at rollover, keep badge ownership permanently.
- Add season history to profile:
  - last season finish
  - badges earned
  - total wars contributed
- Add analytics events:
  - `season_ladder_viewed`
  - `season_cap_row_tapped`
  - `clan_discovery_opened`
  - `clan_joined`
  - `post_join_war_board_viewed`
  - `first_contribution_started`
  - `first_contribution_completed`
  - `war_chest_claimed`
  - `premium_pack_purchased`
  - `premium_cosmetic_equipped`
- Add final guardrails:
  - leader transfer before leader leave
  - archive empty clan
  - avoid same-opponent rematch if possible

**Acceptance gate before release**
- Season rollover runs correctly after 8 wars.
- Seasonal badges grant to correct rank ranges.
- Season history appears on profile.
- All analytics events fire on correct triggers.

## Data Model, Collections, and Interfaces
### Player/Profile
- `clanId`
- `clanRole`
- `clanJoinedAt`
- `equippedNameplateId`
- `equippedFrameId`
- `equippedTitleId`
- `equippedStreakEffectId`
- `equippedResultCardId`
- `contributionStreak`
- `lifetimeContribution`
- `warParticipationCount`
- `warWinCount`
- `mvpCount`
- `lastSeasonFinish`
- `seasonBadges`

### Wallet and Ownership
- wallet adds server-authoritative `crowns`
- ownership adds:
  - `ownedPremiumCosmetics`
  - `ownedClanMilestoneCosmetics`
  - `warFragments`

### Core Collections
- `clans/{clanId}`
- `clans/{clanId}/members/{uid}`
- `clanWars/{warId}`
- `clanWars/{warId}/participants/{uid}`
- `contributionSeasons/{seasonId}`
- `contributionSeasons/{seasonId}/players/{uid}`
- `verifiedDailyEntries/{dateKey}/players/{uid}` remains for `Today`

### Clan Document Required Fields
- `name`
- `tag`
- `description`
- `emblemId`
- `leaderUid`
- `memberCount`
- `activeMemberCount7d`
- `activeBand`
- `openSlots`
- `currentWarId`
- `lastWarStrength`
- `battleJoinRecommended`

### Required Callables
- `createClan(name, tag, description, emblemId)`
- `searchClans(query, limit, cursor)`
- `joinClan(clanId)`
- `leaveClan()`
- `fetchClanWarState(clanId?)`
- `fetchContributionLadder(tab, cursor, pageSize)`
- `fetchClanTasks()`
- `claimClanTaskReward(taskId)`
- `claimWarChest(warId)`
- `purchaseCrowns(productId, purchaseToken, packageName)`
- `fetchPremiumCatalog()`
- `purchasePremiumCosmetic(itemId)`
- `equipPremiumCosmetic(slot, itemIdOrNull)`

## Matchmaking, Jobs, and Indexes
- `warCycleScheduler`: every 15 minutes, advances prep/battle/settlement/pairing/rollover
- `activeMemberExpirySweep`: hourly, expires stale `activeUntil` values and recomputes counts cheaply
- Match clans by:
  - active-member band first
  - recent war strength second
  - avoid immediate rematch where possible
- Required indexes:
  - clan discovery on `battleJoinRecommended`, `activeBand`, `openSlots`
  - Season ladder on `seasonId`, `totalCp desc`, `lastUpdatedAt desc`
  - Today board on current-day verified score ordering
  - member expiry on `activeUntil`

## Full Test Plan
### Milestone Tests
- Treat every milestone checklist above as a mandatory blocking gate.
- Do not begin the next milestone until the current one passes completely.

### End-to-End Product Tests
- Solo player can discover clans through the visible Season cap without forced onboarding.
- Solo player can play full quiz core and shop without clan membership.
- Joined player gets immediate felt benefit via war board and first contribution loop.
- Today board stays fair and unaffected by clan membership.
- Season ladder visibly distinguishes solo-capped and clan-active rows.
- Matchmaking remains fair across size bands and contributor-cap rules.
- War chest, tasks, and badges persist correctly.
- Crown purchases are verified server-side and idempotent.
- Premium cosmetics render correctly across all required surfaces.
- No purchase affects competitive outcomes.
- No ranked codepath, UI, task, or backend branch remains anywhere.

## Assumptions and Defaults
- Fresh app, zero users, no migration requirements.
- `Join Existing Clan` is the primary onboarding path; `Create Clan` is secondary.
- The Season cap row is the main solo-to-clan conversion surface.
- The War board is the main joined-player retention surface.
- Core design principle remains **aspirational gap, not punitive wall**:
  - no solo punishment
  - clear visible clan ceiling advantage
- Business targets remain:
  - `20%` of MAU joins a clan within 90 days
  - `3%` of clan members make at least one monthly purchase
  - ARPPU `₹199–₹249`
  - purchase mix `70% / 20% / 10%`
