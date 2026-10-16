package io.github.roccobot.aiv

import android.content.Context
import android.media.MediaScannerConnection
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * The media scanner of the bench: it answers at once, for every path, with no address (6.25).
 *
 * ⚠️⚠️ **WITHOUT IT, EVERY TEST THAT WRITES A FILE WAITED 8 REAL SECONDS FOR NOTHING**: `FileTree.scan`
 * waits for the scanner's callback up to `SCAN_WAIT_MS`, and Robolectric's own scanner records the
 * paths and never calls back. Eleven tests (`DisfareTest`, `SalvaPlayTest`, `RinominaPlayTest`)
 * spent about 88 s of the bench's 261 there (Setaccio, finding G20).
 * ⚠️ **No address, as a scanner that found nothing would answer**: the bench's MediaStore is empty,
 * and a made-up `content://` address would make `Thumbs.forget` drop a thumbnail that does not
 * exist. The `file://` key is dropped before the wait, as on a phone.
 * ⚠️ **Installed for the whole bench**, in `robolectric.properties`: a test that writes a file
 * gets it without having to remember it.
 *
 * Author: Rocco Casadei, a.k.a. Roccobot
 */
@Implements(MediaScannerConnection::class)
class ScannerImmediato {
    companion object {
        @JvmStatic
        @Implementation
        fun scanFile(
            context: Context,
            paths: Array<String>?,
            mimeTypes: Array<String>?,
            callback: MediaScannerConnection.OnScanCompletedListener?
        ) {
            paths?.forEach { callback?.onScanCompleted(it, null) }
        }
    }
}
