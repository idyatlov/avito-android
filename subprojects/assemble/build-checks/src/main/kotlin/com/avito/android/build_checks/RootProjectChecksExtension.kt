package com.avito.android.build_checks

import com.avito.android.build_checks.RootProjectChecksExtension.RootProjectCheck.AndroidSdk
import com.avito.android.build_checks.RootProjectChecksExtension.RootProjectCheck.MacOSLocalhost
import org.gradle.api.Action
import java.io.Serializable
import kotlin.reflect.full.createInstance

public open class RootProjectChecksExtension : BuildChecksExtension() {

    override val allChecks: List<Check>
        get() {
            return RootProjectCheck::class.sealedSubclasses
                .map { it.createInstance() }
        }

    public fun androidSdk(action: Action<AndroidSdk>): Unit =
        register(AndroidSdk(), action)

    public fun macOSLocalhost(action: Action<MacOSLocalhost>): Unit =
        register(MacOSLocalhost(), action)

    public sealed class RootProjectCheck : Check {

        public override var enabled: Boolean = true

        public open class AndroidSdk : RootProjectCheck(), RequireValidation {

            public data class AndroidSdkVersion(
                val compileSdkVersion: Int,
                val revision: Int,
                val strict: Boolean,
            ) : Serializable

            internal val versions = mutableSetOf<AndroidSdkVersion>()

            public fun version(compileSdkVersion: Int, revision: Int, strict: Boolean = false) {
                versions.add(
                    AndroidSdkVersion(compileSdkVersion, revision, strict)
                )
            }

            override fun validate() {
                require(versions.isNotEmpty()) {
                    "At least one version must be configured in buildChecks.androidSdk"
                }
            }
        }

        public open class MacOSLocalhost : RootProjectCheck()

        override fun equals(other: Any?): Boolean {
            return this.javaClass == other?.javaClass
        }
    }
}
