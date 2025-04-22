package com.skyroute.api

/**
 * Defines the threading behaviour for message delivery in SkyRouteMQ.
 *
 * This enum is used to specify how subscribers will receive messages
 * depending on the desired thread context.
 *
 * @author Andre Suryana
 */
enum class ThreadMode {

    /**
     * The subscriber will be called on the main (UI) thread.
     *
     * Suitable for updating UI elements or interacting with components
     * that require execution on the main thread.
     */
    MAIN,

    /**
     * If the posting thread is the main thread, the subscriber will be called
     * on a background thread. Otherwise, it will be called on the posting thread.
     *
     * This is useful for lightweight work triggered from the UI thread.
     */
    BACKGROUND,

    /**
     * The subscriber will always be called on a new background thread.
     *
     * Suitable for tasks that should never block the posting thread or the main thread.
     */
    ASYNC
}