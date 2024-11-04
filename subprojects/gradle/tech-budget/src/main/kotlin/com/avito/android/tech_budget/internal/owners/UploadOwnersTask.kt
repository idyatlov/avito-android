package com.avito.android.tech_budget.internal.owners

import com.avito.android.OwnerSerializerProvider
import com.avito.android.model.Owner
import com.avito.android.tech_budget.DumpInfoConfiguration
import com.avito.android.tech_budget.internal.dump.DumpInfo
import com.avito.android.tech_budget.internal.owners.models.UploadOwnersRequestBody
import com.avito.android.tech_budget.internal.service.RetrofitBuilderService
import com.avito.android.tech_budget.internal.utils.executeWithHttpFailure
import com.avito.android.tech_budget.owners.TechBudgetOwnerMapper
import com.avito.logger.GradleLoggerPlugin
import com.avito.logger.LoggerFactory
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import retrofit2.create

internal abstract class UploadOwnersTask : DefaultTask() {

    @get:Input
    abstract val owners: SetProperty<Owner>

    @get:Input
    abstract val ownerSerializer: Property<OwnerSerializerProvider>

    @get:Input
    abstract val techBudgetOwnerMapper: Property<TechBudgetOwnerMapper>

    @get:Nested
    abstract val dumpInfoConfiguration: Property<DumpInfoConfiguration>

    @get:Internal
    abstract val retrofitBuilderService: Property<RetrofitBuilderService>

    private val loggerFactory: Provider<LoggerFactory> = GradleLoggerPlugin.provideLoggerFactory(this)

    @TaskAction
    fun uploadOwners() {
        val dumpInfoConfig = dumpInfoConfiguration.get()

        val service = retrofitBuilderService.get()
            .build(loggerFactory.get())
            .create<UploadOwnersApi>()

        val requestBody = UploadOwnersRequestBody(
            DumpInfo.fromExtension(dumpInfoConfig),
            owners.get().map(techBudgetOwnerMapper.get()::map)
        )

        service.dumpOwners(requestBody)
            .executeWithHttpFailure(errorMessage = "Upload owners request failed")
    }

    companion object {
        const val NAME = "uploadOwners"
    }
}
