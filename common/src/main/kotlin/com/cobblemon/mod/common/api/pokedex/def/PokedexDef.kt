/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.pokedex.def

import com.cobblemon.mod.common.api.data.ClientDataSynchronizer
import com.cobblemon.mod.common.api.pokedex.entry.DexEntries
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.Identifier

/**
 * A class that defines what entries are in a Pokedex
 *
 * @since August 24, 2024
 * @author Apion
 */
abstract class PokedexDef: ClientDataSynchronizer<PokedexDef> {
    //The ID used to find the codecs for this "type" of PokedexDef
    abstract val typeId: Identifier
    //The ID of this dex in the Dexes registry
    abstract val id: Identifier
    // The sort order for the dex
    var sortOrder: Int = 0

    abstract fun getEntries(): List<PokedexEntry>

    companion object {
        val CODEC: Codec<PokedexDef> = Identifier.CODEC.dispatch("type", PokedexDef::typeId, PokedexDef::getCodecById)
        val PACKET_CODEC: StreamCodec<ByteBuf, PokedexDef> = Identifier.STREAM_CODEC.dispatch({ it.typeId }, ::getPacketCodecById)

        fun getPacketCodecById(id: Identifier): StreamCodec<ByteBuf, out PokedexDef> {
            return when (id) {
                SimplePokedexDef.ID -> SimplePokedexDef.PACKET_CODEC
                AggregatePokedexDef.ID -> AggregatePokedexDef.PACKET_CODEC
                else -> throw NotImplementedError("Missing pokedex def packet codec for id: $id")
            }
        }

        fun getCodecById(id: Identifier): MapCodec<out PokedexDef> {
            return when (id) {
                SimplePokedexDef.ID -> SimplePokedexDef.CODEC
                AggregatePokedexDef.ID -> AggregatePokedexDef.CODEC
                else -> throw NotImplementedError("Missing pokedex def codec for id: $id")
            }
        }
    }
}