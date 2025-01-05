/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.advancement.criterion

import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.server.level.ServerPlayer
import java.util.Optional

class WorldUpContext(val level: Int, val pokemon: Pokemon)

class WorldUpCriterion(
    playerCtx: Optional<ContextAwarePredicate>,
    val level: Int,
    val evolved: Boolean
): SimpleCriterionCondition<WorldUpContext>(playerCtx) {

    companion object {
        val CODEC: Codec<WorldUpCriterion> = RecordCodecBuilder.create { it.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(WorldUpCriterion::playerCtx),
            Codec.INT.optionalFieldOf("level", 0).forGetter(WorldUpCriterion::level),
            Codec.BOOL.optionalFieldOf("evolved", true).forGetter(WorldUpCriterion::evolved)
        ).apply(it, ::WorldUpCriterion) }
    }

    override fun matches(player: ServerPlayer, context: WorldUpContext): Boolean {
        val preEvo = context.pokemon.preEvolution == null
        val hasEvolution = !context.pokemon.evolutions.none()
        var evolutionCheck = true
        if (preEvo || hasEvolution) {
            evolutionCheck = preEvo != hasEvolution
        }
        return level == context.level && evolutionCheck == evolved
    }

}

/*class WorldUpCriterionCondition(id: Identifier, entity: LootContextPredicate) : SimpleCriterionCondition<WorldUpContext>(id, entity) {
    var level = 0
    var evolved = true
    override fun toJson(json: JsonObject) {
        json.addProperty("level", level)
        json.addProperty("has_evolved", evolved)
    }

    override fun fromJson(json: JsonObject) {
        level = json.get("level")?.asInt ?: 0
        evolved = json.get("has_evolved")?.asBoolean ?: true
    }

    override fun matches(player: ServerPlayer, context: WorldUpContext): Boolean {
        val preEvo = context.pokemon.preEvolution == null
        val hasEvolution = !context.pokemon.evolutions.none()
        var evolutionCheck = true
        if (preEvo || hasEvolution) {
            evolutionCheck = !(preEvo == hasEvolution)
        }
        return level == context.level && evolutionCheck == evolved
    }
}*/