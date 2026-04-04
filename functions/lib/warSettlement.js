/**
 * Pure-logic helpers for war settlement.
 *
 * Extracted from index.js settleCurrentWarsForCycle so the core
 * determination logic (winner, MVP, stat deltas) can be unit-tested
 * without Firestore.
 */

/**
 * Determines the war result from left/right scores.
 * @param {number} leftScore
 * @param {number} rightScore
 * @returns {"left_win"|"right_win"|"draw"}
 */
function determineWarResult(leftScore, rightScore) {
  if (leftScore === rightScore) return "draw";
  return leftScore > rightScore ? "left_win" : "right_win";
}

/**
 * Finds the MVP (the participant with the highest countedScore).
 * @param {Array<{uid: string, countedScore: number}>} participants
 * @returns {{mvpUid: string, mvpScore: number}}
 */
function determineMvp(participants) {
  let mvpUid = "";
  let mvpScore = -1;
  for (const p of participants) {
    const score = Number(p.countedScore || 0);
    if (score > mvpScore) {
      mvpScore = score;
      mvpUid = p.uid;
    }
  }
  return { mvpUid, mvpScore };
}

/**
 * Computes the stat delta for a single member after settlement.
 *
 * @param {string} uid
 * @param {"left"|"right"} side
 * @param {string} result — "left_win", "right_win", or "draw"
 * @param {string} mvpUid
 * @param {Map<string, {countedScore: number}>} participantMap
 * @returns {{contributed: boolean, isWinner: boolean, isMvp: boolean, countedScore: number, contributionStreakReset: boolean}}
 */
function computeMemberStatDelta(uid, side, result, mvpUid, participantMap) {
  const participant = participantMap.get(uid) || {};
  const countedScore = Number(participant.countedScore || 0);
  const contributed = countedScore > 0;
  const isWinner =
    (side === "left" && result === "left_win") ||
    (side === "right" && result === "right_win");
  const isMvp = uid === mvpUid;
  return {
    contributed,
    isWinner,
    isMvp,
    countedScore,
    contributionStreakReset: !contributed,
  };
}

/**
 * Determines which season badge a player earns based on their ladder rank.
 * @param {number} rank — 1-indexed
 * @returns {string}
 */
function determineBadge(rank) {
  if (rank === 1) return "Season Champion";
  if (rank <= 10) return "Season Elite";
  return "Season Veteran";
}

module.exports = {
  determineWarResult,
  determineMvp,
  computeMemberStatDelta,
  determineBadge,
};
