package com.avito.android.ui.test

import com.avito.android.test.app.core.screenRule
import com.avito.android.ui.TabLayoutActivity
import org.junit.Rule
import org.junit.Test

internal class TabLayoutTest {

    @get:Rule
    val rule = screenRule<TabLayoutActivity>(launchActivity = true)

    @Test
    fun tabsCountIs1000() {
        Screen.tabLayoutScreen.tabs.checks.withTabsCount(1000)
    }

    @Test
    fun selectTab500_tabIsDisplayed() {
        Screen.tabLayoutScreen.tabs.select(500)
        Screen.tabLayoutScreen.tabs.checks.withSelectedPosition(500)
    }
}
