/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.data

import com.cobblemon.mod.common.CobblemonFlows
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.writeIdentifier
import com.cobblemon.mod.common.util.writeString
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.util.Identifier

/**
 * A packet that synchronizes the flow registry with the client.
 *
 * @author Hiroku
 * @since February 24th, 2024
 */
class FlowRegistrySyncPacket(entries: Collection<Map.Entry<ResourceLocation, List<ExpressionLike>>>) : DataRegistrySyncPacket<Map.Entry<ResourceLocation, List<ExpressionLike>>, FlowRegistrySyncPacket>(entries){
    companion object {
        val ID = cobblemonResource("flow_registry_sync")
        fun decode(buffer: RegistryFriendlyByteBuf): FlowRegistrySyncPacket = FlowRegistrySyncPacket(emptyList()).apply { decodeBuffer(buffer) }
    }

    override val id = ID

    override fun encodeEntry(buffer: RegistryFriendlyByteBuf, entry: Map.Entry<ResourceLocation, List<ExpressionLike>>) {
        buffer.writeIdentifier(entry.key)
        buffer.writeCollection(entry.value) { _, expression -> buffer.writeString(expression.toString()) }
    }

    override fun decodeEntry(buffer: RegistryFriendlyByteBuf): Map.Entry<ResourceLocation, List<ExpressionLike>> {
        val key = buffer.readIdentifier()
        val value = buffer.readList { buffer.readString().asExpressionLike() }
        return object : Map.Entry<ResourceLocation, List<ExpressionLike>> {
            override val key = key
            override val value = value
        }
    }

    override fun synchronizeDecoded(entries: Collection<Map.Entry<ResourceLocation, List<ExpressionLike>>>) {
        entries.map { (identifier, flows) ->
            val existing = CobblemonFlows.clientFlows.getOrPut(identifier) { mutableListOf() }
            existing += flows
        }
    }
}