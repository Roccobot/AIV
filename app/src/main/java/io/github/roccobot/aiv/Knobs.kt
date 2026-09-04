package io.github.roccobot.aiv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Le due manopole che si regolano strisciando sopra un filmato: **luminosità** a sinistra,
 * **volume** a destra.
 *
 * ⚠️⚠️ **LA LUMINOSITÀ È QUELLA DELLA FINESTRA, NON QUELLA DEL TELEFONO**, ed è la scelta
 * che rende la funzione possibile senza chiedere niente: scrivere la luminosità di sistema
 * vuole `WRITE_SETTINGS`, un permesso che si concede da una pagina di sistema e che per
 * guardare un video nessuno vorrebbe dare. `screenBrightness` della finestra non chiede
 * nulla, vale finché l'app è davanti e sparisce quando si esce, che è esattamente il
 * comportamento dei lettori video.
 * ⚠️ Il volume invece è quello **vero** del telefono (il flusso della musica), perché un
 * volume finto non esiste: l'audio lo suona il sistema.
 */
object Knobs {

    /**
     * La luminosità attuale di questa finestra, da 0 a 1.
     *
     * ⚠️⚠️ **La prima volta la finestra NON HA una luminosità propria** e risponde
     * `BRIGHTNESS_OVERRIDE_NONE`, che vale -1: usarlo come punto di partenza farebbe
     * saltare lo schermo al nero al primo movimento del dito. Si parte allora da quella di
     * **sistema**, che è quella che la persona sta vedendo in quel momento.
     */
    fun brightness(activity: Activity?): Float {
        val own = activity?.window?.attributes?.screenBrightness ?: return HALF
        if (own >= 0f) return own.coerceIn(0f, 1f)
        val system = runCatching {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrNull() ?: return HALF
        return (system / SYSTEM_STEPS).coerceIn(0f, 1f)
    }

    /**
     * Scrive la luminosità di questa finestra.
     *
     * ⚠️ Il minimo non è zero ma [FLOOR]: a zero certi schermi si spengono del tutto, e chi
     * ha strisciato troppo si ritrova al buio senza capire che cosa ha fatto e senza vedere
     * il dito per rimediare.
     */
    fun setBrightness(activity: Activity?, value: Float) {
        val window = activity?.window ?: return
        val fresh: WindowManager.LayoutParams = window.attributes
        fresh.screenBrightness = value.coerceIn(FLOOR, 1f)
        window.attributes = fresh
    }

    /**
     * Ridà la finestra alla luminosità di sistema.
     *
     * ⚠️⚠️ **NON È [setBrightness] CON UN VALORE ALTO**, ed è la ragione per cui esiste una
     * seconda funzione: quella stringe il valore fra [FLOOR] e 1, quindi non può scrivere
     * `BRIGHTNESS_OVERRIDE_NONE`, che è il solo valore che **toglie** l'imposizione invece di
     * sostituirla. Scrivere 1 lascerebbe lo schermo al massimo, che è un'altra cosa da 'come
     * prima'.
     * ⚠️ **Il volume non ha una funzione gemella, e non è una dimenticanza**: [setVolume]
     * scrive il volume vero del telefono e non una proprietà della finestra, quindi
     * rimetterlo a posto vorrebbe dire disfare una scelta fatta sul dispositivo.
     */
    fun clearBrightness(activity: Activity?) {
        val window = activity?.window ?: return
        val fresh: WindowManager.LayoutParams = window.attributes
        fresh.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = fresh
    }

    /** Il volume della musica, da 0 a 1. */
    fun volume(context: Context): Float {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0f
        val top = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (top <= 0) return 0f
        return (audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / top).coerceIn(0f, 1f)
    }

    /**
     * Scrive il volume della musica.
     *
     * ⚠️ Senza bandierine: `FLAG_SHOW_UI` farebbe comparire **anche** il cursore di sistema
     * sopra il nostro, cioè due indicatori per lo stesso gesto.
     */
    fun setVolume(context: Context, value: Float) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val top = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (top <= 0) return
        val step = (value.coerceIn(0f, 1f) * top).roundToInt().coerceIn(0, top)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0) }
    }

    /**
     * L'attività dentro cui gira questa composizione, che serve per la finestra.
     *
     * ⚠️ Non basta un cast: il contesto di una composizione è spesso un `ContextWrapper`
     * (il tema ne mette uno), e il cast diretto risponderebbe null proprio dove l'attività
     * c'è.
     */
    fun activityOf(context: Context): Activity? {
        var walk: Context? = context
        while (walk is ContextWrapper) {
            if (walk is Activity) return walk
            walk = walk.baseContext
        }
        return null
    }

    private const val HALF = 0.5f
    private const val FLOOR = 0.02f
    private const val SYSTEM_STEPS = 255f
}
