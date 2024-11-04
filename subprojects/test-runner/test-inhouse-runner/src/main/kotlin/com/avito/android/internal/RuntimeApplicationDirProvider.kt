package com.avito.android.internal

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.avito.report.ApplicationDirProvider
import com.avito.report.ApplicationDirProviderFactory
import java.io.File

internal class RuntimeApplicationDirProvider(context: Context) : ApplicationDirProvider {

    override val dir: File by lazy {
        val dir = runtimeDir(context)
        ensureConformsContract(dir, context)
        dir
    }

    private fun runtimeDir(context: Context): File {
        return if (Build.VERSION.SDK_INT >= 30) {
            // it's ok to use deprecated method here
            // public dir which is accessible also by adb
            @Suppress("DEPRECATION")
            context.externalMediaDirs[0]
        } else {
            ContextCompat.getExternalFilesDirs(context, null)[0]
        }
    }

    private fun ensureConformsContract(runtimeDir: File, context: Context) {
        val nonRuntimeDir = ApplicationDirProviderFactory.create(
            api = Build.VERSION.SDK_INT,
            packageName = context.packageName,
        ).dir

        require(nonRuntimeDir.canonicalPath == runtimeDir.canonicalPath) {
            "Expected the same dirs: " +
                "from context: $runtimeDir, " +
                "without context (for adb): $nonRuntimeDir"
        }
    }
}
