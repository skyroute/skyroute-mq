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

class TestLogger : Logger {

    val logs = mutableListOf<String>()

    override fun v(tag: String, msg: String) {
        logs += "V: $tag - $msg"
    }

    override fun d(tag: String, msg: String) {
        logs += "D: $tag - $msg"
    }

    override fun d(msg: String) {
        logs += "D: $msg"
    }

    override fun i(tag: String, msg: String) {
        logs += "I: $tag - $msg"
    }

    override fun i(msg: String) {
        logs += "I: $msg"
    }

    override fun w(tag: String, msg: String) {
        logs += "W: $tag - $msg"
    }

    override fun w(msg: String) {
        logs += "W: $msg"
    }

    override fun e(tag: String, msg: String) {
        logs += "E: $tag - $msg"
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        logs += "E: $tag - $msg"
    }
}
