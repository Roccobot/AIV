package io.github.roccobot.aiv

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

/*
 * ⚠️⚠️ **A CHE COSA SERVE QUESTO FILE: A TOGLIERE LE BANDE DA UNA SFUMATURA, E IL DIFETTO È
 * ARITMETICO** (sua segnalazione, 2026-09-09: *secondo me la sfumatura può essere ulteriormente
 * migliorata, vedo ancora del banding. Se per fare un gradiente di qualità superiore serve
 * gestire una profondità colore più alta, o più memoria, o più risorse, per me va bene*).
 *
 * **Il conto.** Lo schermo tiene 256 livelli per canale. Una sfumatura che parte da un quarto di
 * opacità e arriva a zero fa cambiare il colore finale di una frazione di quei livelli: con una
 * tinta che dista un centinaio di livelli dal fondo sono venticinque gradini, distribuiti su
 * mezzo migliaio di pixel di altezza. Cioè **ogni gradino è alto una ventina di pixel**, ed è una
 * striscia che si vede. Non dipende dai colori scelti, non si toglie con più tappe (fra una
 * tappa e l'altra l'interpolazione è già continua: a quantizzare è la destinazione) e non si
 * toglie con più bit dentro l'app, perché il taglio a otto bit avviene alla fine, sul vetro.
 *
 * ⚠️⚠️ **L'UNICO RIMEDIO È IL RUMORE, e si chiama dithering**: si somma al colore un rumore
 * piccolo **prima** dell'arrotondamento, così i pixel a cavallo di un gradino cadono un po' di
 * qua e un po' di là, e il bordo netto si scioglie in una transizione sfumata. È lo stesso
 * mestiere del retino di una stampa.
 *
 * ⚠️⚠️ **LA `1.95` LO CHIEDEVA AL PAINT, E LUI LE BANDE LE VEDE ANCORA**: `isDither` accende il
 * dither di Skia, che è rumore ordinato preso da una matrice piccola. Quello che questo file
 * aggiunge è un dither **esplicito**, scritto da noi e per pixel, di un livello pieno e a
 * distribuzione triangolare, che è la quantità con cui l'errore di arrotondamento smette di
 * dipendere dal colore invece di essere soltanto attenuato.
 * - ⚠️⚠️ **E PERCHÉ QUELLO DEL PAINT NON SIA BASTATO NON SI SA, ED È GIUSTO SCRIVERLO INVECE DI
 *   INVENTARE UNA CAUSA**: sul banco quel dither si vede **agire** (`BandeTest` misura 184 righe
 *   miste su 210 col solo paint, contro 0 senza niente), quindi la riga della `1.95` faceva
 *   qualcosa. Ma il banco disegna col processore e il telefono con la scheda grafica, e quale dei
 *   due percorsi porti davvero quel flag fino al gradiente non è scritto in nessun posto che si
 *   possa leggere da qui. La differenza del rimedio nuovo è che **non dipende da quella
 *   risposta**: il rumore lo scriviamo noi, quindi c'è comunque.
 * - ⚠️ **La riga del paint resta**, e non è una ridondanza: sotto Android 13 il rimedio nuovo non
 *   c'è (vedi [ditherShader]), e là quel dither è tutto quello che si può avere.
 * - ⚠️ **Il rumore si somma ai soli canali del COLORE e non all'opacità**, ed è misurato sul
 *   conto del miscelamento: il colore che finisce a schermo vale `sorgente + fondo * (1 -
 *   opacità)`, quindi un rumore aggiunto anche all'opacità si cancella da sé sui fondi chiari
 *   (dove `fondo` vale quasi uno) e la sfumatura tornerebbe a bande proprio nel tema chiaro.
 *   Sommandolo al solo colore, il rumore sul risultato è quello voluto su qualunque fondo.
 * - ⚠️ **E resta dentro l'opacità** (`clamp` fra zero e `a`): un colore premoltiplicato più
 *   luminoso della propria opacità non è un colore, e che cosa ne farebbe il miscelatore non è
 *   scritto da nessuna parte.
 *
 * ⚠️ **Il rumore è ancorato allo SCHERMO e non alla sfumatura**, perché la coordinata che arriva
 * qui è quella del pixel: scorrendo, la tinta si muove e la grana sta ferma. È la cosa giusta da
 * vedere, e la contraria (una grana che scorre insieme alla tinta) si noterebbe come un velo che
 * si muove.
 *
 * ⚠️⚠️ **LE ALTRE SFUMATURE DELL'APP NON PASSANO DI QUI, E NON È UNA DIMENTICANZA: IL CONTO DICE
 * CHE LÀ NON C'È NIENTE DA TOGLIERE.** Le due in fondo allo schermo (`GroundFade`) vanno dal fondo
 * pieno al trasparente, cioè attraversano **tutti** i livelli in un centinaio di punti: un gradino
 * viene alto **un pixel**, che nessuno vede. Il gradiente dell'intestazione attraversa un quarto
 * dei livelli su quasi tutto lo schermo, e là un gradino viene alto **quasi quaranta pixel**. Lo
 * stesso disegno, due aritmetiche diverse: chi porta il rimedio anche là aggiunge grana dove non
 * serve.
 */

/**
 * Il programma che gira **su ogni pixel**: legge la sfumatura e le somma il rumore.
 *
 * ⚠️⚠️ **LA SFUMATURA ARRIVA COME INGRESSO E NON SI RISCRIVE QUI**, ed è la cosa che tiene una
 * fonte sola: le tappe restano quelle del mockup, dichiarate in Kotlin da chi disegna, e questo
 * programma non sa nemmeno che forma abbiano. Riscriverle in AGSL vorrebbe dire due rampe da
 * tenere allineate, e la prima a cambiare sarebbe quella che nessuno guarda.
 * ⚠️ **Il rumore è la somma di DUE sorgenti indipendenti**, che dà una distribuzione triangolare:
 * con una sorgente sola (distribuzione piatta) l'errore di arrotondamento resta legato al colore,
 * cioè le bande si attenuano invece di sparire. È il risultato classico della teoria del dither,
 * e costa una riga in più.
 * ⚠️ **La sorgente è un rumore a gradiente interlacciato** e non un seno moltiplicato per un
 * numero grande: il secondo, sui numeri a mezza precisione di uno shader, degenera a strisce
 * proprio dove serve uniforme.
 */
private const val DITHER_AGSL = """
uniform shader ramp;

half grain(float2 p) {
    return half(fract(52.9829189 * fract(dot(p, float2(0.06711056, 0.00583715)))));
}

half4 main(float2 p) {
    half4 c = ramp.eval(p);
    half noise = (grain(p) + grain(p + float2(37.0, 17.0)) - 1.0) * half(1.0 / 255.0);
    return half4(clamp(c.rgb + noise, half3(0.0), half3(c.a)), c.a);
}
"""

/**
 * La stessa sfumatura di [brush], dipinta col rumore che le toglie le bande, oppure `null` dove
 * non si può fare.
 *
 * ⚠️⚠️ **`null` VUOL DIRE ANDROID 12 O PRIMA, e chi chiama deve avere la sua strada**: uno shader
 * scritto a mano vuole `RuntimeShader`, che nasce con Android 13. Sotto, la sfumatura si dipinge
 * come prima, col solo dither del paint: il difetto resta più visibile là, ed è un fatto da
 * dichiarare invece di lasciarlo scoprire.
 * ⚠️ **Il secondo `null` è un pennello che non è uno shader**, cioè una tinta unita: una tinta
 * unita non ha nessuna rampa da quantizzare, quindi non c'è niente da ditherare e la strada di
 * prima è già quella giusta.
 * ⚠️ **La misura serve perché una sfumatura è ancorata alla sua scatola**: lo shader nasce per
 * quella misura, quindi va rifatto quando cambia. Chi chiama lo tiene già nella cache del
 * disegno, che si rifà esattamente là.
 *
 * @param brush la sfumatura da dipingere, con le sue tappe.
 * @param size quanto è grande il rettangolo che la porta.
 */
internal fun ditherShader(brush: Brush, size: Size): Shader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val ramp = (brush as? ShaderBrush)?.createShader(size) ?: return null
    return grainOver(ramp)
}

/**
 * Il programma compilato, con la sfumatura agganciata.
 *
 * ⚠️ **Vive in una funzione sua e non dentro [ditherShader]** perché il controllo di versione e
 * l'uso di una classe che nasce con Android 13 devono stare in due posti diversi, o l'analizzatore
 * statico non riconosce la guardia.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun grainOver(ramp: Shader): Shader =
    RuntimeShader(DITHER_AGSL).apply { setInputShader("ramp", ramp) }
