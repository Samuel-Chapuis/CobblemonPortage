/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling.npc

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.net.messages.server.npc.SaveNPCPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

object SaveNPCHandler : ServerNetworkPacketHandler<SaveNPCPacket> {
    override fun handle(packet: SaveNPCPacket, server: MinecraftServer, player: ServerPlayerEntity) {
        val npcEntity = player.world.getEntity(packet.npcId) as? NPCEntity ?: return
        if (npcEntity.editingPlayer != player.uuid) {
            return
        }

        packet.npcConfigurationDTO.apply(npcEntity)
    }
}