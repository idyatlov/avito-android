package com.avito.android.module_type

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

public abstract class ModuleTypeExtension : ExtensionAware {

    public abstract val type: Property<ModuleType>
}
