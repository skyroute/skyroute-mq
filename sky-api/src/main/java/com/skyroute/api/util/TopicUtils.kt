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
package com.skyroute.api.util

/**
 * Utility functions for handling MQTT-style topics matching and wildcard extraction.
 * Provides methods to match topics with wildcards and extract wildcard values from topic patterns.
 *
 * @author Andre Suryana
 */
object TopicUtils {

    /**
     * Extension function to check if the current topic pattern matches a given topic.
     * Supports both single-level (`+`) and multi-level (`#`) wildcards.
     *
     * Usage example:
     * ```
     * "test/+/sensor".matchesTopic("test/abc/sensor") // Returns true
     * "test/+/sensor".matchesTopic("test/abc") // Returns false
     * "test/#".matchesTopic("test/abc/sensor") // Returns true
     * ```
     *
     * @param topic The topic to be compared against the current topic pattern.
     * @return `true` if the topic matches the pattern, `false` otherwise.
     */
    fun String.matchesTopic(topic: String): Boolean {
        val subLevels = this.split('/')
        val incomingLevels = topic.split('/')

        val subSize = subLevels.size
        val incomingSize = incomingLevels.size

        var i = 0
        while (i < subSize) {
            when (val subLevel = subLevels[i]) {
                "#" -> {
                    // '#' must be at the end
                    return i == subSize - 1
                }

                "+" -> {
                    // matches any single level
                    if (i >= incomingSize) return false
                }

                else -> {
                    if (i >= incomingSize || subLevel != incomingLevels[i]) return false
                }
            }
            i++
        }

        // Incoming topic has more levels than subscription, and no '#' was used
        return i == incomingSize
    }

    /**
     * Extracts the wildcards from a subscription topic and an actual topic.
     * Returns a list of matched wildcards or `null` if no match is found.
     * Supports single-level (`+`) and multi-level (`#`) wildcards.
     *
     * Example usage:
     * ```
     * TopicUtils.extractWildcards("test/+/sensor", "test/abc/sensor") // Returns [abc]
     * TopicUtils.extractWildcards("test/#", "test/abc/sensor") // Returns [abc, sensor]
     * TopicUtils.extractWildcards("test/+/sensor", "test/abc") // Returns null (no match)
     * ```
     *
     * @param subscriptionTopic The topic pattern with wildcards.
     * @param actualTopic The actual topic to compare.
     * @return A list of strings representing the matched wildcards, or `null` if no match is found.
     */
    fun extractWildcards(subscriptionTopic: String, actualTopic: String): List<String>? {
        val subLevels = subscriptionTopic.split('/')
        val topicLevels = actualTopic.split('/')

        val wildcards = mutableListOf<String>()

        var i = 0
        while (i < subLevels.size) {
            val sub = subLevels[i]
            if (sub == "#") {
                // Multi-level wildcard matches the remaining topic levels
                wildcards.addAll(topicLevels.subList(i, topicLevels.size))
                return wildcards
            }

            if (i >= topicLevels.size) return null

            val actual = topicLevels[i]
            when {
                sub == "+" -> wildcards.add(actual) // Single-level wildcard
                sub != actual -> return null // Mismatch
            }
            i++
        }

        return if (i == topicLevels.size) wildcards else null
    }
}
