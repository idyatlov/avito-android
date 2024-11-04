package com.avito.android.test.element.field.actions

import android.app.Application
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import com.avito.android.test.espresso.EspressoActions
import com.avito.android.test.util.HiddenApiOpener
import com.avito.android.test.util.executeMethod
import com.avito.android.test.util.getFieldByReflectionWithAnyField
import com.avito.android.waiter.waitFor
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import java.util.concurrent.TimeUnit

/**
 * Type text action that is used for typing instead of [androidx.test.espresso.action.TypeTextAction]
 *
 * Differences:
 *  - We wait for focus on the field
 *  - We wait for at least one text changed event
 *  - We use InputConnection API (for software keyboards) instead of injecting key events to
 *    (window or input managers)
 *
 * Why keyboard API is better?
 *  - We can apply any symbol as text instead of injecting low level key events.
 *    It's hard to apply, for example, cyrillic symbols using key events API.
 *  - Actual user most often uses software keyboards
 */
internal class TypeText(private val stringToBeTyped: String) : ViewAction {

    override fun getConstraints(): Matcher<View> = Matchers.allOf(
        ViewMatchers.isDisplayed(),
        ViewMatchers.isAssignableFrom(EditText::class.java)
    )

    override fun perform(uiController: UiController, view: View) {
        view as EditText

        if (stringToBeTyped.isEmpty()) {
            return
        }

        tapForFocus(uiController = uiController, editText = view)
        writeText(uiController = uiController, editText = view)

        uiController.loopMainThreadUntilIdle()
    }

    private fun tapForFocus(uiController: UiController, editText: EditText) {
        EspressoActions.click().perform(uiController, editText)
        uiController.loopMainThreadUntilIdle()

        waitMainLoopFor(uiController) {
            assertThat(
                "View must have focus after tap before text typing",
                editText.hasFocus(),
                Matchers.`is`(true)
            )
        }
    }

    private fun writeText(uiController: UiController, editText: EditText) {
        HiddenApiOpener.ensureUnseal()

        val inputMethodManager = ApplicationProvider.getApplicationContext<Application>()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val context = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> inputMethodManager
                .getFieldByReflectionWithAnyField("mFallbackInputConnection")
                .executeMethod("getInputConnection")

            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> inputMethodManager
                .getFieldByReflectionWithAnyField("mFallbackInputConnection")
                .getFieldByReflectionWithAnyField("mInputConnection")

            else -> inputMethodManager
                .getFieldByReflectionWithAnyField("mIInputContext")
        }

        var textChangedAtLeastOnce = false
        val textWatcher = object : SimpleTextWatcher() {

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                textChangedAtLeastOnce = true
            }
        }
        editText.addTextChangedListener(textWatcher)

        context.executeMethod("beginBatchEdit")
        context.executeMethod("finishComposingText")
        context.executeMethod("commitText", stringToBeTyped, 1)
        context.executeMethod("endBatchEdit")

        waitMainLoopFor(uiController) {
            assertThat(
                "Failed to write text. Typing event has sent but hasn't handled",
                textChangedAtLeastOnce,
                Matchers.`is`(true)
            )
        }
        editText.removeTextChangedListener(textWatcher)
    }

    private fun waitMainLoopFor(uiController: UiController, action: () -> Unit) = waitFor(
        frequencyMs = 100,
        timeoutMs = TimeUnit.SECONDS.toMillis(3),
        allowedExceptions = setOf(Throwable::class.java),
        sleepAction = { delay -> uiController.loopMainThreadForAtLeast(delay) },
        action = action
    )

    override fun getDescription(): String = "type text $stringToBeTyped"
}

internal open class SimpleTextWatcher : TextWatcher {

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
}
