 AGENTS.md — Tasa BCV - Cleam

> Referencia rápida para agentes. Solo lo no obvio — si no evita un error, no está aquí.
> Este documento manda: si cambias código, actualiza el doc en el mismo cambio. Verificar coherencia en cada tarea.

## 1. Qué es
App Android nativa Kotlin + Compose. Muestra dólar/euro oficial BCV y calculadora Bs↔USD/EUR. Tres tabs: **Ajustes** · **Tasas** · **Calculadora** (Ajustes a la izquierda de Tasas; Tasas SIEMPRE es el tab inicial). Tasas y Calculadora NO tienen título de encabezado (el contenido arranca directo). Paquete `com.tasabcv.app`.

**Lema (texto canónico de la app, string `ajustes_info_lema` en la ficha de información de Ajustes):** "El lema de la aplicación es ser robusta, minimalista, ligera, rápida, optimizada y sin anuncios". Es la brújula de diseño: TODA decisión (código, diseño, dependencia) debe mantenerlo. Si algún día cambia el texto en la UI, actualizar esta línea y las menciones de lema en §10 en el mismo cambio.

**MANDATO DE OPTIMIZACIÓN (estRICTO, aplica a TODO cambio):** toda implementación nueva debe justificar su costo (CPU/GPU/memoria/tamaño de APK). Si dos diseños logran lo mismo, entra el más ligero. Prohibido: trabajo pesado de dibujo por frame (sombras/degradados interpolados por frame, animaciones que nunca asientan), pollings, dependencias nuevas sin necesidad, recomposiciones evitables. El lema manda sobre el brillo visual: si una animación es más "bonita" pero más cara, se elige la barata.

**Dato clave del dominio:** el BCV publica **un solo valor por día hábil** (~4–6:30 pm hora Caracas). No existe tasa "en tiempo real": la app nunca simula actualizaciones intradía. La app usa dolarapi.com como proveedor único (el BCV no tiene API pública) y guarda caché local.

## 2. Stack (no cambiar sin razón)
- Kotlin 1.9.24 / `kotlinCompilerExtensionVersion` 1.5.14 / AGP 8.5.2 / Gradle wrapper 8.7 / JDK 17 / compileSdk 34 (build-tools 34.0.0) / minSdk 24 / targetSdk 34 / Compose BOM 2024.06.00 + Material3
- Deps: `core-ktx:1.13.1`, `activity-compose:1.9.1`, `lifecycle-runtime-ktx:2.8.4`, `lifecycle-viewmodel-compose:2.8.4`, `kotlinx-coroutines-android:1.8.1`, `material-icons-extended`, `navigation-compose:2.7.7`, `ui-tooling` (debug); Compose BOM 2024.06.00 aporta `ui`, `ui-graphics`, `ui-tooling-preview`, `foundation`, `material3`
- **Red/JSON:** `HttpURLConnection` + `org.json` únicamente. No Retrofit/OkHttp/Gson.
- **Fechas:** sin `java.time` (minSdk 24 sin desugaring). `Format.kt` parsea ISO por prefijo de String.
- Poppins embebida (4 pesos). Sin anuncios/analytics/Firebase.

## 3. Mapa mínimo
```
keystore/dolarbcv-release.keystore  # irreemplazable
keystore.properties / local.properties  # no se commitean
KEYSTORE_INFO.txt  # respaldo documentado de la clave de firma (crítico)
app/build.gradle.kts  # versionCode/versionName + firma + copyReleaseToDownloads
app/src/main/java/com/tasabcv/app/
  MainActivity.kt      # edge-to-edge + NavHost (3 tabs, transiciones por índice) + bottom bar
  ConfiguracionScreen.kt # tab Ajustes: ficha añadir widget + ficha info (versión/fuente/lema)
  TasasScreen.kt       # topBar fecha/refresh + CurrencyCard + odómetro
  CalculadoraScreen.kt # bidireccional, sin título de encabezado
  DolarRepository.kt   # dolarapi.com + caché SharedPreferences
  DolarViewModel.kt    # uiState + isRefreshing
  DolarWidgetProvider.kt # RemoteViews (no Glance)
  Format.kt / CurrencyFormat.kt / ClipboardUtils.kt / Theme.kt / Depth.kt / Components.kt
app/src/main/res/{layout/widget_dolar.xml, xml/dolar_widget_info.xml, values/*}
```
Regenerable (no versionar): `.gradle/`, `app/build/`. Nunca borrar ni renombrar: `keystore/`, `gradle-wrapper.jar`. `app/src/` es el código editable.

## 4. Arquitectura y estado
```
Compose collectAsState() → DolarViewModel → DolarRepository (dolarapi.com + caché SharedPreferences) → HTTP GET (HttpURLConnection)
```
- `DolarUiState = usdState + eurState` : `Loading | Loaded(rate,isFromCache,warning) | Empty`. `warning` solo cuando un fetch terminó en error y se muestra caché; la caché pintada al instante en el arranque NO lo muestra. Pie SIEMPRE presente: pill de estado de conexión (estilo chip del delta, color = estado) PERFECTAMENTE centrada en TODOS los estados (el botón "Reintentar" fue eliminado; el reintento vive en el círculo de refrescar de la barra superior y en `onAppResumed`). Estados: `Actualizado` verde / `Sin conexión; mostrando caché` / `Sin conexión; sin caché` rojo / `Conectando…` / `Actualizando…` durante refresco.
- `loadOnFirstOpen()` muestra caché instantánea (`showCacheNow()`), refresca solo si >30min o sin fetch. `refresh()` mantiene visible y actualiza USD+EUR juntos al terminar; `isRefreshing` separado bloquea dobles fetch.
- `onAppResumed()` revalida solo si ≥30min desde el último fetch exitoso. Sin polling en 2º plano ni WorkManager.
- **Caché** `dolar_bcv_prefs`: `promedio_*`, `fecha_*`, `fuente_*`, `cache_time_*`, `previous_*` + `widget_currency`. Números como String (tolerante Float legacy). Sanity `(0,1_000_000]`. Delta local `sameRate` 0.00005, solo en Tasas.

## 5. Fuente de datos
El BCV no tiene API pública; la app usa **dolarapi.com como proveedor único** (antes había una cadena de 3 proveedores: pydolarve.dev y bcv.org.ve fueron eliminados de forma profunda por estar caído el primero y ser frágil el scraping del segundo). Timeout **6 s por moneda**. Constantes en `DolarRepository.companion`; `USER_AGENT = "TasaBCVCleam-App/4.7.3 (Android)"`.

| Moneda | Endpoint | Valor | Fecha |
| --- | --- | --- | --- |
| USD | `https://ve.dolarapi.com/v1/dolares/oficial` | `promedio` | `fechaActualizacion` |
| EUR | `https://ve.dolarapi.com/v1/euros/oficial` | `promedio` | `fechaActualizacion` |

Reglas: 1) Solo HTTPS (`usesCleartextTraffic="false"`). 2) Fin de semana: el proveedor devuelve el valor vigente; no se calcula "qué tasa toca según el día". 3) Si el fetch falla, se muestra la caché con advertencia (o error si no hay caché). 4) USD y EUR se descargan en paralelo (2 GETs independientes; no existe endpoint combinado en ve.dolarapi.com). 5) La respuesta es un body mínimo (~200 bytes) que se lee directo del InputStream y se parsea con `JSONObject` (el `org.json` del SDK no acepta Reader). 6) La región Venezuela solo soporta USD y EUR (oficial BCV; también expone paralelo Yadio, no usado). Sin USDT/criptos.

## 6. Build y entrega
> Solo compilar / `copyReleaseToDownloads` si el usuario lo pide con palabras claras. Nunca asumir `versionName`/`versionCode`: si no los dio explícitos, preguntar siempre antes de compilar.

**AUDITORÍA PRE-COMPILACIÓN (OBLIGATORIA):** tras cualquier implementación y ANTES de compilar, auditar y verificar que todo salió a la perfección — compilar con código sin auditar está prohibido:
1. Balance de llaves/paréntesis en cada Kotlin tocado (script de conteo `{`/`}`, `(`/`)`).
2. Referencias cruzadas resueltas: strings, íconos, constantes, funciones; imports sin uso eliminados.
3. XML bien formados (`[xml]` cast en strings/widget info) y encoding UTF-8 intacto (acentos/flechas: las herramientas de shell de Windows pueden dañarlos — verificar y reparar si hay mojibake).
4. Revisión visual del código nuevo (indentación, estructura, coherencia con el estilo del proyecto).
5. `AGENTS.md` coherente con el código (misma regla §15).

Después de compilar: 0 errores / 0 advertencias en el log, firma verificada (`apksigner verify`), versión del APK confirmada (`aapt2 dump badging`) y peso revisado (checklist §13).

Requisitos: JDK 17, SDK ÚNICO del proyecto `C:\Android\Sdk` (platform-tools, platforms;android-34, build-tools;34.0.0; licenses y cmdline-tools incluidos). **STUDIO-FREE:** prohibido abrir el proyecto con Android Studio; no debe existir `.idea/` ni artefactos de Studio (si reaparecen, borrarlos; `.gitignore` los excluye). Gradle usa `sdk.dir` de `local.properties` → SIEMPRE `C:\Android\Sdk` (barras normales). Gotcha Windows: ruta no-ASCII falla AGP → ya resuelto `android.overridePathCheck=true`.

```powershell
cd "C:\Users\Admin\Proyectos\Tasa BCV - Cleam"
.\gradlew copyReleaseToDownloads
```
Compila `assembleRelease` (minify+shrinkResources+firma) y copia a `C:\Users\Admin\Downloads\TasaBCV-Cleam-v<version>.apk`. Salida `app/build/outputs/apk/release/app-release.apk`.

## 7. Firma (CRÍTICO)
| Archivo | `keystore/dolarbcv-release.keystore` (PKCS12, alias `dolarbcv`, RSA 2048) |
| Contraseñas | `keystore.properties` (gitignore) |
| DN | `CN=Dolar BCV, OU=App, O=Dolar BCV, L=Caracas, ST=Distrito Capital, C=VE` |

Nunca perder/regenerar/renombrar (identidad legacy `Dolar BCV` intencional). Nunca commitear `keystore/` ni `keystore.properties`. `storeFile` relativo a raíz (`rootProject.file(...)`). Verificar:
```bash
"C:/Android/Sdk/build-tools/34.0.0/apksigner.bat" verify --verbose --print-certs "$env:USERPROFILE\Downloads\TasaBCV-Cleam-v<version>.apk"
# debe decir Verifies + CN=Dolar BCV
```

## 8. Identidad
`applicationId com.tasabcv.app` (ex `com.dolarbcv.app` v4.0.0), `versionCode 33 / versionName 4.7.3` en `app/build.gradle.kts` (cada release requiere `versionCode` mayor), nombre `Tasa BCV - Cleam`, permisos solo `INTERNET` + `ACCESS_NETWORK_STATE`.

## 9. Widget
`DolarWidgetProvider` RemoteViews clásico. `widget_dolar.xml` + `dolar_widget_info.xml` (4×2 celdas apaisado: `targetCellWidth 4` / `targetCellHeight 2`, `minWidth 250dp` / `minHeight 110dp` para launchers pre-12, 30min). **Solo lee caché, no hace HTTP.** Toggle `USD↔EUR` broadcast `com.tasabcv.app.TOGGLE_CURRENCY`, tap abre `MainActivity`. Sin delta, `RelativeSizeSpan` 0.5x para `Bs`. `refreshAll()` se llama desde el ViewModel tras cada fetch exitoso; la ficha "Añadir widget" de Ajustes usa `requestPinAppWidget` (API 26+, con pasos manuales como fallback en la misma ficha). Layout: chip de moneda arriba, valor grande (`weight 3`, autoSize 18–44sp) centrado, fecha al pie.

## 10. UI y quirks
- **Buenas prácticas móvil:** Compose eficiente (evitar recomposiciones, `remember`, estado mínimo), 60fps y APK ligero. Subir nivel visual sin romper minimalismo ni rendimiento.
- **Edge-to-edge:** `enableEdgeToEdge()` en `MainActivity`; bottom bar con `navigationBarsPadding()`. Orientación NO bloqueada (el manifest usa `configChanges`). Tasas reparte el alto con `weight`; Calculadora usa `verticalScroll`; Ajustes usa `Spacer(weight)` para clavar su ficha informativa al pie. El splash XML (`themes.xml`) define `statusBarColor`/`navigationBarColor`; en runtime las barras quedan transparentes y `Theme.kt` solo controla la apariencia de los íconos.
- **Lint:** `abortOnError=false` (advierte, no bloquea); desactivados `UnusedResources`, `IconMissingDensityFolder`, `Typos`, `Overdraw` (`app/build.gradle.kts`).
- **Grafito monocromo** `Theme.kt` (`AppColors`/`LocalAppColors`, no dynamic color) + neumorfismo `Depth.kt` (`neuRaised/neuInset` sin blur; `neuSurface` eliminado). `pulseClick` + háptica sutil (`TextHandleMove`) + muelles compartidos (`MotionSpring`/`PopSpring`, ver bullet propio) + `SlidingSegmentedControl` (esquinas 32dp).
- **pulseClick:** respuesta al toque en TODOS los presionables (fichas, refrescar, copiar, barra inferior, selector, ficha widget de Ajustes). Historia: 3 intentos con estado "presionado" persistente fallaron en el dispositivo (clickable+fuente, pointerInput+fuente, pointerInput+estado local con finally) — familia de bugs b/414319919/b/186223077 de foundation 1.6.8; el estado colgado reproducía "ficha hundida hasta el próximo toque". Solución: eliminar el estado persistente DE RAÍZ. `pulseClick`: pulso ONE-SHOT al tocar (Animatable 1 → pressedScale → 1 exacto, 2 `animateTo` con `MotionSpring`); detección con `Modifier.clickable` estándar (indication = null; su fuente NO se observa); `graphicsLayer` lambda = cero recomposiciones y cero trabajo en reposo; superficies SIEMPRE elevadas (`neuRaised`), nunca cambian de forma. Imposible por diseño que el visual quede colgado: el valor siempre converge a 1f.
- **Odómetro por dígitos:** 12 slots anclados a la derecha; cada carácter rueda hacia arriba si sube, hacia abajo si baja, con fade y clip por slot. Delta ▲/▼ en chip, colores adaptados por tema.
- **Lenguaje de movimiento ÚNICO:** TODO movimiento de la app usa los muelles compartidos de `Components.kt`: `MotionSpring` (Float e IntOffset, damping 0.9 / stiffness 350 — indicador deslizante, carrusel USD↔EUR, transición de tabs, odómetro, tamaño del valor, presiones) y `PopSpring` (damping 0.75 / stiffness 420 — entradas pop: delta 0.55→1, mensajes de pill 0.85→1). Los fades acompañan con tweens cortos (140–200 ms). Prohibido añadir springs ad-hoc fuera de este par (§1). El indicador del segmented asienta SIEMPRE dentro de su burbuja (sin overshoot visible) y las transiciones de tabs/selector son direccionales (coherentes entre sí).
- **Transición de tabs (3 tabs):** dirección por comparación de ÍNDICES (`routeIndex`: ajustes 0 / tasas 1 / calculadora 2), no de ruta — entrar desde el lado del tab elegido, salir al opuesto, mismo `MotionSpringIntOffset` + fades 200/160 ms; el indicador de la barra y las pantallas van coherentes.
- **Fichas tasas:** sin botones copiar/compartir (compartir eliminado de raíz; `shareRate` ya no existe). Tocar la ficha copia la tasa: `pulseClick` 0.97f (pulso one-shot de escala con superficie siempre elevada; el hundimiento persistente se eliminó por quedarse colgado en el dispositivo) + pill del pie destella "Tasa copiada" ~1.2 s (único `AnimatedContent` con los muelles compartidos; cero trabajo si el mensaje no cambia). Pie: pill SIEMPRE centrada en todos los estados (Reintentar eliminado; el refresco vive en el círculo de la barra superior). Pill estilo chip del delta: fondo translúcido 10% + borde 28% + esquinas 16dp, color = estado (verde actualizado, rojo sin conexión, neutro conectando/actualizando, acento al copiar); SIN punto separado (el color comunica). Delta en slot fijo 36dp: chip ▲/▼ real con pop de `PopSpring` al entrar, o ficha fantasma "—" con idénticas medidas cuando no hay `previous_*` con qué comparar (nunca queda hueco).
- **Ajustes (ConfiguracionScreen):** sin título. Ficha 1 "Añadir Widget" (`pulseClick` 0.97f): ícono a la IZQUIERDA + título centrado (18sp) como encabezado; textos descriptivos JUSTIFICADOS (12sp explicativo, 11sp pasos manuales — más palabras por línea para que el justificado no estire los espacios); `requestPinAppWidget` si API 26+ y launcher soporta; si no, la MISMA ficha muestra los pasos manuales (sin estados transitorios: soporte consultado UNA vez en `remember`). Ficha 2 fija al pie (sin interacción, compacta): Versión (PackageManager, sin `buildConfig`), Fuente (`DolarRepository.FUENTE`, pública) y Creador ("Sebastián Viloria") en filas etiqueta↔valor 12sp + divisores `divider`; lema (`ajustes_info_lema`) JUSTIFICADO 10sp (texto largo: líneas llenas → justificado limpio). Ambas fichas `fillMaxWidth` (margen 24dp estándar de la app).
- **Calculadora:** `.`=miles visual, `,` decimal (2 dec). `CurrencyVisualTransformation` + `sanitizeInput()`. Selector USD/EUR limpia campos. Cambio de moneda con carrusel horizontal. Sin título de encabezado.
- ProGuard mantiene el modelo (`DolarRate`, `DolarResult`, `DolarUiState`, `SingleRateState` y sus `$*`), `MainActivity`, `DolarWidgetProvider`, `kotlin.Metadata`, `org.json`. Si añades reflexión → añade reglas.

## 11. Git (ESTRICTO)
`origin` privado `https://github.com/null-entropy/Tasa-BCV-Cleam` rama `main`.

**REGLA DOBLE (commit + push SIEMPRE juntos):**
- Si el usuario NO pidió guardar: al terminar cambios, presentar `git status`/`git diff` resumido y PREGUNTAR si quiere "commit + push de todo a la vez". Prohibido comitear sin preguntar.
- Si el usuario dice "guarda", "comitea", "sube", "commit", "push" o similar → ejecutar SIEMPRE `commit + push` juntos a `origin/main`, sin volver a preguntar. Prohibido dejar solo el commit sin subir.
- Antes: `git status`, `git diff`, `git log --oneline -10`. Prohibido `--force`, rebase interactivo, commits vacíos, tocar config.
- Sin hook `post-commit` instalado (verificado): el push lo ejecuta siempre el agente explícitamente.

`.gitignore`: `.gradle/`, `build/`, `app/build/`, `.idea/`, `*.iml`, `.vscode/`, `local.properties`, `keystore/`, `keystore.properties`, `*.apk`, `*.log`, `.DS_Store`.

## 12. Tareas comunes y diagnóstico
- **Publicar:** preguntar `versionName`/`versionCode` si el usuario no los dio → subir versión → `copyReleaseToDownloads` → `apksigner verify`. Nunca compilar sin pedido explícito (§6).
- **Añadir moneda (solo si dolarapi la soporta; la región Venezuela NO tiene USDT/criptos):** 1) verificar que dolarapi la soporte; 2) rama en `DolarRepository` para el nuevo endpoint; 3) tercer `SingleRateState` en `DolarUiState`; 4) tercera tarjeta en `TasasScreen`; 5) opción en la calculadora; 6) actualizar este doc.
- **"No trae datos":** 1) probar los endpoints de dolarapi con curl (§5); 2) si dolarapi cae, la app muestra caché con aviso; 3) revisar `dolar_bcv_prefs`.

## 13. Checklist release
- [ ] 3 tabs (Ajustes · Tasas · Calculadora), transición por índices fluida, Tasas inicial
- [ ] Ajustes: ficha widget ancla o muestra pasos manuales; ficha info (versión/fuente/lema) fija al pie
- [ ] Loading/Loaded/Empty + caché instantánea + pill de estado correcta (actualizado/offline/caché)
- [ ] Ficha: tap copia la tasa (pulso one-shot + destello); sin botones copiar/compartir
- [ ] Delta siempre visible: chip ▲/▼ real o fantasma "—" (sin hueco)
- [ ] Odómetro por dígitos sin cortes ni superposiciones
- [ ] Calculadora no crashea con vacío/letras, formato VE correcto
- [ ] Widget sin delta: tasa y fecha armónicas; toggle USD/EUR funciona
- [ ] `copyReleaseToDownloads` BUILD SUCCESSFUL + APK firmado `CN=Dolar BCV` (verificar la versión del APK en el comando apksigner)
- [ ] No se subió keystore/properties, versionCode incrementado, ProGuard ok
- [ ] `AGENTS.md` sigue fiel al código (versiones, archivos, caché, proveedor)

## 14. Limitaciones (no bugs)
Dependencia del proveedor comunitario dolarapi.com; si cae → caché o reintento posterior. pydolarve.dev (NXDOMAIN) y el scraping de bcv.org.ve (frágil) fueron eliminados del proyecto de forma profunda (§5). Aviso "app no reconocida" es del SO (APK fuera de Play Store). Sin emulador en el entorno de build; verificación física fuera.

## 15. Convenciones
Comentarios en español, paquete `com.tasabcv.app`, estilo Kotlin oficial, opt-ins `ExperimentalMaterial3Api`/`ExperimentalFoundationApi` habilitados en `app/build.gradle.kts`.
- **Buenas prácticas Kotlin móvil:** funciones pequeñas, sin librerías pesadas, respetar `HttpURLConnection+org.json` y ProGuard.
- **Optimización obligatoria (§1):** al añadir código, evaluar costo por frame (nada de sombras/degradados animados por frame en superficies grandes), recomposiciones mínimas, springs que asientan, sin pollings ni dependencias nuevas; en cada release revisar el peso del APK.
- **Coherencia:** si añades pantalla, moneda, clave de caché o cambias build, actualiza este doc en el mismo cambio.

## 16. Comunicación
Asumir SIEMPRE que el usuario no es técnico, incluso si usa palabras técnicas: explicar en el lenguaje más simple posible, paso a paso, con ejemplos concretos de qué hacer y qué va a pasar, y con el detalle necesario para que entienda qué se hizo y por qué. Nunca omitir información ni afirmar cosas no verificadas: explicar con verdad completa (sin simplificaciones que mientan) y, si algo no se sabe, decirlo. Lo técnico profundo queda solo en el código; el resumen para el usuario debe ser sencillo, completo y veraz.