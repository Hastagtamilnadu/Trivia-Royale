function normalizeString(value) {
  return String(value || "").trim();
}

function toNonNegativeInt(value) {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric)) {
    return 0;
  }
  return Math.max(0, Math.trunc(numeric));
}

function normalizeCoinSnapshot(data, fallbackDayKey) {
  if (!data || typeof data !== "object") {
    return null;
  }

  return {
    totalCoins: toNonNegativeInt(data.totalCoins),
    dayKey: normalizeString(data.dayKey) || normalizeString(fallbackDayKey),
    dailyCoinsEarned: toNonNegativeInt(data.dailyCoinsEarned),
    dailyQuizCoinsEarned: toNonNegativeInt(data.dailyQuizCoinsEarned),
    dailyTaskCoinsEarned: toNonNegativeInt(data.dailyTaskCoinsEarned),
    dailyTaskClaims: toNonNegativeInt(data.dailyTaskClaims),
    playSecondsToday: toNonNegativeInt(data.playSecondsToday),
    quizzesPlayedToday: toNonNegativeInt(data.quizzesPlayedToday),
    questionsAnsweredToday: toNonNegativeInt(data.questionsAnsweredToday),
    correctAnswersToday: toNonNegativeInt(data.correctAnswersToday),
  };
}

function validCoinSnapshot(snapshot, limits) {
  if (!snapshot) {
    return false;
  }

  return snapshot.totalCoins >= 0 &&
    snapshot.dailyCoinsEarned >= 0 &&
    snapshot.dailyQuizCoinsEarned >= 0 &&
    snapshot.dailyTaskCoinsEarned >= 0 &&
    snapshot.dailyTaskClaims >= 0 &&
    snapshot.playSecondsToday >= 0 &&
    snapshot.quizzesPlayedToday >= 0 &&
    snapshot.questionsAnsweredToday >= 0 &&
    snapshot.correctAnswersToday >= 0 &&
    snapshot.questionsAnsweredToday >= snapshot.correctAnswersToday &&
    snapshot.dailyCoinsEarned === snapshot.dailyQuizCoinsEarned + snapshot.dailyTaskCoinsEarned &&
    snapshot.dailyTaskClaims <= limits.maxDailyTaskClaims &&
    snapshot.dailyTaskCoinsEarned <= limits.maxDailyTaskCoins &&
    snapshot.dailyQuizCoinsEarned <= limits.maxDailyQuizCoins &&
    snapshot.dailyCoinsEarned <= limits.maxDailyTotalCoins &&
    snapshot.dailyQuizCoinsEarned <= snapshot.correctAnswersToday * limits.maxCoinsPerCorrect &&
    snapshot.questionsAnsweredToday <= snapshot.playSecondsToday * 2 + 30;
}

function evaluateCoinSync(snapshot, wallet, limits, fallbackDayKey) {
  if (!snapshot) {
    return {
      suspicious: false,
      reasons: [],
    };
  }

  const reasons = [];
  if (!validCoinSnapshot(snapshot, limits)) {
    reasons.push("invalid_snapshot");
  }

  const walletCoins = toNonNegativeInt(wallet && wallet.coins);
  const walletDayKey = normalizeString(wallet && wallet.lastValidatedDayKey) || normalizeString(fallbackDayKey);
  const walletValidatedEarnedToday = toNonNegativeInt(wallet && wallet.validatedEarnedToday);

  if (snapshot.totalCoins > walletCoins) {
    reasons.push("client_balance_exceeds_wallet");
  }

  if (
    snapshot.dayKey === walletDayKey &&
    snapshot.dailyTaskCoinsEarned > walletValidatedEarnedToday
  ) {
    reasons.push("client_task_earnings_exceed_wallet");
  }

  return {
    suspicious: reasons.length > 0,
    reasons,
  };
}

module.exports = {
  evaluateCoinSync,
  normalizeCoinSnapshot,
  validCoinSnapshot,
};
