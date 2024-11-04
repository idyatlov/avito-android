package com.avito.android.test.page_object

import android.annotation.SuppressLint
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.SwipeDirections.RIGHT_TO_LEFT
import androidx.test.espresso.matcher.ViewMatchers
import com.avito.android.test.InteractionContext
import com.avito.android.test.SimpleInteractionContext
import com.avito.android.test.espresso.EspressoActions
import com.google.android.material.R
import com.google.android.material.snackbar.Snackbar
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * Snackbar https://developer.android.com/reference/android/support/design/widget/Snackbar.html
 *
 * @param textMatcher optional matcher to handle 2+ snackbars on screen conflicts
 * (use part of message to work with specific snackbar)
 *
 * @warning hidden snackbar stays in hierarchy longer than you might expect;
 * if you encounter weird "duplicate snackbars" it's a known issue
 */
@Deprecated(message = "Has a flaky behaviour", replaceWith = ReplaceWith("SnackbarRule"))
public class SnackbarElement(interactionContext: InteractionContext) : ViewElement(interactionContext) {

    public val message: ViewElement = ViewElement(
        interactionContext.provideChildContext(
            ViewMatchers.withId(R.id.snackbar_text)
        )
    )

    public val button: ViewElement = ViewElement(
        interactionContext.provideChildContext(
            ViewMatchers.withId(R.id.snackbar_action)
        )
    )

    // TODO: migrate to HandleParentContext
    public constructor() : this(SimpleInteractionContext(snackbarLayoutMatcher()))

    public constructor(textMatcher: Matcher<String>? = null) : this(
        SimpleInteractionContext(
            if (textMatcher == null) {
                snackbarLayoutMatcher()
            } else {
                Matchers.allOf(
                    snackbarLayoutMatcher(),
                    ViewMatchers.hasDescendant(ViewMatchers.withText(textMatcher))
                )
            }
        )
    )

    public fun swipeOut(): ViewAction = EspressoActions.swipe(RIGHT_TO_LEFT)
}

@SuppressLint("RestrictedApi")
private fun snackbarLayoutMatcher() = ViewMatchers.isAssignableFrom(Snackbar.SnackbarLayout::class.java)
