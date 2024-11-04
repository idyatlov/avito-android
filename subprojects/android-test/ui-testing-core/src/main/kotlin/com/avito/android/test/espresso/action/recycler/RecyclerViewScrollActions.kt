package com.avito.android.test.espresso.action.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.util.HumanReadables
import com.avito.android.test.espresso.action.scroll.collapseAllAppBarsInParent
import com.avito.android.test.espresso.action.scroll.scrollToScrollableParentCenterPosition
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

private class ScrollToPositionViewAction(private val position: Int) : ViewAction {

    override fun getConstraints(): Matcher<View> = Matchers.allOf(
        ViewMatchers.isAssignableFrom(RecyclerView::class.java),
        ViewMatchers.isDisplayed()
    )

    override fun getDescription(): String = "scroll RecyclerView to position: $position"

    override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView

        recyclerView.scrollItemAtPositionToCenter(
            uiController = uiController,
            position = position
        )
    }
}

internal class SmoothScrollToPositionViewAction(private val position: Int) : ViewAction {

    override fun getConstraints(): Matcher<View> = Matchers.allOf(
        ViewMatchers.isAssignableFrom(RecyclerView::class.java),
        ViewMatchers.isDisplayed()
    )

    override fun getDescription(): String = "smooth scroll RecyclerView to position: $position"

    override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView
        recyclerView.smoothScrollToPosition(position)
    }
}

public fun scrollToPosition(position: Int): ViewAction = ScrollToPositionViewAction(position)

private class ScrollToElementInsideRecyclerViewItem(
    private val position: Int,
    private val targetViewId: Int
) : ViewAction {

    override fun getDescription(): String = "scroll to element in item"

    override fun getConstraints(): Matcher<View> = Matchers.allOf(
        ViewMatchers.isAssignableFrom(RecyclerView::class.java),
        ViewMatchers.isDisplayed()
    )

    override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView

        recyclerView.scrollToViewInsideItemAtPositionToCenter(
            uiController = uiController,
            position = position,
            childId = targetViewId
        )
    }
}

public fun scrollToElementInsideRecyclerViewItem(
    position: Int,
    childViewId: Int
): ViewAction = ScrollToElementInsideRecyclerViewItem(
    position = position,
    targetViewId = childViewId
)

internal class ScrollToViewAction<VH : RecyclerView.ViewHolder>(
    private val match: RecyclerItemsMatcher.Match<VH>
) : ViewAction {

    override fun getConstraints(): Matcher<View> = Matchers.allOf(
        ViewMatchers.isAssignableFrom(RecyclerView::class.java),
        ViewMatchers.isDisplayed()
    )

    override fun getDescription(): String = when (match) {

        is RecyclerItemsMatcher.Match.All ->
            "performing RecyclerView scroll to item matching: ${match.matcher}"

        is RecyclerItemsMatcher.Match.AtPosition ->
            "performing RecyclerView scroll to  ${match.position}-th item matching: ${match.matcher}"
    }

    override fun perform(uiController: UiController, view: View) {
        val recyclerView = view as RecyclerView
        try {
            when (val matchResult = RecyclerItemsMatcher(recyclerView).match(match)) {
                is RecyclerItemsMatcher.Result.NoItem,
                is RecyclerItemsMatcher.Result.IndexOutOfBound,
                is RecyclerItemsMatcher.Result.NoItemAtPosition ->
                    throw AssertionError(matchResult.description)

                is RecyclerItemsMatcher.Result.Found -> {
                    val matchedItem = matchResult.item
                    scrollToPosition(matchedItem.position).perform(
                        uiController,
                        recyclerView
                    )
                    uiController.loopMainThreadUntilIdle()
                }
            }
        } catch (t: Throwable) {
            throw PerformException.Builder()
                .withActionDescription(this.description)
                .withViewDescription(HumanReadables.describe(view)) // TODO Add recycler description
                .withCause(t)
                .build()
        }
    }
}

public fun <VH : RecyclerView.ViewHolder> scrollToHolder(
    viewHolderMatcher: TypeSafeMatcher<VH>,
    position: Int? = null
): ViewAction = ScrollToViewAction(
    RecyclerItemsMatcher.Match.create(
        position,
        viewHolderMatcher
    )
)

private fun RecyclerView.scrollItemAtPositionToCenter(
    uiController: UiController,
    position: Int
) {
    try {
        collapseAllAppBarsInParent()
        uiController.loopMainThreadUntilIdle()
    } catch (t: Throwable) {
        // collapseAllAppBarsInParent contains hard logic to find app bar in parent,
        // so we're just trying to collapse it. This action is optional
    }

    if (!viewForItemAtPositionExists(position)) {
        scrollToPosition(position)
        uiController.loopMainThreadUntilIdle()
    }

    try {
        layoutManager?.findViewByPosition(position)
            ?.scrollToScrollableParentCenterPosition()
    } catch (t: Throwable) {
        // scrollToScrollableParentCenterPosition contains hard logic to find scrollable container,
        // so we're just trying to scroll to center of scrollable parent. This action is optional
    }

    uiController.loopMainThreadUntilIdle()
}

private fun RecyclerView.scrollToViewInsideItemAtPositionToCenter(
    uiController: UiController,
    position: Int,
    childId: Int
) {
    try {
        collapseAllAppBarsInParent()
        uiController.loopMainThreadUntilIdle()
    } catch (t: Throwable) {
        // collapseAllAppBarsInParent contains hard logic to find app bar in parent,
        // so we're just trying to collapse it. This action is optional
    }

    if (!viewForItemAtPositionExists(position)) {
        scrollToPosition(position)
        uiController.loopMainThreadUntilIdle()
    }

    try {
        layoutManager
            ?.findViewByPosition(position)
            ?.findViewById<View>(childId)
            ?.scrollToScrollableParentCenterPosition()
    } catch (t: Throwable) {
        // scrollToScrollableParentCenterPosition contains hard logic to find scrollable container,
        // so we're just trying to scroll to center of scrollable parent. This action is optional
    }

    uiController.loopMainThreadUntilIdle()
}

private fun RecyclerView.viewForItemAtPositionExists(position: Int): Boolean =
    findViewHolderForAdapterPosition(position) != null
