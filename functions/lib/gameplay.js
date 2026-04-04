const SUPPORTED_SESSION_TYPES = new Set([
  "genre",
  "grand_master",
  "daily_challenge",
  "lightning",
]);

const SESSION_LIMITS = {
  genre: { maxQuestions: 10, maxDurationSeconds: 300 },
  grand_master: { maxQuestions: 10, maxDurationSeconds: 300 },
  daily_challenge: { maxQuestions: 8, maxDurationSeconds: 180 },
  lightning: { maxQuestions: 30, maxDurationSeconds: 240 },
};

const FIXED_TASKS = [
  {
    id: "grand-master-daily",
    title: "Grand Master Run",
    description: "Play today's Grand Master quiz once.",
    metric: "GRAND_MASTER_COMPLETED",
    target: 1,
    rewardCoins: 150,
    fixed: true,
  },
  {
    id: "play-30-mins",
    title: "Thirty-Minute Focus",
    description: "Spend 30 minutes actively answering questions today.",
    metric: "PLAY_SECONDS",
    target: 1800,
    rewardCoins: 200,
    fixed: true,
  },
  {
    id: "daily-challenger",
    title: "Daily Challenger",
    description: "Finish today's verified daily challenge.",
    metric: "DAILY_CHALLENGE_PLAYED",
    target: 1,
    rewardCoins: 180,
    fixed: true,
  },
];

const RANDOM_TASK_POOL = [
  {
    id: "quiz-sprint",
    title: "Warm-Up Circuit",
    description: "Finish 25 quizzes today.",
    metric: "QUIZZES_PLAYED",
    target: 25,
    rewardCoins: 120,
    fixed: false,
  },
  {
    id: "answer-storm",
    title: "Answer Storm",
    description: "Answer 25 questions today.",
    metric: "QUESTIONS_ANSWERED",
    target: 25,
    rewardCoins: 100,
    fixed: false,
  },
  {
    id: "precision-pass",
    title: "Precision Pass",
    description: "Get 100 answers correct today.",
    metric: "CORRECT_ANSWERS",
    target: 100,
    rewardCoins: 150,
    fixed: false,
  },
  {
    id: "flash-tap",
    title: "Flash Tap",
    description: "Finish 1 Lightning Round today.",
    metric: "LIGHTNING_ROUNDS",
    target: 1,
    rewardCoins: 90,
    fixed: false,
  },
  {
    id: "hour-grind",
    title: "Hour Grind",
    description: "Answer 100 questions today.",
    metric: "QUESTIONS_ANSWERED",
    target: 100,
    rewardCoins: 100,
    fixed: false,
  },
  {
    id: "steady-hand",
    title: "Steady Hand",
    description: "Win 5 quizzes today.",
    metric: "QUIZZES_WON",
    target: 5,
    rewardCoins: 130,
    fixed: false,
  },
];

const IPL_TASK_POOL = [
  {
    id: "ipl-double-header",
    title: "Powerplay Double Header",
    description: "Finish 20 IPL quizzes today.",
    metric: "IPL_QUIZZES",
    target: 20,
    rewardCoins: 100,
    fixed: false,
  },
  {
    id: "ipl-scoreboard",
    title: "Boundary Hunter",
    description: "Get 30 correct answers in IPL quizzes today.",
    metric: "IPL_CORRECT_ANSWERS",
    target: 30,
    rewardCoins: 110,
    fixed: false,
  },
];

const CLAN_TASKS = [
  {
    id: "opening-move",
    title: "Opening Move",
    description: "Complete 1 eligible contributing quiz.",
    metric: "ELIGIBLE_CLAN_SESSIONS",
    target: 1,
    rewardCoins: 50,
    rewardCp: 10,
  },
  {
    id: "triple-pressure",
    title: "Triple Pressure",
    description: "Complete 3 eligible contributing quizzes.",
    metric: "ELIGIBLE_CLAN_SESSIONS",
    target: 3,
    rewardCoins: 75,
    rewardCp: 10,
  },
  {
    id: "high-stakes",
    title: "High Stakes",
    description: "Complete 1 verified daily challenge or grand master quiz.",
    metric: "HIGH_STAKE_CLAN_SESSIONS",
    target: 1,
    rewardCoins: 100,
    rewardCp: 10,
  },
];

function normalizeString(value) {
  return String(value || "").trim();
}

function todayDateKey(now = new Date()) {
  const value = now instanceof Date ? now : new Date(now);
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(value);
}

function javaStringHash(value) {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = ((hash * 31) + value.charCodeAt(index)) | 0;
  }
  return hash;
}

function seededRandom(seed) {
  let value = seed >>> 0;
  return () => {
    value += 0x6D2B79F5;
    let result = Math.imul(value ^ (value >>> 15), value | 1);
    result ^= result + Math.imul(result ^ (result >>> 7), result | 61);
    return ((result ^ (result >>> 14)) >>> 0) / 4294967296;
  };
}

function seededShuffle(items, seed) {
  const nextRandom = seededRandom(seed);
  const result = [...items];
  for (let index = result.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(nextRandom() * (index + 1));
    [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
  }
  return result;
}

function isIplSeasonActiveForDayKey(dayKey) {
  const parts = normalizeString(dayKey).split("-");
  if (parts.length !== 3) {
    return false;
  }
  const month = Number(parts[1]);
  return month >= 3 && month <= 5;
}

function emptyPlayerStats(uid = "", dayKey = todayDateKey()) {
  return {
    uid,
    dayKey,
    quizzesPlayed: 0,
    questionsAnswered: 0,
    correctAnswers: 0,
    quizzesWon: 0,
    lightningRounds: 0,
    iplQuizzes: 0,
    iplCorrectAnswers: 0,
    playSeconds: 0,
    grandMasterCompleted: 0,
    dailyChallengePlayed: 0,
    eligibleClanSessions: 0,
    highStakeClanSessions: 0,
    claimedTaskIds: [],
    claimedClanTaskIds: [],
    processedSessionIds: [],
  };
}

function coerceNonNegativeInt(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) {
    return 0;
  }
  return Math.max(0, Math.trunc(number));
}

function normalizeStringList(values, maxItems = 1000) {
  if (!Array.isArray(values)) {
    return [];
  }
  return values
    .map((value) => normalizeString(value))
    .filter(Boolean)
    .slice(-maxItems);
}

function normalizePlayerStats(uid, data, dayKey = todayDateKey()) {
  const source = data || {};
  const normalizedUid = normalizeString(uid) || normalizeString(source.uid);
  if (normalizeString(source.dayKey) !== dayKey) {
    return emptyPlayerStats(normalizedUid, dayKey);
  }

  return {
    uid: normalizedUid,
    dayKey,
    quizzesPlayed: coerceNonNegativeInt(source.quizzesPlayed),
    questionsAnswered: coerceNonNegativeInt(source.questionsAnswered),
    correctAnswers: coerceNonNegativeInt(source.correctAnswers),
    quizzesWon: coerceNonNegativeInt(source.quizzesWon),
    lightningRounds: coerceNonNegativeInt(source.lightningRounds),
    iplQuizzes: coerceNonNegativeInt(source.iplQuizzes),
    iplCorrectAnswers: coerceNonNegativeInt(source.iplCorrectAnswers),
    playSeconds: coerceNonNegativeInt(source.playSeconds),
    grandMasterCompleted: coerceNonNegativeInt(source.grandMasterCompleted) > 0 ? 1 : 0,
    dailyChallengePlayed: coerceNonNegativeInt(source.dailyChallengePlayed) > 0 ? 1 : 0,
    eligibleClanSessions: coerceNonNegativeInt(source.eligibleClanSessions),
    highStakeClanSessions: coerceNonNegativeInt(source.highStakeClanSessions),
    claimedTaskIds: normalizeStringList(source.claimedTaskIds),
    claimedClanTaskIds: normalizeStringList(source.claimedClanTaskIds),
    processedSessionIds: normalizeStringList(source.processedSessionIds),
  };
}

function taskDefinitionsForDay(dayKey = todayDateKey()) {
  const seed = javaStringHash(dayKey);
  const iplSeasonActive = isIplSeasonActiveForDayKey(dayKey);
  const randomTasks = seededShuffle(RANDOM_TASK_POOL, seed).slice(0, iplSeasonActive ? 2 : 4);
  const seasonalTasks = iplSeasonActive
    ? seededShuffle(IPL_TASK_POOL, seed + 77).slice(0, 2)
    : [];
  return [...FIXED_TASKS, ...randomTasks, ...seasonalTasks];
}

function progressForMetric(metric, stats) {
  switch (metric) {
    case "QUIZZES_PLAYED":
      return stats.quizzesPlayed;
    case "QUESTIONS_ANSWERED":
      return stats.questionsAnswered;
    case "CORRECT_ANSWERS":
      return stats.correctAnswers;
    case "QUIZZES_WON":
      return stats.quizzesWon;
    case "LIGHTNING_ROUNDS":
      return stats.lightningRounds;
    case "IPL_QUIZZES":
      return stats.iplQuizzes;
    case "IPL_CORRECT_ANSWERS":
      return stats.iplCorrectAnswers;
    case "PLAY_SECONDS":
      return stats.playSeconds;
    case "GRAND_MASTER_COMPLETED":
      return stats.grandMasterCompleted;
    case "DAILY_CHALLENGE_PLAYED":
      return stats.dailyChallengePlayed;
    case "ELIGIBLE_CLAN_SESSIONS":
      return stats.eligibleClanSessions;
    case "HIGH_STAKE_CLAN_SESSIONS":
      return stats.highStakeClanSessions;
    default:
      return 0;
  }
}

function buildTaskStatus(stats, dayKey = todayDateKey()) {
  return taskDefinitionsForDay(dayKey).map((definition) => {
    const progress = Math.max(0, progressForMetric(definition.metric, stats));
    return {
      id: definition.id,
      title: definition.title,
      description: definition.description,
      rewardCoins: definition.rewardCoins,
      progress: Math.min(progress, definition.target),
      target: definition.target,
      complete: progress >= definition.target,
      claimed: stats.claimedTaskIds.includes(definition.id),
      fixed: definition.fixed,
    };
  });
}

function buildClanTaskStatus(stats) {
  return CLAN_TASKS.map((definition) => {
    const progress = Math.max(0, progressForMetric(definition.metric, stats));
    return {
      id: definition.id,
      title: definition.title,
      description: definition.description,
      rewardCoins: definition.rewardCoins,
      rewardCp: definition.rewardCp,
      progress: Math.min(progress, definition.target),
      target: definition.target,
      complete: progress >= definition.target,
      claimed: stats.claimedClanTaskIds.includes(definition.id),
    };
  });
}

function taskDefinitionById(taskId, dayKey = todayDateKey()) {
  return taskDefinitionsForDay(dayKey).find((definition) => definition.id === taskId) || null;
}

function clanTaskDefinitionById(taskId) {
  return CLAN_TASKS.find((definition) => definition.id === taskId) || null;
}

function defaultGenreForSessionType(sessionType) {
  switch (sessionType) {
    case "grand_master":
      return "Grand Master Quiz";
    case "daily_challenge":
      return "Daily Challenge";
    case "lightning":
      return "Lightning Round";
    default:
      return "General Knowledge";
  }
}

function gameplaySessionFromData(data) {
  const sessionType = normalizeString(data && data.sessionType).toLowerCase();
  if (!SUPPORTED_SESSION_TYPES.has(sessionType)) {
    throw new Error("Unsupported gameplay session type.");
  }

  const sessionId = normalizeString(data && data.sessionId)
    .replace(/[^A-Za-z0-9:_-]/g, "")
    .slice(0, 128);
  if (!sessionId) {
    throw new Error("A valid gameplay session id is required.");
  }

  const limits = SESSION_LIMITS[sessionType];
  const questionsAnswered = coerceNonNegativeInt(data && data.questionsAnswered);
  if (questionsAnswered < 1 || questionsAnswered > limits.maxQuestions) {
    throw new Error("Gameplay session question count is invalid.");
  }

  const correctAnswers = coerceNonNegativeInt(data && data.correctAnswers);
  if (correctAnswers > questionsAnswered) {
    throw new Error("Gameplay session score is invalid.");
  }

  const durationSeconds = coerceNonNegativeInt(data && data.durationSeconds);
  const minDurationSeconds = Math.max(5, Math.min(30, questionsAnswered * 2));
  if (durationSeconds < minDurationSeconds || durationSeconds > limits.maxDurationSeconds) {
    throw new Error("Gameplay session duration is outside the allowed range.");
  }

  const bestCorrectStreak = Math.min(
    questionsAnswered,
    coerceNonNegativeInt(data && data.bestCorrectStreak)
  );
  const didWin = Boolean(data && data.didWin);
  const genre =
    normalizeString(data && data.genre).slice(0, 64) || defaultGenreForSessionType(sessionType);

  return {
    sessionId,
    sessionType,
    questionsAnswered,
    correctAnswers,
    durationSeconds,
    playSeconds: Math.min(durationSeconds, limits.maxDurationSeconds),
    bestCorrectStreak,
    didWin,
    genre,
  };
}

function applyGameplaySession(stats, session) {
  const nextStats = {
    ...stats,
    claimedTaskIds: [...stats.claimedTaskIds],
    claimedClanTaskIds: [...stats.claimedClanTaskIds],
    processedSessionIds: [...stats.processedSessionIds],
  };

  if (nextStats.processedSessionIds.includes(session.sessionId)) {
    return { stats: nextStats, duplicate: true };
  }

  nextStats.processedSessionIds.push(session.sessionId);
  nextStats.processedSessionIds = nextStats.processedSessionIds.slice(-1000);
  nextStats.quizzesPlayed += 1;
  nextStats.questionsAnswered += session.questionsAnswered;
  nextStats.correctAnswers += session.correctAnswers;
  nextStats.playSeconds += session.playSeconds;
  if (session.didWin) {
    nextStats.quizzesWon += 1;
  }
  if (session.sessionType === "lightning") {
    nextStats.lightningRounds += 1;
  }
  if (session.sessionType === "grand_master") {
    nextStats.grandMasterCompleted = 1;
    nextStats.highStakeClanSessions += 1;
  }
  if (session.sessionType === "daily_challenge") {
    nextStats.dailyChallengePlayed = 1;
    nextStats.highStakeClanSessions += 1;
  }
  if (session.sessionType === "genre" || session.sessionType === "lightning" ||
      session.sessionType === "grand_master" || session.sessionType === "daily_challenge") {
    nextStats.eligibleClanSessions += 1;
  }
  if (session.genre === "IPL") {
    nextStats.iplQuizzes += 1;
    nextStats.iplCorrectAnswers += session.correctAnswers;
  }

  return { stats: nextStats, duplicate: false };
}

module.exports = {
  applyGameplaySession,
  buildTaskStatus,
  buildClanTaskStatus,
  clanTaskDefinitionById,
  emptyPlayerStats,
  gameplaySessionFromData,
  normalizePlayerStats,
  taskDefinitionById,
  taskDefinitionsForDay,
  todayDateKey,
};
