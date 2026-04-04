const MAX_CLAN_SIZE = 30;
const MIN_ACTIVE_CLAN_SIZE = 3;
const MAX_COUNTED_CONTRIBUTORS = 20;
const MAX_COUNTED_RUNS = 3;
const SOLO_SEASON_CP_CAP = 1799;
const SEASON_WARS = 8;
const ACTIVE_WINDOW_MILLIS = 7 * 24 * 60 * 60 * 1000;
const JOIN_COOLDOWN_MILLIS = 24 * 60 * 60 * 1000;
const WAR_PHASE_DURATIONS = {
  prepMillis: 24 * 60 * 60 * 1000,
  battleMillis: 48 * 60 * 60 * 1000,
  settlementMillis: 12 * 60 * 60 * 1000,
};

const MODE_DURATION_CAPS = {
  genre: 300,
  lightning: 240,
  grand_master: 300,
  daily_challenge: 180,
};

const MODE_CP_BONUS = {
  genre: 0,
  lightning: 15,
  grand_master: 25,
  daily_challenge: 35,
};

const CROWN_PACKS = [
  { id: "crowns_80", name: "80 Crowns", crowns: 80, priceInr: 79 },
  { id: "crowns_220", name: "220 Crowns", crowns: 220, priceInr: 199 },
  { id: "crowns_600", name: "600 Crowns", crowns: 600, priceInr: 499 },
  { id: "crowns_1300", name: "1300 Crowns", crowns: 1300, priceInr: 999 },
];

const PREMIUM_ITEMS = [
  { id: "nameplate_neon_pulse", type: "NAMEPLATE", name: "Neon Pulse", priceCrowns: 120, rarity: "Rare" },
  { id: "nameplate_emerald_warband", type: "NAMEPLATE", name: "Emerald Warband", priceCrowns: 120, rarity: "Rare" },
  { id: "nameplate_crimson_strike", type: "NAMEPLATE", name: "Crimson Strike", priceCrowns: 120, rarity: "Epic" },
  { id: "nameplate_royal_carbon", type: "NAMEPLATE", name: "Royal Carbon", priceCrowns: 120, rarity: "Epic" },
  { id: "frame_gold_edge", type: "FRAME", name: "Gold Edge", priceCrowns: 150, rarity: "Rare" },
  { id: "frame_frost_halo", type: "FRAME", name: "Frost Halo", priceCrowns: 150, rarity: "Rare" },
  { id: "frame_ember_crest", type: "FRAME", name: "Ember Crest", priceCrowns: 150, rarity: "Epic" },
  { id: "frame_obsidian_arc", type: "FRAME", name: "Obsidian Arc", priceCrowns: 150, rarity: "Epic" },
  { id: "title_first_blood", type: "TITLE", name: "First Blood", priceCrowns: 90, rarity: "Common" },
  { id: "title_clan_closer", type: "TITLE", name: "Clan Closer", priceCrowns: 90, rarity: "Rare" },
  { id: "title_perfect_runner", type: "TITLE", name: "Perfect Runner", priceCrowns: 90, rarity: "Rare" },
  { id: "title_night_raider", type: "TITLE", name: "Night Raider", priceCrowns: 90, rarity: "Epic" },
  { id: "streak_lightning_trail", type: "STREAK_EFFECT", name: "Lightning Trail", priceCrowns: 180, rarity: "Epic" },
  { id: "streak_ember_burst", type: "STREAK_EFFECT", name: "Ember Burst", priceCrowns: 180, rarity: "Epic" },
  { id: "streak_aurora_sweep", type: "STREAK_EFFECT", name: "Aurora Sweep", priceCrowns: 180, rarity: "Legendary" },
  { id: "card_victory_glitch", type: "RESULT_CARD", name: "Victory Glitch", priceCrowns: 220, rarity: "Epic" },
  { id: "card_monarch_shine", type: "RESULT_CARD", name: "Monarch Shine", priceCrowns: 220, rarity: "Legendary" },
  { id: "card_phantom_glass", type: "RESULT_CARD", name: "Phantom Glass", priceCrowns: 220, rarity: "Epic" },
  {
    id: "bundle_starter",
    type: "BUNDLE",
    name: "Starter Bundle",
    priceCrowns: 300,
    contents: ["nameplate_neon_pulse", "frame_gold_edge", "title_first_blood"],
    rarity: "Rare",
  },
  {
    id: "bundle_founder_social",
    type: "BUNDLE",
    name: "Founder Social Pack",
    priceCrowns: 420,
    contents: ["nameplate_royal_carbon", "frame_obsidian_arc", "streak_lightning_trail", "title_clan_closer"],
    rarity: "Legendary",
  },
];

const EARNED_ITEMS = [
  { id: "earned_title_frontliner", type: "TITLE", name: "Frontliner", fragmentsRequired: 100, rarity: "Rare" },
  { id: "earned_frame_warband_edge", type: "FRAME", name: "Warband Edge", fragmentsRequired: 150, rarity: "Epic" },
  { id: "earned_nameplate_militia_signal", type: "NAMEPLATE", name: "Militia Signal", fragmentsRequired: 100, rarity: "Rare" },
  { id: "earned_card_final_blow", type: "RESULT_CARD", name: "Final Blow", fragmentsRequired: 200, rarity: "Legendary" },
];

function normalizeString(value) {
  return String(value || "").trim();
}

function normalizeClanName(value) {
  return normalizeString(value).replace(/\s+/g, " ").slice(0, 24);
}

function normalizeClanTag(value) {
  return normalizeString(value).replace(/[^A-Za-z0-9]/g, "").toUpperCase().slice(0, 6);
}

function activeBandForCount(count) {
  if (count >= 19) return "large";
  if (count >= 9) return "medium";
  if (count >= 3) return "small";
  return "inactive";
}

function computeSessionCp(session) {
  const accuracyPoints = Math.round((session.correctAnswers / Math.max(1, session.questionsAnswered)) * 100);
  const durationCap = MODE_DURATION_CAPS[session.sessionType] || 300;
  const speedPoints = Math.round(Math.max(0, 1 - (session.durationSeconds / durationCap)) * 40);
  const streakPoints = Math.min(session.bestCorrectStreak || 0, 10) * 3;
  const modeBonus = MODE_CP_BONUS[session.sessionType] || 0;
  return accuracyPoints + speedPoints + streakPoints + modeBonus;
}

function insertTopContribution(existingScores = [], score) {
  return [...existingScores, Number(score || 0)]
    .filter((value) => Number.isFinite(value) && value > 0)
    .sort((left, right) => right - left)
    .slice(0, MAX_COUNTED_RUNS);
}

function sumScores(scores = []) {
  return scores.reduce((sum, value) => sum + Number(value || 0), 0);
}

function clampSoloSeasonCp(value) {
  return Math.min(SOLO_SEASON_CP_CAP, Math.max(0, Number(value || 0)));
}

function makeSeasonId(seasonIndex) {
  return `season-${seasonIndex}`;
}

function makeWarId(seasonIndex, warIndex) {
  return `war-${seasonIndex}-${warIndex}`;
}

function initializeWarSchedule(nowMillis = Date.now()) {
  const prepStartsAt = nowMillis;
  const battleStartsAt = prepStartsAt + WAR_PHASE_DURATIONS.prepMillis;
  const battleEndsAt = battleStartsAt + WAR_PHASE_DURATIONS.battleMillis;
  const settlementEndsAt = battleEndsAt + WAR_PHASE_DURATIONS.settlementMillis;
  return {
    phase: "prep",
    prepStartsAt,
    battleStartsAt,
    battleEndsAt,
    settlementEndsAt,
  };
}

function resolveWarPhase(systemState, nowMillis = Date.now()) {
  if (!systemState) {
    return initializeWarSchedule(nowMillis);
  }
  if (nowMillis < systemState.battleStartsAt) {
    return { ...systemState, phase: "prep" };
  }
  if (nowMillis < systemState.battleEndsAt) {
    return { ...systemState, phase: "battle" };
  }
  if (nowMillis < systemState.settlementEndsAt) {
    return { ...systemState, phase: "settlement" };
  }
  return { ...systemState, phase: "rollover" };
}

function buildSystemState(previous = null, nowMillis = Date.now()) {
  if (!previous) {
    const schedule = initializeWarSchedule(nowMillis);
    return {
      seasonIndex: 1,
      currentWarIndex: 1,
      seasonId: makeSeasonId(1),
      warId: makeWarId(1, 1),
      ...schedule,
      updatedAt: nowMillis,
    };
  }

  let seasonIndex = Number(previous.seasonIndex || 1);
  let currentWarIndex = Number(previous.currentWarIndex || 1) + 1;
  if (currentWarIndex > SEASON_WARS) {
    currentWarIndex = 1;
    seasonIndex += 1;
  }
  const schedule = initializeWarSchedule(nowMillis);
  return {
    seasonIndex,
    currentWarIndex,
    seasonId: makeSeasonId(seasonIndex),
    warId: makeWarId(seasonIndex, currentWarIndex),
    ...schedule,
    updatedAt: nowMillis,
  };
}

module.exports = {
  ACTIVE_WINDOW_MILLIS,
  CROWN_PACKS,
  EARNED_ITEMS,
  JOIN_COOLDOWN_MILLIS,
  MAX_CLAN_SIZE,
  MAX_COUNTED_CONTRIBUTORS,
  MAX_COUNTED_RUNS,
  MIN_ACTIVE_CLAN_SIZE,
  PREMIUM_ITEMS,
  SEASON_WARS,
  SOLO_SEASON_CP_CAP,
  WAR_PHASE_DURATIONS,
  activeBandForCount,
  buildSystemState,
  clampSoloSeasonCp,
  computeSessionCp,
  initializeWarSchedule,
  insertTopContribution,
  makeSeasonId,
  makeWarId,
  normalizeClanName,
  normalizeClanTag,
  normalizeString,
  resolveWarPhase,
  sumScores,
};
