package com.avito.android.test.report.model

import com.avito.android.test.annotations.TestCaseBehavior
import com.avito.android.test.annotations.TestCasePriority
import com.avito.report.model.Flakiness
import com.avito.report.model.Kind
import com.avito.test.model.TestName

internal fun TestMetadata.Companion.createStubInstance(
    caseId: Int? = null,
    description: String? = null,
    name: TestName = TestName("com.Test", "test"),
    dataSetNumber: Int? = null,
    kind: Kind = Kind.UNKNOWN,
    priority: TestCasePriority? = null,
    behavior: TestCaseBehavior? = null,
    externalId: String? = null,
    featureIds: List<Int> = emptyList(),
    tagIds: List<Int> = emptyList(),
    flakiness: Flakiness = Flakiness.Stable,
    groupList: List<String> = emptyList(),
    isRegression: Boolean = false,
) = TestMetadata(
    caseId = caseId,
    description = description,
    name = name,
    dataSetNumber = dataSetNumber,
    kind = kind,
    priority = priority,
    behavior = behavior,
    externalId = externalId,
    featureIds = featureIds,
    tagIds = tagIds,
    flakiness = flakiness,
    groupList = groupList,
    isRegression = isRegression,
)
