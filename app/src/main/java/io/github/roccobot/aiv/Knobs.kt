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
 * ⚠️ **La luminosità è quella della finestra, non quella del telefono**: scrivere quella di
 * sistema vuole `WRITE_SETTINGS`, un permesso che si concede da una pagina di sistema.
 * `screenBrightness` della finestra non chiede nulla, vale finché l'app è davanti e sparisce
 * quando si esce, come nei lettori video.
 * Il volume è quello vero del telefono (il flusso della musica): l'audio lo suona il sistema.
 */
object Knobs {

    /**
     * La luminosità attuale di questa finestra, da 0 a 1.
     *
     * ⚠️ La prima volta la finestra non ha una luminosità propria e risponde
     * `BRIGHTNESS_OVERRIDE_NONE` (-1): come punto di partenza porterebbe lo schermo al nero al
     * primo movimento del dito. Si parte allora da quella di sistema, cioè da quella visibile.
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
     * Il minimo è [FLOOR]: a zero certi schermi si spengono del tutto, e chi ha strisciato
     * troppo resta al buio senza vedere dove rimediare.
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
     * ⚠️ È una funzione a sé perché [setBrightness] stringe il valore fra [FLOOR] e 1, e non
     * può scrivere `BRIGHTNESS_OVERRIDE_NONE`, il solo valore che toglie l'imposizione.
     * Il volume non ha una funzione gemella: [setVolume] scrive il volume vero del telefono, e
     * rimetterlo a posto disferebbe una scelta fatta sul dispositivo.
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
     * Senza flag: `FLAG_SHOW_UI` mostrerebbe anche il cursore di sistema sopra il nostro.
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
     * Non basta un cast: il contesto di una composizione è spesso un `ContextWrapper` (il tema
     * ne mette uno), e il cast diretto risponderebbe null anche dove l'attività c'è.
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
