/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.advancement.criterion

import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Identifier

class ReelInPokemonContext(val pokemonId : ResourceLocation, val baitId: ResourceLocation)

class ReelInPokemonCriterionCondition(
        playerCtx: Optional<ContextAwarePredicate>,
        val pokemonId: String,
        val baitId: String
) : SimpleCriterionCondition<ReelInPokemonContext>(playerCtx) {

    companion object {
        val CODEC: Codec<ReelInPokemonCriterionCondition> = RecordCodecBuilder.create { it.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter { it.playerCtx },
            Codec.STRING.optionalFieldOf("pokemonId", "any").forGetter { it.pokemonId },
            Codec.STRING.optionalFieldOf("baitId", "empty_bait").forGetter { it.baitId }
        ).apply(it, { playerCtx, pokemonId, baitId -> ReelInPokemonCriterionCondition(playerCtx, pokemonId, baitId.ifEmpty { "empty_bait" }) }) }
    }

    override fun matches(player: ServerPlayer, context: ReelInPokemonContext): Boolean {
        return (context.pokemonId == this.pokemonId.asIdentifierDefaultingNamespace() || this.pokemonId == "any") && (context.baitId == this.baitId.asIdentifierDefaultingNamespace() || this.baitId == "empty_bait")
    }
}
