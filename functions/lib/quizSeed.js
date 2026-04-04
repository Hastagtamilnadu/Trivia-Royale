/**
 * Quiz Question Seeder — Fetches questions from Open Trivia Database (OpenTDB) API,
 * transforms them into the app's format, and stores them in Firestore.
 *
 * Firestore schema:
 *   quiz_questions/{hash}  — individual question documents
 *   quiz_metadata/{genre}  — genre-level aggregate metadata
 */

const https = require("https");
const crypto = require("crypto");

// ── Genre ↔ OpenTDB category mapping ──────────────────────────────────────────
const GENRE_CATEGORY_MAP = {
  "General Knowledge": [
    { otdbId: 9, category: "General Knowledge Spotlight" },
  ],
  Sports: [
    { otdbId: 21, category: "Sports Trivia" },
  ],
  Science: [
    { otdbId: 17, category: "Science & Nature" },
    { otdbId: 18, category: "Computers & Tech" },
  ],
  History: [
    { otdbId: 23, category: "World History" },
  ],
  Geography: [
    { otdbId: 22, category: "World Geography" },
  ],
  Movies: [
    { otdbId: 11, category: "Film Trivia" },
  ],
  Music: [
    { otdbId: 12, category: "Music Trivia" },
  ],
  Entertainment: [
    { otdbId: 14, category: "Television" },
    { otdbId: 16, category: "Board Games" },
  ],
  Tech: [
    { otdbId: 18, category: "Computer Science" },
    { otdbId: 30, category: "Gadgets" },
  ],
  Art: [
    { otdbId: 25, category: "Art & Design" },
  ],
  Literature: [
    { otdbId: 10, category: "Books & Literature" },
  ],
  "Pop Culture": [
    { otdbId: 29, category: "Comics" },
    { otdbId: 31, category: "Anime & Manga" },
    { otdbId: 15, category: "Video Games" },
  ],
};

// ── Difficulty mapping ────────────────────────────────────────────────────────
function difficultyToValue(difficulty) {
  switch (difficulty) {
    case "easy": return 0.2;
    case "medium": return 0.5;
    case "hard": return 0.8;
    default: return 0.5;
  }
}

// ── Decode HTML entities ──────────────────────────────────────────────────────
function decodeHtml(text) {
  return text
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .replace(/&rsquo;/g, "'")
    .replace(/&lsquo;/g, "'")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&eacute;/g, "é")
    .replace(/&ntilde;/g, "ñ")
    .replace(/&uuml;/g, "ü")
    .replace(/&ouml;/g, "ö")
    .replace(/&iacute;/g, "í")
    .replace(/&aacute;/g, "á")
    .replace(/&Eacute;/g, "É")
    .replace(/&shy;/g, "")
    .replace(/&ldquo;/g, "\u201C")
    .replace(/&rdquo;/g, "\u201D")
    .replace(/&hellip;/g, "\u2026")
    .replace(/&pi;/g, "\u03C0")
    .replace(/&deg;/g, "°")
    .replace(/&ndash;/g, "\u2013")
    .replace(/&mdash;/g, "\u2014")
    .replace(/&#(\d+);/g, (_, dec) => String.fromCharCode(dec));
}

// ── Hash question for dedup ───────────────────────────────────────────────────
function hashQuestion(text) {
  return crypto.createHash("sha256")
    .update(text.trim().toLowerCase())
    .digest("hex")
    .slice(0, 16);
}

// ── Fetch from OpenTDB ───────────────────────────────────────────────────────
function fetchFromOpenTDB(amount, categoryId, difficulty, token) {
  return new Promise((resolve, reject) => {
    let url = `https://opentdb.com/api.php?amount=${amount}&category=${categoryId}&type=multiple`;
    if (difficulty) url += `&difficulty=${difficulty}`;
    if (token) url += `&token=${token}`;

    https.get(url, (res) => {
      let data = "";
      res.on("data", (chunk) => { data += chunk; });
      res.on("end", () => {
        try {
          const parsed = JSON.parse(data);
          resolve(parsed);
        } catch (err) {
          reject(new Error(`Failed to parse OpenTDB response: ${err.message}`));
        }
      });
    }).on("error", reject);
  });
}

function requestSessionToken() {
  return new Promise((resolve, reject) => {
    https.get("https://opentdb.com/api_token.php?command=request", (res) => {
      let data = "";
      res.on("data", (chunk) => { data += chunk; });
      res.on("end", () => {
        try {
          const parsed = JSON.parse(data);
          resolve(parsed.response_code === 0 ? parsed.token : null);
        } catch (err) {
          resolve(null);
        }
      });
    }).on("error", () => resolve(null));
  });
}

// ── Transform OpenTDB question → App question ────────────────────────────────
function transformQuestion(raw, genre, category) {
  const questionText = decodeHtml(raw.question);
  const correctAnswer = decodeHtml(raw.correct_answer);
  const incorrectAnswers = raw.incorrect_answers.map(decodeHtml);

  // Build options with correct answer at random position
  const answerIndex = Math.floor(Math.random() * 4);
  const options = [...incorrectAnswers];
  options.splice(answerIndex, 0, correctAnswer);

  // Ensure exactly 4 options
  while (options.length < 4) options.push("");
  if (options.length > 4) options.length = 4;

  return {
    hash: hashQuestion(questionText),
    question: questionText,
    options: options,
    answer: answerIndex,
    difficulty: difficultyToValue(raw.difficulty),
    genre: genre,
    category: category,
    source: "opentdb",
    createdAt: Date.now(),
  };
}

// ── Sleep helper ──────────────────────────────────────────────────────────────
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// ── Main seed function ────────────────────────────────────────────────────────
/**
 * Seeds the Firestore `quiz_questions` collection with questions from OpenTDB.
 *
 * @param {import("firebase-admin/firestore").Firestore} db — Firestore instance
 * @param {Object} options
 * @param {number} [options.questionsPerCategory=50] — how many to fetch per category
 * @param {string[]} [options.genres] — specific genres to seed (default: all)
 * @returns {Promise<{seeded: number, skippedDuplicates: number, errors: string[]}>}
 */
async function seedQuizQuestions(db, options = {}) {
  const questionsPerCategory = options.questionsPerCategory || 50;
  const targetGenres = options.genres || Object.keys(GENRE_CATEGORY_MAP);

  let seeded = 0;
  let skippedDuplicates = 0;
  const errors = [];

  // Get a session token for dedup within this fetch session
  const token = await requestSessionToken();

  for (const genre of targetGenres) {
    const categories = GENRE_CATEGORY_MAP[genre];
    if (!categories) continue;

    for (const { otdbId, category } of categories) {
      // Fetch in batches of 50 (OpenTDB max)
      const batchSize = Math.min(questionsPerCategory, 50);
      const batches = Math.ceil(questionsPerCategory / batchSize);

      for (let batch = 0; batch < batches; batch++) {
        try {
          // Respect rate limit: 1 request per 5 seconds
          if (batch > 0 || seeded > 0) {
            await sleep(5500);
          }

          const difficulties = ["easy", "medium", "hard"];
          const difficulty = difficulties[batch % 3];

          const response = await fetchFromOpenTDB(batchSize, otdbId, difficulty, token);

          if (response.response_code !== 0) {
            if (response.response_code === 4) {
              // Token exhausted for this category, move on
              break;
            }
            errors.push(`OpenTDB code ${response.response_code} for ${genre}/${category}`);
            continue;
          }

          const questions = response.results.map((raw) =>
            transformQuestion(raw, genre, category)
          );

          // Batch write to Firestore
          const writeBatch = db.batch();
          let batchSeeded = 0;

          for (const q of questions) {
            // Check if question already exists
            const docRef = db.collection("quiz_questions").doc(q.hash);
            const existing = await docRef.get();
            if (existing.exists) {
              skippedDuplicates++;
              continue;
            }

            writeBatch.set(docRef, {
              question: q.question,
              options: q.options,
              answer: q.answer,
              difficulty: q.difficulty,
              genre: q.genre,
              category: q.category,
              source: q.source,
              createdAt: q.createdAt,
            });
            batchSeeded++;
          }

          if (batchSeeded > 0) {
            await writeBatch.commit();
            seeded += batchSeeded;
          }
        } catch (err) {
          errors.push(`Error seeding ${genre}/${category}: ${err.message}`);
        }
      }

      // Update genre metadata
      try {
        const snapshot = await db.collection("quiz_questions")
          .where("genre", "==", genre)
          .count()
          .get();
        const totalQuestions = snapshot.data().count;

        const categoriesSnapshot = await db.collection("quiz_questions")
          .where("genre", "==", genre)
          .select("category")
          .limit(500)
          .get();

        const uniqueCategories = [...new Set(
          categoriesSnapshot.docs.map((d) => d.data().category)
        )];

        await db.collection("quiz_metadata").doc(genre).set({
          genre: genre,
          totalQuestions: totalQuestions,
          categories: uniqueCategories,
          lastSeedAt: Date.now(),
        }, { merge: true });
      } catch (err) {
        errors.push(`Metadata update failed for ${genre}: ${err.message}`);
      }
    }
  }

  return { seeded, skippedDuplicates, errors };
}

// ── Fetch questions from Firestore for a quiz session ─────────────────────────
/**
 * Fetches quiz questions from Firestore for a given genre/category.
 *
 * @param {import("firebase-admin/firestore").Firestore} db
 * @param {Object} params
 * @param {string} params.genre — genre name
 * @param {string} [params.category] — optional category filter
 * @param {number} [params.count=10] — number of questions
 * @param {string[]} [params.excludeHashes] — hashes to exclude (dedup)
 * @returns {Promise<Object[]>} — array of question objects
 */
async function fetchQuizQuestions(db, params) {
  const { genre, category, count = 10, excludeHashes = [], recentFirst = false } = params;
  const excludeSet = new Set(excludeHashes.slice(0, 200)); // Cap to prevent abuse

  let query = db.collection("quiz_questions");
  let fetchCount = Math.min(count * 3, 150);

  if (recentFirst) {
    fetchCount = Math.min(Math.max(count * 15, 150), 300);
    query = query.orderBy("createdAt", "desc");
  } else {
    query = query.where("genre", "==", genre);
    if (category) {
      query = query.where("category", "==", category);
    }
  }

  const snapshot = await query.limit(fetchCount).get();

  const allQuestions = snapshot.docs
    .map((doc) => ({ hash: doc.id, ...doc.data() }))
    .filter((q) => q.genre === genre)
    .filter((q) => !category || q.category === category)
    .filter((q) => !excludeSet.has(q.hash));

  const selected = ifRecentFirst(allQuestions, recentFirst, count);

  return selected.map((q) => ({
    hash: q.hash,
    question: q.question,
    options: q.options,
    answer: q.answer,
    difficulty: q.difficulty,
    genre: q.genre,
    category: q.category || "",
  }));
}

function ifRecentFirst(questions, recentFirst, count) {
  if (recentFirst) {
    return questions.slice(0, count);
  }
  return questions.sort(() => Math.random() - 0.5).slice(0, count);
}

/**
 * Fetches quiz metadata for all genres or a specific genre.
 */
async function fetchQuizMetadata(db, genre) {
  if (genre) {
    const doc = await db.collection("quiz_metadata").doc(genre).get();
    return doc.exists ? [{ id: doc.id, ...doc.data() }] : [];
  }

  const snapshot = await db.collection("quiz_metadata").get();
  return snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
}

module.exports = {
  GENRE_CATEGORY_MAP,
  seedQuizQuestions,
  fetchQuizQuestions,
  fetchQuizMetadata,
  hashQuestion,
};
