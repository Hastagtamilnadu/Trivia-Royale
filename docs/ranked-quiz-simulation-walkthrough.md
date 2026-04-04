# Ranked Quiz Simulation Walkthrough

This document explains, in plain language, how the ranked mode in this app creates the feeling that the player is competing against real online players, while the implementation is actually a fully local simulation.

The goal here is not to guess. This walkthrough maps the player-facing experience to the source code that produces it.

## Short answer

Ranked mode is not backed by real-time multiplayer.

What the player sees as:

- matchmaking
- other players joining the lobby
- live opponents answering questions
- players disconnecting
- waiting for other players to finish
- rank results and leaderboard movement

is generated locally on the device using:

- hardcoded name pools
- random delays
- random bot accuracy profiles
- local state mutation
- locally computed RP, placement, percentile, and standings

There is no evidence in this codebase of a real backend, socket connection, HTTP matchmaking service, or online sync for ranked matches.

## Main files involved

- `android/app/src/main/java/com/triviaroyale/ui/screens/HomeScreen.kt`
- `android/app/src/main/java/com/triviaroyale/TriviaRoyaleApp.kt`
- `android/app/src/main/java/com/triviaroyale/ui/screens/RankedQuizScreen.kt`
- `android/app/src/main/java/com/triviaroyale/data/RankedMatchStateManager.kt`
- `android/app/src/main/java/com/triviaroyale/data/GameState.kt`
- `android/app/src/main/java/com/triviaroyale/ui/screens/LeaderboardScreen.kt`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/build.gradle.kts`
- `progress.md`

## What the player believes vs what the code actually does

### 1. Player believes: "I entered an online ranked queue"

What actually happens:

- The player taps the ranked card in `HomeScreen.kt:166`.
- Navigation moves into the ranked screen in `TriviaRoyaleApp.kt:206`.
- The app uses a locally created `GameState` object in `TriviaRoyaleApp.kt:46`.
- No server request is sent when entering ranked mode.

Meaning:

The app moves from one local screen to another. There is no matchmaking request at the entry point.

### 2. Player believes: "The game is searching for real players at my rank"

What actually happens:

- The matchmaking phase starts inside `RankedQuizScreen.kt:269`.
- The subtitle says `"Matching you with opponents at your rank"` in `RankedQuizScreen.kt:272`.
- Opponent names are not fetched from a server. They come from `GameState.getMatchNames()` in `GameState.kt:271`.
- That method pulls names from a hardcoded local `namePool` defined in `GameState.kt:264`.

Meaning:

The "players" in the queue are preselected from a local list of names like `Arjun`, `Priya`, `Ravi`, `Sneha`, and so on. The screen text implies online ranking, but the source of the opponents is just a shuffled in-app list.

### 3. Player believes: "Other players are joining the lobby in real time"

What actually happens:

- The lobby UI is built by `buildLobbySlots()` in `RankedQuizScreen.kt:67`.
- The fake join timing is built by `buildLobbyRevealEvents()` in `RankedQuizScreen.kt:84`.
- That function assigns random delays based on fake player "types" such as `fast`, `slow`, `pro`, and `noob` from `RankedQuizScreen.kt:65`.
- The `MATCHMAKING` effect replays those local reveal events over time in `RankedQuizScreen.kt:285` through `RankedQuizScreen.kt:307`.
- After the reveal sequence finishes, the UI always flips to `"Match Found!"` and `"All players connected"` in `RankedQuizScreen.kt:303` and `RankedQuizScreen.kt:304`.

Meaning:

The lobby is a scripted animation system. The app is not waiting for actual clients to connect. It is only revealing names and "ready" states according to random timers.

### 4. Player believes: "The team composition is a real match lobby"

What actually happens:

- `buildMatchTeamSeeds()` in `RankedQuizScreen.kt:139` converts the fake lobby slots into match teams.
- For solo, each fake name becomes its own opponent team.
- For duo and squad, the function groups names into ally and enemy teams using the lobby slot data that was already generated locally.

Meaning:

The "teams" come from the same fabricated lobby data. The screen never receives team information from outside the device.

### 5. Player believes: "The countdown starts once everyone is truly ready"

What actually happens:

- After the local matchmaking animation completes, the screen waits a fixed short delay and then switches to countdown in `RankedQuizScreen.kt:306` and `RankedQuizScreen.kt:307`.
- The app then loads local questions using `QuizRepository.getQuestions(count = 10)` in `RankedQuizScreen.kt:322`.
- It creates a local `RankedMatchStateManager` in `RankedQuizScreen.kt:324`.

Meaning:

The countdown starts because the local script says so, not because any real remote players finished loading or confirmed readiness.

### 6. Player believes: "The other players are answering live during the quiz"

What actually happens:

- For each question, the screen calls `manager.simulateQuestion(...)` in `RankedQuizScreen.kt:382`.
- The logic for those opponents lives entirely in `RankedMatchStateManager.kt`.
- `simulateQuestion()` begins in `RankedMatchStateManager.kt:116`.
- It filters out the local player and operates on simulated bot participants in `RankedMatchStateManager.kt:117`.

Meaning:

There is no incoming stream of answers from other devices. The screen explicitly asks a local bot manager to generate the opponent events for the current question.

## How the fake opponents are built

### Bot identities

`RankedMatchStateManager` creates participant records in its initializer at `RankedMatchStateManager.kt:89`.

For every non-player participant:

- a local `ParticipantState` is created
- a bot profile is assigned through `createProfile()` in `RankedMatchStateManager.kt:297`
- the bot gets an archetype such as:
  - `TRYHARD`
  - `PONDERER`
  - `GUESSER`
  - `STEADY`

Each archetype gets a random base accuracy range in `RankedMatchStateManager.kt:305`.

Meaning:

Opponent skill is not measured from real users. It is procedurally generated at match start.

### Bot momentum and personality

Bots have fake emotional states:

- `STEADY`
- `CONFIDENT`
- `PANIC`
- `TILTED`

These are defined at `RankedMatchStateManager.kt:7` and derived from streak and miss patterns in `ParticipantState.currentMomentum()` at `RankedMatchStateManager.kt:64`.

Meaning:

The game adds human-like behavior to make the bots feel believable.

## How the app fakes live answer events

Inside `simulateQuestion()` the manager creates event objects for the bots.

### AFK behavior

- There is a 12% chance one active bot becomes AFK on a question in `RankedMatchStateManager.kt:122`.
- That bot gets an `AFK` event scheduled near the end of the timer in `RankedMatchStateManager.kt:131`.

Meaning:

The app deliberately injects "someone missed the timer" moments to make the match feel real.

### Fake disconnects

- The manager can schedule a disconnect through `maybeScheduleDisconnect()` in `RankedMatchStateManager.kt:280`.
- That function randomly decides whether some non-player bot will disconnect in a later question.
- During simulation, disconnect events are emitted in `RankedMatchStateManager.kt:140`.

Meaning:

Even disconnects are staged locally.

### Response times

- Bot response delays are generated by `responseDelayMs()` in `RankedMatchStateManager.kt:359`.
- Different archetypes answer at different timing ranges:
  - tryhards tend to answer early or mid
  - ponderers answer late
  - guessers may answer almost instantly or very late
  - steady bots vary across multiple timing bands

Meaning:

The feeling of "some players are quick, some hesitate" is generated by random timing buckets, not network latency from real people.

### Accuracy and correctness

- Whether a bot answers correctly is decided by `accuracyFor()` in `RankedMatchStateManager.kt:334`.
- That value depends on:
  - random base accuracy from the bot profile
  - current question difficulty
  - bot momentum state
  - whether the bot panic-swipes

Meaning:

Correct and wrong answers are probability rolls, not real human choices.

### Visible ticker events

- The manager creates many raw events, then filters which ones the player will actually see using `pickVisibleEvents()` in `RankedMatchStateManager.kt:244`.
- The visible event budget changes by mode in `RankedMatchStateManager.kt:83`.
- It purposely selects a spread of early, mid, and late events in `RankedMatchStateManager.kt:253` through `RankedMatchStateManager.kt:275`.

Meaning:

The game is not only simulating bot activity, it is curating which bot actions to show so the feed feels believable and paced like a live match.

## How the app fakes the live ticker

Once `simulateQuestion()` returns local bot events:

- the screen replays them over time in `RankedQuizScreen.kt:385`
- each event is delayed by its scheduled `atMs`
- `manager.applyEvent(event)` updates local bot state in `RankedQuizScreen.kt:395`
- if the event is allowed into the ticker, the UI appends a feed item in `RankedQuizScreen.kt:397`

The text shown to the player comes from `buildTickerMessage()` in `RankedMatchStateManager.kt:407`.

Examples of fabricated messages:

- bot got it right
- bot banked points
- bot is feeling confident
- bot panic-swiped and missed
- bot disconnected

Meaning:

The ticker is not reflecting networked game state. It is showing text assembled from local bot events.

## How the app blends the player into the fake feed

The player's own answer feedback is also injected into the same ticker system:

- `selectAnswer()` handles the local player's answer in `RankedQuizScreen.kt:407`
- the player message is built by `buildPlayerTickerMessage()` in `RankedQuizScreen.kt:180`
- that message is not always appended immediately; it uses a slight random delay in `RankedQuizScreen.kt:453`

Meaning:

The app makes the player's actions and bot actions share the same ticker format and timing style, which helps sell the illusion that all entries are coming from one live match feed.

## How scores and standings are faked

### Bot scores

Bot scores are updated locally in `applyEvent()`:

- correct answers add points in `RankedMatchStateManager.kt:193`
- wrong answers reset streaks in `RankedMatchStateManager.kt:201`
- AFK increments missed answers in `RankedMatchStateManager.kt:187`
- disconnect marks the bot inactive in `RankedMatchStateManager.kt:182`

### Team standings

At the end of the match:

- team scores are assembled by `buildTeamScores()` in `RankedMatchStateManager.kt:225`
- the local player score is inserted directly for the player entry in `RankedMatchStateManager.kt:233`
- teams are sorted by score in `RankedMatchStateManager.kt:241`

Meaning:

The final standings are a local ranking of the player's score against simulated bot scores.

## How the app fakes "waiting for other players to finish"

After the last question:

- the screen enters `WAITING` state
- it simply delays for a random 1.2 to 2.2 seconds in `RankedQuizScreen.kt:345` and `RankedQuizScreen.kt:346`
- then it builds the result locally in `RankedQuizScreen.kt:347`

The waiting screen text says:

- `"Calculating results..."`
- `"Waiting for other players to finish..."`

But the code does not wait for any external source. It only waits on a local timer before computing the result.

Meaning:

This is one of the clearest examples of the illusion. The app tells the player it is waiting for others, but it is only sleeping locally and then continuing.

## How RP and percentile are computed locally

The result pipeline is:

- `buildMatchResult()` in `GameState.kt:279`
- `recordRankedResult()` in `GameState.kt:308`

These functions calculate:

- placement
- accuracy
- player team score
- whether the match counts as a win
- RP gain or loss
- rank updates
- percentile

Important detail:

- `getPercentile()` in `GameState.kt:396` includes `Math.random()` in the final percentile calculation

Meaning:

Even the performance comparison against "players at your rank" is partly randomized. It is not derived from any real population of users.

## How persistence works

`GameState` is a local state manager:

- `SharedPreferences` is used in `GameState.kt:19`
- `Gson` serializes state in `GameState.kt:21`
- state is held in `MutableStateFlow` in `GameState.kt:115`
- `loadState()` and `saveState()` are local persistence helpers in `GameState.kt:120` and `GameState.kt:133`

Meaning:

The ranked system stores progress locally on the device. It is not synchronizing ranked data from an online service in the code shown here.

## How the leaderboard is also faked

`LeaderboardScreen.kt` uses hardcoded bot-like names:

- defined in `LeaderboardScreen.kt:29`

Then it generates random values for them:

- random RP and random accuracy in `LeaderboardScreen.kt:31` through `LeaderboardScreen.kt:35`

Meaning:

The leaderboard is not loaded from an online ranking table. It is a local mixed list of:

- random fake players
- the current user

sorted by RP for display.

## Evidence that there is no real multiplayer backend

### 1. No internet permission in the manifest

`AndroidManifest.xml` does not declare `android.permission.INTERNET`.

Without internet permission, an Android app cannot perform normal network communication.

### 2. No networking libraries in Gradle

`android/app/build.gradle.kts` includes Compose, Navigation, Lifecycle, and Gson dependencies, but no obvious networking stack such as:

- Retrofit
- OkHttp
- Ktor
- Firebase client SDKs
- WebSocket libraries
- Pusher
- Supabase

### 3. No networking code found in the source

No evidence was found for:

- socket connections
- WebSocket handling
- HTTP client requests
- real-time listeners
- backend API integration

The ranked flow instead points directly to local simulation classes and randomization.

### 4. Internal project note explicitly mentions simulation

`progress.md:32` contains the phrase:

- `competitive matchmaking simulation`

Meaning:

Even the project notes describe ranked matchmaking as simulation.

## Full ranked flow from tap to results

### Step 1. Enter ranked

- Player taps ranked card in `HomeScreen.kt:166`
- Navigation opens the ranked screen in `TriviaRoyaleApp.kt:206`

### Step 2. Matchmaking UI starts

- Screen phase becomes `MATCHMAKING` in `RankedQuizScreen.kt`
- Title and subtitle claim the game is finding players

### Step 3. Fake names are selected

- `GameState.getMatchNames()` returns names from the local hardcoded pool

### Step 4. Lobby is staged

- `buildLobbySlots()` arranges the names into solo, duo, or squad slots
- `buildLobbyRevealEvents()` creates timed reveal events
- the screen replays those events to simulate players joining

### Step 5. Match is declared found

- after the scripted reveals, the UI says all players connected

### Step 6. Questions begin

- the app loads local questions
- the app creates a local `RankedMatchStateManager`

### Step 7. Opponents are simulated each question

- bot events are generated with random timing and correctness
- ticker entries are chosen and displayed
- bot scores update locally

### Step 8. Player finishes

- once the final question ends, the app shows a waiting screen

### Step 9. Fake wait

- the app delays 1.2 to 2.2 seconds

### Step 10. Results are computed locally

- team standings are built from local simulated scores
- RP is updated locally
- percentile is calculated locally with some randomness

### Step 11. Player sees a polished ranked result

- standings
- RP gain or loss
- rank badge
- percentile text

All of it comes from local computation.

## Why the illusion works well

The fake ranked mode is convincing because the code combines several techniques:

- believable names from a curated name pool
- staggered lobby reveals instead of instant filling
- personality-based response timing
- occasional AFKs and disconnects
- limited ticker visibility so the feed is not too noisy
- slight delays before player and bot ticker messages
- a short post-match waiting screen
- leaderboard and percentile displays that look social and competitive

This is not a lazy fake. It is a deliberate local simulation designed to feel like real multiplayer.

## Bottom line

The ranked mode in this codebase is a single-device simulation dressed as online competitive play.

The app does not appear to:

- match the user with real remote players
- receive live answers from other clients
- sync ranked matches through a backend
- load a real leaderboard from a service

Instead, it:

- invents opponents from a local name list
- animates them into the lobby
- simulates their behavior with random probabilities
- computes all standings and rank outcomes locally

If someone wants to convert this into real multiplayer later, the current code is best understood as a frontend illusion layer plus a bot simulation engine, not as a true online ranked system.
