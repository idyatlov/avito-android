package com.avito.android.test.matcher

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

public class DrawableBackgroundMatcher(
    @DrawableRes private val src: Int? = null,
    @ColorRes private val tint: Int? = null
) :
    DrawableMatcher<View>(
        { it.background },
        { it.backgroundTintList },
        src,
        tint,
        View::class.java
    )
