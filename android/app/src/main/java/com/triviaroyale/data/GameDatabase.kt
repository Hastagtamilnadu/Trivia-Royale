package com.triviaroyale.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val CURRENT_STATE_ID = 1

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = CURRENT_STATE_ID,
    val username: String,
    val uid: String,
    val email: String?,
    val photoUrl: String?,
    val coins: Int,
    val xp: Int,
    val level: Int,
    val quizzesPlayed: Int,
    val quizzesWon: Int,
    val totalQuestionsAnswered: Int,
    val correctAnswers: Int,
    val streak: Int,
    val lastPlayedDate: String?,
    val achievementsJson: String,
    val lastPointsResetMonth: String?,
    val transactionsJson: String,
    val dailyTaskStateJson: String,
    val grandMasterLastPlayedAt: Long,
    val grandMasterLastPlayedDayKey: String?,
    val quizLaunchCooldownUntil: Long,
    val quizCooldownRewardSkipUsedForUntil: Long,
    val breakCumulativeSeconds: Int,
    val breakRewardSkipUsedForUntil: Long,
    val lastLocalSaveAt: Long,
    val lastCloudSyncAt: Long,
    val lastWithdrawalRequestedAt: Long,
    val dailyCoinDate: String?,
    val dailyCoinsEarned: Int,
    val dailyQuizCoinsEarned: Int,
    val dailyTaskCoinsEarned: Int,
    val dailyTaskClaims: Int,
    val localIntegrityHash: String
) {
    fun toState(gson: Gson): GameState.State {
        return GameState.State(
            username = username,
            uid = uid,
            email = email,
            photoUrl = photoUrl,
            coins = coins,
            xp = xp,
            level = level,
            quizzesPlayed = quizzesPlayed,
            quizzesWon = quizzesWon,
            totalQuestionsAnswered = totalQuestionsAnswered,
            correctAnswers = correctAnswers,
            streak = streak,
            lastPlayedDate = lastPlayedDate,
            achievements = parseAchievements(gson),
            lastPointsResetMonth = lastPointsResetMonth,
            transactions = parseTransactions(gson),
            dailyTaskState = parseDailyTaskState(gson),
            grandMasterLastPlayedAt = grandMasterLastPlayedAt,
            grandMasterLastPlayedDayKey = grandMasterLastPlayedDayKey,
            quizLaunchCooldownUntil = quizLaunchCooldownUntil,
            quizCooldownRewardSkipUsedForUntil = quizCooldownRewardSkipUsedForUntil,
            breakCumulativeSeconds = breakCumulativeSeconds,
            breakRewardSkipUsedForUntil = breakRewardSkipUsedForUntil,
            lastLocalSaveAt = lastLocalSaveAt,
            lastCloudSyncAt = lastCloudSyncAt,
            lastWithdrawalRequestedAt = lastWithdrawalRequestedAt,
            dailyCoinDate = dailyCoinDate,
            dailyCoinsEarned = dailyCoinsEarned,
            dailyQuizCoinsEarned = dailyQuizCoinsEarned,
            dailyTaskCoinsEarned = dailyTaskCoinsEarned,
            dailyTaskClaims = dailyTaskClaims,
            localIntegrityHash = localIntegrityHash
        )
    }

    private fun parseAchievements(gson: Gson): MutableList<String> {
        return runCatching {
            gson.fromJson<List<String>>(achievementsJson, stringListType)
        }.getOrDefault(emptyList()).toMutableList()
    }

    private fun parseTransactions(gson: Gson): MutableList<GameState.Transaction> {
        return runCatching {
            gson.fromJson<List<GameState.Transaction>>(transactionsJson, transactionListType)
        }.getOrDefault(emptyList()).toMutableList()
    }

    private fun parseDailyTaskState(gson: Gson): GameState.DailyTaskState {
        val parsed = runCatching {
            gson.fromJson(dailyTaskStateJson, GameState.DailyTaskState::class.java)
        }.getOrNull()

        return GameState.DailyTaskState(
            dayKey = parsed?.dayKey,
            baseQuizzesPlayed = parsed?.baseQuizzesPlayed ?: 0,
            baseQuestionsAnswered = parsed?.baseQuestionsAnswered ?: 0,
            baseCorrectAnswers = parsed?.baseCorrectAnswers ?: 0,
            baseWins = parsed?.baseWins ?: 0,
            dailyChallengePlayed = parsed?.dailyChallengePlayed ?: false,
            lightningRounds = parsed?.lightningRounds ?: 0,
            iplQuizzes = parsed?.iplQuizzes ?: 0,
            iplCorrectAnswers = parsed?.iplCorrectAnswers ?: 0,
            playSeconds = parsed?.playSeconds ?: 0,
            claimedTaskIds = parsed?.claimedTaskIds.orEmpty().toMutableList()
        )
    }

    companion object {
        private val stringListType = object : TypeToken<List<String>>() {}.type
        private val transactionListType = object : TypeToken<List<GameState.Transaction>>() {}.type

        fun fromState(state: GameState.State, gson: Gson): GameStateEntity {
            return GameStateEntity(
                username = state.username,
                uid = state.uid,
                email = state.email,
                photoUrl = state.photoUrl,
                coins = state.coins,
                xp = state.xp,
                level = state.level,
                quizzesPlayed = state.quizzesPlayed,
                quizzesWon = state.quizzesWon,
                totalQuestionsAnswered = state.totalQuestionsAnswered,
                correctAnswers = state.correctAnswers,
                streak = state.streak,
                lastPlayedDate = state.lastPlayedDate,
                achievementsJson = gson.toJson(state.achievements),
                lastPointsResetMonth = state.lastPointsResetMonth,
                transactionsJson = gson.toJson(state.transactions),
                dailyTaskStateJson = gson.toJson(state.dailyTaskState ?: GameState.DailyTaskState()),
                grandMasterLastPlayedAt = state.grandMasterLastPlayedAt,
                grandMasterLastPlayedDayKey = state.grandMasterLastPlayedDayKey,
                quizLaunchCooldownUntil = state.quizLaunchCooldownUntil,
                quizCooldownRewardSkipUsedForUntil = state.quizCooldownRewardSkipUsedForUntil,
                breakCumulativeSeconds = state.breakCumulativeSeconds,
                breakRewardSkipUsedForUntil = state.breakRewardSkipUsedForUntil,
                lastLocalSaveAt = state.lastLocalSaveAt,
                lastCloudSyncAt = state.lastCloudSyncAt,
                lastWithdrawalRequestedAt = state.lastWithdrawalRequestedAt,
                dailyCoinDate = state.dailyCoinDate,
                dailyCoinsEarned = state.dailyCoinsEarned,
                dailyQuizCoinsEarned = state.dailyQuizCoinsEarned,
                dailyTaskCoinsEarned = state.dailyTaskCoinsEarned,
                dailyTaskClaims = state.dailyTaskClaims,
                localIntegrityHash = state.localIntegrityHash
            )
        }
    }
}

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = :id LIMIT 1")
    fun getState(id: Int = CURRENT_STATE_ID): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: GameStateEntity)

    @Query("DELETE FROM game_state")
    fun clearAll()
}

@Database(
    entities = [GameStateEntity::class],
    version = 7,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao

    companion object {
        @Volatile
        private var instance: GameDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN dailyTaskStateJson TEXT NOT NULL DEFAULT '{}'"
                )
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN grandMasterLastPlayedAt INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN grandMasterLastPlayedDayKey TEXT"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN quizLaunchCooldownUntil INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN quizCooldownRewardSkipUsedForUntil INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN breakCumulativeSeconds INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE game_state ADD COLUMN breakRewardSkipUsedForUntil INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): GameDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "trivia_royale.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
