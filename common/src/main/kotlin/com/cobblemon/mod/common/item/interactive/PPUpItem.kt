/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.interactive

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.item.PokemonAndMoveSelectingItem
import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.item.CobblemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.entity.player.Player
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.World

class PPUpItem(val amount: Int) : CobblemonItem(Properties()), PokemonAndMoveSelectingItem {
    override val bagItem = null
    override fun canUseOnPokemon(pokemon: Pokemon) = pokemon.moveSet.any(::canUseOnMove)
    override fun canUseOnMove(move: Move) = move.raisedPpStages < 3
    override fun applyToPokemon(
        player: ServerPlayerEntity,
        stack: ItemStack,
        pokemon: Pokemon,
        move: Move
    ) {
        if (move.raiseMaxPP(amount)) {
            if (!player.isCreative) {
                stack.shrink(1)
            }
            pokemon.entity?.playSound(CobblemonSounds.MEDICINE_PILLS_USE, 1F, 1F)
        }
    }

    override fun use(world: World, user: Player, hand: Hand): InteractionResultHolder<ItemStack> {
        if (user is ServerPlayerEntity) {
            use(user, user.getStackInHand(hand))?.let { return it }
        }
        return InteractionResultHolder.success(user.getStackInHand(hand))
    }
}