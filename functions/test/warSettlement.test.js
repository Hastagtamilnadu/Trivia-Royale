const test = require("node:test");
const assert = require("node:assert/strict");

const {
  determineWarResult,
  determineMvp,
  computeMemberStatDelta,
  determineBadge,
} = require("../lib/warSettlement");

// ── determineWarResult ──────────────────────────────────────────

test("determineWarResult returns left_win when left score is higher", () => {
  assert.equal(determineWarResult(120, 80), "left_win");
});

test("determineWarResult returns right_win when right score is higher", () => {
  assert.equal(determineWarResult(50, 90), "right_win");
});

test("determineWarResult returns draw when scores are equal", () => {
  assert.equal(determineWarResult(100, 100), "draw");
});

test("determineWarResult handles zero scores as draw", () => {
  assert.equal(determineWarResult(0, 0), "draw");
});

// ── determineMvp ────────────────────────────────────────────────

test("determineMvp selects the participant with the highest countedScore", () => {
  const participants = [
    { uid: "user_a", countedScore: 30 },
    { uid: "user_b", countedScore: 70 },
    { uid: "user_c", countedScore: 50 },
  ];
  const { mvpUid, mvpScore } = determineMvp(participants);
  assert.equal(mvpUid, "user_b");
  assert.equal(mvpScore, 70);
});

test("determineMvp returns first max-score participant on tie", () => {
  const participants = [
    { uid: "user_a", countedScore: 50 },
    { uid: "user_b", countedScore: 50 },
  ];
  const { mvpUid } = determineMvp(participants);
  assert.equal(mvpUid, "user_a");
});

test("determineMvp handles empty participants list", () => {
  const { mvpUid, mvpScore } = determineMvp([]);
  assert.equal(mvpUid, "");
  assert.equal(mvpScore, -1);
});

test("determineMvp handles single participant", () => {
  const participants = [{ uid: "solo", countedScore: 10 }];
  const { mvpUid, mvpScore } = determineMvp(participants);
  assert.equal(mvpUid, "solo");
  assert.equal(mvpScore, 10);
});

test("determineMvp treats missing countedScore as 0", () => {
  const participants = [
    { uid: "user_a" },
    { uid: "user_b", countedScore: 5 },
  ];
  const { mvpUid } = determineMvp(participants);
  assert.equal(mvpUid, "user_b");
});

// ── computeMemberStatDelta ──────────────────────────────────────

test("computeMemberStatDelta marks winner on left side with left_win", () => {
  const participantMap = new Map([["uid1", { countedScore: 40 }]]);
  const delta = computeMemberStatDelta("uid1", "left", "left_win", "uid1", participantMap);
  assert.equal(delta.contributed, true);
  assert.equal(delta.isWinner, true);
  assert.equal(delta.isMvp, true);
  assert.equal(delta.countedScore, 40);
  assert.equal(delta.contributionStreakReset, false);
});

test("computeMemberStatDelta marks loser on left side with right_win", () => {
  const participantMap = new Map([["uid1", { countedScore: 20 }]]);
  const delta = computeMemberStatDelta("uid1", "left", "right_win", "uid2", participantMap);
  assert.equal(delta.contributed, true);
  assert.equal(delta.isWinner, false);
  assert.equal(delta.isMvp, false);
});

test("computeMemberStatDelta resets streak for non-contributor", () => {
  const participantMap = new Map();
  const delta = computeMemberStatDelta("uid_inactive", "right", "right_win", "uid_other", participantMap);
  assert.equal(delta.contributed, false);
  // isWinner reflects the clan's result, not individual contribution
  // the backend uses `contributed && isWinner` for warWinCount increments
  assert.equal(delta.isWinner, true);
  assert.equal(delta.isMvp, false);
  assert.equal(delta.countedScore, 0);
  assert.equal(delta.contributionStreakReset, true);
});

test("computeMemberStatDelta handles draw — nobody wins", () => {
  const participantMap = new Map([["uid1", { countedScore: 30 }]]);
  const delta = computeMemberStatDelta("uid1", "left", "draw", "uid1", participantMap);
  assert.equal(delta.isWinner, false);
  assert.equal(delta.isMvp, true);
  assert.equal(delta.contributed, true);
});

test("computeMemberStatDelta — right side winner with right_win", () => {
  const participantMap = new Map([["uid_r", { countedScore: 55 }]]);
  const delta = computeMemberStatDelta("uid_r", "right", "right_win", "uid_r", participantMap);
  assert.equal(delta.isWinner, true);
  assert.equal(delta.isMvp, true);
  assert.equal(delta.countedScore, 55);
});

// ── determineBadge ──────────────────────────────────────────────

test("determineBadge returns Season Champion for rank 1", () => {
  assert.equal(determineBadge(1), "Season Champion");
});

test("determineBadge returns Season Elite for ranks 2-10", () => {
  assert.equal(determineBadge(2), "Season Elite");
  assert.equal(determineBadge(5), "Season Elite");
  assert.equal(determineBadge(10), "Season Elite");
});

test("determineBadge returns Season Veteran for ranks 11+", () => {
  assert.equal(determineBadge(11), "Season Veteran");
  assert.equal(determineBadge(50), "Season Veteran");
  assert.equal(determineBadge(100), "Season Veteran");
});

// ── Full settlement scenario ────────────────────────────────────

test("full settlement scenario — left clan wins, MVP determined correctly", () => {
  const leftScore = 150;
  const rightScore = 90;

  // determine result
  const result = determineWarResult(leftScore, rightScore);
  assert.equal(result, "left_win");

  // participants from both sides
  const allParticipants = [
    { uid: "left_1", countedScore: 80, side: "left" },
    { uid: "left_2", countedScore: 70, side: "left" },
    { uid: "right_1", countedScore: 60, side: "right" },
    { uid: "right_2", countedScore: 30, side: "right" },
  ];

  // determine MVP across all participants
  const { mvpUid } = determineMvp(allParticipants);
  assert.equal(mvpUid, "left_1");

  // build participant map
  const participantMap = new Map(
    allParticipants.map((p) => [p.uid, { countedScore: p.countedScore }]),
  );

  // verify stat deltas
  const left1 = computeMemberStatDelta("left_1", "left", result, mvpUid, participantMap);
  assert.equal(left1.isWinner, true);
  assert.equal(left1.isMvp, true);
  assert.equal(left1.contributed, true);

  const right1 = computeMemberStatDelta("right_1", "right", result, mvpUid, participantMap);
  assert.equal(right1.isWinner, false);
  assert.equal(right1.isMvp, false);
  assert.equal(right1.contributed, true);

  // non-participant on the LOSING side — isWinner is false because clan lost
  const rightInactive = computeMemberStatDelta("right_3", "right", result, mvpUid, participantMap);
  assert.equal(rightInactive.isWinner, false);
  assert.equal(rightInactive.contributed, false);
  assert.equal(rightInactive.contributionStreakReset, true);
});

test("full settlement scenario — draw with MVP still awarded", () => {
  const result = determineWarResult(100, 100);
  assert.equal(result, "draw");

  const allParticipants = [
    { uid: "a1", countedScore: 60 },
    { uid: "b1", countedScore: 40 },
  ];

  const { mvpUid } = determineMvp(allParticipants);
  assert.equal(mvpUid, "a1");

  const participantMap = new Map(
    allParticipants.map((p) => [p.uid, { countedScore: p.countedScore }]),
  );

  const a1 = computeMemberStatDelta("a1", "left", result, mvpUid, participantMap);
  assert.equal(a1.isWinner, false); // draw = nobody wins
  assert.equal(a1.isMvp, true);

  const b1 = computeMemberStatDelta("b1", "right", result, mvpUid, participantMap);
  assert.equal(b1.isWinner, false);
  assert.equal(b1.isMvp, false);
});
