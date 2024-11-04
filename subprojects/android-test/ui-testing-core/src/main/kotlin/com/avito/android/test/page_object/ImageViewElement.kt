package com.avito.android.test.page_object

import android.graphics.Bitmap
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.avito.android.test.InteractionContext
import com.avito.android.test.SimpleInteractionContext
import com.avito.android.test.checks.Checks
import com.avito.android.test.checks.ChecksDriver
import com.avito.android.test.checks.ChecksImpl
import com.avito.android.test.matcher.BitmapMatcher
import com.avito.android.test.matcher.DrawableMatcherImageView
import com.avito.android.test.matcher.ImageShownMatcher
import org.hamcrest.Matcher

public open class ImageViewElement(
    interactionContext: InteractionContext
) : ViewElement(interactionContext) {

    override val checks: ImageViewChecks = ImageViewChecksImpl(interactionContext)

    // TODO: remove this constructor and use element fabric method to create an instance
    public constructor(matcher: Matcher<View>) : this(SimpleInteractionContext(matcher))
}

public interface ImageViewChecks : Checks {

    public fun withSourceDrawable(@DrawableRes src: Int? = null, @ColorRes tint: Int? = null)

    public fun withShownImage()

    public fun withImage(bitmap: Bitmap)
}

public class ImageViewChecksImpl(private val driver: ChecksDriver) : ImageViewChecks,
    Checks by ChecksImpl(driver) {

    override fun withSourceDrawable(@DrawableRes src: Int?, @ColorRes tint: Int?) {
        driver.check(matches(DrawableMatcherImageView(src, tint)))
    }

    override fun withShownImage() {
        driver.check(matches(ImageShownMatcher()))
    }

    override fun withImage(bitmap: Bitmap) {
        driver.check(matches(BitmapMatcher(bitmap)))
    }
}
