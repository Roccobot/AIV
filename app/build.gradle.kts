plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.roccobot.aiv"
    /*
     * Compiled against 37 because the whole Compose stack demands it: dropping to 36
     * fails the build with eleven dependencies at once (foundation 1.12.0, material3
     * 1.5.0-alpha26, animation, ui-graphics, and Coil 3.6.0), each asking for
     * "compileSdk of at least 37". Measured on 2026-09-01, not assumed.
     *
     * ⚠️⚠️ **API 37 IS STABLE, and the note that used to sit here said the opposite.**
     * It called the platform "a preview one, installed from the SDK preview channel",
     * and that mattered: an artifact built against a preview SDK is refused by Google
     * Play, so the stale line read as a blocker to publishing. The measurement that
     * settles it is the platform's own `source.properties`, where a preview carries a
     * codename and a stable one carries none: both android-37.0 and android-37.1 have
     * `AndroidVersion.CodeName=` empty. Stable libraries requiring it say the same
     * thing from the other side.
     *
     * targetSdk stays on 36 on purpose: compiling against a newer SDK and opting in to
     * its runtime behaviour are two separate decisions, and only the first is forced on
     * us here. Play's current floor is API 36, so 36 is compliant.
     */
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.roccobot.aiv"
        // minSdk 28 è una scelta, non un valore di comodo: da lì in su
        // esiste ImageDecoder, che decodifica HEIF e rispetta l'orientamento
        // EXIF senza codice nostro. Sotto, servirebbe una seconda strada per
        // ogni formato, cioè il doppio del codice per telefoni del 2017.
        minSdk = 28
        targetSdk = 36

        /*
         * ⚠️⚠️ **NESSUN FILTRO DI ARCHITETTURA, e la riga che c'era è uscita insieme
         * alla ricerca per contenuto (1.12)**: dalla `0.88` alla `1.11` qui c'era un
         * `abiFilters` su `arm64-v8a`, perché ONNX Runtime porta una libreria nativa
         * per architettura e senza filtro l'APK le portava tutte e quattro. Tolto il
         * motore, quello che resta di nativo sono **due librerie AndroidX da dieci
         * kilobyte** (`graphics.path` e `datastore_shared_counter`), cioè 73 KB per
         * tutte e quattro insieme: non c'è più niente da filtrare, e l'app torna a
         * installarsi anche sui telefoni a 32 bit e sugli emulatori `x86`.
         * ⚠️ **Se un domani rientra una libreria nativa, quella riga torna con lei**,
         * perché la ragione era sua e non dell'app: qui si scarica un APK unico da un
         * sito, quindi non c'è nessuno split per ABI a fare il lavoro.
         */
        // versionCode has to grow at every PUBLISHED build, or Android refuses
        // the update as a downgrade. It is not tied to versionName and nothing
        // checks it, so nothing will remind you: 0.11 went out carrying 1, so
        // from here on every published version needs its own number.
        versionCode = 132
        // Single source of the version, in SlimVer. The release workflow reads
        // it from here and refuses to run when the tag disagrees, so the tag
        // confirms this number instead of being a second one.
        versionName = "1.42"
    }

    // The signing material comes from the environment and never from the
    // repository: the keystore and its passwords live in the GitHub secrets of
    // this repo, and the release job writes them out for the length of a single
    // run. Read through `providers` rather than System.getenv so the
    // configuration cache tracks them instead of going stale.
    val keystorePath = providers.environmentVariable("AIV_KEYSTORE_FILE").orNull
    val canSign = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = providers.environmentVariable("AIV_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("AIV_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("AIV_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Absent the environment, the release build stays UNSIGNED instead
            // of failing: a contributor without the keystore can still compile
            // and check the shrinker, which is what a local release build is
            // for. Only the workflow produces an installable artifact.
            signingConfig = if (canSign) signingConfigs.getByName("release") else null
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    lint {
        // ⚠️⚠️ TypographyEllipsis wants '...' replaced with the single character
        // '…', and here it is WRONG: that character is forbidden across every
        // Roccobot project, in every output, and three dots are the required
        // form. Disabled with the reason written down, because a warning left
        // standing is a warning someone eventually 'fixes'.
        disable += "TypographyEllipsis"
        // ⚠️ ScopedStorage segnala `MANAGE_EXTERNAL_STORAGE` e ha ragione in
        // generale: è il permesso più largo che esista, e la maggior parte delle
        // app dovrebbe cavarsela con quello sulle sole immagini. Qui è una scelta
        // esplicita dell'utente (*preferisco chiedere un permesso pesante prima e
        // poi essere a posto per sempre*), e il motivo tecnico che la sostiene sta
        // nel manifest: quello leggero, da Android 14, apre la porta all'accesso
        // parziale, che farebbe dichiarare 'tre' a una cartella da quattrocento.
        // Spento con la ragione scritta, o qualcuno prima o poi lo 'corregge'.
        disable += "ScopedStorage"
    }

    /*
     * ⚠️⚠️ **LE DUE VARIANTI x86 DI LIBAVIF RESTANO FUORI, e vale 2,9 MB su un APK che
     * ne pesa 5.** Misurate dentro l'AAR e non stimate: `x86` 1.131.128 byte e `x86_64`
     * 1.829.008, contro 868.592 di `arm64-v8a` e 655.780 di `armeabi-v7a`. Una libreria
     * nativa nell'APK sta **non compressa**, quindi quei byte si pagano tutti.
     * ⚠️ **Chi userebbe le due escluse sono gli EMULATORI**, e qui l'APK si scarica da un
     * sito, dove nessuno spacchetta per architettura: l'unica conseguenza è che su un
     * emulatore x86 gli AVIF non si aprono, mentre l'app si installa e funziona come
     * prima. È la stessa scelta che il progetto aveva già fatto per ONNX Runtime, e la
     * nota in `defaultConfig` la prevedeva: *se un domani rientra una libreria nativa,
     * quella riga torna con lei*.
     * ⚠️⚠️ **NON è un `abiFilters`**, ed è la differenza che conta: quello butterebbe
     * **tutte** le librerie x86, comprese le due di AndroidX, e l'app smetterebbe di
     * installarsi su quei dispositivi. Qui esce un file solo, e `Avif.ready` se ne accorge
     * da sé perché `System.loadLibrary` solleva.
     */
    packaging {
        jniLibs {
            excludes += "lib/x86/libavif_android.so"
            excludes += "lib/x86_64/libavif_android.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // ⚠️ Arriva col PEZZO 2 dei video (0.86) e serve a una cosa sola: `LocalLifecycleOwner`
    // di `androidx.lifecycle.compose`, con cui il lettore si mette in pausa quando l'app va
    // in fondo. Quello vecchio di `compose.ui.platform` è deprecato e rimanda qui.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.datastore.preferences)
    // Solo per le MINIATURE della griglia: vedi la nota nel catalogo delle
    // versioni. ⚠️ Non porta niente per la rete (quello sarebbe
    // `coil-network-okhttp`), e non serve: qui le immagini sono locali.
    implementation(libs.coil.compose)

    /*
     * ⚠️⚠️ **MEDIA3 ENTRA COL PEZZO 2 DEI VIDEO (0.86)**: riprodurre un filmato in
     * casa vuol dire un decoder, un buffer, la gestione del fuoco audio e i codici
     * che ogni telefono implementa a modo suo, cioè esattamente quello che una
     * libreria di sistema fa.
     * ⚠️⚠️ **`media3-ui-compose` E NON `media3-ui`, ed è una scelta sui GESTI.**
     * Il secondo porta `PlayerView`, cioè i comandi già fatti, ma è una `View`
     * che si prende i tocchi: dentro un `AndroidView` a tutto schermo la
     * strisciata per cambiare fotografia non arriverebbe più al genitore Compose,
     * e sfogliando una cartella mista si resterebbe bloccati sul filmato, che è
     * il difetto che la `0.83` esiste per non avere. `PlayerSurface` invece non
     * ascolta nessun tocco: i comandi li scriviamo noi (sono un tasto e una
     * barra) e i gesti restano quelli di Compose.
     */
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)

    implementation(libs.gifdecoder)

    /*
     * ⚠️⚠️ **LIBAVIF ENTRA CON LA 1.26**, e la ragione non è che AVIF mancasse: è che
     * `ImageDecoder` lo dichiara da API 31 e poi lo gira al decodificatore AV1 del
     * telefono, che sui file veri risponde `getPixels failed with error invalid input`.
     * La misura sul file dell'utente sta in testa a `Avif.kt`: profilo AV1 High, croma
     * 4:4:4, livello 6.0, 24 megapixel, cioè tre cose che la CDD di Android non obbliga
     * nessun telefono a saper leggere. Questa libreria decodifica in **software**, quindi
     * non dipende da che cosa sa fare l'apparecchio.
     */
    implementation(libs.avif.android)

    /*
     * ⚠️⚠️ **ANDROIDSVG ENTRA CON LA 1.31**, e qui non c'è nessun decodificatore di
     * sistema da provare prima: Android non sa disegnare un SVG e non l'ha mai saputo.
     * `ImageDecoder` conosce i formati a pixel, e la grafica vettoriale del sistema è
     * un'altra cosa, cioè `VectorDrawable`, che è un XML di Android e **non** un SVG (non
     * ha CSS, non ha `<text>`, non ha i filtri, e vive dentro le risorse dell'app). Quindi
     * la scelta non era 'sistema o libreria': era 'libreria o niente'.
     * ⚠️ **Pesa 202.395 byte di AAR**, misurati sul file scaricato da Maven Central e non
     * stimati, ed è **tutto Java**: nessuna libreria nativa, quindi nessun byte per
     * architettura e nessuna esclusione da fare come per libavif.
     * ⚠️⚠️ **NELL'APK NE ARRIVA LA METÀ, e il numero è un CONFRONTO e non una stima**: due
     * `assembleRelease` sulla stessa macchina e con gli stessi flag, uno sull'albero della
     * `1.30` e uno su questo, dànno 6.536.925 e 6.636.649 byte, cioè **+99.724 byte
     * (+1,5%)**. La differenza col peso dell'AAR è il minificatore, che qui può lavorare
     * perché è codice Java: sulla libreria nativa di libavif non poteva, ed è la ragione per
     * cui quella costava 1,5 MB veri.
     */
    implementation(libs.androidsvg)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
}
