/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.conditional

import com.google.gson.JsonElement
import net.minecraft.core.Registry
import net.minecraft.util.Identifier

/**
 * A condition for some registry type which asserts that the entry must have the given [Identifier].
 *
 * @author Hiroku
 * @since July 16th, 2022
 */
open class RegistryLikeIdentifierCondition<T : Any>(val identifier: Identifier) : RegistryLikeCondition<T> {
    companion object {
        fun <T: Any> resolver(
            constructor: (Identifier) -> RegistryLikeIdentifierCondition<T>
        ): (JsonElement) -> RegistryLikeIdentifierCondition<T>? = { constructor(Identifier.parse(it.asString)) }
    }
    override fun fits(t: T, registry: Registry<T>) = registry.getKey(t) == identifier
}