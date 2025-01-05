/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.multiblock

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.World
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

interface MultiblockStructure {
    val controllerBlockPos: BlockPos

    fun useWithoutItem(
        blockState: BlockState,
        world: World,
        blockPos: BlockPos,
        player: Player,
        blockHitResult: BlockHitResult
    ): InteractionResult

    fun playerWillDestroy(world: World, pos: BlockPos, state: BlockState, player: Player?)

    fun tick(world: World)

    fun syncToClient(world: World)

    fun markDirty(world: World)
    fun writeToNbt(registryLookup: HolderLookup.Provider): CompoundTag
    fun getAnalogOutputSignal(state: BlockState, world: World?, pos: BlockPos?): Int {
        return 0
    }

    fun setRemoved(world: World)
    fun onTriggerEvent(state: BlockState?, world: ServerLevel?, pos: BlockPos?, random: RandomSource?)
}