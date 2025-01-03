/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.structureprocessors

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.registries.Registries
import net.minecraft.registry.RegistryKey

object CobblemonStructureProcessorLists {
    @JvmField
    val CROP_TO_BERRY = RegistryKey.create(Registries.PROCESSOR_LIST, cobblemonResource("crop_to_berry"))
}