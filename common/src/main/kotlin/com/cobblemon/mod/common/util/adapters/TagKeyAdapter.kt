/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.google.gson.*
import net.minecraft.core.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.util.Identifier
import net.minecraft.tags.TagKey
import java.lang.reflect.Type

/**
 * An adapter for [TagKey]s.
 * [TagKey]s are just [Identifier]s attached to a certain registry.
 *
 * @param T The type of the [Registry] this [TagKey] belongs to.
 * @property key The [RegistryKey] used to create new [TagKey]s.
 *
 * @author Licious
 * @since July 2nd, 2022
 */
class TagKeyAdapter<T>(private val key: RegistryKey<Registry<T>>) : JsonDeserializer<TagKey<T>>, JsonSerializer<TagKey<T>> {

    override fun deserialize(element: JsonElement, type: Type, ctx: JsonDeserializationContext): TagKey<T> {
        val identifier = Identifier.parse(element.asString)
        return TagKey.create(this.key, identifier)
    }

    override fun serialize(tagKey: TagKey<T>, type: Type, ctx: JsonSerializationContext): JsonElement {
        return JsonPrimitive(tagKey.location.toString())
    }

}