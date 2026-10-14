# AGENTS.md — Tasa BCV - Clean

> Referencia rápida para agentes. Solo lo no obvio — si no evita un error, no está aquí.
> Este documento manda: si cambias código, actualiza el doc en el mismo cambio. Verificar coherencia en cada tarea.

## 0. Regla de comunicación — ABSOLUTA (manda sobre TODO este documento)
**Asumir SIEMPRE que el usuario no es técnico, desde el PRIMER mensaje de cualquier conversación, sin excepción.** Aplica a TODO lo que se le escriba: respuestas, planes, preguntas, resúmenes, avisos y disculpas. Incumplirla es un error grave del mismo nivel que romper la firma del APK. No importa el agente, el modelo ni el contexto: la regla se cumple siempre.
- Solo palabras de todos los días. Prohibida la jerga de programación en cualquier mensaje (nada de términos como "compilar", "commit", "recomposición", "variable", "fallback").
- Si un término técnico es inevitable (nombre de archivo, de carpeta, de herramienta o de una etiqueta visible en la propia app), se explica EN EL MISMO MENSAJE con un ejemplo concreto de la vida diaria. Ejemplo de estilo: "…el archivo `app/build.gradle.kts` (donde se decide cómo se prepara la app para el teléfono)…".
- **Tabla de traducción (usar SIEMPRE la columna de la derecha):** compilar/build/generar → "preparar la app para instalarla en el teléfono" · APK/instalable → "el archivo de instalación de la app (como un .exe en una computadora)" · commit/push/repo/rama → "guardar los cambios en el respaldo en línea" · código/fuente/clase/función → "los archivos donde está escrita la app" · bug/crash/excepción → "falla / la app se cierra sola" · variable/estado/recomposición/listener → "lo que la pantalla muestra o guarda en ese momento" · caché/SharedPreferences → "el último dato guardado en el teléfono" · endpoint/API/fetch/petición → "la consulta al sitio de internet donde se publica la tasa" · widget → "el recuadro de la tasa en la pantalla de inicio del teléfono" · logs/terminal/shell → "la pantalla de comandos del computador".
- **Antes de enviar (checklist):** 1) releer el mensaje ENTERO y traducir toda palabra de programador; 2) todo término técnico inevitable lleva su explicación en ese mismo mensaje; 3) explicar paso a paso qué se hizo y por qué, con el detalle completo y la verdad completa (sin simplificaciones que mientan); 4) decir claramente lo que no se sabe o no se pudo comprobar; 5) cero abreviaturas del oficio ni nombres de herramientas sin explicar.
- Los textos que la persona VE dentro de la app también van en lenguaje llano (así están hoy en `strings.xml`; mantenerlos así al editarlos).
- El detalle técnico profundo vive SOLO en el código y en este documento; NUNCA en los mensajes al usuario. Detalle operativo en §16.

## 1. Qué es
App Android nativa Kotlin + Compose. Muestra dólar/euro oficial BCV y calculadora Bs↔USD/EUR. Tres tabs: **Ajustes** · **Tasas** · **Calculadora** (Ajustes a la izquierda de Tasas; Tasas SIEMPRE es el tab inicial). Tasas y Calculadora NO tienen título de encabezado (el contenido arranca directo). Paquete `com.tasabcv.app`.

**Lema (texto canónico de la app, string `ajustes_info_lema` en la ficha de información de Ajustes):** "El lema de la aplicación es ser robusta, minimalista, ligera, rápida, optimizada y sin anuncios". Es la brújula de diseño: TODA decisión (código, diseño, dependencia) debe mantenerlo. Si algún día cambia el texto en la UI, actualizar esta línea y las menciones de lema en §10 en el mismo cambio.

**MANDATO DE OPTIMIZACIÓN (estricto, aplica a TODO cambio):** toda implementación nueva debe justificar su costo (CPU/GPU/memoria/tamaño de APK). Si dos diseños logran lo mismo, entra el más ligero. Prohibido: trabajo pesado de dibujo por frame (sombras/degradados interpolados por frame, animaciones que nunca asientan), pollings, dependencias nuevas sin necesidad, recomposiciones evitables. El lema manda sobre el brillo visual: si una animación es más "bonita" pero más cara, se elige la barata.

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
README.md          # portada de GitHub con botón de descarga directa de la última versión
app/build.gradle.kts  # versionCode/versionName + firma
app/proguard-rules.pro  # qué clases NO se pueden borrar al preparar el APK (si añades reflexión, añade reglas)
app/src/main/java/com/tasabcv/app/
  MainActivity.kt      # edge-to-edge + NavHost (3 tabs, transiciones por índice) + bottom bar + cartel diario de actualización + aplicar ícono en onStop
  ConfiguracionScreen.kt # tab Ajustes: 4 fichas (widget + Personalización + Buscar actualización + info) con 3 márgenes idénticos
  AppUpdater.kt        # actualización desde GitHub Releases (revisión diaria 24h + manual desde Ajustes + descarga + FileProvider)
  TasasScreen.kt       # topBar fecha/refresh + CurrencyCard + odómetro
  CalculadoraScreen.kt # bidireccional, sin título de encabezado
  DolarRepository.kt   # dolarapi.com + caché SharedPreferences
  DolarViewModel.kt    # uiState + isRefreshing
  DolarWidgetProvider.kt # RemoteViews (no Glance)
  Format.kt / CurrencyFormat.kt / ClipboardUtils.kt / Theme.kt / Depth.kt / Components.kt
app/src/main/res/{layout/widget_dolar.xml, xml/dolar_widget_info.xml, xml/file_paths.xml, values/*, values-night/*, drawable/widget_*.xml (8 fondos + 2 chips), drawable/ic_launcher*_{foreground,background}.xml, mipmap-anydpi*/ic_launcher*.xml (4 variantes), font/poppins_*.ttf (4 pesos)}
```
Regenerable (no versionar): `.gradle/`, `.kotlin/`, `app/build/`. Nunca borrar ni renombrar: `keystore/`, `gradle-wrapper.jar`. `app/src/` es el código editable.

## 4. Arquitectura y estado
```
Compose collectAsState() → DolarViewModel → DolarRepository (dolarapi.com + caché SharedPreferences) → HTTP GET (HttpURLConnection)
```
- `DolarUiState = usdState + eurState` : `Loading | Loaded(rate,isFromCache,warning) | Empty`. `warning` solo cuando un fetch terminó en error y se muestra caché; la caché pintada al instante en el arranque NO lo muestra. Pie SIEMPRE presente: pill de estado de conexión (estilo chip del delta, color = estado) PERFECTAMENTE centrada en TODOS los estados (el botón "Reintentar" fue eliminado; el reintento vive en el círculo de refrescar de la barra superior y en `onAppResumed`). Estados: `Actualizado` verde / `Sin conexión; mostrando caché` / `Sin conexión; sin caché` rojo / `Conectando…` / `Actualizando…` durante refresco.
- `loadOnFirstOpen()` muestra caché instantánea (`showCacheNow()`), refresca solo si ≥30min o sin fetch. `refresh()` mantiene visible y actualiza USD+EUR juntos al terminar; `isRefreshing` separado bloquea dobles fetch.
- `onAppResumed()` revalida solo si ≥30min desde el último fetch exitoso. Sin polling en 2º plano ni WorkManager.
- **Caché** `dolar_bcv_prefs`: `promedio_*`, `fecha_*`, `fuente_*`, `cache_time_*`, `previous_*` (comparación del delta = tasa del día publicado inmediatamente antes, traída del proveedor; respaldo y uso sin internet) + `widget_currency` + `app_theme_variant` (tema elegido en Ajustes, `ThemeStore`) + `last_update_check_time` (marca de revisión diaria de actualizaciones, `AppUpdater`). Números como String (tolerante Float legacy). Sanity `(0,1_000_000]`. Delta local `sameRate` 0.00005, solo en Tasas.

## 5. Fuente de datos
El BCV no tiene API pública; la app usa **dolarapi.com como proveedor único** (antes había una cadena de 3 proveedores: pydolarve.dev y bcv.org.ve fueron eliminados de forma profunda por estar caído el primero y ser frágil el scraping del segundo). Timeout **6 s por moneda**. Constantes en `DolarRepository.companion`; el User-Agent se construye en runtime con el `versionName` real del instalable (`buildUserAgent` vía PackageManager): una sola fuente de verdad, imposible que se desfase al publicar una versión nueva.

| Moneda | Endpoint | Valor | Fecha |
| --- | --- | --- | --- |
| USD | `https://ve.dolarapi.com/v1/dolares/oficial` | `promedio` | `fechaActualizacion` |
| EUR | `https://ve.dolarapi.com/v1/euros/oficial` | `promedio` | `fechaActualizacion` |
| USD anterior | `https://ve.dolarapi.com/v1/historicos/dolares/oficial/{AAAA/MM/DD}` | `promedio` | `fecha` |
| EUR anterior | `https://ve.dolarapi.com/v1/historicos/euros/oficial/{AAAA/MM/DD}` | `promedio` | `fecha` |

Reglas: 1) Solo HTTPS (`usesCleartextTraffic="false"`). 2) Fin de semana: el proveedor devuelve el valor vigente; no se calcula "qué tasa toca según el día". 3) Si el fetch falla, se muestra la caché con advertencia (o error si no hay caché). 4) USD y EUR se descargan en paralelo (2 GETs independientes; no existe endpoint combinado en ve.dolarapi.com) + una búsqueda en el historial POR MONEDA para la tasa anterior (hasta 5 consultas por moneda: una por cada día hábil retrocedido, ver regla 7). 5) La respuesta es un body mínimo (~200 bytes; el histórico diario ~100) que se lee directo del InputStream y se parsea con `JSONObject` (el `org.json` del SDK no acepta Reader). 6) La región Venezuela solo soporta USD y EUR (oficial BCV; también expone paralelo Yadio, no usado). Sin USDT/criptos. 7) **Tasa anterior (delta):** la fecha se PIDE con barras (`2026/10/02`) y la respuesta la trae con guiones (`2026-10-02`); los días sin publicación (fines de semana y feriados) responden 404 → se retrocede día a día SALTANDO sábados y domingos (máx. 5 días HÁBILES consultados, 3 s por intento); cualquier otro fallo corta el retroceso al instante y se usa la última comparación guardada. El histórico COMPLETO (`/v1/historicos/...` sin fecha) trae todos los días desde 2023 (~112 KB): PROHIBIDO usarlo (va contra el lema).

## 6. Build y entrega
> Solo compilar (`assembleRelease`) si el usuario lo pide con palabras claras. Nunca asumir `versionName`/`versionCode`: si no los dio explícitos, preguntar siempre antes de compilar.
> **REGLA DE SUBIDA AUTOMÁTICA DEL APK (SIN CARPETA DESCARGAS):** no existe paso por la carpeta `Downloads`. Al compilar una nueva versión del APK (`.\gradlew assembleRelease`, salida única en `app/build/outputs/apk/release/app-release.apk`), tras verificar la firma se debe **subir automáticamente a GitHub** (`commit + push` a `origin/main` + publicación como ÚNICA release activa en GitHub Releases adjuntando `TasaBCV-Clean.apk`, ver §12).

**AUDITORÍA PRE-COMPILACIÓN (OBLIGATORIA):** tras cualquier implementación y ANTES de compilar, auditar y verificar que todo salió a la perfección — compilar con código sin auditar está prohibido:
1. Balance de llaves/paréntesis en cada Kotlin tocado (script de conteo `{`/`}`, `(`/`)`).
2. Referencias cruzadas resueltas: strings, íconos, constantes, funciones; imports sin uso eliminados.
3. XML bien formados (`[xml]` cast en strings/widget info) y encoding UTF-8 intacto (acentos/flechas: las herramientas de shell de Windows pueden dañarlos — verificar y reparar si hay mojibake).
4. Revisión visual del código nuevo (indentación, estructura, coherencia con el estilo del proyecto).
5. `AGENTS.md` coherente con el código (misma regla §15).

Después de compilar: 0 errores / 0 advertencias en el log (ÚNICO aviso aceptado: el de "experimental" de `android.overridePathCheck=true`, siempre presente e intencional), firma verificada (`apksigner verify`), versión del APK confirmada (`aapt2 dump badging`), peso revisado y APK subido a GitHub Releases (checklist §13).

**Sin pruebas automáticas:** `app/src` solo contiene `main` (no hay carpetas de pruebas ni suites que ejecutar). La verificación real es esta auditoría + checklist §13 + probar la app en un teléfono.

Requisitos: JDK 17, SDK ÚNICO del proyecto `C:\Android\Sdk` (platform-tools, platforms;android-34, build-tools;34.0.0; licenses y cmdline-tools incluidos). **STUDIO-FREE:** prohibido abrir el proyecto con Android Studio; no debe existir `.idea/` ni artefactos de Studio (si reaparecen, borrarlos; `.gitignore` los excluye). Gradle usa `sdk.dir` de `local.properties` → SIEMPRE `C:\Android\Sdk` (barras normales). Gotcha Windows: ruta no-ASCII falla AGP → ya resuelto `android.overridePathCheck=true`.

```powershell
cd "C:\Users\Admin\Proyectos\Tasa BCV - Clean"
.\gradlew assembleRelease
```
Compila `assembleRelease` (minify+shrinkResources+firma). Salida única: `app/build/outputs/apk/release/app-release.apk`.

## 7. Firma (CRÍTICO)
| Archivo | `keystore/dolarbcv-release.keystore` (PKCS12, alias `dolarbcv`, RSA 2048) |
| Contraseñas | `keystore.properties` (gitignore) |
| DN | `CN=Dolar BCV, OU=App, O=Dolar BCV, L=Caracas, ST=Distrito Capital, C=VE` |

Nunca perder/regenerar/renombrar (identidad legacy `Dolar BCV` intencional). Nunca commitear `keystore/` ni `keystore.properties`. `storeFile` relativo a raíz (`rootProject.file(...)`). Verificar:
```bash
"C:/Android/Sdk/build-tools/34.0.0/apksigner.bat" verify --verbose --print-certs "app/build/outputs/apk/release/app-release.apk"
# debe decir Verifies + CN=Dolar BCV
```
Gotcha de verificación: `apksigner` puede reportar `v1 scheme: false` AUNQUE la firma v1 (JAR) exista (convive con v2). Si `jarsigner -verify` dice `jar verified`, la firma v1 está bien; no "arreglar" nada.

## 8. Identidad
`applicationId com.tasabcv.app` (ex `com.dolarbcv.app` v4.0.0), `versionCode 43 / versionName 4.9.1` en `app/build.gradle.kts` (cada release requiere `versionCode` mayor), nombre `Tasa BCV - Clean`, permisos `INTERNET` + `ACCESS_NETWORK_STATE` + `REQUEST_INSTALL_PACKAGES` (actualización propia vía `FileProvider`).

## 9. Widget
`DolarWidgetProvider` RemoteViews clásico. `widget_dolar.xml` + `dolar_widget_info.xml` (4×2 celdas apaisado: `targetCellWidth 4` / `targetCellHeight 2`, `minWidth 250dp` / `minHeight 110dp` para launchers pre-12, 30min). **Solo lee caché, no hace HTTP.** Toggle `USD↔EUR` broadcast `com.tasabcv.app.TOGGLE_CURRENCY`, tap abre `MainActivity`. Sin delta, `RelativeSizeSpan` 0.5x para `Bs`. `refreshAll()` se llama desde el ViewModel tras cada fetch exitoso y desde la ficha Personalización al cambiar de tema. Layout: chip de moneda arriba, valor grande (`weight 3`, autoSize 18–44sp) centrado, fecha al pie. **Tintado por tema:** `setInt(widget_root, "setBackgroundResource", …)` con 8 shape XML translúcidos: `widget_background` = grafito OSCURO, `widget_bg_grafito_light` = grafito claro y los otros 6 = `widget_bg_<variante>_<modo>.xml`. ATENCIÓN: NO existe `widget_bg_grafito_dark.xml` (el oscuro de grafito ES `widget_background`): no lo crees buscando simetría, quedaría sin usar. Y chip genérico por modo (`widget_currency_chip` oscuro / `widget_chip_light`); los colores de texto (acento/superficie/apagado) se aplican programáticamente desde `ThemeStore.paletteFor(variant, modoSistema)`. `buildViews` llama a `ThemeStore.load(context)` ANTES de pintar: el widget se dibuja también sin abrir la app (reinicio del teléfono, proceso muerto, aviso de 30 min) y debe respetar el tema elegido.

## 10. UI y quirks
- **Buenas prácticas móvil:** Compose eficiente (evitar recomposiciones, `remember`, estado mínimo), 60fps y APK ligero. Subir nivel visual sin romper minimalismo ni rendimiento.
- **Edge-to-edge:** `enableEdgeToEdge()` en `MainActivity`; bottom bar con `navigationBarsPadding()`. Orientación NO bloqueada (el manifest usa `configChanges`). Tasas reparte el alto con `weight`; Calculadora usa `verticalScroll`; Ajustes usa `Spacer(weight(1f))` + `Spacer(fichaGap)` en los 3 huecos entre sus cuatro fichas (widget, Personalización, Buscar actualización e info al pie — 3 márgenes idénticos, sin scroll). El splash XML (`themes.xml`) define `statusBarColor`/`navigationBarColor`; en runtime las barras quedan transparentes y `Theme.kt` solo controla la apariencia de los íconos.
- **Lint:** `abortOnError=false` (advierte, no bloquea); desactivados `UnusedResources`, `IconMissingDensityFolder`, `Typos`, `Overdraw` (`app/build.gradle.kts`).
- **Cristal (identidad visual):** `Theme.kt` (`AppColors`/`LocalAppColors`, no dynamic color) + `Depth.kt` (`glassRaised`/`glassInset` — VIDRIO estático: relleno translúcido de las paletas + degradado diagonal + bisel luminoso 1px + sombra; SIN RenderEffect/blur, que es caro y solo API 31+; `neuRaised/neuInset` y `neuSurface` eliminados). `pulseClick` + háptica sutil (`TextHandleMove`) + muelles compartidos (`MotionSpring`/`PopSpring`, ver bullet propio) + `SlidingSegmentedControl` (esquinas 32dp).
- **Temas de color (Theme.kt/ThemeStore):** 4 variantes (`SISTEMA`=Grafito original, `AZUL`, `VERDE`, `ROSADO`) × 2 modos del sistema = 8 paletas; el modo SIEMPRE lo decide el sistema (no hay override manual). `ThemeStore` (estado Compose + prefs `app_theme_variant`) se carga en `MainActivity.onCreate` y en `DolarWidgetProvider.buildViews` (el widget se pinta sin abrir la app) y es fuente única para app y widget. Transición entre temas: `animateColorAsState` campo por campo (`animatedAppColors`) con muelle de color `MotionSpringColor` (mismas físicas 0.9/350 del MotionSpring) — primitivo probado, arranca siempre desde el valor visible; reposo = costo cero; arranque sin animación. `error` vive en `AppColors` (ex `colorScheme.error`) para animar junto a la paleta. Paletas de color INTENSAS (claro con tinte evidente, oscuro no casi negro); las previsualizaciones de la ficha leen las mismas paletas (sin duplicados).
- **Ficha Personalización (Ajustes):** encabezado con ícono `Brush` + título centrado; banner informativo (estático, SIN chevron ni pulseClick): ícono `Brightness6` + ÚNICO texto justificado «El tema se adapta al modo de tu dispositivo» en color `muted` y tamaño `bodyFontSize` (discreto, igual familia que el texto de la ficha widget), burbuja `heightIn(min)` + padding vertical (crece si el texto ocupa 2 líneas; nunca se corta ni superpone — texto con `weight(1f)` ocupa solo el ancho restante del Row); fila «Temas» con indicador ☀/☾ del modo del sistema (activo resaltado); cuadrícula de 4 celdas `weight(1f)` (tamaño idéntico) con previsualización de DOBLE tono (banda clara arriba + hairline + banda oscura, cada una con su mini esqueleto usando la paleta real; Sistema con grises neutros), borde acento + insignia check en la seleccionada, `pulseClick` en todas; al elegir: persistir + `refreshAll` del widget. Presets de medidas (`AjustesSizing`) por altura de pantalla (≥800/≥700/<700dp) escalan tipografía, paddings y grid; separadores entre las 4 fichas = `Spacer(weight(1f))` + `Spacer(fichaGap FIJO)` ADYACENTES en los 3 huecos — el weight puede colapsar a 0 pero el margen fijo (`fichaGap`: 16/12/8dp) NUNCA (weight con fill=true impone altura exacta y anula heightIn: lección v4.7.6/v4.7.7); info clavada al pie, sin scroll.
- **pulseClick:** respuesta al toque en TODOS los presionables (fichas, refrescar, copiar, barra inferior, selector, ficha widget de Ajustes). Historia: 3 intentos con estado "presionado" persistente fallaron en el dispositivo (clickable+fuente, pointerInput+fuente, pointerInput+estado local con finally) — familia de bugs b/414319919/b/186223077 de foundation 1.6.8; el estado colgado reproducía "ficha hundida hasta el próximo toque". Solución: eliminar el estado persistente DE RAÍZ. `pulseClick`: pulso ONE-SHOT al tocar (Animatable 1 → pressedScale → 1 exacto, 2 `animateTo` con `MotionSpring`); detección con `Modifier.clickable` estándar (indication = null; su fuente NO se observa); `graphicsLayer` lambda = cero recomposiciones y cero trabajo en reposo; superficies SIEMPRE elevadas (`glassRaised`), nunca cambian de forma. Imposible por diseño que el visual quede colgado: el valor siempre converge a 1f.
- **Odómetro por dígitos:** 12 slots anclados a la derecha; cada carácter rueda hacia arriba si sube, hacia abajo si baja, con fade y clip por slot. Delta ▲/▼ en chip, colores adaptados por tema.
- **Lenguaje de movimiento ÚNICO:** TODO movimiento de la app usa los muelles compartidos de `Components.kt`: `MotionSpring` (Float e IntOffset, damping 0.9 / stiffness 350 — indicador deslizante, carrusel USD↔EUR, transición de tabs, odómetro, tamaño del valor, presiones) y `PopSpring` (damping 0.75 / stiffness 420 — entradas pop: delta 0.55→1, mensajes de pill 0.85→1). Los fades acompañan con tweens cortos (140–200 ms). Prohibido añadir springs ad-hoc fuera de este par (§1); el `MotionSpringColor` de `Theme.kt` es el único tercero de la familia (mismas físicas 0.9/350, para colores). El indicador del segmented asienta SIEMPRE dentro de su burbuja (sin overshoot visible) y las transiciones de tabs/selector son direccionales (coherentes entre sí).
- **Transición de tabs (3 tabs):** dirección por comparación de ÍNDICES (`routeIndex`: ajustes 0 / tasas 1 / calculadora 2), no de ruta — entrar desde el lado del tab elegido, salir al opuesto, mismo `MotionSpringIntOffset` + fades 200/160 ms; el indicador de la barra y las pantallas van coherentes.
- **Fichas tasas:** sin botones copiar/compartir (compartir eliminado de raíz; `shareRate` ya no existe). Tocar la ficha copia la tasa: `pulseClick` 0.97f (pulso one-shot de escala, ver bullet `pulseClick`) + pill del pie destella "Tasa copiada" ~1.2 s (`AnimatedContent` con los muelles compartidos; cero trabajo si el mensaje no cambia). Pie: pill SIEMPRE centrada en todos los estados. Pill estilo chip del delta: fondo translúcido 10% + borde 28% + esquinas 16dp, color = estado (verde actualizado, rojo sin conexión, neutro conectando/actualizando, acento al copiar); SIN punto separado (el color comunica). Delta en slot fijo 36dp: chip de FLECHA DE TENDENCIA (`Icons.AutoMirrored.Filled.TrendingUp`/`TrendingDown`, estilo "gráfica", 16dp junto al texto) con pop de `PopSpring` al entrar, o ficha fantasma "—" con idénticas medidas cuando no hay con qué comparar o el valor no cambió (nunca queda hueco). La comparación SIEMPRE es contra el día publicado inmediatamente antes (historial del proveedor, ver §5 regla 7): funciona desde la instalación nueva y sin internet (respaldo `previous_*`); sin fecha de comparación en el chip (diseño limpio).
- **Ajustes (ConfiguracionScreen):** sin título. Cuatro fichas con **3 márgenes idénticos** (`Spacer(weight(1f))` + `Spacer(fichaGap)` en los 3 huecos entre fichas; `AjustesSizing` escala por altura ≥800/≥700/<700dp, sin scroll). Ficha 1 "Añadir Widget" (`pulseClick` 0.97f): ícono a la IZQUIERDA + título centrado; textos descriptivos JUSTIFICADOS; `requestPinAppWidget` si API 26+ y launcher soporta; si no, muestra los pasos manuales (`remember` una vez). Ficha 2 "Personalización" (ver bullet propio). Ficha 3 "Buscar actualización" (`UpdateCheckCard`, `pulseClick` 0.97f): ficha-botón compacta SIN ícono y SOLO con el texto centrado (`titleFontSize`, `SemiBold`), animando cambios de texto con la MISMA transición de `StatusPill` del pie de Tasas (`Buscar actualización` → `Buscando…` → `Ya tienes la última versión` verde / `Sin conexión` rojo 2.5s → `Buscar actualización`, o si hay versión nueva descarga directo mostrando `Descargando actualización… X %` e instala). Ficha 4 fija al pie (sin interacción salvo Fuente): Versión (PackageManager, fija), Fuente (`DolarRepository.FUENTE`, clicable que abre `https://dolarapi.com`), Creador ("Sebastián Viloria") y lema (`ajustes_info_lema`) centrado al cierre.
- **Ícono de la app por tema:** signo `$` gordito, proporción 1:1, 100% simétrico respecto a `(54, 54)` con 2 barras verticales que asoman arriba y abajo sin cruzar el centro de la "S". Siempre orientado al modo oscuro: fondo plano (`#000000` en Sistema, `#1B2A47` en Azul, `#1A3225` en Verde, `#331B26` en Rosado) + glifo en el tono claro del tema (`#F4F4F5`, `#7CB0FF`, `#45E4AE`, `#FA90C8`). 4 `<activity-alias>` en `AndroidManifest.xml` (`LauncherSistema`, `LauncherAzul`, `LauncherVerde`, `LauncherRosado`) apuntando a `.MainActivity` (que permanece siempre habilitada); el relevo ocurre al salir de la app (`MainActivity.onStop` → `ThemeStore.applyIconIfNeeded`) para no interrumpir la transición de colores en Ajustes.
- **Actualización propia (`AppUpdater.kt`):** consulta `https://api.github.com/repos/null-entropy/Tasa-BCV-Clean/releases/latest` (`HttpURLConnection` + `org.json`). Revisión automática 1 vez cada 24h (`last_update_check_time`), 2 s después de abrir la app; si hay versión mayor, abre `UpdatePromptDialog` (Cristal, versión + notas + "Actualizar ahora" / "Más tarde"). Descarga el APK en `cacheDir/updates/` y lanza el instalador vía `FileProvider` (`xml/file_paths.xml`).
- **Calculadora:** `.`=miles visual, `,` decimal (2 dec). `CurrencyVisualTransformation` + `sanitizeInput()`. SIN etiquetas "Monto en…": las siglas (USD/EUR, Bs) viven como `placeholder` NATIVO del TextField dentro de la píldora, al lado izquierdo, DISCRETAS (16sp, gris `muted` — nunca agrandan la cápsula), y aparecen SOLO con el campo vacío (desaparecen al escribir; reaparecen al cambiar de moneda porque los campos se limpian). Selector USD/EUR limpia campos. Cambio de moneda con carrusel horizontal. Dos botones de copiar (uno por campo: ícono `ContentCopy` en círculo elevado con `pulseClick`; copia el valor formateado + Toast). Sin título de encabezado.
- ProGuard mantiene el modelo (`DolarRate`, `DolarResult`, `DolarUiState`, `SingleRateState`, `UpdateInfo`, `ManualUpdateState` y sus `$*`), `MainActivity`, `DolarWidgetProvider`, `kotlin.Metadata`, `org.json`. Si añades reflexión → añade reglas.

## 11. Git (ESTRICTO)
`origin` público `https://github.com/null-entropy/Tasa-BCV-Clean` rama `main`.

**REGLA DOBLE (commit + push SIEMPRE juntos):**
- Si el usuario NO pidió guardar: al terminar cambios, presentar `git status`/`git diff` resumido y PREGUNTAR si quiere "commit + push de todo a la vez". Prohibido comitear sin preguntar.
- Si el usuario dice "guarda", "comitea", "sube", "commit", "push" o similar → ejecutar SIEMPRE `commit + push` juntos a `origin/main`, sin volver a preguntar. Prohibido dejar solo el commit sin subir.
- Antes: `git status`, `git diff`, `git log --oneline -10`. Prohibido `--force`, rebase interactivo, commits vacíos, tocar config.
- Sin hook `post-commit` instalado (verificado): el push lo ejecuta siempre el agente explícitamente.

`.gitignore`: `.gradle/`, `build/`, `app/build/`, `.kotlin/`, `.idea/`, `*.iml`, `.vscode/`, `local.properties`, `keystore/`, `keystore.properties`, `*.apk`, `*.log`, `.DS_Store`.

## 12. Tareas comunes y diagnóstico
- **Compilar y publicar APK en GitHub Releases (FLUJO ÚNICO AUTOMÁTICO, SIN CARPETA DESCARGAS):**
  - Preguntar `versionName`/`versionCode` si el usuario no los dio → subir versión en `app/build.gradle.kts` y `AGENTS.md` → `.\gradlew assembleRelease` → `apksigner verify` sobre `app/build/outputs/apk/release/app-release.apk` → `commit + push` a `origin/main` → publicar en GitHub Releases como única versión activa.
  - **Prohibido acumular versiones:** en GitHub (`null-entropy/Tasa-BCV-Clean`) SIEMPRE debe existir **UNA SOLA release vigente (la última)**. Antes de crear la nueva release es OBLIGATORIO eliminar todas las releases anteriores y borrar todos los tags anteriores (remotos y locales).
  - **Nota de autenticación en este entorno Windows:** `gh` CLI no tiene sesión propia guardada, pero el administrador de credenciales de Git de Windows SÍ tiene el token válido. Extraerlo siempre con `git credential fill` y usar la API REST de GitHub (o inyectarlo en `$env:GH_TOKEN`). Nunca pedir token al usuario ni guardarlo en archivos.
  - **Parámetros obligatorios que consumen `AppUpdater.kt` (`/releases/latest`) y el botón de descarga directa de `README.md`:**
    - `tag_name`: `"v<versionName>"` (ej. `"v4.9.1"`).
    - `target_commitish`: `"main"` (el commit de esa versión ya debe estar subido a `origin/main`).
    - `name`: `"Tasa BCV - Clean v<versionName>"`.
    - `body`: resumen claro y breve en lenguaje llano de las mejoras de la versión (este texto se muestra directo al usuario dentro de `UpdatePromptDialog` en la app; cumplir Regla 0).
    - `draft`: `false`, `prerelease`: `false`.
    - Asset adjunto: el archivo firmado `app/build/outputs/apk/release/app-release.apk` subido con `name=TasaBCV-Clean.apk` y `Content-Type: application/vnd.android.package-archive`. Usar el nombre fijo `TasaBCV-Clean.apk` garantiza que el enlace directo de la portada (`https://github.com/null-entropy/Tasa-BCV-Clean/releases/latest/download/TasaBCV-Clean.apk`) descargue siempre la última versión con un solo clic, y `AppUpdater.kt` lo detecta por terminar en `.apk`.
  - **Script canónico completo (purga todas las releases/tags anteriores + publica la nueva + adjunta el APK desde `app/build/outputs/apk/release/app-release.apk` + verifica):**
    ```powershell
    $version = "<versionName>" # ej. "4.9.1"
    $tag = "v$version"
    $apkPath = "app/build/outputs/apk/release/app-release.apk"
    $apkName = "TasaBCV-Clean.apk"
    $notas = "<resumen breve de mejoras en lenguaje llano para el cartel de la app>"
    if (!(Test-Path $apkPath)) { throw "No existe el APK firmado: $apkPath" }

    # 1. Obtener token desde Git Credential Manager de Windows
    $credText = ("protocol=https`nhost=github.com`n`n" | git credential fill 2>$null) | Out-String
    if ($credText -notmatch "password=([^\r\n]+)") { throw "No se encontró credencial de GitHub" }
    $token = $Matches[1].Trim()
    $headers = @{
        "Authorization" = "Bearer $token"
        "Accept"        = "application/vnd.github+json"
        "User-Agent"    = "TasaBCVClean-ReleaseScript"
    }
    $repoApi = "https://api.github.com/repos/null-entropy/Tasa-BCV-Clean"

    # 2. Eliminar TODAS las releases anteriores y sus tags (mantener SIEMPRE solo la última)
    $existingReleases = Invoke-RestMethod -Uri "$repoApi/releases?per_page=100" -Headers $headers -Method Get
    foreach ($rel in $existingReleases) {
        Invoke-RestMethod -Uri "$repoApi/releases/$($rel.id)" -Headers $headers -Method Delete
        if ($rel.tag_name) {
            try { Invoke-RestMethod -Uri "$repoApi/git/refs/tags/$($rel.tag_name)" -Headers $headers -Method Delete } catch {}
            git tag -d $rel.tag_name 2>$null
        }
    }
    # Limpiar cualquier otro tag huérfano remoto/local
    $existingTags = Invoke-RestMethod -Uri "$repoApi/tags?per_page=100" -Headers $headers -Method Get
    foreach ($t in $existingTags) {
        try { Invoke-RestMethod -Uri "$repoApi/git/refs/tags/$($t.name)" -Headers $headers -Method Delete } catch {}
        git tag -d $t.name 2>$null
    }

    # 3. Crear la nueva release única
    $payload = @{
        tag_name         = $tag
        target_commitish = "main"
        name             = "Tasa BCV - Clean $tag"
        body             = $notas
        draft            = $false
        prerelease       = $false
    } | ConvertTo-Json -Compress
    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
    $newRelease = Invoke-RestMethod -Uri "$repoApi/releases" -Headers $headers -Method Post -Body $payloadBytes -ContentType "application/json; charset=utf-8"

    # 4. Subir el APK firmado como asset de la release (nombre fijo TasaBCV-Clean.apk)
    $uploadUrl = "https://uploads.github.com/repos/null-entropy/Tasa-BCV-Clean/releases/$($newRelease.id)/assets?name=$apkName"
    $asset = Invoke-RestMethod -Uri $uploadUrl -Headers $headers -Method Post -InFile $apkPath -ContentType "application/vnd.android.package-archive"

    # 5. Verificación pública sin credenciales (exactamente 1 release activa y APK accesible)
    $publicList = Invoke-RestMethod -Uri "$repoApi/releases" -Method Get
    $latest = Invoke-RestMethod -Uri "$repoApi/releases/latest" -Method Get
    Write-Output "Total releases activas: $($publicList.Count) | Última: $($latest.tag_name) | APK URL: $($latest.assets[0].browser_download_url)"
    ```
- **Añadir moneda (solo si dolarapi la soporta; la región Venezuela NO tiene USDT/criptos):** 1) verificar que dolarapi la soporte; 2) rama en `DolarRepository` para el nuevo endpoint; 3) tercer `SingleRateState` en `DolarUiState`; 4) tercera tarjeta en `TasasScreen`; 5) opción en la calculadora; 6) actualizar este doc.
- **"No trae datos":** 1) probar los endpoints de dolarapi con curl (§5); 2) si dolarapi cae, la app muestra caché con aviso; 3) revisar `dolar_bcv_prefs`.

## 13. Checklist release
- [ ] 3 tabs (Ajustes · Tasas · Calculadora), transición por índices fluida, Tasas inicial
- [ ] Ajustes: 4 fichas caben SIN scroll con 3 márgenes idénticos (widget / Personalización / Buscar actualización / info al pie); ficha widget ancla o muestra pasos manuales
- [ ] Personalización: 4 celdas idénticas con doble tono claro/oscuro; transición de color suave al cambiar tema; banner modo informativo (no tocable); ícono de app cambia al salir según el tema elegido
- [ ] Ficha Buscar actualización: solo texto centrado con la misma animación del pie de Tasas; revisa, descarga e instala directo o avisa que está al día
- [ ] Widget tintado por tema elegido (8 fondos) en claro y oscuro, incluso sin abrir la app (reinicio/proceso muerto); toggle USD/EUR funciona
- [ ] Loading/Loaded/Empty + caché instantánea + pill de estado correcta (actualizado/offline/caché)
- [ ] Ficha: tap copia la tasa (pulso one-shot + destello); sin botones copiar/compartir
- [ ] Delta siempre visible: chip ▲/▼ real o fantasma "—" (sin hueco)
- [ ] Odómetro por dígitos sin cortes ni superposiciones
- [ ] Calculadora no crashea con vacío/letras, formato VE correcto
- [ ] Widget sin delta: tasa y fecha armónicas; toggle USD/EUR funciona
- [ ] `assembleRelease` BUILD SUCCESSFUL + APK firmado `CN=Dolar BCV` (firma con `apksigner`, versión con `aapt2 dump badging`) + subido como única release activa en GitHub (`TasaBCV-Clean.apk`)
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
Ver **Regla 0** (al inicio: manda sobre todo, sin excepción, desde el primer mensaje). Operativo: lenguaje llano siempre, ejemplos de la vida diaria, paso a paso, verdad completa, decir "no lo sé" cuando así sea; antes de enviar, releer y reescribir cualquier jerga. El detalle técnico profundo queda solo en el código y en este documento.