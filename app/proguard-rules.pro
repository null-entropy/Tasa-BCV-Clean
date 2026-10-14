# ==========================================================================
#  Reglas ProGuard / R8 para Tasa BCV - Clean (com.tasabcv.app)
# ==========================================================================
#  Compose + HttpURLConnection + org.json. Sin reflexión de aplicación.
#  El manifest retiene Activity/Widget; aquí reforzamos modelo y puntos de entrada.

# ---------- Modelo de datos ----------
-keep class com.tasabcv.app.DolarRate { *; }
-keep class com.tasabcv.app.DolarResult { *; }
-keep class com.tasabcv.app.DolarResult$* { *; }
-keep class com.tasabcv.app.DolarUiState { *; }
-keep class com.tasabcv.app.SingleRateState { *; }
-keep class com.tasabcv.app.SingleRateState$* { *; }
-keep class com.tasabcv.app.UpdateInfo { *; }
-keep class com.tasabcv.app.ManualUpdateState { *; }
-keep class com.tasabcv.app.ManualUpdateState$* { *; }

# ---------- Componentes instanciados por el sistema (manifest) ----------
-keep class com.tasabcv.app.MainActivity { *; }
-keep class com.tasabcv.app.DolarWidgetProvider { *; }

# ---------- Compose ----------
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ---------- Kotlin Metadata ----------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---------- org.json ----------
-dontwarn org.json.**
