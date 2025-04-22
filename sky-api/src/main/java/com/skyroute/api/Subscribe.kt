package com.skyroute.api

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(
    val topic: String,
    val threadMode: ThreadMode = ThreadMode.POSTING,
)
