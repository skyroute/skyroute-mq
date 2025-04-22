package com.skyroute.api

enum class ThreadMode {

    /** Same thread (default) */
    POSTING,

    /** UI thread */
    MAIN,

    /** Background if currently on main, else use same thread */
    BACKGROUND,

    /** Always a new thread */
    ASYNC
}