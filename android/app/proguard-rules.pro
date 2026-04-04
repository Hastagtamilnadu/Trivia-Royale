# Keep Compose-related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Gson model types used by GameState serialization
-keep class com.triviaroyale.data.GameState$State { *; }
-keep class com.triviaroyale.data.GameState$Transaction { *; }
-keep class com.triviaroyale.data.GameState$RPRank { *; }

# Keep nested GameState data-class members used with Gson
-keepclassmembers class com.triviaroyale.data.GameState$* {
    <fields>;
    <init>(...);
}
