package io.github.roccobot.aiv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HealingEditorTest {
    @get:Rule val screen = createComposeRule()
    private val app: Context get() = ApplicationProvider.getApplicationContext()

    private fun text(id: Int) = app.getString(id)

    private fun command(id: Int) = screen.onNodeWithContentDescription(text(id))

    private fun label(id: Int) = screen.onNodeWithText(text(id))

    @Before fun prepareHints() =
        runBlocking {
            Hint.MODULES.remember(app)
            Hint.EDITOR_TOOLS.remember(app)
        }

    @Test fun `painting waits for Apply and corrections use normal undo redo and save`() {
        var saved: Look? = null
        open { saved = it }
        val bounds = command(R.string.look_compare).fetchSemanticsNode().boundsInRoot
        command(R.string.look_compare).performTouchInput {
            down(center)
            moveBy(Offset(25f, 0f))
            moveBy(Offset(25f, 0f))
            up()
        }
        screen.waitForIdle()
        label(R.string.editor_apply).assertIsEnabled()
        assertEquals("Draft painting must not resize the image", bounds,
            command(R.string.look_compare).fetchSemanticsNode().boundsInRoot)
        command(R.string.editor_undo).assertIsNotEnabled()
        label(R.string.editor_save).assertIsNotEnabled()
        assertNull(saved)
        label(R.string.editor_apply).performClick()
        screen.waitUntil(20_000) {
            !command(R.string.editor_undo)
                .fetchSemanticsNode()
                .config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) &&
                screen.onAllNodesWithText(text(R.string.look_heal_pending)).fetchSemanticsNodes().isEmpty()
        }
        screen.waitForIdle()
        command(R.string.editor_undo).assertIsEnabled()
        label(R.string.editor_apply).assertIsNotEnabled()
        label(R.string.editor_save).assertIsEnabled()
        label(R.string.editor_save).performClick()
        assertEquals(1, saved!!.healing.patches.size)
        command(R.string.editor_undo).performClick()
        label(R.string.editor_save).assertIsNotEnabled()
        command(R.string.editor_redo).performClick()
        label(R.string.editor_save).performClick()
        assertEquals(1, saved!!.healing.patches.size)
    }

    @Test fun `two finger zoom does not paint and a tap selection can be cleared`() {
        open()
        command(R.string.look_compare).performTouchInput {
            down(0, center - Offset(25f, 0f))
            down(1, center + Offset(25f, 0f))
            moveTo(0, center - Offset(45f, 0f))
            moveTo(1, center + Offset(45f, 0f))
            up(0)
            up(1)
        }
        screen.waitForIdle()
        label(R.string.editor_apply).assertIsNotEnabled()
        command(R.string.look_compare).performTouchInput { click(center) }
        screen.waitForIdle()
        label(R.string.editor_apply).assertIsEnabled()
        label(R.string.look_heal_clear).performClick()
        label(R.string.editor_apply).assertIsNotEnabled()
        command(R.string.editor_undo).assertIsNotEnabled()
    }

    private fun open(onSave: (Look) -> Unit = {}) {
        screen.setContent { Scene(onSave) }
        screen.waitUntil(5_000) {
            screen.onAllNodesWithContentDescription(text(R.string.look_compare)).fetchSemanticsNodes().isNotEmpty()
        }
        command(R.string.look_detail).performClick()
        command(R.string.look_heal).performClick()
        label(R.string.look_brush_size).assertExists()
    }

    @Composable private fun Scene(onSave: (Look) -> Unit) {
        val file = File(app.filesDir, "healing-editor.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        AivTheme(darkTheme = false) {
            CompositionLocalProvider(LocalPadLook provides PadLook()) {
                Box(Modifier.fillMaxSize()) {
                    AdvancedEditorScreen(
                        uri = file.toUri(),
                        busy = false,
                        marked = false,
                        marking = false,
                        hasMark = false,
                        onMark = {},
                        onMarkSetup = {},
                        resize = Resize.NONE,
                        saved = Resize.NONE,
                        resizing = false,
                        onResize = {},
                        onResizeDefault = {},
                        onSave = { look, _ -> onSave(look) },
                        onBack = {},
                    )
                }
            }
        }
    }
}
