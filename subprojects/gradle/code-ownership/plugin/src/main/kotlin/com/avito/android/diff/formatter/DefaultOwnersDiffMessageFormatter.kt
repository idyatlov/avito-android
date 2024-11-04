package com.avito.android.diff.formatter

import com.avito.android.diff.model.OwnersDiff

public class DefaultOwnersDiffMessageFormatter : OwnersDiffMessageFormatter {

    override fun formatDiffMessage(diffs: OwnersDiff): String {
        return buildString {
            append("Found difference in code owners structure!\n")
            if (diffs.removed.isNotEmpty()) {
                val removedOwners = diffs.removed.joinToString { it.toString() }
                append("*Removed owners:* $removedOwners\n")
            }
            if (diffs.added.isNotEmpty()) {
                val addedOwners = diffs.added.joinToString { it.toString() }
                append("*Added owners:* $addedOwners")
            }
        }
    }
}
