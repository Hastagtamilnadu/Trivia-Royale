const { HttpsError } = require("firebase-functions/v2/https");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");

// Rate limit windows in milliseconds (15 minutes)
const WINDOW_MS = 15 * 60 * 1000;

// Default limits per function per 15-minute window
const RATE_LIMITS = {
  recordGameplaySession: 30,  // ~2/min — realistic max play speed
  claimDailyTaskReward: 10,   // tasks are limited anyway
  syncCoinBalance: 10,        // periodic sync only
  submitRedeemRequest: 3,     // very rare action
};

/**
 * Checks if the user has exceeded the rate limit for a given function.
 * Uses a lightweight Firestore document that auto-resets each window.
 *
 * @param {string} uid - The user's UID
 * @param {string} functionName - The Cloud Function being called
 * @throws {HttpsError} if rate limit exceeded
 */
async function enforceRateLimit(uid, functionName) {
  const maxCalls = RATE_LIMITS[functionName];
  if (!maxCalls) {
    // No rate limit configured for this function
    return;
  }

  const db = getFirestore();
  const windowKey = currentWindowKey();
  const docRef = db.collection("rateLimits").doc(uid);

  try {
    const result = await db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(docRef);
      const data = snapshot.exists ? snapshot.data() || {} : {};

      // Each function gets a field like "recordGameplaySession_2026-04-02T19:45"
      const fieldName = `${functionName}_${windowKey}`;
      const currentCount = Number(data[fieldName] || 0);

      if (currentCount >= maxCalls) {
        return { blocked: true, count: currentCount };
      }

      // Increment the counter. Also store a TTL timestamp for cleanup.
      transaction.set(docRef, {
        [fieldName]: currentCount + 1,
        _lastUpdated: FieldValue.serverTimestamp(),
        _ttl: new Date(Date.now() + WINDOW_MS * 2), // for TTL policy cleanup
      }, { merge: true });

      return { blocked: false, count: currentCount + 1 };
    });

    if (result.blocked) {
      throw new HttpsError(
        "resource-exhausted",
        "Too many requests. Please wait a few minutes and try again."
      );
    }
  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }
    // If rate limiting itself fails (e.g. Firestore issue), allow the request
    // rather than blocking legitimate users. Log for monitoring.
    console.warn(`Rate limit check failed for ${uid}/${functionName}:`, error.message);
  }
}

/**
 * Returns a window key like "2026-04-02T19:45" (15-minute buckets).
 */
function currentWindowKey() {
  const now = new Date();
  const minutes = Math.floor(now.getUTCMinutes() / 15) * 15;
  const year = now.getUTCFullYear();
  const month = String(now.getUTCMonth() + 1).padStart(2, "0");
  const day = String(now.getUTCDate()).padStart(2, "0");
  const hours = String(now.getUTCHours()).padStart(2, "0");
  const mins = String(minutes).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${mins}`;
}

module.exports = { enforceRateLimit };
