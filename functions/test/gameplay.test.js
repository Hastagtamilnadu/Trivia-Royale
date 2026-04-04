const test = require("node:test");
const assert = require("node:assert/strict");

const {
  applyGameplaySession,
  buildTaskStatus,
  buildClanTaskStatus,
  emptyPlayerStats,
  gameplaySessionFromData,
  taskDefinitionsForDay,
} = require("../lib/gameplay");

test("taskDefinitionsForDay always returns seven tasks", () => {
  const offseason = taskDefinitionsForDay("2026-06-15");
  const iplSeason = taskDefinitionsForDay("2026-04-15");

  assert.equal(offseason.length, 7);
  assert.equal(iplSeason.length, 7);
  assert.equal(offseason.filter((task) => task.id.startsWith("ipl-")).length, 0);
  assert.equal(iplSeason.filter((task) => task.id.startsWith("ipl-")).length, 2);
});

test("applyGameplaySession is idempotent per session id", () => {
  const stats = emptyPlayerStats("user-1", "2026-04-01");
  const session = gameplaySessionFromData({
    sessionId: "session-1",
    sessionType: "lightning",
    questionsAnswered: 20,
    correctAnswers: 14,
    durationSeconds: 90,
    didWin: true,
    genre: "Lightning Round",
    bestCorrectStreak: 6,
  });

  const first = applyGameplaySession(stats, session);
  const second = applyGameplaySession(first.stats, session);

  assert.equal(first.duplicate, false);
  assert.equal(first.stats.quizzesPlayed, 1);
  assert.equal(first.stats.lightningRounds, 1);
  assert.equal(first.stats.eligibleClanSessions, 1);
  assert.equal(second.duplicate, true);
  assert.equal(second.stats.quizzesPlayed, 1);
  assert.equal(second.stats.processedSessionIds.length, 1);
});

test("buildTaskStatus reflects progress from server stats", () => {
  const stats = emptyPlayerStats("user-2", "2026-04-10");
  stats.playSeconds = 1800;
  stats.grandMasterCompleted = 1;
  stats.dailyChallengePlayed = 1;

  const tasks = buildTaskStatus(stats, "2026-04-10");

  assert.equal(tasks.length, 7);
  assert.equal(tasks.find((task) => task.id === "play-30-mins").complete, true);
  assert.equal(tasks.find((task) => task.id === "grand-master-daily").complete, true);
  assert.equal(tasks.find((task) => task.id === "daily-challenger").complete, true);
});

test("buildClanTaskStatus reflects clan-eligible session progress", () => {
  const stats = emptyPlayerStats("user-3", "2026-04-10");
  stats.eligibleClanSessions = 3;
  stats.highStakeClanSessions = 1;

  const tasks = buildClanTaskStatus(stats);

  assert.equal(tasks.length, 3);
  assert.equal(tasks.find((task) => task.id === "opening-move").complete, true);
  assert.equal(tasks.find((task) => task.id === "triple-pressure").complete, true);
  assert.equal(tasks.find((task) => task.id === "high-stakes").complete, true);
});

test("daily reward schedule stays within the 1000-coin daily cap", () => {
  const offseason = taskDefinitionsForDay("2026-06-15");
  const iplSeason = taskDefinitionsForDay("2026-04-15");

  const offseasonTotal = offseason.reduce((sum, task) => sum + task.rewardCoins, 0);
  const iplTotal = iplSeason.reduce((sum, task) => sum + task.rewardCoins, 0);

  assert.ok(offseasonTotal <= 1000);
  assert.ok(iplTotal <= 1000);
});

test("gameplaySessionFromData rejects implausibly short sessions", () => {
  assert.throws(
    () => gameplaySessionFromData({
      sessionId: "fast-run",
      sessionType: "genre",
      questionsAnswered: 10,
      correctAnswers: 10,
      durationSeconds: 5,
      didWin: true,
      genre: "Science",
      bestCorrectStreak: 10,
    }),
    /duration/i,
  );
});
