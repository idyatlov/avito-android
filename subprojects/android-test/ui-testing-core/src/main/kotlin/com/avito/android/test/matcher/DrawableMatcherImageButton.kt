package com.avito.android.test.matcher

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

@Deprecated("Use DrawableMatcherImageView")
public class DrawableMatcherImageButton(
    @DrawableRes private val src: Int? = null,
    @ColorRes private val tint: Int? = null
) :
    DrawableMatcher<ImageView>(
        { it.drawable },
        { it.imageTintList },
        src,
        tint,
        ImageView::class.java
    )
