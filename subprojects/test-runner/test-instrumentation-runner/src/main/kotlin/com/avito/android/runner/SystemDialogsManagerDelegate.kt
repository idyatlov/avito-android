package com.avito.android.runner

import com.avito.logger.LoggerFactory

internal class SystemDialogsManagerDelegate(loggerFactory: LoggerFactory) : InstrumentationTestRunnerDelegate() {

    private val systemDialogsManager = SystemDialogsManager(
        loggerFactory = loggerFactory
    )

    override fun beforeOnStart() {
        systemDialogsManager.closeSystemDialogs()
    }
}
