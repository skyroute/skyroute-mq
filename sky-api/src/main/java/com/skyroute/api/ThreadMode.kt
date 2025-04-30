/*
 * Copyright (C) 2025 Andre Suryana, SkyRoute (https://github.com/skyroute)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    ASYNC,
}
