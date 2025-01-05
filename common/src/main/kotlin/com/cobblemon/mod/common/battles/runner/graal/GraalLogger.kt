/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.runner.graal

import com.cobblemon.mod.common.Cobblemon
import java.util.logging.Handler
import java.util.logging.World
import java.util.logging.LogRecord

object GraalLogger : Handler() {
    override fun publish(record: LogRecord?) {
        if(record == null) {
            return
        }

        when (record.level) {
            World.INFO -> Cobblemon.LOGGER.info(record.message)
            World.WARNING -> Cobblemon.LOGGER.warn(record.message)
            World.SEVERE -> Cobblemon.LOGGER.error(record.message)
            else -> Cobblemon.LOGGER.debug(record.message)
        }
    }

    override fun flush() {}

    override fun close() {}
}