const test = require("node:test");
const assert = require("node:assert/strict");

const {
  evaluateCoinSync,
  normalizeCoinSnapshot,
} = require("../lib/coinSecurity");

const LIMITS = {
  maxDailyTotalCoins: 500,
  maxDailyTaskCoins: 500,
  maxDailyQuizCoins: 0,
  maxDailyTaskClaims: 7,
  maxCoinsPerCorrect: 0,
};

test("normalizeCoinSnapshot keeps the expected fields", () => {
  const snapshot = normalizeCoinSnapshot({
    totalCoins: 120,
    dayKey: "2026-04-01",
    dailyCoinsEarned: 40,
    dailyQuizCoinsEarned: 0,
    dailyTaskCoinsEarned: 40,
    dailyTaskClaims: 1,
    playSecondsToday: 900,
    quizzesPlayedToday: 4,
    questionsAnsweredToday: 40,
    correctAnswersToday: 25,
  }, "2026-04-01");

  assert.deepEqual(snapshot, {
    totalCoins: 120,
    dayKey: "2026-04-01",
    dailyCoinsEarned: 40,
    dailyQuizCoinsEarned: 0,
    dailyTaskCoinsEarned: 40,
    dailyTaskClaims: 1,
    playSecondsToday: 900,
    quizzesPlayedToday: 4,
    questionsAnsweredToday: 40,
    correctAnswersToday: 25,
  });
});

test("evaluateCoinSync accepts a matching wallet snapshot", () => {
  const snapshot = normalizeCoinSnapshot({
    totalCoins: 220,
    dayKey: "2026-04-01",
    dailyCoinsEarned: 90,
    dailyQuizCoinsEarned: 0,
    dailyTaskCoinsEarned: 90,
    dailyTaskClaims: 2,
    playSecondsToday: 1800,
    quizzesPlayedToday: 12,
    questionsAnsweredToday: 80,
    correctAnswersToday: 50,
  }, "2026-04-01");

  const result = evaluateCoinSync(snapshot, {
    coins: 220,
    lastValidatedDayKey: "2026-04-01",
    validatedEarnedToday: 90,
  }, LIMITS, "2026-04-01");

  assert.equal(result.suspicious, false);
  assert.deepEqual(result.reasons, []);
});

test("evaluateCoinSync flags client balances above the server wallet", () => {
  const snapshot = normalizeCoinSnapshot({
    totalCoins: 310,
    dayKey: "2026-04-01",
    dailyCoinsEarned: 40,
    dailyQuizCoinsEarned: 0,
    dailyTaskCoinsEarned: 40,
    dailyTaskClaims: 1,
    playSecondsToday: 900,
    quizzesPlayedToday: 4,
    questionsAnsweredToday: 40,
    correctAnswersToday: 25,
  }, "2026-04-01");

  const result = evaluateCoinSync(snapshot, {
    coins: 220,
    lastValidatedDayKey: "2026-04-01",
    validatedEarnedToday: 40,
  }, LIMITS, "2026-04-01");

  assert.equal(result.suspicious, true);
  assert.ok(result.reasons.includes("client_balance_exceeds_wallet"));
});

test("evaluateCoinSync flags task earnings above validated server earnings", () => {
  const snapshot = normalizeCoinSnapshot({
    totalCoins: 220,
    dayKey: "2026-04-01",
    dailyCoinsEarned: 120,
    dailyQuizCoinsEarned: 0,
    dailyTaskCoinsEarned: 120,
    dailyTaskClaims: 3,
    playSecondsToday: 1800,
    quizzesPlayedToday: 12,
    questionsAnsweredToday: 80,
    correctAnswersToday: 50,
  }, "2026-04-01");

  const result = evaluateCoinSync(snapshot, {
    coins: 220,
    lastValidatedDayKey: "2026-04-01",
    validatedEarnedToday: 90,
  }, LIMITS, "2026-04-01");

  assert.equal(result.suspicious, true);
  assert.ok(result.reasons.includes("client_task_earnings_exceed_wallet"));
});
