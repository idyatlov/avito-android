package com.avito.android.test.context

import android.view.View
import androidx.test.espresso.matcher.RootMatchers
import com.avito.android.test.SimpleInteractionContext
import org.hamcrest.Matcher

public class PopupInteractionContext(
    matcher: Matcher<View>,
    precondition: () -> Unit = {}
) : SimpleInteractionContext(
    matcher = matcher,
    rootMatcher = RootMatchers.isPlatformPopup(),
    precondition = precondition
)
