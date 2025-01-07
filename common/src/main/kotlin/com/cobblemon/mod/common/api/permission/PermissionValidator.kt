/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.permission

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Responsible for evaluating permissions for a given player or command source.
 * To register your implementation replace the instance in [Cobblemon.permissionValidator].
 *
 * @author Licious
 * @since September 23rd, 2022
 */
interface PermissionValidator {

    /**
     * Invoked when the validator replaces the existing one in [Cobblemon.permissionValidator].
     */
    fun initialize()

    /**
     * Validates a permission for [ServerPlayer].
     *
     * @param player The target [ServerPlayer].
     * @param permission The [Permission] being queried.
     * @return If the [player] has the [permission].
     */
    fun hasPermission(player: ServerPlayer, permission: Permission): Boolean
    /**
     * Validates a permission for [ServerPlayer] based only on a permission string and a permission level.
     *
     * @param player The target [ServerPlayer].
     * @param permission The permission string being queried such as cobblemon.command.giveallpokemon.
     * @param level The permission level being queried. 4 is generally used for cheats.
     */
    fun hasPermission(player: ServerPlayer, permission: String, level: Int): Boolean = hasPermission(player, CobblemonPermission(permission, PermissionLevel.byNumericValue(level)))

    /**
     * Validates a permission for [CommandSource].
     *
     * @param source The target [CommandSource].
     * @param permission The [Permission] being queried.
     * @return If the [source] has the [permission].
     */
    fun hasPermission(source: CommandSourceStack, permission: Permission): Boolean
    /**
     * Validates a permission for [CommandSourceStack] based only on a permission string and a permission level.
     *
     * @param source The target [CommandSourceStack].
     * @param permission The permission string being queried such as cobblemon.command.giveallpokemon.
     * @param level The permission level being queried. 4 is generally used for cheats.
     */
    fun hasPermission(source: CommandSourceStack, permission: String, level: Int): Boolean = hasPermission(source, CobblemonPermission(permission, PermissionLevel.byNumericValue(level)))
}