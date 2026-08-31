plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.roccobot.aiv"
    // Compiled against 37 because the Compose alpha demands it; the platform
    // itself is a preview one, installed from the SDK preview channel. targetSdk
    // stays on the last stable API: compiling against a newer SDK and opting in
    // to its runtime behaviour are two separate decisions, and only the first is
    // forced on us here.
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
         * ⚠️⚠️ **SOLO `arm64-v8a` DALLA 0.88, ed è una rinuncia dichiarata**: ONNX
         * Runtime porta una libreria nativa per architettura, e senza questo filtro
         * l'APK le porterebbe tutte e quattro, cioè decine di megabyte moltiplicati
         * per architetture che nessuno usa più. Questa app si scarica **da un sito**
         * come APK unico, quindi non c'è nessuno split per ABI a fare il lavoro.
         * ⚠️ **Il costo, dichiarato**: l'app non si installa più sui telefoni a 32
         * bit (`armeabi-v7a`) né sugli emulatori `x86`. Tutti i telefoni Android
         * usciti dal 2019 in poi sono a 64 bit, e Google Play li pretende tali dal
         * 2019: la rinuncia riguarda hardware più vecchio del `minSdk`.
         */
        ndk { abiFilters += listOf("arm64-v8a") }
        // versionCode has to grow at every PUBLISHED build, or Android refuses
        // the update as a downgrade. It is not tied to versionName and nothing
        // checks it, so nothing will remind you: 0.11 went out carrying 1, so
        // from here on every published version needs its own number.
        versionCode = 81
        // Single source of the version, in SlimVer. The release workflow reads
        // it from here and refuses to run when the tag disagrees, so the tag
        // confirms this number instead of being a second one.
        versionName = "0.91"
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

    /*
     * ⚠️⚠️ **ONNX RUNTIME ENTRA CON LA RICERCA PER CONTENUTO (0.88), e con lui il
     * salto di peso dell'APK**: sono decine di megabyte di libreria nativa, ed è
     * il prezzo di far girare due modelli sul telefono invece che su un server.
     * I **modelli** invece restano fuori e si scaricano (vedi `ClipModels`): quelli
     * sono altri 65 MB, e li paga solo chi accende la funzione.
     * ⚠️ **Il motore che ci gira sopra è stato provato su una JVM** con la libreria
     * desktop della stessa versione, che ha la stessa API Java: vedi `ClipEngine`.
     */
    implementation(libs.onnxruntime.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
}
