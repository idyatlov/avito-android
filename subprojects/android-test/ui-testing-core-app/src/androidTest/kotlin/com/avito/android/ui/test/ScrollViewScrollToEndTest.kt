package com.avito.android.ui.test

import com.avito.android.test.app.core.screenRule
import com.avito.android.ui.DistantViewOnScrollActivity
import org.junit.Rule
import org.junit.Test

internal class ScrollViewScrollToEndTest {

    @get:Rule
    val rule = screenRule<DistantViewOnScrollActivity>(launchActivity = true)

    @Test
    fun isVisible_viewIsNotDisplayed() {
        Screen.distantViewOnScroll.view.checks.isNotDisplayed()
    }
}
