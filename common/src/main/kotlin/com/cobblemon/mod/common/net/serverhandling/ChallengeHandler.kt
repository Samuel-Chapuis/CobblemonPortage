/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleTypes
import com.cobblemon.mod.common.battles.ChallengeManager
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.battle.BattleChallengeNotificationPacket
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket
import com.cobblemon.mod.common.util.canInteractWith
import com.cobblemon.mod.common.util.party
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Processes a player's interaction request to battle with another player or Pokemon.
 *
 * If valid player interaction, creates a respective [BattleChallenge] and sends a [BattleChallengeNotificationPacket]
 * to the player to decide upon.
 *
 * If valid Pokemon interaction, initiates a PVE battle.
 *
 * @author Hiroku
 * @since April 23rd, 2022
 */
object ChallengeHandler : ServerNetworkPacketHandler<BattleChallengePacket> {
    override fun handle(packet: BattleChallengePacket, server: MinecraftServer, player: ServerPlayerEntity) {
        val targetedEntity = player.world.getEntity(packet.targetedEntityId)?.let {
            when (it) {
                is PokemonEntity -> it.owner ?: it
                is ServerPlayerEntity -> it
                else -> null
            }
        } ?: return

        val leadingPokemon = player.party()[packet.selectedPokemonId]?.uuid ?: return   // validate id
        if (targetedEntity is PokemonEntity && player.canInteractWith(targetedEntity, Cobblemon.config.battleWildMaxDistance) && targetedEntity.canBattle(player)) {
            BattleBuilder.pve(player, targetedEntity, leadingPokemon).ifErrored { it.sendTo(player) { it.red() } }
        }
        else if (targetedEntity is ServerPlayerEntity) {
            ChallengeManager.setLead(player, leadingPokemon)
            val challenge =
                if (packet.battleFormat.battleType.name == BattleTypes.MULTI.name)
                    ChallengeManager.MultiBattleChallenge(player, targetedEntity, leadingPokemon, packet.battleFormat)
                else
                    ChallengeManager.SinglesBattleChallenge(player, targetedEntity, leadingPokemon, packet.battleFormat)

            // player interaction validation is done on sendRequest
            ChallengeManager.sendRequest(challenge)
        }
    }
}