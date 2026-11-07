# AGENTS.md — Tasa BCV - Clean

> Instrucciones para agentes. Solo lo no obvio; los detalles visuales exactos viven en el código.
> Este archivo manda: si cambias comportamiento, actualízalo en el mismo cambio.

## 0. Comunicación — regla absoluta

**Asume SIEMPRE que el usuario no es técnico, desde el primer mensaje.** Aplica a respuestas, planes, preguntas y avisos. Incumplirlo es un error grave.
- Solo palabras de todos los días; prohibida la jerga de programación.
- Si un término técnico es inevitable (archivo, carpeta, herramienta o etiqueta visible), explícalo en el mismo mensaje con un ejemplo de la vida diaria.
- Traducciones obligatorias: compilar/build → "preparar la app para instalarla en el teléfono" · APK → "el archivo de instalación de la app (como un .exe)" · commit/push/repo/rama → "guardar los cambios en el respaldo en línea" · código/fuente/clase/función → "los archivos donde está escrita la app" · bug/crash → "falla / la app se cierra sola" · variable/estado/recomposición → "lo que la pantalla muestra o guarda en ese momento" · caché/SharedPreferences → "el último dato guardado en el teléfono" · endpoint/API/petición → "la consulta al sitio de internet donde se publica la tasa" · widget → "el recuadro de la tasa en la pantalla de inicio" · logs/terminal → "la pantalla de comandos del computador".
- Antes de enviar: relee el mensaje entero, traduce toda palabra de programador, explica paso a paso, di la verdad completa y di claramente lo que no se pudo comprobar.
- Los textos que el usuario ve en la app (`strings.xml`) también van en lenguaje llano. El detalle técnico profundo vive solo en el código y en este archivo; nunca en los mensajes al usuario.

## 1. Producto y reglas de oro

- App Android nativa Kotlin + Compose, paquete `com.tasabcv.app`. Tres pestañas: **Ajustes · Tasas · Calculadora**; **Tasas siempre es la inicial**.
- Muestra dólar/euro oficial BCV y una calculadora Bs↔USD/EUR; incluye widget.
- El BCV publica **un solo valor por día hábil** (~4–6:30 pm Caracas). La app nunca simula actualizaciones intradía.
- Lema visible: `"Aplicación confiable, ligera y sin anuncios"` (string `ajustes_info_lema`, centrado en 1 línea). Debe coincidir también en `README.md`.
- **Optimización obligatoria:** si dos diseños logran lo mismo, entra el más ligero. Prohibido trabajo pesado por frame, animaciones que nunca asientan, pollings, dependencias nuevas sin necesidad y recomposiciones evitables.
- Sin anuncios, analytics ni Firebase.

## 2. Entorno y comandos

- JDK 17. SDK único: `C:\Android\Sdk`. `local.properties` puede traer la ruta escapada (`sdk.dir=C\:\\Android\\Sdk`) o con barras normales; ambas funcionan. `android.overridePathCheck=true` ya resuelve la ruta con espacios.
- **STUDIO-FREE:** no abrir con Android Studio; no debe existir `.idea/` ni artefactos de Studio.
- Kotlin 1.9.24 · `kotlinCompilerExtensionVersion` 1.5.14 · AGP 8.5.2 · Gradle 8.7 · compileSdk/targetSdk 34 · build-tools 34.0.0 · minSdk 24 · Compose BOM 2024.06.00 + Material3.
- Preparar entrega: `.\gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
- **No hay pruebas automáticas:** `app/src` solo contiene `main`. No busques `gradlew test`; la verificación real es compilar, verificar la firma y probar en un teléfono.
- No hay CI ni workflows: nada se valida automáticamente al subir cambios; la verificación es local.
- Lint: `abortOnError=false`; desactivados `UnusedResources`, `IconMissingDensityFolder`, `Typos`, `Overdraw`.
- Versión actual: `versionCode 51 / versionName 4.9.10` en `app/build.gradle.kts`. Cada publicación exige `versionCode` mayor. Nunca asumas los números: si el usuario no los dio, pregúntalos.
- La pantalla de comandos de Windows muestra acentos y flechas como caracteres raros aunque el archivo esté bien en UTF-8; no reescribas un archivo solo por lo que veas ahí. Guarda UTF-8 y verifica con el editor.

## 3. Arquitectura y datos

```
Compose collectAsState() → DolarViewModel → DolarRepository → HTTP GET (HttpURLConnection)
```

- `DolarUiState` = estado USD + estado EUR; cada uno `Loading | Loaded(rate,isFromCache,warning) | Empty`. La caché se pinta al instante al arrancar sin advertencia; `warning` solo aparece cuando una consulta falló y se muestra lo guardado.
- `loadOnFirstOpen()` muestra la caché y refresca solo si tiene ≥30 min o nunca hubo consulta. `onAppResumed()` revalida con la misma regla. **Sin polling en segundo plano ni WorkManager.**
- `refresh()` actualiza USD y EUR juntos al terminar; `isRefreshing` bloquea dobles consultas.
- Caché en `dolar_bcv_prefs`: `promedio_*`, `fecha_*`, `fuente_*`, `cache_time_*`, `previous_*` (tasa del día publicado anterior), `widget_currency`, `app_theme_variant`, `last_update_check_time`. Números como String (tolera Float viejo); sano si `(0, 1_000_000]`; comparación `sameRate` 0.00005.
- Proveedor único: **dolarapi.com** (el BCV no tiene API pública). Solo HTTPS (`usesCleartextTraffic="false"`). La región Venezuela solo expone USD y EUR oficiales; no hay USDT ni criptomonedas.
- USD y EUR se descargan en paralelo, 2 GETs independientes, timeout **6 s** por moneda.
- Tasa anterior: se reutiliza `previous_*` si la fecha publicada no cambió; si no, se consulta el histórico diario con fecha `AAAA/MM/DD`, retrocediendo día a día, **saltando sábados y domingos**, máximo **5 días hábiles**, timeout **3 s** por intento. 404 = día sin publicación; cualquier otro fallo corta el retroceso y se usa lo guardado.
- Fin de semana o feriado: el proveedor devuelve el valor vigente; no calcules qué tasa "toca" según el día.
- **Prohibido** el histórico completo sin fecha (~112 KB): va contra el lema.
- El User-Agent se construye en runtime con el `versionName` real del instalable.
- Sin `java.time` (minSdk 24 sin desugaring): `Format.kt` parsea ISO por prefijo.

| Moneda | Endpoint | Valor | Fecha |
| --- | --- | --- | --- |
| USD | `https://ve.dolarapi.com/v1/dolares/oficial` | `promedio` | `fechaActualizacion` |
| EUR | `https://ve.dolarapi.com/v1/euros/oficial` | `promedio` | `fechaActualizacion` |
| USD anterior | `https://ve.dolarapi.com/v1/historicos/dolares/oficial/{AAAA/MM/DD}` | `promedio` | `fecha` |
| EUR anterior | `https://ve.dolarapi.com/v1/historicos/euros/oficial/{AAAA/MM/DD}` | `promedio` | `fecha` |

## 4. Límites técnicos

- Red/JSON: **`HttpURLConnection` + `org.json` únicamente**. No Retrofit, OkHttp ni Gson. `JSONObject` no acepta `InputStream` ni `Reader`: convierte el cuerpo a `String` y parsea con `JSONObject(body)`.
- No añadir dependencias (ni Firebase/analytics/ads) sin justificar el costo y sin actualizar este archivo.
- ProGuard mantiene el modelo (`DolarRate`, `DolarResult`, `DolarUiState`, `SingleRateState`, `UpdateInfo`, `ManualUpdateState` y sus `$*`), `MainActivity`, `DolarWidgetProvider` y `kotlin.Metadata`; `org.json` solo lleva `dontwarn` porque es del sistema. Si añades reflexión, añade reglas.
- Nombre visible `Tasa BCV - Clean`; permisos declarados: `INTERNET`, `ACCESS_NETWORK_STATE` (hoy no se consulta) y `REQUEST_INSTALL_PACKAGES` (actualización propia). No añadir permisos sin necesidad.
- Comentarios en español, estilo Kotlin oficial, opt-ins `ExperimentalMaterial3Api`/`ExperimentalFoundationApi`.

## 5. Invariantes de interfaz

- Edge-to-edge; orientación **bloqueada en vertical**; **cero scroll** en las 3 pantallas. Todas usan `BoxWithConstraints`, `MaxContentWidth = 560.dp` y tipografía acotada (`boundedSp`).
- Márgenes **adaptables**, no fijos: horizontal 24 dp normal, 20 dp si el ancho <370 dp, 16 dp si <340 dp; separación vertical 20 dp normal, 16/12/8 dp en pantallas bajas; barra inferior con 22 dp laterales. Usa las funciones de `Components.kt`; no las copies a mano.
- Transición de pestañas por **índice** (`routeIndex`: ajustes 0 · tasas 1 · calculadora 2), nunca por texto de ruta.
- El ícono de cada pestaña se elige comparando la etiqueta visible en `NavItemContent` (`when (label)`), no el índice. Si cambias un texto de pestaña, actualiza ese `when` o el ícono caerá en el genérico.
- **Un solo lenguaje de movimiento:** `MotionSpring` (0.9/350) y `PopSpring` (0.75/420) de `Components.kt`; fades de 140–200 ms. Prohibido crear muelles ad-hoc. `MotionSpringColor` es el único de color.
- `pulseClick`: pulso **one-shot**; detección con `Modifier.clickable` (indication null); `graphicsLayer` sin recomposiciones. **Nunca reintroduzcas el estado "presionado" persistente**: es la familia de fallas b/414319919 y b/186223077; el valor siempre debe converger a 1f.
- Cristal 3D: degradado vertical estático + contorno de 3 paradas + sombra. **Sin `RenderEffect` ni blur** (caro y solo API 31+).
- Temas: 4 variantes (`SISTEMA`=Grafito, `AZUL`, `VERDE`, `ROSADO`) × modo claro/oscuro del sistema = 8 paletas. El modo lo decide el sistema, no hay override manual. `ThemeStore` persiste en `dolar_bcv_prefs` y se carga en `MainActivity.onCreate` y en `DolarWidgetProvider.buildViews` **antes de pintar**.
- Widget: `RemoteViews` clásico (no Glance), **solo lee caché, nunca hace HTTP**; 4×2, mínimo 250×110 dp, aviso cada 30 min. Toggle `com.tasabcv.app.TOGGLE_CURRENCY`; tocar abre `MainActivity`.
- Fondos del widget: 8 shapes. `widget_background` = grafito **oscuro**, `widget_bg_grafito_light` = grafito claro y 6 `widget_bg_<variante>_<modo>.xml`. **NO existe `widget_bg_grafito_dark.xml`**: no lo crees por simetría. Chips: `widget_currency_chip` (oscuro) y `widget_chip_light`; colores de texto desde `ThemeStore.paletteFor`.
- Íconos: 4 alias (`LauncherSistema`, `LauncherAzul`, `LauncherVerde`, `LauncherRosado`) con MAIN/LAUNCHER. `.MainActivity` debe seguir `enabled="true"` **sin** LAUNCHER (si lo deshabilitas, se rompen el widget y los alias). El cambio se aplica en `MainActivity.onStop` con `ThemeStore.applyIconIfNeeded`.
- Tasas: sin botones de copiar/compartir. Tocar la ficha copia la tasa (`pulseClick` 0.97f) y muestra "Tasa copiada" ~1.2 s. Pill de estado siempre centrada; **no hay botón "Reintentar"** (el refresco vive en la barra superior y en `onAppResumed`). El chip de delta siempre ocupa su espacio: ▲/▼ real o "—" fantasma.
- Ajustes: sin título, 4 fichas, 3 separaciones iguales, sin scroll. La ficha del widget llama a `requestPinAppWidget` si el launcher lo soporta; **si no lo soporta, el toque no hace nada** (no hay pantalla de pasos manuales). La ficha de actualización solo muestra texto animado. La ficha de información cierra con el lema en 1 línea.
- Calculadora: `.` = miles, `,` = decimales (2), máximo 12 dígitos enteros; `CurrencyVisualTransformation` + `sanitizeInput()`; sin etiquetas "Monto en…" (las siglas son placeholder); el selector limpia campos; 2 botones de copiar y 4 atajos (5/10/20/50); sin scroll y con `imePadding`.
- `sanitizeInput()` trata un punto con exactamente 3 dígitos detrás y parte entera de 1-3 dígitos (distinta de cero) como separador de miles: `1.000` → `1000`. El resto de puntos sueltos son decimales: `36.50` → `36,50` y `0.500` → `0,50`. Al teclear, el punto se convierte en coma en el primer carácter, así que ese caso solo aparece al pegar o autocompletar.
- Actualización propia: `https://api.github.com/repos/null-entropy/Tasa-BCV-Clean/releases/latest`; revisión cada 24 h, 2 s después de abrir; **404 = al día, no "sin conexión"**; descarga en `cacheDir/updates/` e instala con `FileProvider`; si falta permiso de orígenes desconocidos, se reanuda en `MainActivity.onResume`.

## 6. Publicar versión y firma

> Solo publicar si el usuario lo pide expresamente. No sugerirlo por cuenta propia. Si no dio `versionName`/`versionCode`, preguntar.

1. Subir versión en `app/build.gradle.kts` y en este archivo.
2. `.\gradlew assembleRelease` (0 errores; único aviso aceptado: el "experimental" de `android.overridePathCheck=true`).
3. Verificar firma y versión:

    ```bash
    "C:/Android/Sdk/build-tools/34.0.0/apksigner.bat" verify --verbose --print-certs "app/build/outputs/apk/release/app-release.apk"
    # debe decir Verifies + CN=Dolar BCV
    "C:/Android/Sdk/build-tools/34.0.0/aapt2.exe" dump badging "app/build/outputs/apk/release/app-release.apk"
    ```

4. Guardar cambios en `origin/main` y publicar **una sola versión** en GitHub Releases.
5. **Antes de crear la nueva, borrar todas las releases y tags anteriores** (remotos y locales). En el repositorio debe quedar solo la última.

Firma (crítico): `keystore/dolarbcv-release.keystore` (PKCS12, alias `dolarbcv`, RSA 2048); contraseñas en `keystore.properties`; DN `CN=Dolar BCV, OU=App, O=Dolar BCV, L=Caracas, ST=Distrito Capital, C=VE`. **Nunca** perder, regenerar, renombrar ni subir la clave o el archivo de contraseñas. `storeFile` es relativo a la raíz. La firma v1 (JAR) y v2 están activas; si `apksigner` dice `v1 scheme: false` pero `jarsigner -verify` dice `jar verified`, no "arregles" nada.

La release debe llevar: `tag_name` = `v<versionName>`, `target_commitish` = `main`, `name` = `Tasa BCV - Clean v<versionName>`, `body` en lenguaje llano, `draft`/`prerelease` en `false`, y el APK adjunto con nombre fijo **`TasaBCV-Clean.apk`** y tipo `application/vnd.android.package-archive`. Ese nombre sostiene el enlace directo del README y lo detecta `AppUpdater.kt`.

Autenticación en Windows: no pidas el token al usuario ni lo guardes en archivos. Extráelo del administrador de credenciales con `git credential fill` (rellena antes `<versionName>` y el resumen):

```powershell
$version = "<versionName>"; $tag = "v$version"; $notes = "<resumen en lenguaje llano>"
$apk = "app/build/outputs/apk/release/app-release.apk"; $apkName = "TasaBCV-Clean.apk"
if (!(Test-Path $apk)) { throw "Falta el APK firmado: $apk" }

$cred = ("protocol=https`nhost=github.com`n`n" | git credential fill 2>$null) | Out-String
if ($cred -notmatch "password=([^\r\n]+)") { throw "Sin credencial de GitHub" }
$h = @{ Authorization = "Bearer $($Matches[1].Trim())"; Accept = "application/vnd.github+json"; "User-Agent" = "TasaBCVClean-Release" }
$api = "https://api.github.com/repos/null-entropy/Tasa-BCV-Clean"

foreach ($r in Invoke-RestMethod -Uri "$api/releases?per_page=100" -Headers $h) {
    Invoke-RestMethod -Uri "$api/releases/$($r.id)" -Headers $h -Method Delete
    if ($r.tag_name) {
        try { Invoke-RestMethod -Uri "$api/git/refs/tags/$($r.tag_name)" -Headers $h -Method Delete } catch {}
        git tag -d $r.tag_name 2>$null
    }
}
foreach ($t in Invoke-RestMethod -Uri "$api/tags?per_page=100" -Headers $h) {
    try { Invoke-RestMethod -Uri "$api/git/refs/tags/$($t.name)" -Headers $h -Method Delete } catch {}
    git tag -d $t.name 2>$null
}

$body = @{ tag_name = $tag; target_commitish = "main"; name = "Tasa BCV - Clean $tag"; body = $notes; draft = $false; prerelease = $false } | ConvertTo-Json -Compress
$rel = Invoke-RestMethod -Uri "$api/releases" -Headers $h -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -ContentType "application/json; charset=utf-8"
Invoke-RestMethod -Uri "https://uploads.github.com/repos/null-entropy/Tasa-BCV-Clean/releases/$($rel.id)/assets?name=$apkName" -Headers $h -Method Post -InFile $apk -ContentType "application/vnd.android.package-archive"

$list = Invoke-RestMethod -Uri "$api/releases"; $latest = Invoke-RestMethod -Uri "$api/releases/latest"
$code = curl.exe -s -L -I -o NUL -w "%{http_code}" "https://github.com/null-entropy/Tasa-BCV-Clean/releases/latest/download/$apkName"
Write-Output "Releases activas: $($list.Count) | Última: $($latest.tag_name) | Descarga: $code"
```

Auditoría previa a la entrega: llaves/paréntesis balanceados en cada Kotlin tocado, referencias e imports resueltos, XML bien formado y UTF-8 intacto, revisión visual del código nuevo y este archivo coherente con la app.

Checklist de entrega:
- [ ] 3 pestañas, Tasas inicial, transición fluida.
- [ ] Ajustes sin scroll: 4 fichas y 3 separaciones iguales.
- [ ] Widget respeta el tema (8 fondos) incluso sin abrir la app; toggle USD/EUR.
- [ ] Estados/caché correctos, pill de estado centrada, delta con ▲/▼ o "—".
- [ ] Calculadora con formato venezolano, pegado de "1.000" y sin fallas con campos vacíos.
- [ ] `versionCode` mayor, peso del APK revisado y sin `keystore/` ni `keystore.properties` en el cambio.
- [ ] `assembleRelease` correcto, firma `CN=Dolar BCV`, versión confirmada, una sola release con `TasaBCV-Clean.apk`.

## 7. Git

- `origin` público `https://github.com/null-entropy/Tasa-BCV-Clean`, rama `main`.
- Si el usuario **no** pidió guardar: muestra `git status`/`git diff` y **pregunta** antes de comitear. Prohibido comitear sin permiso.
- Si el usuario dice "guarda", "comitea", "sube" o similar: **commit + push juntos** a `origin/main`, sin volver a preguntar.
- Antes: `git status`, `git diff`, `git log --oneline -10`. Prohibido `--force`, rebase interactivo, commits vacíos y tocar configuración.
- No hay hook `post-commit`: el push lo haces tú explícitamente.
- `keystore/`, `keystore.properties` y `local.properties` nunca se suben.

## 8. Limitaciones (no son fallas)

- Dependencia de dolarapi.com (comunitario): si cae, la app muestra caché con aviso o error si no hay nada guardado.
- Fuera de Play Store: Android puede avisar "app no reconocida".
- Sin emulador en este entorno: la prueba final es en un teléfono físico.
