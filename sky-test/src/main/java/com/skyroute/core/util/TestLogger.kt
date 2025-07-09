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
