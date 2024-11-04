package com.avito.android.test.espresso.action.click

import android.graphics.Color
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.PrecisionDescriber
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import org.hamcrest.Matcher
import org.hamcrest.Matchers

internal class ClickAction(
    private val event: Event,
    private val coordinatesProvider: CoordinatesProvider,
    private val precisionDescriber: PrecisionDescriber,
    private val visualizeClicks: Boolean
) : ViewAction {

    override fun getDescription(): String = event.description()

    override fun getConstraints(): Matcher<View> = Matchers.allOf()

    override fun perform(uiController: UiController, view: View) {
        val rootView = view.rootView
        val precision = precisionDescriber.describePrecision()
        val coordinates = coordinatesProvider.calculateCoordinates(view)

        event.perform(
            uiController = uiController,
            view = view,
            rootView = rootView,
            coordinates = coordinates,
            precision = precision,
            visualize = visualizeClicks
        )
    }

    sealed class Event {

        abstract fun perform(
            uiController: UiController,
            view: View,
            rootView: View,
            coordinates: FloatArray,
            precision: FloatArray,
            visualize: Boolean
        )

        abstract fun description(): String

        object ClickEvent : Event() {

            override fun description(): String = "single click"

            override fun perform(
                uiController: UiController,
                view: View,
                rootView: View,
                coordinates: FloatArray,
                precision: FloatArray,
                visualize: Boolean
            ) {
                val clickVisualization = when (visualize) {
                    true -> ClickVisualization(coordinates[0], coordinates[1])
                    else -> null
                }
                val downEvent = downEvent(
                    coordinates = coordinates,
                    precision = precision
                )
                clickVisualization?.attachTo(rootView)
                rootView.dispatchTouchEvent(downEvent)

                uiController.loopMainThreadForAtLeast(ViewConfiguration.getTapTimeout().toLong())

                val upEvent = upEvent(downEvent)
                rootView.dispatchTouchEvent(upEvent)
                clickVisualization?.detach()

                downEvent.recycle()
                upEvent.recycle()

                uiController.loopMainThreadForAtLeast(ViewConfiguration.getPressedStateDuration().toLong())

                /**
                 * According to [androidx.test.espresso.action.GeneralClickAction.perform]
                 */
                if (view is WebView) {
                    uiController.loopMainThreadForAtLeast(ViewConfiguration.getDoubleTapTimeout().toLong())
                }
            }
        }

        object LongClickEvent : Event() {

            override fun description(): String = "long click"

            override fun perform(
                uiController: UiController,
                view: View,
                rootView: View,
                coordinates: FloatArray,
                precision: FloatArray,
                visualize: Boolean
            ) {
                val clickVisualization = when (visualize) {
                    true -> ClickVisualization(
                        x = coordinates[0],
                        y = coordinates[1],
                        color = Color.argb(0xA0, 0x00, 0x00, 0xFF)
                    )
                    else -> null
                }
                val downEvent = downEvent(
                    coordinates = coordinates,
                    precision = precision
                )
                clickVisualization?.attachTo(rootView)
                rootView.dispatchTouchEvent(downEvent)

                // Factor 1.5 is needed, otherwise a long press is not safely detected.
                // See android.test.TouchUtils longClickView
                uiController.loopMainThreadForAtLeast((1.5f * ViewConfiguration.getLongPressTimeout()).toLong())

                val upEvent = upEvent(downEvent)
                rootView.dispatchTouchEvent(upEvent)
                clickVisualization?.detach()

                downEvent.recycle()
                upEvent.recycle()

                uiController.loopMainThreadForAtLeast(ViewConfiguration.getPressedStateDuration().toLong())

                /**
                 * according to [androidx.test.espresso.action.GeneralClickAction.perform]
                 * TODO: is it correct only for single click?
                 */
                if (view is WebView) {
                    uiController.loopMainThreadForAtLeast(ViewConfiguration.getDoubleTapTimeout().toLong())
                }
            }
        }
    }
}

internal fun inProcessClickAction(
    coordinatesProvider: CoordinatesProvider,
    visualizeClicks: Boolean
): ViewAction = actionWithAssertions(
    ClickAction(
        coordinatesProvider = coordinatesProvider,
        precisionDescriber = Press.FINGER,
        event = ClickAction.Event.ClickEvent,
        visualizeClicks = visualizeClicks
    )
)

internal fun inProcessLongClickAction(
    coordinatesProvider: CoordinatesProvider,
    visualizeClicks: Boolean
): ViewAction = actionWithAssertions(
    ClickAction(
        coordinatesProvider = coordinatesProvider,
        precisionDescriber = Press.FINGER,
        event = ClickAction.Event.LongClickEvent,
        visualizeClicks = visualizeClicks
    )
)
