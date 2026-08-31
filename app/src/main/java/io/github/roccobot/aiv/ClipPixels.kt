package io.github.roccobot.aiv

/**
 * Da una fotografia ai numeri che l'encoder immagine di MobileCLIP si aspetta.
 *
 * ⚠️⚠️ **TUTTO QUELLO CHE C'È QUI È MISURATO IL 2026-08-30 eseguendo il modello vero**, non
 * dedotto da come funziona di solito CLIP. Le tre cose che il CLIP classico farebbe e che qui
 * sarebbero **sbagliate**:
 * - il lato è **256 e non 224**;
 * - **non c'è nessuna normalizzazione media/deviazione**: `preprocessor_config.json` dichiara
 *   `do_normalize: false`, perché MobileCLIP normalizza **dentro** il modello. Chi ci mette
 *   le medie di OpenAI rompe la ricerca **in silenzio**, cioè senza nessun errore;
 * - l'ingresso è **float32** anche col modello `fp16`, che casta dentro.
 *
 * ⚠️⚠️ **È PURA E NON SA NIENTE DI ANDROID, ed è deliberato**: prende i pixel come numeri e
 * torna numeri, quindi lo stesso codice gira su una JVM normale con ONNX Runtime desktop, che
 * è **esattamente come è stato verificato** prima di arrivare su un telefono. Una funzione che
 * prendesse un `Bitmap` non si sarebbe potuta provare qui.
 */
object ClipPixels {

    /** Il lato del quadrato che il modello vuole. **256**, misurato, non 224. */
    const val SIDE = 256

    /**
     * I pixel pronti per il modello: `[1, 3, 256, 256]` in ordine canale-riga-colonna.
     *
     * ⚠️ **Ridimensiona il LATO CORTO e poi ritaglia al centro**, che è l'ordine di
     * `preprocessor_config.json`: ridimensionare il lato lungo lascerebbe bande vuote, e
     * ritagliare prima butterebbe via i bordi che poi servivano.
     * ⚠️ **Bilineare scritta in casa e non presa dalla piattaforma**, ed è la stessa ragione
     * per cui questa funzione è pura: quella di Android e quella della JVM non danno gli
     * stessi numeri, quindi la prova fatta qui non direbbe niente di quello che succede là.
     * Il costo, dichiarato: chi chiama deve arrivare con una fotografia già **rimpicciolita**
     * (`BitmapFactory` con `inSampleSize`, come fa `Thumbs`), o si paga una bilineare in
     * Kotlin su venti megapixel.
     *
     * @param argb i pixel in `0xAARRGGBB`, riga per riga, come li dà `Bitmap.getPixels`.
     */
    fun square(argb: IntArray, width: Int, height: Int): FloatArray {
        require(width > 0 && height > 0 && argb.size >= width * height) { "misure" }
        // Il lato corto va a 256: la scala è la stessa per i due assi, o la fotografia si
        // deformerebbe.
        val scale = SIDE.toFloat() / minOf(width, height)
        val grownW = maxOf(SIDE, Math.round(width * scale))
        val grownH = maxOf(SIDE, Math.round(height * scale))
        // Il ritaglio centrato: da dove comincia il quadrato dentro l'immagine ridimensionata.
        val left = (grownW - SIDE) / 2
        val top = (grownH - SIDE) / 2

        val out = FloatArray(3 * SIDE * SIDE)
        val plane = SIDE * SIDE
        for (y in 0 until SIDE) {
            // La riga di questa uscita, riportata sulle coordinate dell'immagine di partenza.
            val srcY = (y + top + 0.5f) * height / grownH - 0.5f
            val y0 = Math.floor(srcY.toDouble()).toInt()
            val fy = srcY - y0
            val ya = y0.coerceIn(0, height - 1)
            val yb = (y0 + 1).coerceIn(0, height - 1)
            for (x in 0 until SIDE) {
                val srcX = (x + left + 0.5f) * width / grownW - 0.5f
                val x0 = Math.floor(srcX.toDouble()).toInt()
                val fx = srcX - x0
                val xa = x0.coerceIn(0, width - 1)
                val xb = (x0 + 1).coerceIn(0, width - 1)

                val p00 = argb[ya * width + xa]
                val p01 = argb[ya * width + xb]
                val p10 = argb[yb * width + xa]
                val p11 = argb[yb * width + xb]

                val at = y * SIDE + x
                // ⚠️ **RGB e non BGR**, e l'ordine dei canali è la specie di errore che dà
                // risultati plausibili e sbagliati: il modello vuole rosso, verde, blu.
                out[at] = mix(r(p00), r(p01), r(p10), r(p11), fx, fy)
                out[plane + at] = mix(g(p00), g(p01), g(p10), g(p11), fx, fy)
                out[2 * plane + at] = mix(b(p00), b(p01), b(p10), b(p11), fx, fy)
            }
        }
        return out
    }

    /**
     * I quattro vicini pesati, e la divisione per 255 **che è tutta la normalizzazione**.
     *
     * ⚠️ Si divide QUI e non alla fine: farlo dopo vorrebbe dire una seconda passata su
     * duecentomila numeri per una moltiplicazione che si può fare mentre si è già lì.
     */
    private fun mix(v00: Int, v01: Int, v10: Int, v11: Int, fx: Float, fy: Float): Float {
        val top = v00 + (v01 - v00) * fx
        val bottom = v10 + (v11 - v10) * fx
        return (top + (bottom - top) * fy) / 255f
    }

    private fun r(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun g(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun b(pixel: Int): Int = pixel and 0xFF
}
