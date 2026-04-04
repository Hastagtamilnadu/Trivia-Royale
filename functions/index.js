const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const {
  applyGameplaySession,
  buildClanTaskStatus,
  buildTaskStatus,
  clanTaskDefinitionById,
  emptyPlayerStats,
  gameplaySessionFromData,
  normalizePlayerStats,
  taskDefinitionById,
  todayDateKey,
} = require("./lib/gameplay");

// Returns yesterday's date string (YYYY-MM-DD) in India Standard Time (UTC+05:30).
// This is used for the leaderboard so it always shows the previous day's final results,
// rotating automatically at midnight IST without any manual Firestore update.
function yesterdayIndiaDateKey() {
  const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;
  const nowIst = new Date(Date.now() + IST_OFFSET_MS);
  nowIst.setUTCDate(nowIst.getUTCDate() - 1); // go back one day
  const yyyy = nowIst.getUTCFullYear();
  const mm = String(nowIst.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(nowIst.getUTCDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}
const {
  evaluateCoinSync,
  normalizeCoinSnapshot,
} = require("./lib/coinSecurity");
const { enforceRateLimit } = require("./lib/rateLimit");
const {
  fetchQuizQuestions,
  fetchQuizMetadata,
  seedQuizQuestions,
} = require("./lib/quizSeed");
const {
  ACTIVE_WINDOW_MILLIS,
  JOIN_COOLDOWN_MILLIS,
  MAX_CLAN_SIZE,
  MAX_COUNTED_CONTRIBUTORS,
  MIN_ACTIVE_CLAN_SIZE,
  SEASON_WARS,
  SOLO_SEASON_CP_CAP,
  activeBandForCount,
  buildSystemState,
  clampSoloSeasonCp,
  computeSessionCp,
  insertTopContribution,
  makeSeasonId,
  makeWarId,
  normalizeClanName,
  normalizeClanTag,
  sumScores,
} = require("./lib/clans");

initializeApp();

const REGION = "asia-south1";
// Set to true for production to enforce App Check attestation on all functions.
// Set to false during development so debug APKs can call functions without a
// registered debug token in Firebase Console.
const ENFORCE_APP_CHECK = false;
const MAX_ACCOUNTS_PER_DEVICE = 2;
const MAX_DAILY_TOTAL_COINS = 1500;
const MAX_DAILY_TASK_COINS = 1000;
const MAX_DAILY_QUIZ_COINS = 0;
const MAX_DAILY_TASK_CLAIMS = 10;
const MAX_COINS_PER_CORRECT = 0;
const VERIFIED_LEADERBOARD_LIMIT = 100;
const CLAN_SYSTEM_DOC = "clanMeta/system";
const COSMETIC_CATALOG = [
  { id: "title_quiz_rookie", name: "Quiz Rookie", type: "TITLE", coinsRequired: 1000 },
  { id: "title_trivia_addict", name: "Trivia Addict", type: "TITLE", coinsRequired: 3000 },
  { id: "title_knowledge_seeker", name: "Knowledge Seeker", type: "TITLE", coinsRequired: 5000 },
  { id: "title_quiz_legend", name: "Quiz Legend", type: "TITLE", coinsRequired: 15000 },
  { id: "title_trivia_master", name: "Trivia Master", type: "TITLE", coinsRequired: 30000 },
  { id: "title_brain_royale", name: "Brain Royale", type: "TITLE", coinsRequired: 100000 },
  { id: "frame_neon_purple", name: "Neon Purple", type: "FRAME", coinsRequired: 4000 },
  { id: "frame_cyan_wave", name: "Cyan Wave", type: "FRAME", coinsRequired: 4000 },
  { id: "frame_golden_crown", name: "Golden Crown", type: "FRAME", coinsRequired: 12000 },
  { id: "frame_diamond_edge", name: "Diamond Edge", type: "FRAME", coinsRequired: 25000 },
  { id: "frame_inferno", name: "Inferno", type: "FRAME", coinsRequired: 40000 },
  { id: "frame_aurora", name: "Aurora Borealis", type: "FRAME", coinsRequired: 100000 },
  { id: "badge_chrome", name: "Chrome Finish", type: "BADGE_SKIN", coinsRequired: 6000 },
  { id: "badge_holographic", name: "Holographic", type: "BADGE_SKIN", coinsRequired: 16000 },
  { id: "badge_obsidian", name: "Obsidian", type: "BADGE_SKIN", coinsRequired: 30000 },
  { id: "badge_celestial", name: "Celestial", type: "BADGE_SKIN", coinsRequired: 80000 },
];

const ABILITY_CATALOG = [
  { id: "ability_rp_boost", name: "RP Surge", type: "ABILITY", coinsRequired: 2000, durationDays: 3, effect: "rp_boost_50" },
  { id: "ability_time_extend", name: "Time Warp", type: "ABILITY", coinsRequired: 1500, durationDays: 3, effect: "time_extend_5s" },
  { id: "ability_streak_shield", name: "Streak Shield", type: "ABILITY", coinsRequired: 1800, durationDays: 3, effect: "streak_shield" },
  { id: "ability_hint_unlock", name: "Hint Master", type: "ABILITY", coinsRequired: 2500, durationDays: 3, effect: "hint_unlock" },
  { id: "ability_double_xp", name: "XP Overdrive", type: "ABILITY", coinsRequired: 2000, durationDays: 3, effect: "double_xp" },
];

const db = getFirestore();
const auth = getAuth();

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }
  return request.auth;
}

function normalizeString(value) {
  return String(value || "").trim();
}

function normalizeDeviceId(value) {
  return normalizeString(value).replace(/[^A-Za-z0-9:_-]/g, "").slice(0, 128);
}

function sanitizeDisplayName(value) {
  return normalizeString(value)
    .replace(/[^A-Za-z0-9 _]/g, "")
    .replace(/\s+/g, " ")
    .slice(0, 12);
}

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function sanitizeChallengeQuestions(questions) {
  return questions.map((question, index) => ({
    id: normalizeString(question.id) || `q${index + 1}`,
    question: normalizeString(question.question),
    options: Array.isArray(question.options)
      ? question.options.map((option) => normalizeString(option))
      : [],
  }));
}

function findCosmetic(cosmeticId) {
  return COSMETIC_CATALOG.find((c) => c.id === cosmeticId);
}

function findAbility(abilityId) {
  return ABILITY_CATALOG.find((a) => a.id === abilityId);
}

function sortedLeaderboardEntries(entries) {
  return entries
    .filter((entry) => normalizeString(entry.displayName))
    .sort((left, right) => {
      if (right.correctAnswers !== left.correctAnswers) {
        return right.correctAnswers - left.correctAnswers;
      }
      if (left.elapsedMillis !== right.elapsedMillis) {
        return left.elapsedMillis - right.elapsedMillis;
      }
      return normalizeString(left.displayName).localeCompare(normalizeString(right.displayName));
    })
    .slice(0, 10);
}

function accuracyFor(correctAnswers, totalQuestions) {
  if (!totalQuestions || totalQuestions <= 0) {
    return 0;
  }
  return Math.round((correctAnswers / totalQuestions) * 100);
}

function rpFor(correctAnswers, elapsedMillis) {
  const speedBonus = Math.max(0, 180 - Math.floor(elapsedMillis / 1000));
  return Math.max(0, correctAnswers * 150 + speedBonus);
}

function walletFromData(uid, data) {
  const source = data || {};
  return {
    uid,
    coins: Number(source.coins || 0),
    lastValidatedDayKey: normalizeString(source.lastValidatedDayKey) || todayDateKey(),
    validatedEarnedToday: Number(source.validatedEarnedToday || 0),
    suspiciousCount: Number(source.suspiciousCount || 0),
  };
}

function walletResponse(wallet, nowMillis = Date.now()) {
  return {
    uid: wallet.uid,
    coins: wallet.coins,
    updatedAt: nowMillis,
    lastValidatedDayKey: wallet.lastValidatedDayKey,
    validatedEarnedToday: wallet.validatedEarnedToday,
    suspiciousCount: wallet.suspiciousCount,
  };
}

function buildWalletWrite(wallet) {
  return {
    uid: wallet.uid,
    coins: wallet.coins,
    lastValidatedDayKey: wallet.lastValidatedDayKey,
    validatedEarnedToday: wallet.validatedEarnedToday,
    suspiciousCount: wallet.suspiciousCount,
    updatedAt: FieldValue.serverTimestamp(),
  };
}

function buildPlayerStatsWrite(stats) {
  return {
    uid: stats.uid,
    dayKey: stats.dayKey,
    quizzesPlayed: stats.quizzesPlayed,
    questionsAnswered: stats.questionsAnswered,
    correctAnswers: stats.correctAnswers,
    quizzesWon: stats.quizzesWon,
    lightningRounds: stats.lightningRounds,
    iplQuizzes: stats.iplQuizzes,
    iplCorrectAnswers: stats.iplCorrectAnswers,
    playSeconds: stats.playSeconds,
    grandMasterCompleted: stats.grandMasterCompleted,
    dailyChallengePlayed: stats.dailyChallengePlayed,
    eligibleClanSessions: stats.eligibleClanSessions,
    highStakeClanSessions: stats.highStakeClanSessions,
    claimedTaskIds: stats.claimedTaskIds,
    claimedClanTaskIds: stats.claimedClanTaskIds,
    processedSessionIds: stats.processedSessionIds,
    updatedAt: FieldValue.serverTimestamp(),
  };
}

function systemRef() {
  return db.doc(CLAN_SYSTEM_DOC);
}

function buildDefaultSystem(nowMillis = Date.now()) {
  const state = buildSystemState(null, nowMillis - (24 * 60 * 60 * 1000));
  return {
    ...state,
    phase: "battle",
  };
}

async function ensureClanSystemState() {
  const ref = systemRef();
  const snapshot = await ref.get();
  if (snapshot.exists) {
    return snapshot.data() || buildDefaultSystem();
  }
  console.log("ensureClanSystemState: initializing clan system for the first time.");
  const state = buildDefaultSystem();
  await ref.set(state, { merge: true });
  return state;
}

function seasonPlayerRef(seasonId, uid) {
  return db.collection("contributionSeasons").doc(seasonId).collection("players").doc(uid);
}

function buildDefaultUserProfile(uid, authContext, raw = {}) {
  const displayName = sanitizeDisplayName(
    raw.displayName || raw.username || authContext.token.name || "Player"
  ) || "Player";
  return {
    uid,
    displayName,
    clanId: normalizeString(raw.clanId),
    clanRole: normalizeString(raw.clanRole) || "",
    clanTag: normalizeString(raw.clanTag) || "",
    clanJoinedAt: Number(raw.clanJoinedAt || 0),
    contributionCooldownEndsAt: Number(raw.contributionCooldownEndsAt || 0),
    equippedTitleId: normalizeString(raw.equippedTitleId) || "",
    equippedFrameId: normalizeString(raw.equippedFrameId) || "",
    equippedNameplateId: normalizeString(raw.equippedNameplateId) || "",
    equippedStreakEffectId: normalizeString(raw.equippedStreakEffectId) || "",
    equippedResultCardId: normalizeString(raw.equippedResultCardId) || "",
    contributionStreak: Number(raw.contributionStreak || 0),
    lifetimeContribution: Number(raw.lifetimeContribution || 0),
    warParticipationCount: Number(raw.warParticipationCount || 0),
    warWinCount: Number(raw.warWinCount || 0),
    mvpCount: Number(raw.mvpCount || 0),
    warChestClaims: Number(raw.warChestClaims || 0),
    lastSeasonFinish: Number(raw.lastSeasonFinish || 0),
    seasonBadges: Array.isArray(raw.seasonBadges) ? raw.seasonBadges.map(normalizeString).filter(Boolean) : [],
  };
}

function clanFromSnapshot(snapshot) {
  const data = snapshot.data() || {};
  return {
    id: snapshot.id,
    name: normalizeClanName(data.name || snapshot.id),
    tag: normalizeClanTag(data.tag),
    description: normalizeString(data.description).slice(0, 120),
    emblemId: normalizeString(data.emblemId) || "shield",
    leaderUid: normalizeString(data.leaderUid),
    memberCount: Number(data.memberCount || 0),
    activeMemberCount7d: Number(data.activeMemberCount7d || 0),
    activeBand: normalizeString(data.activeBand) || activeBandForCount(Number(data.activeMemberCount7d || 0)),
    openSlots: Number(data.openSlots || Math.max(0, MAX_CLAN_SIZE - Number(data.memberCount || 0))),
    currentWarId: normalizeString(data.currentWarId),
    battleJoinRecommended: Boolean(data.battleJoinRecommended),
    lastWarStrength: Number(data.lastWarStrength || 0),
  };
}

function buildClanWrite(clan) {
  return {
    name: clan.name,
    tag: clan.tag,
    description: clan.description,
    emblemId: clan.emblemId,
    leaderUid: clan.leaderUid,
    memberCount: clan.memberCount,
    activeMemberCount7d: clan.activeMemberCount7d,
    activeBand: activeBandForCount(clan.activeMemberCount7d),
    openSlots: Math.max(0, MAX_CLAN_SIZE - clan.memberCount),
    currentWarId: clan.currentWarId || "",
    battleJoinRecommended: Boolean(clan.battleJoinRecommended),
    lastWarStrength: Number(clan.lastWarStrength || 0),
    updatedAt: FieldValue.serverTimestamp(),
  };
}

function buildSeasonRow(uid, profile, existing = {}) {
  return {
    uid,
    displayName: profile.displayName,
    clanId: profile.clanId || "",
    clanTag: profile.clanTag || "",
    equippedTitleId: profile.equippedTitleId || "",
    equippedFrameId: profile.equippedFrameId || "",
    equippedNameplateId: profile.equippedNameplateId || "",
    dailyCp: Number(existing.dailyCp || 0),
    warCp: Number(existing.warCp || 0),
    taskCp: Number(existing.taskCp || 0),
    totalCp: Number(existing.totalCp || 0),
    soloCapApplied: Boolean(existing.soloCapApplied),
    lastUpdatedAt: Date.now(),
  };
}

function ownershipFromData(data) {
  const source = data || {};
  return {
    premiumOwnedIds: Array.isArray(source.premiumOwnedIds) ? source.premiumOwnedIds.map(normalizeString).filter(Boolean) : [],
    earnedOwnedIds: Array.isArray(source.earnedOwnedIds) ? source.earnedOwnedIds.map(normalizeString).filter(Boolean) : [],
    warFragments: Number(source.warFragments || 0),
  };
}

function buildOwnershipWrite(state) {
  return {
    premiumOwnedIds: state.premiumOwnedIds,
    earnedOwnedIds: state.earnedOwnedIds,
    warFragments: state.warFragments,
    updatedAt: FieldValue.serverTimestamp(),
  };
}

function findPack(productId) {
  return CROWN_PACKS.find((pack) => pack.id === productId);
}

function findPremiumItem(itemId) {
  return PREMIUM_ITEMS.find((item) => item.id === itemId);
}

function findEarnedItem(itemId) {
  return EARNED_ITEMS.find((item) => item.id === itemId);
}

async function pairEligibleClans(systemState) {
  const snapshot = await db.collection("clans")
    .where("activeMemberCount7d", ">=", MIN_ACTIVE_CLAN_SIZE)
    .get();
  const clans = snapshot.docs.map(clanFromSnapshot).filter((clan) => clan.memberCount > 0);
  const groups = {
    small: [],
    medium: [],
    large: [],
  };
  clans.forEach((clan) => {
    if (groups[clan.activeBand]) {
      groups[clan.activeBand].push(clan);
    }
  });

  const batch = db.batch();
  let pairIndex = 0;
  Object.values(groups).forEach((group) => {
    group.sort((left, right) => right.lastWarStrength - left.lastWarStrength);
    for (let index = 0; index < group.length; index += 2) {
      const left = group[index];
      const right = group[index + 1];
      if (!right) {
        batch.set(db.collection("clans").doc(left.id), buildClanWrite({
          ...left,
          currentWarId: "",
          battleJoinRecommended: false,
        }), { merge: true });
        continue;
      }

      pairIndex += 1;
      const warId = `${systemState.warId}-${pairIndex}`;
      const contributorCap = Math.max(
        3,
        Math.min(MAX_COUNTED_CONTRIBUTORS, Math.min(left.activeMemberCount7d, right.activeMemberCount7d))
      );
      batch.set(db.collection("clanWars").doc(warId), {
        seasonId: systemState.seasonId,
        cycleNumber: systemState.currentWarIndex,
        phase: systemState.phase,
        leftClanId: left.id,
        rightClanId: right.id,
        leftScore: 0,
        rightScore: 0,
        contributorCap,
        leftActiveAtPairing: left.activeMemberCount7d,
        rightActiveAtPairing: right.activeMemberCount7d,
        prepStartsAt: systemState.prepStartsAt,
        battleStartsAt: systemState.battleStartsAt,
        battleEndsAt: systemState.battleEndsAt,
        settlementEndsAt: systemState.settlementEndsAt,
        result: "pending",
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      batch.set(db.collection("clans").doc(left.id), buildClanWrite({
        ...left,
        currentWarId: warId,
        battleJoinRecommended: systemState.phase === "battle" && left.memberCount < MAX_CLAN_SIZE,
      }), { merge: true });
      batch.set(db.collection("clans").doc(right.id), buildClanWrite({
        ...right,
        currentWarId: warId,
        battleJoinRecommended: systemState.phase === "battle" && right.memberCount < MAX_CLAN_SIZE,
      }), { merge: true });
    }
  });

  await batch.commit();
}

async function settleCurrentWarsForCycle(systemState) {
  const snapshot = await db.collection("clanWars")
    .where("seasonId", "==", systemState.seasonId)
    .where("cycleNumber", "==", systemState.currentWarIndex)
    .get();
  if (snapshot.empty) {
    return;
  }

  for (const warDoc of snapshot.docs) {
    const war = warDoc.data() || {};
    if (Boolean(war.statsApplied)) {
      continue;
    }

    const [participantsSnapshot, leftMembersSnapshot, rightMembersSnapshot] = await Promise.all([
      warDoc.ref.collection("participants").get(),
      db.collection("clans").doc(normalizeString(war.leftClanId)).collection("members").get(),
      db.collection("clans").doc(normalizeString(war.rightClanId)).collection("members").get(),
    ]);

    const participantMap = new Map(
      participantsSnapshot.docs.map((doc) => [doc.id, doc.data() || {}]),
    );
    const leftScore = Number(war.leftScore || 0);
    const rightScore = Number(war.rightScore || 0);
    const result = leftScore === rightScore
      ? "draw"
      : leftScore > rightScore
        ? "left_win"
        : "right_win";

    let mvpUid = "";
    let mvpScore = -1;
    participantsSnapshot.docs.forEach((doc) => {
      const countedScore = Number(doc.get("countedScore") || 0);
      if (countedScore > mvpScore) {
        mvpScore = countedScore;
        mvpUid = doc.id;
      }
    });

    const batch = db.batch();
    batch.set(warDoc.ref, {
      result,
      mvpUid,
      statsApplied: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    const applyMemberStats = (memberDoc, side) => {
      const uid = memberDoc.id;
      const participant = participantMap.get(uid) || {};
      const countedScore = Number(participant.countedScore || 0);
      const contributed = countedScore > 0;
      const isWinner = (side === "left" && result === "left_win") || (side === "right" && result === "right_win");
      const isMvp = uid === mvpUid;
      const userRef = db.collection("users").doc(uid);
      batch.set(memberDoc.ref, {
        contributionStreak: contributed ? FieldValue.increment(1) : 0,
        lifetimeContribution: FieldValue.increment(countedScore),
        warParticipationCount: contributed ? FieldValue.increment(1) : FieldValue.increment(0),
        warWinCount: contributed && isWinner ? FieldValue.increment(1) : FieldValue.increment(0),
        mvpCount: isMvp ? FieldValue.increment(1) : FieldValue.increment(0),
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      batch.set(userRef, {
        contributionStreak: contributed ? FieldValue.increment(1) : 0,
        lifetimeContribution: FieldValue.increment(countedScore),
        warParticipationCount: contributed ? FieldValue.increment(1) : FieldValue.increment(0),
        warWinCount: contributed && isWinner ? FieldValue.increment(1) : FieldValue.increment(0),
        mvpCount: isMvp ? FieldValue.increment(1) : FieldValue.increment(0),
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    };

    leftMembersSnapshot.docs.forEach((doc) => applyMemberStats(doc, "left"));
    rightMembersSnapshot.docs.forEach((doc) => applyMemberStats(doc, "right"));
    await batch.commit();
  }
}

async function grantSeasonBadges(systemState) {
  const seasonRef = db.collection("contributionSeasons").doc(systemState.seasonId);
  const seasonSnapshot = await seasonRef.get();
  if (seasonSnapshot.exists && Boolean(seasonSnapshot.get("badgesGranted"))) {
    return;
  }

  const ladderSnapshot = await seasonRef.collection("players")
    .orderBy("totalCp", "desc")
    .orderBy("lastUpdatedAt", "desc")
    .limit(100)
    .get();
  if (ladderSnapshot.empty) {
    await seasonRef.set({
      badgesGranted: true,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return;
  }

  const batch = db.batch();
  ladderSnapshot.docs.forEach((doc, index) => {
    const rank = index + 1;
    let badge = "";
    if (rank === 1) badge = "Season Champion";
    else if (rank <= 10) badge = "Season Elite";
    else badge = "Season Veteran";

    batch.set(db.collection("users").doc(doc.id), {
      lastSeasonFinish: rank,
      seasonBadges: FieldValue.arrayUnion(badge),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
  batch.set(seasonRef, {
    badgesGranted: true,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  await batch.commit();
}

function normalizeMillis(value) {
  const millis = Number(value || 0);
  return Number.isFinite(millis) ? Math.max(0, Math.trunc(millis)) : 0;
}

async function deleteDocumentsInBatches(query, batchSize = 200) {
  while (true) {
    const snapshot = await query.limit(batchSize).get();
    if (snapshot.empty) {
      return;
    }

    const batch = db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    if (snapshot.size < batchSize) {
      return;
    }
  }
}

exports.registerDevice = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const deviceId = normalizeDeviceId(request.data && request.data.deviceId);
  if (!deviceId || deviceId.length < 12) {
    throw new HttpsError("invalid-argument", "A valid device id is required.");
  }

  const deviceRef = db.collection("deviceRegistrations").doc(deviceId);
  const result = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(deviceRef);
    const existing = snapshot.exists ? snapshot.data() || {} : {};
    const accountIds = Array.isArray(existing.accountIds)
      ? existing.accountIds.map((value) => normalizeString(value)).filter(Boolean)
      : [];
    const alreadyLinked = accountIds.includes(uid);

    if (!alreadyLinked && accountIds.length >= MAX_ACCOUNTS_PER_DEVICE) {
      transaction.set(deviceRef, {
        deviceId,
        accountIds,
        accountCount: accountIds.length,
        blocked: true,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      return {
        blocked: true,
        deviceId,
        accountCount: accountIds.length,
        maxAccounts: MAX_ACCOUNTS_PER_DEVICE,
      };
    }

    const nextAccountIds = alreadyLinked ? accountIds : [...accountIds, uid];
    transaction.set(deviceRef, {
      deviceId,
      accountIds: nextAccountIds,
      accountCount: nextAccountIds.length,
      blocked: false,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      blocked: false,
      deviceId,
      accountCount: nextAccountIds.length,
      maxAccounts: MAX_ACCOUNTS_PER_DEVICE,
    };
  });

  return result;
});

exports.syncCoinBalance = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "syncCoinBalance");
  const walletRef = db.collection("wallets").doc(uid);
  const clientSnapshot = normalizeCoinSnapshot(request.data || {}, todayDateKey());

  return db.runTransaction(async (transaction) => {
    const walletSnapshot = await transaction.get(walletRef);
    const currentWallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    const evaluation = evaluateCoinSync(
      clientSnapshot,
      currentWallet,
      {
        maxDailyTotalCoins: MAX_DAILY_TOTAL_COINS,
        maxDailyTaskCoins: MAX_DAILY_TASK_COINS,
        maxDailyQuizCoins: MAX_DAILY_QUIZ_COINS,
        maxDailyTaskClaims: MAX_DAILY_TASK_CLAIMS,
        maxCoinsPerCorrect: MAX_COINS_PER_CORRECT,
      },
      todayDateKey(),
    );

    let nextWallet = currentWallet;
    if (evaluation.suspicious) {
      nextWallet = {
        ...currentWallet,
        suspiciousCount: currentWallet.suspiciousCount + 1,
      };
      transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    }

    return {
      acceptedCoins: currentWallet.coins,
      suspicious: evaluation.suspicious,
      reasons: evaluation.reasons,
      wallet: walletResponse(nextWallet),
    };
  });
});

exports.loadGameBackup = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const backupRef = db.collection("gameBackups").doc(uid);
  const snapshot = await backupRef.get();

  if (!snapshot.exists) {
    return {
      exists: false,
    };
  }

  return {
    exists: true,
    stateJson: String(snapshot.get("stateJson") || ""),
    updatedAt: normalizeMillis(snapshot.get("updatedAt")),
  };
});

exports.saveGameBackup = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const stateJson = String((request.data && request.data.stateJson) || "").trim();
  if (!stateJson) {
    throw new HttpsError("invalid-argument", "Backup payload is required.");
  }
  if (stateJson.length > 700000) {
    throw new HttpsError("invalid-argument", "Backup payload is too large.");
  }

  const updatedAt = Date.now();
  const backupRef = db.collection("gameBackups").doc(uid);
  await backupRef.set({
    uid,
    stateJson,
    updatedAt,
    updatedAtServer: FieldValue.serverTimestamp(),
  }, { merge: true });

  return {
    updatedAt,
  };
});

exports.recordGameplaySession = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "recordGameplaySession");
  let session;
  try {
    session = gameplaySessionFromData(request.data || {});
  } catch (error) {
    throw new HttpsError("invalid-argument", error.message || "Gameplay session payload is invalid.");
  }

  const statsRef = db.collection("dailyPlayerStats").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);
  const userRef = db.collection("users").doc(uid);
  return db.runTransaction(async (transaction) => {
    const [statsSnapshot, userSnapshot, systemSnapshot] = await Promise.all([
      transaction.get(statsRef),
      transaction.get(userRef),
      transaction.get(systemRef()),
    ]);
    const currentStats = normalizePlayerStats(
      uid,
      statsSnapshot.exists ? statsSnapshot.data() : null,
    );

    // Anomaly: daily session cap — no one plays 100+ quizzes in a day
    if (currentStats.quizzesPlayed >= 100) {
      throw new HttpsError("resource-exhausted", "Daily session limit reached. Come back tomorrow!");
    }

    const result = applyGameplaySession(currentStats, session);
    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    const systemState = systemSnapshot.exists ? (systemSnapshot.data() || {}) : buildDefaultSystem();
    const sessionCp = computeSessionCp(session);

    if (!result.duplicate) {
      transaction.set(statsRef, buildPlayerStatsWrite(result.stats), { merge: true });
      transaction.set(systemRef(), systemState, { merge: true });

      const seasonRowRef = seasonPlayerRef(systemState.seasonId || makeSeasonId(1), uid);
      const seasonSnapshot = await transaction.get(seasonRowRef);
      const seasonRow = buildSeasonRow(uid, profile, seasonSnapshot.exists ? seasonSnapshot.data() : {});

      if (!profile.clanId && session.sessionType === "daily_challenge") {
        seasonRow.dailyCp = clampSoloSeasonCp(seasonRow.dailyCp + sessionCp);
        seasonRow.totalCp = clampSoloSeasonCp(seasonRow.dailyCp + seasonRow.warCp + seasonRow.taskCp);
        seasonRow.soloCapApplied = seasonRow.totalCp >= SOLO_SEASON_CP_CAP;
        transaction.set(seasonRowRef, seasonRow, { merge: true });
      } else if (profile.clanId) {
        const clanRef = db.collection("clans").doc(profile.clanId);
        const memberRef = clanRef.collection("members").doc(uid);
        const [clanSnapshot, memberSnapshot] = await Promise.all([
          transaction.get(clanRef),
          transaction.get(memberRef),
        ]);

        if (clanSnapshot.exists && memberSnapshot.exists) {
          const clan = clanFromSnapshot(clanSnapshot);
          const previousActiveUntil = Number(memberSnapshot.get("activeUntil") || 0);
          const wasActive = Boolean(memberSnapshot.get("activeCounted")) && previousActiveUntil >= Date.now();
          transaction.set(memberRef, {
            activeUntil: Date.now() + ACTIVE_WINDOW_MILLIS,
            activeCounted: true,
            displayName: profile.displayName,
            clanTag: profile.clanTag,
            equippedTitleId: profile.equippedTitleId,
            equippedFrameId: profile.equippedFrameId,
            equippedNameplateId: profile.equippedNameplateId,
            updatedAt: FieldValue.serverTimestamp(),
          }, { merge: true });

          if (!wasActive) {
            transaction.set(clanRef, buildClanWrite({
              ...clan,
              activeMemberCount7d: clan.activeMemberCount7d + 1,
            }), { merge: true });
          }

          if (session.sessionType === "daily_challenge") {
            seasonRow.dailyCp += sessionCp;
          }

          if (clan.currentWarId) {
            const warRef = db.collection("clanWars").doc(clan.currentWarId);
            const participantRef = warRef.collection("participants").doc(uid);
            const [warSnapshot, participantSnapshot] = await Promise.all([
              transaction.get(warRef),
              transaction.get(participantRef),
            ]);
            if (warSnapshot.exists) {
              const war = warSnapshot.data() || {};
              const previousScores = Array.isArray(participantSnapshot.get("bestContributionScores"))
                ? participantSnapshot.get("bestContributionScores")
                : [];
              const nextScores = insertTopContribution(previousScores, sessionCp);
              const previousCounted = Number(participantSnapshot.get("countedScore") || 0);
              const nextCounted = sumScores(nextScores);
              const delta = nextCounted - previousCounted;
              transaction.set(participantRef, {
                uid,
                clanId: profile.clanId,
                bestContributionScores: nextScores,
                countedRuns: nextScores.length,
                countedScore: nextCounted,
                updatedAt: FieldValue.serverTimestamp(),
              }, { merge: true });
              if (delta > 0) {
                if (profile.clanId === normalizeString(war.leftClanId)) {
                  transaction.set(warRef, {
                    leftScore: FieldValue.increment(delta),
                    updatedAt: FieldValue.serverTimestamp(),
                  }, { merge: true });
                } else if (profile.clanId === normalizeString(war.rightClanId)) {
                  transaction.set(warRef, {
                    rightScore: FieldValue.increment(delta),
                    updatedAt: FieldValue.serverTimestamp(),
                  }, { merge: true });
                }
                seasonRow.warCp += delta;
              }
            }
          }

          seasonRow.totalCp = seasonRow.dailyCp + seasonRow.warCp + seasonRow.taskCp;
          transaction.set(seasonRowRef, seasonRow, { merge: true });
        }
      }

      // Anomaly: flag suspiciously perfect accuracy
      // If user has 50+ correct with 100% accuracy AND this session is also perfect
      if (
        currentStats.correctAnswers >= 50 &&
        currentStats.correctAnswers === currentStats.questionsAnswered &&
        session.correctAnswers === session.questionsAnswered &&
        session.questionsAnswered >= 5
      ) {
        // Increment suspicious count on wallet for monitoring
        transaction.set(walletRef, {
          suspiciousCount: FieldValue.increment(1),
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
    }

    return {
      accepted: !result.duplicate,
      dayKey: result.stats.dayKey,
      contributionPoints: sessionCp,
    };
  });
});

exports.fetchDailyTaskStatus = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const statsSnapshot = await db.collection("dailyPlayerStats").doc(uid).get();
  const stats = normalizePlayerStats(
    uid,
    statsSnapshot.exists ? statsSnapshot.data() : emptyPlayerStats(uid),
  );

  return {
    dayKey: stats.dayKey,
    tasks: buildTaskStatus(stats, stats.dayKey),
  };
});

exports.claimDailyTaskReward = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "claimDailyTaskReward");
  const taskId = normalizeString(request.data && request.data.taskId);
  const adBoosted = Boolean(request.data && request.data.adBoosted);
  if (!taskId) {
    throw new HttpsError("invalid-argument", "A valid task id is required.");
  }

  const statsRef = db.collection("dailyPlayerStats").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);
  return db.runTransaction(async (transaction) => {
    const [statsSnapshot, walletSnapshot] = await Promise.all([
      transaction.get(statsRef),
      transaction.get(walletRef),
    ]);

    const currentStats = normalizePlayerStats(
      uid,
      statsSnapshot.exists ? statsSnapshot.data() : null,
    );
    const taskDefinition = taskDefinitionById(taskId, currentStats.dayKey);
    if (!taskDefinition) {
      throw new HttpsError("invalid-argument", "That daily task is not available.");
    }
    if (currentStats.claimedTaskIds.includes(taskDefinition.id)) {
      throw new HttpsError("already-exists", "That daily task reward is already claimed.");
    }

    const taskStatus = buildTaskStatus(currentStats, currentStats.dayKey)
      .find((task) => task.id === taskDefinition.id);
    if (!taskStatus || !taskStatus.complete) {
      throw new HttpsError("failed-precondition", "That daily task is not complete yet.");
    }

    const baseReward = taskDefinition.rewardCoins;
    const rewardCoins = adBoosted ? Math.floor(baseReward * 1.5) : baseReward;

    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    const validatedEarnedToday = wallet.lastValidatedDayKey === currentStats.dayKey
      ? wallet.validatedEarnedToday
      : 0;
    const nextValidatedEarnedToday = validatedEarnedToday + rewardCoins;
    if (nextValidatedEarnedToday > MAX_DAILY_TASK_COINS || nextValidatedEarnedToday > MAX_DAILY_TOTAL_COINS) {
      throw new HttpsError("failed-precondition", "Daily reward cap reached.");
    }

    const nextStats = {
      ...currentStats,
      claimedTaskIds: [...currentStats.claimedTaskIds, taskDefinition.id],
    };
    const nextWallet = {
      uid,
      coins: wallet.coins + rewardCoins,
      lastValidatedDayKey: currentStats.dayKey,
      validatedEarnedToday: nextValidatedEarnedToday,
      suspiciousCount: wallet.suspiciousCount,
    };

    transaction.set(statsRef, buildPlayerStatsWrite(nextStats), { merge: true });
    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });

    return {
      taskId: taskDefinition.id,
      taskTitle: taskDefinition.title,
      baseReward,
      rewardCoins,
      adBoosted,
      wallet: walletResponse(nextWallet),
    };
  });
});

exports.createClan = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const input = request.data || {};
  const name = normalizeClanName(input.name);
  const tag = normalizeClanTag(input.tag);
  const description = normalizeString(input.description).slice(0, 120);
  const emblemId = normalizeString(input.emblemId) || "shield";

  console.log(`createClan called by ${uid}: name="${name}", tag="${tag}"`);

  if (name.length < 3 || tag.length < 2) {
    console.warn(`createClan validation failed for ${uid}: name.length=${name.length}, tag.length=${tag.length}`);
    throw new HttpsError("invalid-argument", "Clan name (min 3 chars) and tag (min 2 chars) are required.");
  }

  // Check if user already has a clan — return it instead of erroring.
  // This makes createClan idempotent so retries after a partial success don't break.
  const userRef = db.collection("users").doc(uid);
  const existingUserSnapshot = await userRef.get();
  const existingProfile = buildDefaultUserProfile(uid, authContext, existingUserSnapshot.exists ? existingUserSnapshot.data() : {});
  if (existingProfile.clanId) {
    console.log(`createClan: user ${uid} already in clan ${existingProfile.clanId}, returning existing.`);
    return {
      clanId: existingProfile.clanId,
      clanTag: existingProfile.clanTag || tag,
      landingTab: "war",
      alreadyInClan: true,
    };
  }

  const duplicateTag = await db.collection("clans").where("tag", "==", tag).limit(1).get();
  if (!duplicateTag.empty) {
    console.warn(`createClan: tag "${tag}" already taken.`);
    throw new HttpsError("already-exists", "That clan tag is already taken.");
  }

  const clanRef = db.collection("clans").doc();
  const joinedAt = Date.now();
  await ensureClanSystemState();

  await db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    // Double-check inside transaction (race condition guard)
    if (profile.clanId) {
      console.log(`createClan transaction: user ${uid} already joined clan ${profile.clanId} (race guard).`);
      return;
    }

    const clan = {
      id: clanRef.id,
      name,
      tag,
      description,
      emblemId,
      leaderUid: uid,
      memberCount: 1,
      activeMemberCount7d: 0,
      activeBand: activeBandForCount(0),
      openSlots: MAX_CLAN_SIZE - 1,
      currentWarId: "",
      battleJoinRecommended: false,
      lastWarStrength: 0,
    };

    transaction.set(clanRef, buildClanWrite(clan), { merge: true });
    transaction.set(userRef, {
      clanId: clanRef.id,
      clanRole: "leader",
      clanTag: tag,
      clanJoinedAt: joinedAt,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(clanRef.collection("members").doc(uid), {
      uid,
      role: "leader",
      joinedAt,
      contributionCooldownEndsAt: 0,
      activeUntil: 0,
      activeCounted: false,
      displayName: profile.displayName,
      clanTag: tag,
      equippedTitleId: profile.equippedTitleId,
      equippedFrameId: profile.equippedFrameId,
      equippedNameplateId: profile.equippedNameplateId,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  console.log(`createClan: clan ${clanRef.id} created successfully for user ${uid}.`);
  return {
    clanId: clanRef.id,
    clanTag: tag,
    landingTab: "war",
    alreadyInClan: false,
  };
});

exports.searchClans = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  requireAuth(request);
  const query = normalizeString(request.data && request.data.query).toLowerCase();
  const limit = Math.min(Math.max(Number(request.data && request.data.limit) || 20, 1), 50);
  await ensureClanSystemState();

  const snapshot = await db.collection("clans")
    .orderBy("battleJoinRecommended", "desc")
    .orderBy("activeMemberCount7d", "desc")
    .limit(60)
    .get();

  const clans = snapshot.docs
    .map(clanFromSnapshot)
    .filter((clan) => clan.memberCount < MAX_CLAN_SIZE)
    .filter((clan) => {
      if (!query) return true;
      return clan.name.toLowerCase().includes(query) || clan.tag.toLowerCase().includes(query);
    })
    .sort((left, right) => {
      if (Number(right.battleJoinRecommended) !== Number(left.battleJoinRecommended)) {
        return Number(right.battleJoinRecommended) - Number(left.battleJoinRecommended);
      }
      if (right.activeMemberCount7d !== left.activeMemberCount7d) {
        return right.activeMemberCount7d - left.activeMemberCount7d;
      }
      return left.name.localeCompare(right.name);
    })
    .slice(0, limit);

  return {
    clans: clans.map((clan) => ({
      id: clan.id,
      name: clan.name,
      tag: clan.tag,
      description: clan.description,
      emblemId: clan.emblemId,
      memberCount: clan.memberCount,
      activeMemberCount7d: clan.activeMemberCount7d,
      activeBand: clan.activeBand,
      openSlots: clan.openSlots,
      battleJoinRecommended: clan.battleJoinRecommended,
    })),
  };
});

exports.joinClan = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const clanId = normalizeString(request.data && request.data.clanId);
  if (!clanId) {
    throw new HttpsError("invalid-argument", "Clan id is required.");
  }

  const userRef = db.collection("users").doc(uid);
  const clanRef = db.collection("clans").doc(clanId);
  const joinedAt = Date.now();
  await ensureClanSystemState();

  return db.runTransaction(async (transaction) => {
    const [userSnapshot, clanSnapshot] = await Promise.all([
      transaction.get(userRef),
      transaction.get(clanRef),
    ]);
    if (!clanSnapshot.exists) {
      throw new HttpsError("not-found", "Clan not found.");
    }

    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    if (profile.clanId) {
      throw new HttpsError("failed-precondition", "Leave your current clan first.");
    }

    const clan = clanFromSnapshot(clanSnapshot);
    if (clan.memberCount >= MAX_CLAN_SIZE) {
      throw new HttpsError("failed-precondition", "Clan is full.");
    }

    transaction.set(userRef, {
      clanId,
      clanRole: "member",
      clanTag: clan.tag,
      clanJoinedAt: joinedAt,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(clanRef, buildClanWrite({
      ...clan,
      memberCount: clan.memberCount + 1,
      openSlots: MAX_CLAN_SIZE - (clan.memberCount + 1),
    }), { merge: true });
    transaction.set(clanRef.collection("members").doc(uid), {
      uid,
      role: "member",
      joinedAt,
      contributionCooldownEndsAt: Number(profile.contributionCooldownEndsAt || 0),
      activeUntil: 0,
      activeCounted: false,
      displayName: profile.displayName,
      clanTag: clan.tag,
      equippedTitleId: profile.equippedTitleId,
      equippedFrameId: profile.equippedFrameId,
      equippedNameplateId: profile.equippedNameplateId,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      clanId,
      landingTab: "war",
      contributionCooldownEndsAt: Number(profile.contributionCooldownEndsAt || 0),
    };
  });
});

exports.leaveClan = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const userRef = db.collection("users").doc(uid);
  const nowMillis = Date.now();
  console.log(`leaveClan called by ${uid}`);

  return db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    if (!profile.clanId) {
      console.warn(`leaveClan: user ${uid} is not in a clan.`);
      throw new HttpsError("failed-precondition", "You are not in a clan.");
    }

    const clanRef = db.collection("clans").doc(profile.clanId);
    const memberRef = clanRef.collection("members").doc(uid);
    const [clanSnapshot, memberSnapshot] = await Promise.all([
      transaction.get(clanRef),
      transaction.get(memberRef),
    ]);

    // If the clan document or member record is missing, clean up the user's stale clan reference
    if (!clanSnapshot.exists || !memberSnapshot.exists) {
      console.warn(`leaveClan: clan ${profile.clanId} or membership not found for ${uid}, cleaning up stale reference.`);
      transaction.set(userRef, {
        clanId: FieldValue.delete(),
        clanRole: FieldValue.delete(),
        clanTag: FieldValue.delete(),
        clanJoinedAt: FieldValue.delete(),
        contributionCooldownEndsAt: 0,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      return { contributionCooldownEndsAt: 0 };
    }

    const clan = clanFromSnapshot(clanSnapshot);
    const role = normalizeString(memberSnapshot.get("role")) || "member";
    // If leader is the only member, allow them to leave (effectively disbanding)
    if (role === "leader" && clan.memberCount > 1) {
      throw new HttpsError("failed-precondition", "Transfer leadership before leaving.");
    }

    const contributionCooldownEndsAt = nowMillis + JOIN_COOLDOWN_MILLIS;
    transaction.set(userRef, {
      clanId: FieldValue.delete(),
      clanRole: FieldValue.delete(),
      clanTag: FieldValue.delete(),
      clanJoinedAt: FieldValue.delete(),
      contributionCooldownEndsAt,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.delete(memberRef);

    const nextMemberCount = Math.max(0, clan.memberCount - 1);
    if (nextMemberCount === 0) {
      // Last member leaving — delete the clan entirely
      console.log(`leaveClan: last member ${uid} leaving clan ${profile.clanId}, deleting clan.`);
      transaction.delete(clanRef);
    } else {
      transaction.set(clanRef, buildClanWrite({
        ...clan,
        memberCount: nextMemberCount,
        openSlots: MAX_CLAN_SIZE - nextMemberCount,
        battleJoinRecommended: false,
      }), { merge: true });
    }

    console.log(`leaveClan: user ${uid} left clan ${profile.clanId} successfully.`);
    return { contributionCooldownEndsAt };
  });
});

exports.fetchContributionLadder = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const tab = normalizeString(request.data && request.data.tab).toLowerCase() || "season";
  const pageSize = Math.min(Math.max(Number(request.data && request.data.pageSize) || 25, 1), 100);
  const userSnapshot = await db.collection("users").doc(uid).get();
  const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});

  if (tab === "today") {
    const dateKey = todayDateKey();
    const snapshot = await db.collection("verifiedDailyEntries")
      .doc(dateKey)
      .collection("players")
      .orderBy("correctAnswers", "desc")
      .orderBy("elapsedMillis", "asc")
      .limit(pageSize)
      .get();

    return {
      tab: "today",
      rows: snapshot.docs.map((doc, index) => ({
        rank: index + 1,
        uid: doc.id,
        ...doc.data(),
      })),
      currentUserRow: null,
      metadata: { dateKey, cap: null },
    };
  }

  const systemState = await ensureClanSystemState();
  const seasonRef = db.collection("contributionSeasons").doc(systemState.seasonId);
  await seasonRef.set({
    seasonId: systemState.seasonId,
    warCount: systemState.currentWarIndex,
    seasonLengthWars: SEASON_WARS,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  const snapshot = await seasonRef.collection("players")
    .orderBy("totalCp", "desc")
    .orderBy("lastUpdatedAt", "desc")
    .limit(pageSize)
    .get();
  const rows = snapshot.docs.map((doc, index) => ({
    rank: index + 1,
    uid: doc.id,
    ...doc.data(),
  }));

  let currentUserRow = rows.find((row) => row.uid === uid) || null;
  if (!currentUserRow) {
    const currentSnapshot = await seasonPlayerRef(systemState.seasonId, uid).get();
    currentUserRow = currentSnapshot.exists
      ? { rank: null, uid, ...currentSnapshot.data() }
      : {
        rank: null,
        uid,
        displayName: profile.displayName,
        clanId: profile.clanId,
        clanTag: profile.clanTag,
        equippedTitleId: profile.equippedTitleId,
        equippedFrameId: profile.equippedFrameId,
        equippedNameplateId: profile.equippedNameplateId,
        dailyCp: 0,
        warCp: 0,
        taskCp: 0,
        totalCp: 0,
        soloCapApplied: false,
      };
  }

  if (!profile.clanId && currentUserRow.totalCp > SOLO_SEASON_CP_CAP) {
    currentUserRow.totalCp = clampSoloSeasonCp(currentUserRow.totalCp);
    currentUserRow.soloCapApplied = true;
  }

  return {
    tab: "season",
    rows,
    currentUserRow,
    metadata: {
      seasonId: systemState.seasonId,
      cap: profile.clanId ? null : SOLO_SEASON_CP_CAP,
      currentWarIndex: systemState.currentWarIndex,
    },
  };
});

exports.fetchClanWarState = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const userSnapshot = await db.collection("users").doc(uid).get();
  const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
  const clanId = normalizeString(request.data && request.data.clanId) || profile.clanId;
  if (!clanId) {
    throw new HttpsError("failed-precondition", "Join a clan first.");
  }

  const clanSnapshot = await db.collection("clans").doc(clanId).get();
  if (!clanSnapshot.exists) {
    throw new HttpsError("not-found", "Clan not found.");
  }

  const clan = clanFromSnapshot(clanSnapshot);
  const systemState = await ensureClanSystemState();
  const memberSnapshot = await db.collection("clans").doc(clanId).collection("members").get();

  if (!clan.currentWarId) {
    return {
      warId: "",
      clanId,
      clanName: clan.name,
      clanTag: clan.tag,
      phase: systemState.phase,
      countdownMillis: Math.max(0, Number(systemState.battleStartsAt || Date.now()) - Date.now()),
      contributorCap: 0,
      clanScore: 0,
      opponentScore: 0,
      opponentName: "Waiting for match",
      result: "pending",
      canClaimChest: false,
      chestClaimed: false,
      isMvp: false,
      members: memberSnapshot.docs.map((doc) => ({
        uid: doc.id,
        displayName: normalizeString(doc.get("displayName")) || "Player",
        equippedTitleId: normalizeString(doc.get("equippedTitleId")),
        equippedFrameId: normalizeString(doc.get("equippedFrameId")),
        equippedNameplateId: normalizeString(doc.get("equippedNameplateId")),
        countedRuns: 0,
        countedScore: 0,
      })),
      currentPlayerRow: {
        uid,
        displayName: profile.displayName,
        equippedTitleId: profile.equippedTitleId,
        equippedFrameId: profile.equippedFrameId,
        equippedNameplateId: profile.equippedNameplateId,
        countedRuns: 0,
        countedScore: 0,
      },
    };
  }

  const warSnapshot = await db.collection("clanWars").doc(clan.currentWarId).get();
  if (!warSnapshot.exists) {
    throw new HttpsError("not-found", "War not found.");
  }
  const war = warSnapshot.data() || {};
  const opponentClanId = clanId === normalizeString(war.leftClanId) ? normalizeString(war.rightClanId) : normalizeString(war.leftClanId);
  const opponentSnapshot = opponentClanId ? await db.collection("clans").doc(opponentClanId).get() : null;
  const opponentName = opponentSnapshot && opponentSnapshot.exists
    ? normalizeClanName(opponentSnapshot.get("name") || opponentClanId)
    : "Waiting for match";
  const participantSnapshot = await db.collection("clanWars").doc(clan.currentWarId)
    .collection("participants")
    .orderBy("countedScore", "desc")
    .limit(MAX_COUNTED_CONTRIBUTORS)
    .get();

  const participantMap = new Map(
    participantSnapshot.docs.map((doc) => [doc.id, doc.data() || {}]),
  );
  const members = memberSnapshot.docs
    .map((doc) => {
      const participant = participantMap.get(doc.id) || {};
      return {
        uid: doc.id,
        displayName: normalizeString(doc.get("displayName")) || "Player",
        equippedTitleId: normalizeString(doc.get("equippedTitleId")),
        equippedFrameId: normalizeString(doc.get("equippedFrameId")),
        equippedNameplateId: normalizeString(doc.get("equippedNameplateId")),
        countedRuns: Number(participant.countedRuns || 0),
        countedScore: Number(participant.countedScore || 0),
      };
    })
    .sort((left, right) => right.countedScore - left.countedScore);
  const currentPlayerRow = members.find((member) => member.uid === uid) || {
    uid,
    displayName: profile.displayName,
    equippedTitleId: profile.equippedTitleId,
    equippedFrameId: profile.equippedFrameId,
    equippedNameplateId: profile.equippedNameplateId,
    countedRuns: 0,
    countedScore: 0,
  };
  const currentParticipant = participantMap.get(uid) || {};

  return {
    warId: clan.currentWarId,
    clanId,
    clanName: clan.name,
    clanTag: clan.tag,
    phase: normalizeString(war.phase) || systemState.phase,
    countdownMillis: Math.max(0, Number(
      normalizeString(war.phase) === "prep"
        ? war.battleStartsAt
        : normalizeString(war.phase) === "battle"
          ? war.battleEndsAt
          : war.settlementEndsAt
    ) - Date.now()),
    contributorCap: Number(war.contributorCap || 0),
    clanScore: clanId === normalizeString(war.leftClanId) ? Number(war.leftScore || 0) : Number(war.rightScore || 0),
    opponentScore: clanId === normalizeString(war.leftClanId) ? Number(war.rightScore || 0) : Number(war.leftScore || 0),
    opponentName,
    result: normalizeString(war.result) || "pending",
    canClaimChest: normalizeString(war.phase) === "settlement" && Number(currentParticipant.countedScore || 0) > 0 && Number(currentParticipant.chestClaimedAt || 0) <= 0,
    chestClaimed: Number(currentParticipant.chestClaimedAt || 0) > 0,
    isMvp: normalizeString(war.mvpUid) === uid,
    members,
    currentPlayerRow,
  };
});

exports.fetchClanTasks = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const userSnapshot = await db.collection("users").doc(uid).get();
  const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
  if (!profile.clanId) {
    throw new HttpsError("failed-precondition", "Join a clan first.");
  }
  const statsSnapshot = await db.collection("dailyPlayerStats").doc(uid).get();
  const stats = normalizePlayerStats(uid, statsSnapshot.exists ? statsSnapshot.data() : null);
  return {
    dayKey: stats.dayKey,
    tasks: buildClanTaskStatus(stats),
  };
});

exports.claimClanTaskReward = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const taskId = normalizeString(request.data && request.data.taskId);
  const definition = clanTaskDefinitionById(taskId);
  if (!definition) {
    throw new HttpsError("invalid-argument", "Task not found.");
  }

  const userRef = db.collection("users").doc(uid);
  const statsRef = db.collection("dailyPlayerStats").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);

  return db.runTransaction(async (transaction) => {
    const [userSnapshot, statsSnapshot, walletSnapshot, systemSnapshot] = await Promise.all([
      transaction.get(userRef),
      transaction.get(statsRef),
      transaction.get(walletRef),
      transaction.get(systemRef()),
    ]);

    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    if (!profile.clanId) {
      throw new HttpsError("failed-precondition", "Join a clan first.");
    }

    const stats = normalizePlayerStats(uid, statsSnapshot.exists ? statsSnapshot.data() : null);
    const task = buildClanTaskStatus(stats).find((item) => item.id === taskId);
    if (!task || !task.complete) {
      throw new HttpsError("failed-precondition", "Task not complete.");
    }
    if (task.claimed) {
      throw new HttpsError("already-exists", "Task already claimed.");
    }

    const nextStats = {
      ...stats,
      claimedClanTaskIds: [...stats.claimedClanTaskIds, taskId],
    };
    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    const nextWallet = {
      ...wallet,
      coins: wallet.coins + definition.rewardCoins,
    };
    const systemState = systemSnapshot.exists ? (systemSnapshot.data() || {}) : buildDefaultSystem();
    const seasonRowRef = seasonPlayerRef(systemState.seasonId || makeSeasonId(1), uid);
    const seasonSnapshot = await transaction.get(seasonRowRef);
    const seasonRow = buildSeasonRow(uid, profile, seasonSnapshot.exists ? seasonSnapshot.data() : null);
    seasonRow.taskCp += definition.rewardCp;
    seasonRow.totalCp = seasonRow.dailyCp + seasonRow.warCp + seasonRow.taskCp;

    transaction.set(statsRef, buildPlayerStatsWrite(nextStats), { merge: true });
    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    transaction.set(seasonRowRef, seasonRow, { merge: true });

    return {
      taskId,
      rewardCoins: definition.rewardCoins,
      rewardCp: definition.rewardCp,
      wallet: walletResponse(nextWallet),
      totalCp: seasonRow.totalCp,
    };
  });
});

exports.claimWarChest = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const warId = normalizeString(request.data && request.data.warId);
  if (!warId) {
    throw new HttpsError("invalid-argument", "War id is required.");
  }

  const userRef = db.collection("users").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);
  const ownershipRef = db.collection("ownedCosmetics").doc(uid);
  const participantRef = db.collection("clanWars").doc(warId).collection("participants").doc(uid);

  return db.runTransaction(async (transaction) => {
    const [userSnapshot, walletSnapshot, ownershipSnapshot, warSnapshot, participantSnapshot] = await Promise.all([
      transaction.get(userRef),
      transaction.get(walletRef),
      transaction.get(ownershipRef),
      transaction.get(db.collection("clanWars").doc(warId)),
      transaction.get(participantRef),
    ]);

    if (!warSnapshot.exists || !participantSnapshot.exists) {
      throw new HttpsError("not-found", "War or participation entry not found.");
    }

    const war = warSnapshot.data() || {};
    if (normalizeString(war.phase) !== "settlement") {
      throw new HttpsError("failed-precondition", "War chest is only claimable during settlement.");
    }

    const participant = participantSnapshot.data() || {};
    if (Number(participant.countedScore || 0) <= 0) {
      throw new HttpsError("failed-precondition", "No eligible chest for this war.");
    }
    if (Number(participant.chestClaimedAt || 0) > 0) {
      throw new HttpsError("already-exists", "Chest already claimed.");
    }

    const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    const ownership = ownershipFromData(ownershipSnapshot.exists ? ownershipSnapshot.data() : null);
    const clanId = normalizeString(participant.clanId);
    const isWinner = (clanId === normalizeString(war.leftClanId) && normalizeString(war.result) === "left_win") ||
      (clanId === normalizeString(war.rightClanId) && normalizeString(war.result) === "right_win");
    const isMvp = normalizeString(war.mvpUid) === uid;
    const coinReward = isWinner ? 125 : 75;
    const fragmentReward = (isWinner ? 60 : 40) + (isMvp ? 20 : 0);
    const nextWallet = {
      ...wallet,
      coins: wallet.coins + coinReward,
    };
    const nextOwnership = {
      ...ownership,
      warFragments: ownership.warFragments + fragmentReward,
    };

    const nextBadges = [...profile.seasonBadges];
    const nextChestClaims = profile.warChestClaims + 1;
    const nextWarWins = profile.warWinCount;
    const nextMvpCount = profile.mvpCount;
    if (nextChestClaims >= 5 && !nextBadges.includes("Warborn")) nextBadges.push("Warborn");
    if (nextChestClaims >= 15 && !nextBadges.includes("Siegebreaker")) nextBadges.push("Siegebreaker");
    if (nextWarWins >= 10 && !nextBadges.includes("Clan Victor")) nextBadges.push("Clan Victor");
    if (nextMvpCount >= 3 && !nextBadges.includes("MVP Ace")) nextBadges.push("MVP Ace");

    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    transaction.set(ownershipRef, buildOwnershipWrite(nextOwnership), { merge: true });
    transaction.set(userRef, {
      warChestClaims: nextChestClaims,
      seasonBadges: nextBadges,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(participantRef, {
      chestClaimedAt: Date.now(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      warId,
      coinReward,
      fragmentReward,
      isWinner,
      isMvp,
      wallet: walletResponse(nextWallet),
      warFragments: nextOwnership.warFragments,
      seasonBadges: nextBadges,
    };
  });
});

exports.fetchPremiumCatalog = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const [walletSnapshot, ownershipSnapshot, userSnapshot] = await Promise.all([
    db.collection("wallets").doc(uid).get(),
    db.collection("ownedCosmetics").doc(uid).get(),
    db.collection("users").doc(uid).get(),
  ]);
  const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
  const ownership = ownershipFromData(ownershipSnapshot.exists ? ownershipSnapshot.data() : null);
  const profile = buildDefaultUserProfile(uid, authContext, userSnapshot.exists ? userSnapshot.data() : {});
  return {
    crowns: Number(wallet.crowns || 0),
    warFragments: ownership.warFragments,
    crownPacks: CROWN_PACKS,
    premiumItems: PREMIUM_ITEMS,
    earnedItems: EARNED_ITEMS,
    ownedPremiumIds: ownership.premiumOwnedIds,
    ownedEarnedIds: ownership.earnedOwnedIds,
    equipped: {
      titleId: profile.equippedTitleId,
      frameId: profile.equippedFrameId,
      nameplateId: profile.equippedNameplateId,
    },
  };
});

exports.purchaseCrowns = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const productId = normalizeString(request.data && request.data.productId);
  const purchaseToken = normalizeString(request.data && request.data.purchaseToken);
  const pack = findPack(productId);
  if (!pack || !purchaseToken) {
    throw new HttpsError("invalid-argument", "Purchase payload is invalid.");
  }

  // --- Server-side Google Play receipt verification ---
  // Uses the Google Play Developer API to verify the purchase is legitimate.
  // Cloud Functions uses Application Default Credentials (ADC) automatically.
  // The service account must have Play Developer API access granted in Play Console.
  const PLAY_PACKAGE = "com.triviaroyale";
  const PLAY_PRODUCT_ID_MAP = {
    crowns_80: "crowns_80",
    crowns_220: "crowns_220",
    crowns_600: "crowns_600",
    crowns_1300: "crowns_1300",
  };
  const googleProductId = PLAY_PRODUCT_ID_MAP[productId];

  try {
    const { google } = require("googleapis");
    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const androidpublisher = google.androidpublisher({ version: "v3", auth });
    const verifyResponse = await androidpublisher.purchases.products.get({
      packageName: PLAY_PACKAGE,
      productId: googleProductId || productId,
      token: purchaseToken,
    });
    const purchaseState = verifyResponse.data && verifyResponse.data.purchaseState;
    // purchaseState 0 = Purchased, 1 = Canceled, 2 = Pending
    if (purchaseState !== 0) {
      throw new HttpsError("failed-precondition", "Purchase is not in a valid state.");
    }
    functions.logger.info("[purchaseCrowns] Play receipt verified", { uid, productId });
  } catch (verifyError) {
    if (verifyError instanceof HttpsError) {
      throw verifyError;
    }
    functions.logger.error("[purchaseCrowns] Play verification failed", verifyError);
    throw new HttpsError("internal", "Could not verify purchase with Google Play.");
  }
  // --- End verification ---

  const tokenRef = db.collection("billingTokens").doc(`${uid}_${purchaseToken}`);
  const walletRef = db.collection("wallets").doc(uid);
  return db.runTransaction(async (transaction) => {
    const [tokenSnapshot, walletSnapshot] = await Promise.all([
      transaction.get(tokenRef),
      transaction.get(walletRef),
    ]);
    if (tokenSnapshot.exists) {
      throw new HttpsError("already-exists", "Purchase already processed.");
    }
    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    const nextWallet = {
      ...wallet,
      crowns: Number(wallet.crowns || 0) + Number(pack.crowns || 0),
    };
    transaction.set(tokenRef, {
      uid,
      productId,
      purchaseToken,
      verified: true,
      processedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    return {
      grantedCrowns: pack.crowns,
      wallet: walletResponse(nextWallet),
    };
  });
});

exports.purchasePremiumCosmetic = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const itemId = normalizeString(request.data && request.data.itemId);
  const item = findPremiumItem(itemId);
  const earnedItem = findEarnedItem(itemId);
  if (!item && !earnedItem) {
    throw new HttpsError("invalid-argument", "Item not found.");
  }

  const walletRef = db.collection("wallets").doc(uid);
  const ownershipRef = db.collection("ownedCosmetics").doc(uid);
  return db.runTransaction(async (transaction) => {
    const [walletSnapshot, ownershipSnapshot] = await Promise.all([
      transaction.get(walletRef),
      transaction.get(ownershipRef),
    ]);
    const ownership = ownershipFromData(ownershipSnapshot.exists ? ownershipSnapshot.data() : null);
    if (item) {
      const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
      const contents = Array.isArray(item.contents) ? item.contents : [item.id];
      if (contents.every((ownedId) => ownership.premiumOwnedIds.includes(ownedId))) {
        throw new HttpsError("already-exists", "You already own this cosmetic.");
      }
      if (Number(wallet.crowns || 0) < Number(item.priceCrowns || 0)) {
        throw new HttpsError("failed-precondition", "Not enough Crowns.");
      }
      const nextWallet = {
        ...wallet,
        crowns: Number(wallet.crowns || 0) - Number(item.priceCrowns || 0),
      };
      const nextOwnership = {
        ...ownership,
        premiumOwnedIds: [...new Set([...ownership.premiumOwnedIds, ...contents])],
      };
      transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
      transaction.set(ownershipRef, buildOwnershipWrite(nextOwnership), { merge: true });
      return {
        wallet: walletResponse(nextWallet),
        ownedPremiumIds: nextOwnership.premiumOwnedIds,
        ownedEarnedIds: nextOwnership.earnedOwnedIds,
        warFragments: nextOwnership.warFragments,
      };
    }

    if (ownership.earnedOwnedIds.includes(earnedItem.id)) {
      throw new HttpsError("already-exists", "You already own this item.");
    }
    if (ownership.warFragments < Number(earnedItem.fragmentsRequired || 0)) {
      throw new HttpsError("failed-precondition", "Not enough fragments.");
    }
    const nextOwnership = {
      ...ownership,
      earnedOwnedIds: [...ownership.earnedOwnedIds, earnedItem.id],
      warFragments: ownership.warFragments - Number(earnedItem.fragmentsRequired || 0),
    };
    transaction.set(ownershipRef, buildOwnershipWrite(nextOwnership), { merge: true });
    return {
      wallet: walletResponse(walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null)),
      ownedPremiumIds: nextOwnership.premiumOwnedIds,
      ownedEarnedIds: nextOwnership.earnedOwnedIds,
      warFragments: nextOwnership.warFragments,
    };
  });
});

exports.equipPremiumCosmetic = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const slot = normalizeString(request.data && request.data.slot);
  const itemId = normalizeString(request.data && request.data.itemIdOrNull);
  const slotMap = {
    title: "equippedTitleId",
    frame: "equippedFrameId",
    nameplate: "equippedNameplateId",
  };
  const field = slotMap[slot];
  if (!field) {
    throw new HttpsError("invalid-argument", "Invalid slot.");
  }

  const ownershipSnapshot = await db.collection("ownedCosmetics").doc(uid).get();
  const ownership = ownershipFromData(ownershipSnapshot.exists ? ownershipSnapshot.data() : null);
  if (itemId && !ownership.premiumOwnedIds.includes(itemId)) {
    throw new HttpsError("failed-precondition", "Item not owned.");
  }
  await db.collection("users").doc(uid).set({
    [field]: itemId,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  return {
    slot,
    itemIdOrNull: itemId,
  };
});

exports.startVerifiedDailyChallenge = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const setRef = db.collection("dailyChallengeSets").doc("current");
  const setSnapshot = await setRef.get();
  if (!setSnapshot.exists) {
    throw new HttpsError("failed-precondition", "Daily challenge is not configured yet.");
  }

  const setData = setSnapshot.data() || {};
  const dateKey = normalizeString(setData.dateKey) || todayDateKey();
  const questions = Array.isArray(setData.questions) ? setData.questions : [];
  if (questions.length < 5) {
    throw new HttpsError("failed-precondition", "Daily challenge set is incomplete.");
  }

  const sessionId = `${uid}_${dateKey}`;
  const sessionRef = db.collection("dailyChallengeSessions").doc(sessionId);
  const sessionSnapshot = await sessionRef.get();
  const existing = sessionSnapshot.exists ? sessionSnapshot.data() || {} : {};
  if (existing.status === "submitted") {
    throw new HttpsError("already-exists", "You already submitted a verified entry today.");
  }

  if (existing.status !== "started") {
    await sessionRef.set({
      uid,
      dateKey,
      title: normalizeString(setData.title) || "Verified Daily Challenge",
      answerKey: questions.map((question) => Number(question.answer || 0)),
      questionCount: questions.length,
      status: "started",
      startedAt: FieldValue.serverTimestamp(),
      endedAt: null,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  }

  return {
    sessionId,
    dateKey,
    title: normalizeString(setData.title) || "Verified Daily Challenge",
    questions: sanitizeChallengeQuestions(questions),
  };
});

exports.submitVerifiedDailyChallenge = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  const sessionId = normalizeString(request.data && request.data.sessionId);
  const answers = Array.isArray(request.data && request.data.answers)
    ? request.data.answers.map((value) => Number(value))
    : [];
  if (!sessionId) {
    throw new HttpsError("invalid-argument", "Missing session id.");
  }

  const sessionRef = db.collection("dailyChallengeSessions").doc(sessionId);

  return db.runTransaction(async (transaction) => {
    const sessionSnapshot = await transaction.get(sessionRef);
    if (!sessionSnapshot.exists) {
      throw new HttpsError("not-found", "Daily challenge session not found.");
    }

    const session = sessionSnapshot.data() || {};
    if (normalizeString(session.uid) !== uid) {
      throw new HttpsError("permission-denied", "This session does not belong to you.");
    }
    if (session.status === "submitted") {
      throw new HttpsError("already-exists", "This daily challenge was already submitted.");
    }

    const answerKey = Array.isArray(session.answerKey) ? session.answerKey.map((value) => Number(value)) : [];
    if (answers.length !== answerKey.length || answerKey.length === 0) {
      throw new HttpsError("invalid-argument", "Answer payload is incomplete.");
    }

    const startedAt = session.startedAt && typeof session.startedAt.toMillis === "function"
      ? session.startedAt.toMillis()
      : Date.now();
    const elapsedMillis = Math.max(0, Date.now() - startedAt);
    if (elapsedMillis < 11000) {
      throw new HttpsError("failed-precondition", "Submission was too fast to verify.");
    }
    if (elapsedMillis > 150000) {
      throw new HttpsError("failed-precondition", "Submission window expired.");
    }

    let correctAnswers = 0;
    let bestCorrectStreak = 0;
    let currentStreak = 0;
    answerKey.forEach((answer, index) => {
      if (answers[index] === answer) {
        correctAnswers += 1;
        currentStreak += 1;
        bestCorrectStreak = Math.max(bestCorrectStreak, currentStreak);
      } else {
        currentStreak = 0;
      }
    });

    if (correctAnswers === answerKey.length && elapsedMillis < answerKey.length * 2500) {
      throw new HttpsError("failed-precondition", "Perfect submissions require more realistic completion time.");
    }

    const profileRef = db.collection("users").doc(uid);
    const statsRef = db.collection("dailyPlayerStats").doc(uid);
    const resultRef = db.collection("verifiedDailyEntries").doc(normalizeString(session.dateKey) || todayDateKey())
      .collection("players").doc(uid);
    const [profileSnapshot, statsSnapshot, systemSnapshot] = await Promise.all([
      transaction.get(profileRef),
      transaction.get(statsRef),
      transaction.get(systemRef()),
    ]);

    const displayName = sanitizeDisplayName(
      profileSnapshot.get("displayName") || profileSnapshot.get("username") || authContext.token.name || "Player"
    ) || "Player";
    const accuracy = accuracyFor(correctAnswers, answerKey.length);
    const rp = rpFor(correctAnswers, elapsedMillis);
    const dateKey = normalizeString(session.dateKey) || todayDateKey();
    const profile = buildDefaultUserProfile(uid, authContext, profileSnapshot.exists ? profileSnapshot.data() : {});
    const systemState = systemSnapshot.exists ? (systemSnapshot.data() || {}) : buildDefaultSystem();

    transaction.set(sessionRef, {
      status: "submitted",
      correctAnswers,
      elapsedMillis,
      bestCorrectStreak,
      endedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    transaction.set(resultRef, {
      uid,
      dateKey,
      displayName,
      clanTag: profile.clanTag,
      equippedTitleId: profile.equippedTitleId,
      equippedFrameId: profile.equippedFrameId,
      equippedNameplateId: profile.equippedNameplateId,
      correctAnswers,
      elapsedMillis,
      accuracy,
      rp,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    const currentStats = normalizePlayerStats(
      uid,
      statsSnapshot.exists ? statsSnapshot.data() : null,
      dateKey,
    );
    const gameplayStats = applyGameplaySession(currentStats, {
      sessionId: `daily:${sessionId}`,
      sessionType: "daily_challenge",
      questionsAnswered: answerKey.length,
      correctAnswers,
      durationSeconds: Math.max(1, Math.floor(elapsedMillis / 1000)),
      playSeconds: Math.max(1, Math.floor(elapsedMillis / 1000)),
      didWin: accuracy >= 50,
      genre: "Daily Challenge",
      bestCorrectStreak: correctAnswers,
    });
    if (!gameplayStats.duplicate) {
      transaction.set(statsRef, buildPlayerStatsWrite(gameplayStats.stats), { merge: true });
    }

    const seasonRowRef = seasonPlayerRef(systemState.seasonId || makeSeasonId(1), uid);
    const seasonSnapshot = await transaction.get(seasonRowRef);
    const seasonRow = buildSeasonRow(uid, profile, seasonSnapshot.exists ? seasonSnapshot.data() : {});
    if (profile.clanId) {
      seasonRow.dailyCp += computeSessionCp({
        sessionType: "daily_challenge",
        correctAnswers,
        questionsAnswered: answerKey.length,
        durationSeconds: Math.max(1, Math.floor(elapsedMillis / 1000)),
        bestCorrectStreak,
      });
      seasonRow.totalCp = seasonRow.dailyCp + seasonRow.warCp + seasonRow.taskCp;
    } else {
      seasonRow.dailyCp = clampSoloSeasonCp(
        seasonRow.dailyCp + computeSessionCp({
          sessionType: "daily_challenge",
          correctAnswers,
          questionsAnswered: answerKey.length,
          durationSeconds: Math.max(1, Math.floor(elapsedMillis / 1000)),
          bestCorrectStreak,
        })
      );
      seasonRow.totalCp = clampSoloSeasonCp(seasonRow.dailyCp + seasonRow.warCp + seasonRow.taskCp);
      seasonRow.soloCapApplied = seasonRow.totalCp >= SOLO_SEASON_CP_CAP;
    }
    transaction.set(seasonRowRef, seasonRow, { merge: true });

    return {
      correctAnswers,
      elapsedMillis,
      bestCorrectStreak,
      leaderboardRank: null,
    };
  });
});

exports.fetchVerifiedDailyLeaderboard = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  requireAuth(request);

  const dateKey = todayDateKey();

  // Still read the current challenge set for the display title only.
  const setRef = db.collection("dailyChallengeSets").doc("current");
  const setSnapshot = await setRef.get();
  const title = setSnapshot.exists
    ? (normalizeString((setSnapshot.data() || {}).title) || "Verified Daily Challenge")
    : "Verified Daily Challenge";

  const leaderboardSnapshot = await db
    .collection("verifiedDailyEntries")
    .doc(dateKey)
    .collection("players")
    .orderBy("correctAnswers", "desc")
    .orderBy("elapsedMillis", "asc")
    .limit(VERIFIED_LEADERBOARD_LIMIT)
    .get();

  return {
    title,
    dateKey,
    entries: leaderboardSnapshot.docs.map((doc) => ({
      uid: normalizeString(doc.get("uid")) || doc.id,
      displayName: sanitizeDisplayName(doc.get("displayName")) || "Player",
      correctAnswers: Number(doc.get("correctAnswers") || 0),
      elapsedMillis: Number(doc.get("elapsedMillis") || 0),
      accuracy: Number(doc.get("accuracy") || 0),
      rp: Number(doc.get("rp") || 0),
    })),
  };
});

exports.purchaseCosmeticReward = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "purchaseCosmeticReward");
  const input = request.data || {};
  const cosmeticId = normalizeString(input.cosmeticId);
  const cosmetic = findCosmetic(cosmeticId);

  if (!cosmetic) {
    throw new HttpsError("invalid-argument", "The selected cosmetic reward is not available.");
  }

  const walletRef = db.collection("wallets").doc(uid);
  const cosmeticsRef = db.collection("cosmetics").doc(uid);

  return db.runTransaction(async (transaction) => {
    const [walletSnapshot, cosmeticsSnapshot] = await Promise.all([
      transaction.get(walletRef),
      transaction.get(cosmeticsRef),
    ]);

    // Check if already owned
    const ownedIds = cosmeticsSnapshot.exists
      ? (cosmeticsSnapshot.data().ownedIds || [])
      : [];
    if (ownedIds.includes(cosmeticId)) {
      throw new HttpsError("already-exists", "You already own this cosmetic.");
    }

    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    if (wallet.coins < cosmetic.coinsRequired) {
      throw new HttpsError("failed-precondition", "You do not have enough coins.");
    }

    const nextWallet = {
      uid,
      coins: wallet.coins - cosmetic.coinsRequired,
      lastValidatedDayKey: wallet.lastValidatedDayKey,
      validatedEarnedToday: wallet.validatedEarnedToday,
      suspiciousCount: wallet.suspiciousCount,
    };
    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    transaction.set(cosmeticsRef, {
      ownedIds: [...ownedIds, cosmeticId],
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      cosmeticId,
      remainingCoins: nextWallet.coins,
    };
  });
});

exports.fetchOwnedCosmetics = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;

  const cosmeticsDoc = await db.collection("cosmetics").doc(uid).get();
  const ownedIds = cosmeticsDoc.exists ? (cosmeticsDoc.data().ownedIds || []) : [];

  return { cosmeticIds: ownedIds };
});

exports.deleteAccount = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;

  const userRef = db.collection("users").doc(uid);
  const walletRef = db.collection("wallets").doc(uid);
  const backupRef = db.collection("gameBackups").doc(uid);
  const statsRef = db.collection("dailyPlayerStats").doc(uid);
  const cosmeticsRef = db.collection("cosmetics").doc(uid);

  await Promise.all([
    deleteDocumentsInBatches(db.collection("dailyChallengeSessions").where("uid", "==", uid)),
    deleteDocumentsInBatches(db.collectionGroup("players").where("uid", "==", uid)),
    userRef.delete().catch(() => {}),
    walletRef.delete().catch(() => {}),
    backupRef.delete().catch(() => {}),
    statsRef.delete().catch(() => {}),
    cosmeticsRef.delete().catch(() => {}),
  ]);

  const deviceSnapshot = await db
    .collection("deviceRegistrations")
    .where("accountIds", "array-contains", uid)
    .get();

  await Promise.all(deviceSnapshot.docs.map(async (doc) => {
    const accountIds = Array.isArray(doc.get("accountIds"))
      ? doc.get("accountIds").map((value) => normalizeString(value)).filter(Boolean)
      : [];
    const nextAccountIds = accountIds.filter((value) => value !== uid);
    await doc.ref.set({
      accountIds: nextAccountIds,
      accountCount: nextAccountIds.length,
      blocked: false,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  }));

  await auth.deleteUser(uid);

  return { success: true };
});

exports.purchaseAbility = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "purchaseAbility");
  const input = request.data || {};
  const abilityId = normalizeString(input.abilityId);
  const ability = findAbility(abilityId);

  if (!ability) {
    throw new HttpsError("invalid-argument", "The selected ability is not available.");
  }

  const walletRef = db.collection("wallets").doc(uid);
  const abilitiesRef = db.collection("abilities").doc(uid);

  return db.runTransaction(async (transaction) => {
    const [walletSnapshot, abilitiesSnapshot] = await Promise.all([
      transaction.get(walletRef),
      transaction.get(abilitiesRef),
    ]);

    // Check if ability is already active
    const activeAbilities = abilitiesSnapshot.exists
      ? (abilitiesSnapshot.data().active || [])
      : [];
    const nowMillis = Date.now();
    const alreadyActive = activeAbilities.some(
      (a) => a.effect === ability.effect && a.expiresAt > nowMillis,
    );
    if (alreadyActive) {
      throw new HttpsError("already-exists", "This ability is already active.");
    }

    const wallet = walletFromData(uid, walletSnapshot.exists ? walletSnapshot.data() : null);
    if (wallet.coins < ability.coinsRequired) {
      throw new HttpsError("failed-precondition", "You do not have enough coins.");
    }

    const expiresAt = nowMillis + (ability.durationDays * 24 * 60 * 60 * 1000);
    const newAbility = {
      id: ability.id,
      effect: ability.effect,
      name: ability.name,
      activatedAt: nowMillis,
      expiresAt,
    };

    // Remove expired abilities and add new one
    const updatedAbilities = activeAbilities
      .filter((a) => a.expiresAt > nowMillis)
      .concat(newAbility);

    const nextWallet = {
      uid,
      coins: wallet.coins - ability.coinsRequired,
      lastValidatedDayKey: wallet.lastValidatedDayKey,
      validatedEarnedToday: wallet.validatedEarnedToday,
      suspiciousCount: wallet.suspiciousCount,
    };

    transaction.set(walletRef, buildWalletWrite(nextWallet), { merge: true });
    transaction.set(abilitiesRef, {
      active: updatedAbilities,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return {
      abilityId: ability.id,
      abilityName: ability.name,
      effect: ability.effect,
      expiresAt,
      remainingCoins: nextWallet.coins,
    };
  });
});

exports.fetchActiveAbilities = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;

  const abilitiesDoc = await db.collection("abilities").doc(uid).get();
  const allAbilities = abilitiesDoc.exists ? (abilitiesDoc.data().active || []) : [];
  const nowMillis = Date.now();
  const activeAbilities = allAbilities.filter((a) => a.expiresAt > nowMillis);

  return {
    abilities: activeAbilities.map((a) => ({
      id: normalizeString(a.id),
      effect: normalizeString(a.effect),
      name: normalizeString(a.name),
      activatedAt: Number(a.activatedAt || 0),
      expiresAt: Number(a.expiresAt || 0),
    })),
  };
});

// ── Dynamic Quiz Questions ────────────────────────────────────────────────────

exports.getQuizQuestions = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  await enforceRateLimit(db, authContext.uid, "getQuizQuestions", { maxPerMinute: 30 });

  const genre = normalizeString(request.data.genre);
  const category = normalizeString(request.data.category) || null;
  const count = Math.min(Math.max(Number(request.data.count) || 10, 1), 50);
  const recentFirst = Boolean(request.data && request.data.recentFirst);
  const excludeHashes = Array.isArray(request.data.excludeHashes)
    ? request.data.excludeHashes.filter((h) => typeof h === "string").slice(0, 200)
    : [];

  if (!genre) {
    throw new HttpsError("invalid-argument", "Genre is required.");
  }

  const questions = await fetchQuizQuestions(db, { genre, category, count, excludeHashes, recentFirst });

  return {
    questions: questions,
    genre: genre,
    category: category || "",
    count: questions.length,
  };
});

exports.getQuizMetadata = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  await enforceRateLimit(db, authContext.uid, "getQuizMetadata", { maxPerMinute: 10 });

  const genre = normalizeString(request.data.genre) || null;
  const metadata = await fetchQuizMetadata(db, genre);

  return { metadata };
});

exports.adminSeedQuizQuestions = onCall({ region: REGION }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;

  // Only allow specific admin UIDs
  const adminDoc = await db.collection("admins").doc(uid).get();
  if (!adminDoc.exists) {
    throw new HttpsError("permission-denied", "Admin access required.");
  }

  const genres = Array.isArray(request.data.genres) ? request.data.genres : undefined;
  const questionsPerCategory = Math.min(Math.max(Number(request.data.questionsPerCategory) || 50, 10), 50);

  const result = await seedQuizQuestions(db, { questionsPerCategory, genres });

  return result;
});

exports.warCycleScheduler = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Kolkata",
    region: REGION,
    timeoutSeconds: 540,
    memory: "256MiB",
  },
  async () => {
    const nowMillis = Date.now();
    const ref = systemRef();
    const snapshot = await ref.get();
    if (!snapshot.exists) {
      const initialState = buildDefaultSystem(nowMillis);
      await ref.set(initialState, { merge: true });
      await pairEligibleClans(initialState);
      return;
    }

    const current = snapshot.data() || buildDefaultSystem(nowMillis);
    const previousPhase = normalizeString(current.phase) || "prep";
    let nextPhase = "prep";
    if (nowMillis >= Number(current.battleEndsAt || 0)) {
      nextPhase = nowMillis >= Number(current.settlementEndsAt || 0) ? "rollover" : "settlement";
    } else if (nowMillis >= Number(current.battleStartsAt || 0)) {
      nextPhase = "battle";
    }

    if (nextPhase === "rollover") {
      await settleCurrentWarsForCycle(current);
      if (Number(current.currentWarIndex || 0) >= SEASON_WARS) {
        await grantSeasonBadges(current);
      }
      const nextState = buildSystemState(current, nowMillis);
      await ref.set(nextState, { merge: true });
      await pairEligibleClans(nextState);
      return;
    }

    await ref.set({ ...current, phase: nextPhase, updatedAt: nowMillis }, { merge: true });
    if (previousPhase !== nextPhase && nextPhase === "settlement") {
      await settleCurrentWarsForCycle(current);
    }
    if (previousPhase !== nextPhase && nextPhase === "battle") {
      const clansSnapshot = await db.collection("clans")
        .where("currentWarId", "!=", "")
        .get();
      const batch = db.batch();
      clansSnapshot.docs.forEach((doc) => {
        const clan = clanFromSnapshot(doc);
        batch.set(doc.ref, buildClanWrite({
          ...clan,
          battleJoinRecommended: clan.memberCount < MAX_CLAN_SIZE,
        }), { merge: true });
      });
      await batch.commit();
    }
  }
);

exports.activeMemberExpirySweep = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Asia/Kolkata",
    region: REGION,
    timeoutSeconds: 540,
    memory: "256MiB",
  },
  async () => {
    const nowMillis = Date.now();
    const snapshot = await db.collectionGroup("members")
      .where("activeCounted", "==", true)
      .where("activeUntil", "<", nowMillis)
      .limit(200)
      .get();
    if (snapshot.empty) {
      return;
    }

    const batch = db.batch();
    for (const doc of snapshot.docs) {
      const clanId = normalizeString(doc.get("clanId"));
      if (!clanId) continue;
      const clanDoc = await db.collection("clans").doc(clanId).get();
      if (!clanDoc.exists) continue;
      const clan = clanFromSnapshot(clanDoc);
      batch.set(doc.ref, {
        activeCounted: false,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      batch.set(clanDoc.ref, buildClanWrite({
        ...clan,
        activeMemberCount7d: Math.max(0, clan.activeMemberCount7d - 1),
      }), { merge: true });
    }
    await batch.commit();
  }
);

// ── Scheduled Daily Quiz Refresh ──────────────────────────────────────────────
// Runs daily at 3:00 AM IST — pulls 10 fresh questions per category from OpenTDB
// and cleans up questions older than 30 days to keep the collection lean.
exports.scheduledQuizRefresh = onSchedule(
  {
    schedule: "0 3 * * *",
    timeZone: "Asia/Kolkata",
    region: REGION,
    timeoutSeconds: 540,
    memory: "256MiB",
  },
  async () => {
    const logger = require("firebase-functions/logger");
    logger.info("[scheduledQuizRefresh] Starting daily quiz refresh...");

    // Check if already seeded today (within 20 hours)
    const metaSnapshot = await db.collection("quiz_metadata").limit(1).get();
    if (!metaSnapshot.empty) {
      const lastSeedAt = metaSnapshot.docs[0].data().lastSeedAt || 0;
      const hoursSinceLastSeed = (Date.now() - lastSeedAt) / (1000 * 60 * 60);
      if (hoursSinceLastSeed < 20) {
        logger.info(`[scheduledQuizRefresh] Already seeded ${hoursSinceLastSeed.toFixed(1)}h ago, skipping.`);
        return;
      }
    }

    try {
      // Seed 10 questions per category (small batch, rate-limit safe)
      const result = await seedQuizQuestions(db, { questionsPerCategory: 10 });
      logger.info(`[scheduledQuizRefresh] Seeded ${result.seeded} questions, skipped ${result.skippedDuplicates} duplicates.`);
      if (result.errors.length > 0) {
        logger.warn(`[scheduledQuizRefresh] Errors: ${result.errors.join("; ")}`);
      }
    } catch (err) {
      logger.error(`[scheduledQuizRefresh] Seed failed: ${err.message}`);
    }

    // Cleanup: delete questions older than 30 days to keep collection lean
    try {
      const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);
      const oldQuestionsQuery = db.collection("quiz_questions")
        .where("createdAt", "<", thirtyDaysAgo)
        .limit(200);

      let deletedCount = 0;
      let snapshot = await oldQuestionsQuery.get();
      while (!snapshot.empty) {
        const batch = db.batch();
        snapshot.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        deletedCount += snapshot.size;
        if (snapshot.size < 200) break;
        snapshot = await oldQuestionsQuery.get();
      }

      if (deletedCount > 0) {
        logger.info(`[scheduledQuizRefresh] Cleaned up ${deletedCount} old questions.`);
      }
    } catch (err) {
      logger.warn(`[scheduledQuizRefresh] Cleanup failed: ${err.message}`);
    }
  }
);

exports.fetchSeasonHistory = onCall({ region: REGION, enforceAppCheck: ENFORCE_APP_CHECK }, async (request) => {
  const authContext = requireAuth(request);
  const uid = authContext.uid;
  await enforceRateLimit(uid, "fetchSeasonHistory");

  const userRef = db.collection("users").doc(uid);
  const userSnapshot = await userRef.get();
  const raw = userSnapshot.exists ? userSnapshot.data() || {} : {};

  return {
    lastSeasonFinish: Number(raw.lastSeasonFinish || 0),
    seasonBadges: Array.isArray(raw.seasonBadges) ? raw.seasonBadges.map(normalizeString).filter(Boolean) : [],
    warParticipationCount: Number(raw.warParticipationCount || 0),
    warWinCount: Number(raw.warWinCount || 0),
    mvpCount: Number(raw.mvpCount || 0),
    contributionStreak: Number(raw.contributionStreak || 0),
    lifetimeContribution: Number(raw.lifetimeContribution || 0),
    warChestClaims: Number(raw.warChestClaims || 0),
  };
});
