/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.permission

import com.cobblemon.mod.common.command.AbandonMultiTeam

object CobblemonPermissions {

    private const val COMMAND_PREFIX = "command."
    private val permissions = arrayListOf<Permission>()

    val CHANGE_EYE_HEIGHT = this.create("${COMMAND_PREFIX}changeeyeheight", PermissionWorld.ALL_COMMANDS)
    val CHANGE_SCALE_AND_SIZE = this.create("${COMMAND_PREFIX}changescaleandsize", PermissionWorld.ALL_COMMANDS)
    val CHANGE_WALK_SPEED = this.create("${COMMAND_PREFIX}changewalkspeed", PermissionWorld.ALL_COMMANDS)
    val CHECKSPAWNS = this.create("${COMMAND_PREFIX}checkspawns", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val GET_NBT = this.create("${COMMAND_PREFIX}getnbt", PermissionWorld.ALL_COMMANDS)

    private const val GIVE_POKEMON_BASE = "${COMMAND_PREFIX}givepokemon"
    val GIVE_POKEMON_SELF = this.create("${GIVE_POKEMON_BASE}.self", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val GIVE_POKEMON_OTHER = this.create("${GIVE_POKEMON_BASE}.other", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    private const val HEAL_POKEMON_BASE = "${COMMAND_PREFIX}healpokemon"
    val HEAL_POKEMON_SELF = this.create("$HEAL_POKEMON_BASE.self", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val HEAL_POKEMON_OTHER = this.create("$HEAL_POKEMON_BASE.other", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)


    private const val LEVEL_UP_BASE = "${COMMAND_PREFIX}levelup"
    val LEVEL_UP_SELF = this.create("$LEVEL_UP_BASE.self", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val LEVEL_UP_OTHER = this.create("$LEVEL_UP_BASE.other", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val OPEN_STARTER_SCREEN = this.create("${COMMAND_PREFIX}openstarterscreen", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val BEDROCK_PARTICLE = this.create("${COMMAND_PREFIX}bedrockparticle", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val OPEN_DIALOGUE = this.create("${COMMAND_PREFIX}opendialogue", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    private const val POKEMON_EDIT_BASE = "${COMMAND_PREFIX}pokemonedit"
    val POKEMON_EDIT_SELF = this.create("$POKEMON_EDIT_BASE.self", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val POKEMON_EDIT_OTHER = this.create("$POKEMON_EDIT_BASE.other", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val SPAWN_ALL_POKEMON = this.create("${COMMAND_PREFIX}spawnallpokemon", PermissionWorld.ALL_COMMANDS)
    val GIVE_ALL_POKEMON = this.create("${COMMAND_PREFIX}giveallpokemon", PermissionWorld.ALL_COMMANDS)

    val SPAWN_POKEMON = this.create("${COMMAND_PREFIX}spawnpokemon", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val SPAWN_NPC = this.create("${COMMAND_PREFIX}spawnnpc", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val STOP_BATTLE = this.create("${COMMAND_PREFIX}stopbattle", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val TAKE_POKEMON = this.create("${COMMAND_PREFIX}takepokemon", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val TEACH = this.create("${COMMAND_PREFIX}teach.base", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val TEACH_BYPASS_LEARNSET = this.create("${COMMAND_PREFIX}teach.bypass", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val FRIENDSHIP = this.create("${COMMAND_PREFIX}friendship", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val HELD_ITEM = this.create("${COMMAND_PREFIX}helditem", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val PC = this.create("${COMMAND_PREFIX}pc", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val POKEBOX = this.create("${COMMAND_PREFIX}pokebox", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val TEST_STORE = this.create("${COMMAND_PREFIX}teststore", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val QUERY_LEARNSET = this.create("${COMMAND_PREFIX}querylearnset", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val TEST_PC_SLOT = this.create("${COMMAND_PREFIX}testpcslot", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val TEST_PARTY_SLOT = this.create("${COMMAND_PREFIX}testpartyslot", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val CLEAR_PARTY = this.create("${COMMAND_PREFIX}clearparty", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val CLEAR_PC = this.create("${COMMAND_PREFIX}clearpc", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val POKEDEX = this.create("${COMMAND_PREFIX}pokedex", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val NPC_EDIT = this.create("${COMMAND_PREFIX}npcedit", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val NPC_DELETE = this.create("${COMMAND_PREFIX}npcdelete", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val FREEZE_POKEMON = this.create("${COMMAND_PREFIX}freezepokemon", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)
    val APPLY_PLAYER_TEXTURE = this.create("${COMMAND_PREFIX}applyplayertexture", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    val ABANDON_MULTITEAM = this.create("${COMMAND_PREFIX}abandonmultiteam", PermissionWorld.NONE)

    val RUN_MOLANG_SCRIPT = this.create("${COMMAND_PREFIX}runmolangscript", PermissionWorld.CHEAT_COMMANDS_AND_COMMAND_BLOCKS)

    fun all(): Iterable<Permission> = this.permissions

    private fun create(node: String, level: PermissionWorld): Permission {
        val permission = CobblemonPermission(node, level)
        this.permissions += permission
        return permission
    }

}