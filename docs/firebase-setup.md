# Firebase Setup

The Android app now initializes Firebase manually from `android/local.properties`, so you do not need `google-services.json` to compile the project.

Add these keys to `android/local.properties`:

```properties
firebaseApiKey=YOUR_ANDROID_API_KEY
firebaseAppId=YOUR_FIREBASE_APP_ID
firebaseProjectId=YOUR_FIREBASE_PROJECT_ID
firebaseSenderId=YOUR_SENDER_ID
firebaseStorageBucket=YOUR_STORAGE_BUCKET
firebaseWebClientId=YOUR_WEB_CLIENT_ID
admobAppId=YOUR_ADMOB_APP_ID
admobInterstitialAdUnitId=YOUR_INTERSTITIAL_AD_UNIT_ID
admobRewardedAdUnitId=YOUR_REWARDED_AD_UNIT_ID
```

The Firebase MCP entry is configured in `.codex/config.toml` and points to:

```toml
[mcp_servers.firebase]
command = "firebase"
args = ["mcp", "--dir", "/path/to/your/project"]
cwd = "/path/to/your/project"
enabled = true
```

Before MCP-backed Firebase actions can work against a real project, log in with the Firebase CLI:

```powershell
firebase login
firebase use --add
```

Firestore config files added for the project:

- `firebase.json`
- `firestore.rules`
- `firestore.indexes.json`

Current client contract:

- Clients may read and update only their own profile doc at `users/{uid}`.
- Clients may read their own validated wallet at `wallets/{uid}`.
- Clients may read their own pending redeem request docs in `redeemRequests`.
- Redeem request creation now happens only through a callable function so wallet deduction is server-authoritative.
- Clients may not write leaderboards, wallet balances, game backups, or device registrations directly.
- Verified daily leaderboard reads now happen through a callable function backed by `verifiedDailyEntries/{dateKey}/players/{uid}`.
- Optional remote quiz updates are metadata-first through `public/quizCatalog/categories/{bankId}` and category content docs under `public/quizContent/banks/{bankId}`.
- The home-screen Grand Master quiz can be remotely overridden by publishing `public/quizContent/banks/grand_master_quiz` with `genre`, `category`, `updatedAt`, and a `questions` array. If the doc is missing or invalid, the app falls back to the local General Knowledge quiz.

App Check:

- The Android app installs Firebase App Check during bootstrap.
- Debug builds use the debug provider.
- Non-debug builds use the Play Integrity provider.
- Register both SHA-1 and SHA-256 fingerprints for every Android signing key that can reach Firebase.
- For Play-distributed releases, also register the Play App Signing SHA-1 and SHA-256 fingerprints from Play Console before enabling App Check for production traffic.

Operational notes:

- Wallet coins are the validated version of task-earned game coins.
- Gameplay no longer grants coins directly. Coins are earned only from claimable daily tasks.
- Ranked mode remains local simulation. The public leaderboard UI is now honest about that instead of accepting client-written scores.
- Authenticated profiles now mirror an encrypted local save plus a server-side backup written through callable functions.
- Manual redeem review happens in Firebase console by editing `redeemRequests/{requestId}` and, if needed, `wallets/{uid}`.
- `public/quizCatalog/categories/{bankId}` docs should include `bankId`, `genre`, `category`, `version`, and `updatedAt`.
- `dailyChallengeSets/current` should include `dateKey`, `title`, and `questions`.
- Each daily challenge question should include `id`, `question`, `options`, and `answer`. The server stores the answer key, but clients should only receive `id`, `question`, and `options`.
- Verified daily submissions are stored in `verifiedDailyEntries/{dateKey}/players/{uid}` and queried server-side for leaderboard reads.
- Recommended production Firestore hardening:

```powershell
firebase firestore:databases:update "(default)" --delete-protection ENABLED --point-in-time-recovery ENABLED
firebase firestore:backups:schedules:create -d "(default)" --recurrence DAILY --retention 14d
```

- After changing `firestore.rules`, deploy them before testing the wallet flow:

```powershell
firebase deploy --only firestore:rules
```
