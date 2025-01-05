/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning

import com.cobblemon.mod.common.api.spawning.context.SpawningContext
import com.cobblemon.mod.common.api.spawning.prospecting.SpawningProspector
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerWorld
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.max

/**
 * A slice of the world that can be accessed safely from an async thread. This includes all of the information
 * that might be unsafe to access async from the world itself for the purposes of spawning.
 *
 * This is generated by a [SpawningProspector].
 *
 * @author Hiroku
 * @since January 31st, 2022
 */
class WorldSlice(
    val cause: SpawnCause,
    val world: ServerWorld,
    val baseX: Int,
    val baseY: Int,
    val baseZ: Int,
    val blocks: Array<Array<Array<BlockData>>>,
    val skyWorld: Array<Array<Int>>,
    var nearbyEntityPositions: List<Vec3>
) {
    class BlockData(
        val state: BlockState,
        val light: Int,
        val skyLight: Int
    )

    val length = blocks.size
    val height = blocks[0].size
    val width = blocks[0][0].size

    private val structureChunkCaches = mutableMapOf<ChunkPos, SpawningContext.StructureChunkCache>()

    fun getStructureCache(pos: BlockPos): SpawningContext.StructureChunkCache {
        return structureChunkCaches.getOrPut(ChunkPos(pos), SpawningContext::StructureChunkCache)
    }

    companion object {
        val stoneState = Blocks.STONE.defaultBlockState()
    }

    fun isInBounds(x: Int, y: Int, z: Int) = x >= baseX && x < baseX + length && y >= baseY && y < baseY + height && z >= baseZ && z < baseZ + width
    fun getBlockData(x: Int, y: Int, z: Int): BlockData = blocks[x - baseX][y - baseY][z - baseZ]
    fun getBlockData(position: BlockPos) = getBlockData(position.x, position.y, position.z)

    fun getBlockState(x: Int, y: Int, z: Int, elseBlock: BlockState = stoneState): BlockState {
        return if (!isInBounds(x, y, z)) {
            elseBlock
        } else {
            blocks[x - baseX][y - baseY][z - baseZ].state
        }
    }
    fun getBlockState(position: BlockPos, elseBlock: BlockState = stoneState) = getBlockState(position.x, position.y, position.z, elseBlock)

    fun getLight(x: Int, y: Int, z: Int, elseLight: Int = 0): Int {
        return if (!isInBounds(x, y, z)) {
            elseLight
        } else {
            getBlockData(x, y, z).light
        }
    }
    fun getLight(position: BlockPos, elseLight: Int = 0) = getLight(position.x, position.y, position.z, elseLight)

    fun getSkyLight(x: Int, y: Int, z: Int, elseLight: Int = 0): Int {
        return if (!isInBounds(x, y, z)) {
            elseLight
        } else {
            getBlockData(x, y, z).skyLight
        }
    }
    fun getSkyLight(position: BlockPos, elseLight: Int = 0) = getSkyLight(position.x, position.y, position.z, elseLight)

    fun skySpaceAbove(x: Int, y: Int, z: Int): Int {
        return if (!isInBounds(x, y, z) || skyWorld[x - baseX][z - baseZ] > y) {
            0
        } else {
            max(0, world.maxBuildHeight - y)
        }
    }
    fun skySpaceAbove(position: BlockPos) = skySpaceAbove(position.x, position.y, position.z)

    fun canSeeSky(x: Int, y: Int, z: Int, elseCanSeeSky: Boolean = false): Boolean {
        return if (!isInBounds(x, y, z)) {
            elseCanSeeSky
        } else {
            y >= skyWorld[x - baseX][z - baseZ]
        }
    }
    fun canSeeSky(position: BlockPos, elseCanSeeSky: Boolean = false) = canSeeSky(position.x, position.y, position.z, elseCanSeeSky)

    fun nearbyBlocks(position: BlockPos, maxHorizontalRadius: Int, maxVerticalRadius: Int) = nearbyBlocks(position.x, position.y, position.z, maxHorizontalRadius, maxVerticalRadius)
    fun nearbyBlocks(centerX: Int, centerY: Int, centerZ: Int, maxHorizontalRadius: Int, maxVerticalRadius: Int): List<BlockState> {
        val blocks = mutableListOf<BlockState>()

        val minX = (centerX - maxHorizontalRadius).coerceAtLeast(baseX)
        val minY = (centerY - maxVerticalRadius).coerceAtLeast(baseY)
        val minZ = (centerZ - maxHorizontalRadius).coerceAtLeast(baseZ)
        val maxX = (centerX + maxHorizontalRadius).coerceAtMost(baseX + length)
        val maxY = (centerY + maxVerticalRadius).coerceAtMost(baseY + height)
        val maxZ = (centerZ + maxHorizontalRadius).coerceAtMost(baseZ + width)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    blocks.add(getBlockState(x, y, z))
                }
            }
        }

        return blocks
    }

    fun horizontalSpace(position: BlockPos, condition: (BlockState) -> Boolean, maximum: Int)
        = horizontalSpace(position.x, position.y, position.z, condition, maximum)
    fun horizontalSpace(centerX: Int, centerY: Int, centerZ: Int, condition: (BlockState) -> Boolean, maximum: Int): Int {
        var space = 1
        var radius = 1
        while (radius <= maximum) {
            val minX = centerX - radius
            val maxX = centerX + radius
            val minZ = centerZ - radius
            val maxZ = centerZ + radius

            if (!isInBounds(minX, centerY, minZ) || !isInBounds(maxX, centerY, maxZ)) {
                return space
            }

            // Check left side of square
            var x = minX
            var z = minZ
            while (z <= maxZ) {
                if (!condition(getBlockState(x, centerY, z))) {
                    return space
                }
                z++
            }

            // Check right side of square
            x = maxX
            z = minZ
            while (z <= maxZ) {
                if (!condition(getBlockState(x, centerY, z))) {
                    return space
                }
                z++
            }

            // Check bottom side of square minus the corners (minX and maxX)
            z = minZ
            x = minX + 1
            while (x < maxX) {
                if (!condition(getBlockState(x, centerY, z))) {
                    return space
                }
                x++
            }

            // Check top side of square minus the corners (minX and maxX)
            z = maxZ
            x = minX + 1
            while (x < maxX) {
                if (!condition(getBlockState(x, centerY, z))) {
                    return space
                }
                x++
            }

            radius++
            space += 2
        }

        return space
    }

    fun heightSpace(centerX: Int, centerY: Int, centerZ: Int, condition: (BlockState) -> Boolean, maximum: Int): Int {
        var space = 1
        while (space <= maximum) {
            val y = centerY + space
            if (y >= baseY + height) {
                return space
            }

            if (!condition(getBlockState(centerX, y, centerZ))) {
                return space
            }

            space++
        }

        return space
    }

    fun depthSpace(centerX: Int, centerY: Int, centerZ: Int, condition: (BlockState) -> Boolean, maximum: Int): Int {
        var space = 1
        while (space <= maximum) {
            val y = centerY - space
            if (y < baseY) {
                return space
            }

            if (!condition(getBlockState(centerX, y, centerZ))) {
                return space
            }

            space++
        }

        return space
    }
}