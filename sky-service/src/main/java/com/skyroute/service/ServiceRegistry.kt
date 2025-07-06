package com.skyroute.service

import com.skyroute.core.util.Logger

/**
 * A singleton object that holds required configuration for the SkyRouteService.
 *
 * @author Andre Suryana
 */
object ServiceRegistry {

    private var _logger: Logger? = null

    val logger: Logger
        get() = _logger ?: Logger.Default()

    fun initLogger(logger: Logger) {
        check(_logger == null) { "Logger has already been initialized" }
        _logger = logger
    }
}
