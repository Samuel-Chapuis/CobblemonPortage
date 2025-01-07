/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument

fun CommandContext<ServerCommandSource>.player(argumentName: String = "player") = EntityArgument.getPlayer(this, argumentName)
fun CommandContext<ServerCommandSource>.string(argumentName: String) = this.getArgument(argumentName, String::class.java)
fun CommandContext<ServerCommandSource>.uuid(argumentName: String) = this.getArgument(argumentName, String::class.java).asUUID
fun CommandContext<ServerCommandSource>.resourceLocation(argumentName: String) = ResourceLocationArgument.getId(this, argumentName)
