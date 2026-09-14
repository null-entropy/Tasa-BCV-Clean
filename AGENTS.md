# AGENTS.md — Tasa BCV - Cleam

> Documento de referencia del proyecto. Léelo completo antes de tocar nada.

---

## 1. Qué es

**Tasa BCV - Cleam** (antes "Dólar BCV" / "Tasa BCV - Clean") es una app Android
nativa (Kotlin + Jetpack Compose). Muestra las tasas oficiales del dólar y el
euro del Banco Central de Venezuela (BCV) y trae calculadora bidireccional
Bs ↔ USD/EUR con formato venezolano. Dos pestañas: **Tasas** y **Calculadora**.

**Dato clave del dominio:** el BCV publica un solo valor por día hábil bancario
(~4–6:30 pm hora Caracas). No existe tasa "en tiempo real": la app nunca simula
actualizaciones intradía.

---

## 2. Stack (no cambiar sin razón)

| Componente | Versión |
| --- | --- |
| Kotlin plugin / `kotlinCompilerExtensionVersion` | 1.9.24 / 1.5.14 |
| AGP / Gradle wrapper / JDK | 8.5.2 / 8.7 / 17 |
| minSdk / targetSdk / compileSdk | 24 / 34 / 34 (build-tools 34.0.0) |
| Compose | BOM `2024.06.00`, Material 3 |

Deps: `core-ktx:1.13.1`, `activity-compose:1.9.1`, `lifecycle-runtime-ktx:2.8.4`,
`lifecycle-viewmodel-compose:2.8.4`, `kotlinx-coroutines-android:1.8.1`,
Compose BOM (`ui`, `ui-graphics`, `ui-tooling-preview`, `foundation`, `material3`),
`material-icons-extended`, `navigation-compose:2.7.7`, `ui-tooling` (debug).

- **Red/JSON:** `HttpURLConnection` + `org.json` (SDK). NO Retrofit/OkHttp/Gson/Moshi.
- **Fechas:** sin `java.time` (minSdk 24 sin desugaring).
- Tipografía Poppins embebida (4 pesos). Sin anuncios, analítica ni Firebase.

---
## 3. Estructura

```
├── AGENTS.md / KEYSTORE_INFO.txt      ← este doc y el de la clave de firma (crítico)
├── keystore.properties                ← credenciales de firma (NO se sube a Git)
├── local.properties                   ← ruta del SDK local (NO se sube a Git)
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradlew(.bat) / gradle/wrapper/    ← wrapper 8.7
├── keystore/dolarbcv-release.keystore ← CLAVE DE FIRMA. Irreemplazable.
└── app/
    ├── build.gradle.kts               ← deps, firma, tarea copyReleaseToDownloads
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/tasabcv/app/
        │   ├── MainActivity.kt        ← Activity + NavHost + bottom bar
        │   ├── TasasScreen.kt         ← barra sup. fecha/refresh + tarjetas + odómetro
        │   ├── CalculadoraScreen.kt   ← calculadora bidireccional
        │   ├── CurrencyFormat.kt      ← VisualTransformation VE + sanitize
        │   ├── ClipboardUtils.kt      ← copiar/compartir
        │   ├── Components.kt          ← bounceClick, SlidingSegmentedControl
        │   ├── Depth.kt               ← neumorfismo neuRaised/neuInset/neuSurface
        │   ├── Theme.kt               ← paleta Grafito + Poppins
        │   ├── Format.kt              ← números VE, fechas ISO (sin java.time)
        │   ├── DolarViewModel.kt      ← estado UI + refresh coordinado
        │   ├── DolarRepository.kt     ← red + caché + 3 proveedores
        │   └── DolarWidgetProvider.kt ← widget RemoteViews
        └── res/                       ← drawable, font, layout/widget_dolar.xml,
                                         mipmap-anydpi-v26, values(+night), xml/
```

Regenerable (no se versiona): `.gradle/`, `app/build/`. **NUNCA borrar ni
modificar:** `keystore/`, `gradle/wrapper/gradle-wrapper.jar`, `app/src/`.

---

## 4. Arquitectura y estado

```
UI (Compose) → collectAsState() → DolarViewModel (AndroidViewModel)
  → DolarRepository (3 proveedores en cadena + caché SharedPreferences)
  → HTTP GET (HttpURLConnection)
```

- **`DolarUiState`** = `usdState` + `eurState`, ambos `SingleRateState`:
  `Loading` | `Loaded(rate, isFromCache, warning)` | `Empty(errorMessage)`.
  `isFromCache=true` **con** `warning` → pie "Sin conexión: mostrando el último
  dato guardado" en la ficha (solo cuando un fetch terminó en error; la caché
  pintada al instante en el arranque NO lo muestra). Sin aviso, el pie se
  colapsa por completo (0dp) y la zona de tasa crece: valor 52/46/40sp y delta
  17sp; con aviso vuelve a 48/42/36sp y delta 16sp (transiciones animadas).
- **Barra superior de Tasas (v4.4.7):** superficie elevada con la estética de
  las fichas (relieve neumorfista v4.5.0: esquinas 32dp) que muestra
  "Actualizado: dd/MM/yyyy" (fecha del primer estado Loaded; "—" si no hay
  datos) a la izquierda y el botón de refrescar a la derecha (círculo 44dp,
  spinner mientras `isRefreshing`). Sustituye al header con título; la fecha
  ya no aparece dentro de las fichas.
- **`loadOnFirstOpen()` (v4.3.1):** muestra la caché al instante
  (`showCacheNow()`, sin pantalla vacía) y refresca en background solo si la
  caché tiene más de 30 min o nunca hubo fetch. Con caché reciente NO se golpea
  la red (un valor por día hábil; abrir la app a los 5 min no amerita descarga).
- **`refresh()` (v4.3.1):** las tasas visibles se MANTIENEN (caché o dato
  fresco) mientras USD y EUR se descargan en paralelo; los estados se actualizan
  solo cuando **ambas** terminaron. `isRefreshing` es un `StateFlow` aparte del
  estado mostrado: alimenta el botón de actualizar y bloquea dobles fetch.
- **`onAppResumed()`:** en `MainActivity.onResume()` revalida solo si pasaron
  ≥ 30 min desde el último fetch exitoso. Sin polling en 2º plano ni WorkManager.
- **Rotación:** ViewModel sobrevive (`by viewModels()`); calculadora usa
  `rememberSaveable`; navegación con `saveState`/`restoreState`.
- **Caché:** `SharedPreferences` `dolar_bcv_prefs`, claves por moneda:
  `promedio_*`, `fecha_*`, `fuente_*`, `cache_time_*`, `previous_*`; más
  `widget_currency`. Valores numéricos como String (tolera Float legacy).
  Sanity check `(0, 1_000_000]` en todos los proveedores.
- **Delta:** local (ningún proveedor da histórico). Si el nuevo valor difiere del
  anterior (umbral `sameRate` 0,00005), el viejo pasa a `previous_*`.
  Se muestra SOLO en la pantalla Tasas; el widget no lo usa (v4.3.1).

---

## 5. Fuentes de datos

El BCV no tiene API pública. Cadena de 3 proveedores; el primero válido gana.
Timeout **8 s por fuente**. Constantes en `DolarRepository.companion`;
`USER_AGENT = "TasaBCVCleam-App/4.5.0 (Android)"`.

| # | Proveedor | USD | EUR | Valor | Fecha |
| - | --- | --- | --- | --- | --- |
| 1 | dolarapi.com | `…/v1/dolares/oficial` | `…/v1/euros/oficial` | `promedio` | `fechaActualizacion` |
| 2 | pydolarve.dev | `…/v1/dollar/bcv` | `…/v1/euro/bcv` | `price` | `last_update` |
| 3 | bcv.org.ve | `https://www.bcv.org.ve/` (scraping regex) | igual, regex euro/EUR | regex | vacía |

**Reglas:** 1) No cambiar el orden (dolarapi es el más fiable; el scraping del
BCV es frágil, si su HTML cambia se revisa primero). 2) Solo HTTPS
(`usesCleartextTraffic="false"`). 3) Fin de semana: los proveedores devuelven el
valor vigente; no se calcula "qué tasa toca según el día". 4) pydolarve.dev está
caído (NXDOMAIN) pero se mantiene por si vuelve; la cadena efectiva es
dolarapi + BCV + caché. 5) El HTML del BCV se descarga **una sola vez por ciclo
de refresh** (`getBcvHtml()` memoizado, TTL 10 s) y se reutiliza para USD y EUR:
evita descargar ~200KB dos veces cuando la cadena cae al proveedor #3.
`USER_AGENT = "TasaBCVCleam-App/4.5.0 (Android)"`.

---

## 6. Compilación y entrega

**Requisitos:** JDK 17, Android SDK en `C:\Android\Sdk` (platform-tools,
`platforms;android-34`, `build-tools;34.0.0`). No se necesita Android Studio.
**Gotcha:** AGP falla con rutas no-ASCII en Windows; ya resuelto con
`android.overridePathCheck=true` y `local.properties` con barras normales.

**Compilar release y dejar el APK en Descargas (entrega estándar):**

```powershell
cd "C:\Users\Admin\Folder Code - Root\Tasa BCV - Cleam"
.\gradlew copyReleaseToDownloads
```

- La tarea `copyReleaseToDownloads` (`app/build.gradle.kts`) compila
  `assembleRelease` y copia el APK firmado a
  `C:\Users\Admin\Downloads\TasaBCV-Cleam-v<version>.apk`.
- **release:** minify + shrinkResources + firma con el keystore (lo que se
  entrega). **debug:** sin minificar. Salida intermedia:
  `app/build/outputs/apk/release/app-release.apk`.

---

## 7. Firma y keystore (CRÍTICO)

| Dato | Valor |
| --- | --- |
| Archivo | `keystore/dolarbcv-release.keystore` (PKCS12, alias `dolarbcv`, RSA 2048, 10.000 días) |
| Contraseñas | `keystore.properties` (NO se sube a Git) |
| DN del certificado | `CN=Dolar BCV, OU=App, O=Dolar BCV, L=Caracas, ST=Distrito Capital, C=VE` |

**Reglas:** 1) **NUNCA perder este keystore** (sin él no hay actualizaciones de
`com.tasabcv.app`). El keystore, alias y CN conservan a propósito la identidad
legacy "Dolar BCV": es la clave original de la cadena de firma. No renombrar ni
regenerar. 2) **NUNCA subirlo a un repositorio público** (tampoco
`keystore.properties`); ambos en `.gitignore`. Respaldo manual en
`KEYSTORE_INFO.txt`. 3) `storeFile` en `keystore.properties` es relativo a la
raíz del proyecto (`rootProject.file(...)`).

**Verificar firma:**

```bash
"C:/Android/Sdk/build-tools/34.0.0/apksigner.bat" verify --verbose --print-certs "$env:USERPROFILE\Downloads\TasaBCV-Cleam-v4.5.0.apk"
```

Debe decir `Verifies` y `CN=Dolar BCV`.

---

## 8. Identidad

| Campo | Valor |
| --- | --- |
| applicationId | `com.tasabcv.app` (migrado de `com.dolarbcv.app` en v4.0.0: Android lo trata como app nueva; usuarios con ≤ 3.1.0 deben desinstalar) |
| versionCode / versionName | 19 / `4.5.0` (en `app/build.gradle.kts` → `defaultConfig`; cada release requiere versionCode mayor) |
| Nombre visible | `Tasa BCV - Cleam` (`strings.xml` → `app_name`) |
| Permisos | solo `INTERNET` y `ACCESS_NETWORK_STATE` |

---

## 9. ProGuard / R8

Release minifica + shrinkResources. Las reglas mantienen: data classes del modelo,
componentes del manifest (`MainActivity`, `DolarWidgetProvider`), runtime Compose,
`kotlin.Metadata`, `org.json`. **Si añades una librería con reflexión, agrega sus
reglas** o el release crasheará aunque el debug funcione.

---

## 10. Widget de pantalla de inicio

- `DolarWidgetProvider` (RemoteViews clásico, **no** Glance).
- Layout `res/layout/widget_dolar.xml`; colores en `values(-night)/colors.xml`;
  metadata `res/xml/dolar_widget_info.xml` (2x2, update 30 min, descripción API 31+).
- **Solo lee caché, no hace HTTP.** `widget_currency` (default `"USD"`) elige
  moneda; toggle por broadcast `com.tasabcv.app.TOGGLE_CURRENCY`; tocar el
  cuerpo abre `MainActivity`. `refreshAll()` se llama desde el ViewModel tras
  cada fetch exitoso.
- **Sin delta (v4.3.1):** no muestra ▲/▼ de variación. Elementos: chip de
  moneda arriba, valor grande (weight 3, autoSize 18-44sp) centrado como un
  solo bloque con el sufijo "Bs" más pequeño en el mismo texto vía
  `RelativeSizeSpan` (0.5x, v4.4.7: grupo dígitos+Bs centrado, sin anclajes al
  borde), fecha al pie — proporciones rebalanceadas. `contentDescription` en
  chip/título/cuerpo; el TextView del valor lleva `contentDescription` "X Bs"
  para accesibilidad.

---

## 11. Ícono y tema

- **Adaptive icon** "Grafito" (v4.5.0): fondo degradado negro
  `#1C1D21→#101114→#060708`, glifo `$` blanco, monochrome para Android 13+.
  `mipmap-anydpi-v26/` + legacy `drawable/ic_launcher.xml`.
- **Paleta "Grafito" (v4.5.0): identidad MONOCROMA, sin azul.** Definida en
  `Theme.kt` como `AppColors` (expuesta vía `LocalAppColors`/`appColors`);
  `DolarBCVTheme` sin dynamic color.
  - Claro: fondo `#EFF0F3→#E8E9ED`, superficies casi blancas
    `#FCFDFE→#F1F2F5`, texto `#17181B`, acento casi negro `#1B1C20`.
  - Oscuro (dark REAL, neutro): fondo `#0A0A0C→#060607`, superficies
    `#1B1B1F→#121215`, texto `#F4F4F5`, acento casi blanco `#E6E7EA`.
  - Delta adaptado por tema: claro `#F87171`/`#34D399` (claritos),
    oscuro `#DC2626`/`#059669` (oscuros). Errores vía `colorScheme.error`.
- **Neumorfismo (v4.5.0) en `Depth.kt`:** relieve pronunciado SIN blur
  (costo GPU mínimo): superficies elevadas = degradado diagonal + bisel
  (borde con degradado luz→sombra) + sombra proyectada; huecos hundidos =
  bisel invertido sin sombra (`neuRaised`/`neuInset`/`neuSurface`). Los
  botones circulares (refresh, copiar, compartir) se HUNDEN al presionar
  (swap raised→inset + scale de `bounceClick`). El `SlidingSegmentedControl`
  es un hueco con píldora indicadora elevada. Fondos de pantalla con
  degradado vertical sutil. El widget replica la estética con degradados XML
  (`widget_background.xml`, `widget_currency_chip.xml`; colores neutros en
  `values(-night)/colors.xml`).
- Splash (`Theme.DolarBCV` en `themes.xml`) usa el mismo fondo (sin flash).
- Poppins vía `AppTypography` (y `android:fontFamily` en el widget).
- **Edge-to-edge (v4.3.1):** `enableEdgeToEdge()` en `MainActivity`; la barra
  inferior usa `navigationBarsPadding()` y las pantallas pisan el fondo tras las
  barras del sistema. `Theme.kt` solo controla la apariencia de los íconos de la
  barra de estado; los colores de las barras (`statusBarColor`/
  `navigationBarColor`) solo los define el tema XML del splash (`themes.xml`),
  para la ventana de arranque (en runtime las barras quedan transparentes).
- **Haptics (v4.3.1):** `bounceClick` (`Components.kt`) dispara un tick sutil
  (`HapticFeedbackType.TextHandleMove`) al presionar. El botón de refrescar usa
  además `pressedScale=0.85f` + `RefreshPressSpring` (v4.4.7): hundimiento con
  "peso" y rebote contenido, más evidente que el 0.94f estándar de segmentos y
  botones copiar.
- **Lenguaje de formas (v4.4.7):** fichas, barra superior de Tasas, barra de
  navegación inferior y selector USD/EUR de la calculadora comparten esquinas
  redondeadas de 32dp (la inferior ya era píldora 64dp/32dp; el selector pasó
  de 28 a 32dp vía el parámetro `cornerRadius` de `SlidingSegmentedControl`).

---

## 12. Calculadora (formato venezolano)

- Punto `.` = miles (visual), coma `,` = decimal (máx. 2). Ej: `1.500.000,50`.
- `baseText`/`bsText` guardan **solo dígitos y coma**; los puntos los dibuja
  `CurrencyVisualTransformation` (`CurrencyFormat.kt`) con `OffsetMapping`
  bidireccional. `sanitizeInput()` filtra; `Format.parseUserInput()` → Double;
  `formatDoubleToRaw()` → texto con coma.
- Selector USD/EUR = `FloatingSegmentedButton` (basado en
  `SlidingSegmentedControl`, `Components.kt`): indicador que **se desliza con
  spring**; esquinas 32dp (v4.4.7). Al cambiar moneda se limpian los campos.
- Animaciones: entrada de NavHost con `scaleIn` + spring (overshoot sutil);
  cambio USD↔EUR con carrusel horizontal; actualización de tasa con **odómetro
  por dígitos** (v4.4.7): 12 slots anclados a la derecha, cada carácter rueda
  de forma independiente (hacia arriba si sube, hacia abajo si baja) con fade
  dominante y clip por slot — sin cortes ni superposiciones. Todas nativas de
  Compose.

---

## 13. Git

- Repo local (rama `main`) con **remoto `origin`** en **GitHub** (privado):
  `https://github.com/Sebas040810/Tasa-BCV-Cleam` (cuenta `Sebas040810`).
- **Auto-push tras cada commit (hook `post-commit`):** el archivo
  `.git/hooks/post-commit` (NO se versiona) ejecuta `git push --quiet origin HEAD`
  después de cada commit del usuario, por lo que cada commit sube solo a GitHub.
  Si no hay red, el commit se queda local y el push se reintenta en el próximo
  commit (o con `git push` a mano). El hook es solo de esta máquina.
- Desactivar el auto-push: borrar `.git/hooks/post-commit`. Hacer el repo
  público: `gh repo edit Sebas040810/Tasa-BCV-Cleam --visibility public`
  (requiere `gh auth login`).
- `.gitignore` excluye: `.gradle/`, `build/`, `app/build/`, `.idea/`, `.vscode/`,
  `local.properties`, `keystore/`, `keystore.properties`, `*.apk`.
- **Nunca** hacer commit del keystore ni de `keystore.properties` (regla §7).
  Con el auto-push activo esto es crítico: un commit con secretos se sube solo.
- **REGLAS DE COLABORACIÓN DEL AGENTE (obligatorias):**
  - **NUNCA hacer `git commit` ni `git push` automáticamente.**
  - Siempre preguntar al usuario y **esperar su confirmación explícita** antes de
    comitear, incluso cuando el agente considere el commit muy necesario.
  - Solo comitear si el usuario lo pide explícitamente.
  - Al terminar una tarea con cambios, presentar el estado del repo
    (`git status`/`git diff` resumido) y dejar la decisión de commitear al usuario.
  - Nota: si el usuario autoriza un commit, el hook `post-commit` lo subirá solo
    a GitHub. Eso NO contradice la regla de arriba: el push lo ejecuta el hook
    (mecanismo del usuario), nunca el agente.

---

## 14. Tareas comunes

- **Publicar versión:** subir `versionCode`/`versionName` en
  `app/build.gradle.kts`, `.\gradlew copyReleaseToDownloads`, verificar firma
  con apksigner (§6-7).
- **Añadir una moneda (ej. USDT):** 1) verificar que dolarapi la soporte;
  2) rama en los 3 proveedores de `DolarRepository`; 3) tercer `SingleRateState`
  en `DolarUiState`; 4) tercera tarjeta en `TasasScreen`; 5) opción en la
  calculadora; 6) actualizar este documento.
- **Cambiar colores:** `Theme.kt` (`LightColors`/`DarkColors`), contraste AA.
- **Regenerar wrapper si se borra el .jar:**
  `gradle wrapper --gradle-version 8.7 --distribution-type bin`.
- **"La app no trae datos":** 1) probar endpoints con curl (§5); 2) si el BCV
  cambió su HTML, las regex del proveedor #3 se rompen → revisar primero;
  3) el caché (`dolar_bcv_prefs`) debe tener un valor previo.

---

## 15. Checklist de cierre

- [ ] Temas claro/oscuro 100% monocromos (sin azul): claro blanco/gris claro, oscuro negro/gris; delta verde/rojo adaptado por tema
- [ ] Loading/Loaded/Empty funcionan para USD y EUR
- [ ] Caché instantánea: con caché previo la app muestra valores al instante y refresca en background; sin caché muestra Loading
- [ ] Modo offline muestra caché con advertencia (no crashea); sin aviso el pie de la ficha no deja espacio en blanco
- [ ] Odómetro: dígitos ruedan hacia arriba si la tasa sube y hacia abajo si baja, con fade y sin superposiciones
- [ ] Calculadora no crashea con entradas inválidas (vacío, letras, enormes); en Loading/Empty muestra su estado, no "No hay tasa"
- [ ] Puntos de miles y coma decimal correctos en la calculadora
- [ ] Widget sin delta: muestra tasa y fecha con proporciones armónicas; toggle USD/EUR funciona
- [ ] `copyReleaseToDownloads` termina con `BUILD SUCCESSFUL` y el APK en Descargas
- [ ] APK release firmado con `CN=Dolar BCV` (no debug)
- [ ] No se subió el keystore ni `keystore.properties` al repositorio
- [ ] Cada commit sube a GitHub (hook `post-commit` activo; `origin/main` al día)
- [ ] versionCode mayor al anterior (si hubo release)
- [ ] Reglas ProGuard actualizadas (si se añadieron librerías con reflexión)

---

## 16. Limitaciones conocidas (no son bugs)

1. Dependencia de proveedores comunitarios (dolarapi/pydolarve); si caen →
   caché o scraper. pydolarve actualmente NXDOMAIN (§5).
2. El aviso "aplicación no reconocida" al instalar es del SO Android (APK fuera
   de Play Store), no corregible por código.
3. Sin emulador/dispositivo en el entorno de build; la verificación física se
   hace fuera de este entorno.
4. El scraping del BCV es frágil (depende del HTML, que cambia sin aviso).

---

## 17. Convenciones

- Comentarios en español. Paquete `com.tasabcv.app`.
- Estilo Kotlin oficial; opt-ins `ExperimentalMaterial3Api` y
  `ExperimentalFoundationApi` habilitados globalmente en `app/build.gradle.kts`.
