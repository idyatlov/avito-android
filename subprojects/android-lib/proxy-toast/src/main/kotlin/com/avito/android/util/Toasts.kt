package com.avito.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting

/**
 * Over-Platform-Type abstraction for more control, specially to cancel real usage in UI tests because of flakiness
 *
 * You should use only provided showToast extensions to be able to test it
 */
public interface ProxyToast {

    public fun show(toast: Toast)

    public companion object {
        @VisibleForTesting
        public var instance: ProxyToast = PlatformProxyToast()
    }
}

/**
 * Just a proxy over real platform [android.widget.Toast]
 */
public open class PlatformProxyToast : ProxyToast {
    override fun show(toast: Toast) {
        toast.show()
    }
}

@SuppressLint("ShowToast")
@JvmOverloads
public fun Context.showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(this, resId, duration).apply { ProxyToast.instance.show(this) }
}

@SuppressLint("ShowToast")
@JvmOverloads
public fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(this, text, duration).apply { ProxyToast.instance.show(this) }
}
