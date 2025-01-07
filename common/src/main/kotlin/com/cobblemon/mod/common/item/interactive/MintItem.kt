/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.interactive

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.item.PokemonSelectingItem
import com.cobblemon.mod.common.item.CobblemonItem
import com.cobblemon.mod.common.pokemon.Nature
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.lang
import net.minecraft.world.entity.player.Player
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.World

class MintItem(val nature: Nature) : CobblemonItem(Properties()), PokemonSelectingItem {

    override val bagItem = null
    override fun canUseOnPokemon(pokemon: Pokemon) = pokemon.effectiveNature != nature
    override fun applyToPokemon(
        player: ServerPlayerEntity,
        stack: ItemStack,
        pokemon: Pokemon
    ): InteractionResultHolder<ItemStack> {
        return if (pokemon.effectiveNature != nature) {
            if (!player.isCreative) {
                stack.shrink(1)
            }
            pokemon.entity?.playSound(CobblemonSounds.MEDICINE_HERB_USE, 1F, 1F)
            pokemon.mintedNature = nature
            player.sendSystemMessage(lang("mint.interact", pokemon.getDisplayName(), stack.hoverName), true)
            InteractionResultHolder.success(stack)
        } else {
            player.sendSystemMessage(lang("mint.same_nature", pokemon.getDisplayName(), stack.hoverName), true)
            InteractionResultHolder.fail(stack)
        }
    }

    override fun use(world: World, user: Player, hand: Hand): InteractionResultHolder<ItemStack> {
        if (user is ServerPlayerEntity) {
            return use(user, user.getStackInHand(hand))
        }
        return InteractionResultHolder.success(user.getStackInHand(hand))
    }
}