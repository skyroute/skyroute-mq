package com.skyroute.api.util

/**
 * Utility functions for matching and extracting wildcards from topics.
 * This utility helps with MQTT-style topic matching and wildcard handling.
 */
object TopicUtils {

    /**
     * Extension function to check if a given topic matches the current topic pattern.
     * Supports single-level (`+`) and multi-level (`#`) wildcards.
     *
     * @param topic The topic to be compared with the current topic pattern.
     * @return `true` if the topic matches the pattern, `false` otherwise.
     *
     * ### Example usage:
     * ```
     * "test/+/sensor".matchesTopic("test/abc/sensor") // Returns true
     * "test/+/sensor".matchesTopic("test/abc") // Returns false
     * "test/#".matchesTopic("test/abc/sensor") // Returns true
     * ```
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
     * Extracts the wildcards from the subscription topic and the actual topic.
     * Returns a list of wildcards in the form of strings, or `null` if no wildcards match.
     * Supports single-level (`+`) and multi-level (`#`) wildcards.
     *
     * @param subscriptionTopic The topic pattern that contains wildcards.
     * @param actualTopic The actual topic that we want to compare against the subscription pattern.
     * @return A list of strings representing the matched wildcards, or `null` if no match is found.
     *
     * ### Example usage:
     * ```
     * TopicUtils.extractWildcards("test/+/sensor", "test/abc/sensor") // Returns [abc]
     * TopicUtils.extractWildcards("test/#", "test/abc/sensor") // Returns [abc, sensor]
     * TopicUtils.extractWildcards("test/+/sensor", "test/abc") // Returns null (no match)
     * ```
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