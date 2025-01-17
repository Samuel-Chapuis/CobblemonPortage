/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events.pokemon

import com.cobblemon.mod.common.api.events.Cancelable
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import net.minecraft.server.network.ServerPlayerEntity

interface HatchEggEvent {
    val egg: PokemonProperties
    val player: ServerPlayerEntity

    data class Pre(override var egg : PokemonProperties, override var player: ServerPlayerEntity) : HatchEggEvent, Cancelable()

    data class Post(override var egg : PokemonProperties, override var player: ServerPlayerEntity) : HatchEggEvent
}
