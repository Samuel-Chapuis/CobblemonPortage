/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.mojang.datafixers.util.Either
import net.minecraft.core.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.util.Identifier
import net.minecraft.tags.TagKey
import java.lang.reflect.Type

/**
 * An adapter that can deserialize a string field that is either an [Identifier] or a [TagKey]
 * for the registry specified by the [ResourceKey] parameter.
 *
 * @author Hiroku
 * @since May 7th, 2023
 */
class EitherIdentifierOrTagAdapter<E, T : Registry<E>>(val ResourceKey: ResourceKey<T>) : JsonDeserializer<Either<Identifier, TagKey<E>>> {
    override fun deserialize(
        element: JsonElement,
        type: Type,
        ctx: JsonDeserializationContext
    ): Either<Identifier, TagKey<E>> {
        val string = element.asString
        return if (string.startsWith("#")) {
            Either.right(TagKey.create(ResourceKey, Identifier.parse(string.substring(1))))
        } else {
            Either.left(Identifier.parse(string))
        }
    }
}