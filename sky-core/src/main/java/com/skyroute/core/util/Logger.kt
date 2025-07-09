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
package com.skyroute.core.util

import android.util.Log

/**
 * A platform-agnostic logging interface for structured logging at various log levels.
 *
 * This interface abstracts typical Android-style logging methods, allowing implementations
 * to log messages based on severity. It is intended to be implemented by platform-specific
 * loggers (e.g., AndroidLogger using [android.util.Log]) and can also be used with no-op or
 * standard output loggers for testing purposes.
 *
 * @author Andre Suryana
 */
interface Logger {

    /**
     * Logs a verbose message.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     */
    fun v(tag: String, msg: String)

    /**
     * Logs a debug message.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     */
    fun d(tag: String, msg: String)

    /**
     * Logs a debug message.
     *
     * @param msg The message to log.
     */
    fun d(msg: String)

    /**
     * Logs an informational message.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     */
    fun i(tag: String, msg: String)

    /**
     * Logs an informational message.
     *
     * @param msg The message to log.
     */
    fun i(msg: String)

    /**
     * Logs a warning message.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     */
    fun w(tag: String, msg: String)

    /**
     * Logs a warning message.
     *
     * @param msg The message to log.
     */
    fun w(msg: String)

    /**
     * Logs an error message.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     */
    fun e(tag: String, msg: String)

    /**
     * Logs an error message along with a [Throwable] stack trace.
     *
     * @param tag A string used to identify the source of the log message.
     * @param msg The message to log.
     * @param tr An optional [Throwable] whose stack trace should be logged.
     */
    fun e(tag: String, msg: String, tr: Throwable?)

    /**
     * A default implementation of [Logger] that uses Android's [android.util.Log] for logging.
     */
    class Default : Logger {

        override fun v(tag: String, msg: String) {
            Log.v(tag, msg)
        }

        override fun d(tag: String, msg: String) {
            Log.d(tag, msg)
        }

        override fun d(msg: String) {
            Log.d(getCallerClassName(), msg)
        }

        override fun i(tag: String, msg: String) {
            Log.i(tag, msg)
        }

        override fun i(msg: String) {
            Log.i(getCallerClassName(), msg)
        }

        override fun w(tag: String, msg: String) {
            Log.w(tag, msg)
        }

        override fun w(msg: String) {
            Log.w(getCallerClassName(), msg)
        }

        override fun e(tag: String, msg: String) {
            Log.e(tag, msg)
        }

        override fun e(tag: String, msg: String, tr: Throwable?) {
            Log.e(tag, msg, tr)
        }
    }

    /**
     * A simple [Logger] implementation that logs to standard output using `println`.
     * Suitable for unit testing or non-Android environments.
     */
    class Stdout : Logger {
        override fun v(tag: String, msg: String) = println("V/$tag: $msg")
        override fun d(tag: String, msg: String) = println("D/$tag: $msg")
        override fun d(msg: String) = println("D/${getCallerClassName()}: $msg")
        override fun i(msg: String) = println("I/${getCallerClassName()}: $msg")
        override fun w(msg: String) = println("W/${getCallerClassName()}: $msg")
        override fun i(tag: String, msg: String) = println("I/$tag: $msg")
        override fun w(tag: String, msg: String) = println("W/$tag: $msg")
        override fun e(tag: String, msg: String) = println("E/$tag: $msg")
        override fun e(tag: String, msg: String, tr: Throwable?) {
            println("E/$tag: $msg")
            tr?.printStackTrace()
        }
    }

    /**
     * A [Logger] implementation that does nothing.
     */
    class None : Logger {
        override fun v(tag: String, msg: String) = Unit
        override fun d(tag: String, msg: String) = Unit
        override fun d(msg: String) = Unit
        override fun i(msg: String) = Unit
        override fun i(tag: String, msg: String) = Unit
        override fun w(tag: String, msg: String) = Unit
        override fun w(msg: String) = Unit
        override fun e(tag: String, msg: String) = Unit
        override fun e(tag: String, msg: String, tr: Throwable?) = Unit
    }

    companion object {
        fun getCallerClassName(): String {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                if (
                    !element.className.startsWith("java.lang.Thread") &&
                    !element.className.contains("Logger") &&
                    !element.className.contains("kotlin.") &&
                    !element.className.contains("sun.reflect.")
                ) {
                    return element.className.substringAfterLast('.')
                }
            }
            return ""
        }
    }
}
