<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-BOM_2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-Backend-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Status-Shelved-FF6B6B?style=for-the-badge" />
</p>

# 🏆 Trivia Royale

> A competitive mobile trivia quiz game for Android with **Clan Wars**, a **Season Ladder**, daily challenges, and a cosmetic shop — built for the Indian market.

---

## ✨ Features

### 🎮 Quiz Modes
- **Genre Quiz** — Pick a topic and test your knowledge across multiple categories
- **Lightning Round** — Fast-paced quiz with adaptive difficulty scaling
- **Grand Master Challenge** — One premium daily quiz, once per day
- **Verified Daily Challenge** — Community-wide daily quiz with a public leaderboard

### ⚔️ Clan Wars
- Create or join clans, compete in 24h-prep / 48h-battle / 12h-settlement war cycles
- Contribute quiz scores to your clan's war effort
- Active member tracking with 7-day rolling windows
- War board with real-time contribution tracking

### 🏅 Season Ladder
- Public contribution point (CP) leaderboard
- Solo-capped progression (max 1,799 CP without a clan)
- Season rollover with badge rewards

### 🛒 Shops & Economy
- **Coin Shop** — Titles, frames, badge skins purchasable with earned coins
- **Ability Shop** — XP Overdrive, Streak Shield, and more
- **Crown Shop** *(planned)* — Premium cosmetics via in-app purchases
- Server-authoritative coin balance with anti-cheat protection

### 📋 Tasks & Achievements
- Daily solo tasks with coin rewards
- Clan-specific daily tasks
- Achievement milestones and badges

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│          Android Client (Kotlin)            │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ 18 Compose│ │ GameState│ │   Firebase  │ │
│  │  Screens  │ │ (Room DB)│ │  Cloud Repo │ │
│  └──────────┘ └──────────┘ └──────┬──────┘ │
└───────────────────────────────────┼─────────┘
                                    │
                    ┌───────────────▼──────────────┐
                    │      Firebase Backend        │
                    │  ┌────────────────────────┐  │
                    │  │  Cloud Functions (v2)  │  │
                    │  │  Node 22 • asia-south1 │  │
                    │  └───────────┬────────────┘  │
                    │  ┌───────────▼────────────┐  │
                    │  │  Cloud Firestore       │  │
                    │  │  Auth • App Check      │  │
                    │  │  Crashlytics           │  │
                    │  └────────────────────────┘  │
                    └──────────────────────────────┘
```

### Tech Stack

| Layer | Technology | Version |
|:------|:-----------|:--------|
| Language | Kotlin | 1.9.23 |
| UI Framework | Jetpack Compose | BOM 2024.12.01 |
| Navigation | Navigation Compose | 2.8.5 |
| Local Database | Room | 2.6.1 |
| Backend | Firebase Cloud Functions v2 | 7.2.2 |
| Database | Cloud Firestore | firebase-admin 13.7.0 |
| Auth | Firebase Auth (Google + Anonymous) | — |
| Ads | AdMob (Interstitial + Rewarded) | 23.6.0 |
| Billing | Google Play Billing | 7.1.1 |
| Min SDK | Android 8.0 | API 26 |
| Target SDK | Android 15 | API 35 |

---

## 📂 Project Structure

```
Trivia Royale/
├── android/                          # Android app
│   └── app/src/main/java/com/triviaroyale/
│       ├── MainActivity.kt           # Entry point
│       ├── TriviaRoyaleApp.kt        # Main composable, nav, auth
│       ├── ui/
│       │   ├── screens/              # 18 screen composables
│       │   ├── components/           # Reusable UI components
│       │   └── theme/                # Material3 dark theme
│       ├── data/                     # GameState, Room DB, managers
│       ├── firebase/                 # Cloud repository, auth, bootstrap
│       ├── ads/                      # AdMob integration
│       ├── billing/                  # Google Play Billing
│       └── security/                 # Emulator & root detection
│
├── functions/                        # Firebase Cloud Functions
│   ├── index.js                      # 25+ callable functions (~2500 lines)
│   └── lib/                          # Shared modules
│       ├── clans.js                  # Clan constants, CP, war IDs
│       ├── gameplay.js               # Session validation, tasks
│       ├── coinSecurity.js           # Anti-cheat evaluation
│       ├── rateLimit.js              # Per-user rate limiting
│       ├── quizSeed.js              # Quiz generation via Gemini API
│       └── warSettlement.js          # War result computation
│
├── firestore.rules                   # Security rules
├── firestore.indexes.json            # Composite indexes
├── firebase.json                     # Firebase project config
└── PLAN.md                           # Full 7-milestone roadmap
```

---

## 🚀 Cloud Functions

| Function | Purpose |
|:---------|:--------|
| `createClan` | Idempotent clan creation with transactions |
| `searchClans` / `joinClan` / `leaveClan` | Clan discovery & membership |
| `recordGameplaySession` | Records quiz results, stats, CP, war contributions |
| `syncCoinBalance` | Server-authoritative coin validation + anti-cheat |
| `fetchContributionLadder` | Season CP leaderboard with pagination |
| `fetchGenreQuestions` / `fetchQuizCatalog` | Quiz content delivery |
| `submitDailyChallenge` / `fetchDailyBoard` | Verified daily challenge system |
| `purchaseCosmeticReward` / `purchaseAbility` | Shop transactions |
| `fetchAvailableTasks` / `claimTaskReward` | Daily task system |
| `warCycleScheduler` | ⏰ Scheduled — advances war phases, pairs clans, settles |
| `activeMemberExpirySweep` | ⏰ Scheduled — expires stale active members |
| `deleteUserAccount` | GDPR-compliant account deletion |

---

## 📊 Development Status

| # | Milestone | Status |
|:--|:----------|:-------|
| 1 | Remove Ranked, Reframe Navigation | ✅ Complete |
| 2 | Public Season Ladder & Clan Discovery | ✅ Complete |
| 3 | Join-to-War Vertical Slice | 🔶 Partial |
| 4 | Server-Authoritative CP & War Scoring | 🔶 Partial |
| 5 | Clan Tasks, War Chest, Streaks, Badges | 🔶 Partial |
| 6 | Crown Shop & Premium Cosmetics | ❌ Not started |
| 7 | Season Rollover, Analytics, Final Polish | ❌ Not started |

### What's Working
- ✅ Google Sign-In + Anonymous auth
- ✅ All quiz modes (Genre, Lightning, Grand Master, Daily Challenge)
- ✅ Coin economy with server-side anti-cheat
- ✅ Cosmetic & ability shops
- ✅ Daily tasks with rewards
- ✅ Clan creation, discovery, join/leave
- ✅ Season ladder with solo cap
- ✅ Cloud save/restore
- ✅ AdMob interstitials + rewarded ads
- ✅ Custom Canvas rank badges (7 tiers)

---

## 🛠️ Setup

### Prerequisites
- Android Studio (Arctic Fox or later)
- Node.js 22+
- Firebase CLI (`npm install -g firebase-tools`)

### Android App
```bash
# 1. Clone the repo
git clone https://github.com/Hastagtamilnadu/Trivia-Royale.git

# 2. Download google-services.json from Firebase Console
#    Place at: android/app/google-services.json

# 3. Create android/local.properties with Firebase config:
#    firebaseApiKey=<from Firebase Console>
#    firebaseAppId=<from Firebase Console>
#    firebaseProjectId=<from Firebase Console>
#    firebaseSenderId=<from Firebase Console>
#    firebaseStorageBucket=<from Firebase Console>
#    firebaseWebClientId=<from Firebase Console>
#    backupSecret=<your secret>

# 4. Open in Android Studio, sync Gradle, and build
```

### Cloud Functions
```bash
cd functions
npm install
firebase deploy --only functions
```

### Firestore Rules & Indexes
```bash
firebase deploy --only firestore
```

---

## 🔧 Infrastructure

| Item | Value |
|:-----|:------|
| Region | `asia-south1` (Mumbai) |
| Package | `com.triviaroyale` |
| Auth Providers | Google Sign-In, Anonymous |
| App Check | Play Integrity (dev mode) |

---

## 📄 License

This project is private and not licensed for public use.

---

<p align="center">
  <sub>Built with ❤️</sub>
</p>
