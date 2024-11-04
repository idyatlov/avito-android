package com.avito.android.runner.annotation.resolver

import com.avito.android.runner.annotation.UserAgent

public class UserAgentAnnotationResolver : AnnotationResolver<UserAgent>(
    "userAgent",
    UserAgent::class.java,
    { annotation -> TestMetadataResolver.Resolution.ReplaceString(annotation.userAgent) }
)
