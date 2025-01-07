/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.npc

import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.entity.PokemonSender
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext
import com.cobblemon.mod.common.api.net.serializers.IdentifierDataSerializer
import com.cobblemon.mod.common.api.net.serializers.NPCPlayerTextureSerializer
import com.cobblemon.mod.common.api.net.serializers.PoseTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.StringSetDataSerializer
import com.cobblemon.mod.common.api.net.serializers.UUIDSetDataSerializer
import com.cobblemon.mod.common.api.npc.NPCClasses
import com.cobblemon.mod.common.api.npc.configuration.NPCBattleConfiguration
import com.cobblemon.mod.common.api.npc.configuration.NPCBehaviourConfiguration
import com.cobblemon.mod.common.api.npc.configuration.NPCInteractConfiguration
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.entity.PosableEntity
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.npc.CloseNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.npc.OpenNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnNPCPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.*
import com.google.common.collect.ImmutableList
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import com.mojang.serialization.Dynamic
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Identifier
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.Brain
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.ai.sensing.SensorType
import net.minecraft.world.entity.npc.Npc
import net.minecraft.world.entity.player.Player
import net.minecraft.world.World

class NPCEntity(world: World) : AgeableMob(CobblemonEntities.NPC, world), Npc, PosableEntity, PokemonSender, Schedulable {
    override val schedulingTracker = SchedulingTracker()

    override val struct = this.asMoLangValue()

    val runtime = MoLangRuntime().setup().withNPCValue(value = this)

    var editingPlayer: UUID? = null

    var npc = NPCClasses.dummy()
        set(value) {
            entityData.set(NPC_CLASS, value.id)
            val valueChanged = field != value
            field = value
            if (valueChanged) {
                customName = value.names.randomOrNull() ?: "NPC".text()
                if (!level().isClientSide) {
                    brain = makeBrain(brainDynamic ?: makeEmptyBrainDynamic())
                }
            }

            this.refreshDimensions()
        }

    val level: Int
        get() = entityData.get(LEVEL)

    var skill: Int? = null // range from 0 - 5

    var hitbox: EntityDimensions? = null

    var party: NPCPartyStore? = null

    var isMovable: Boolean? = null

    var isInvulnerable: Boolean? = null

    var isLeashable: Boolean? = null

    var allowProjectileHits: Boolean? = null

    fun getPartyForChallenge(players: List<ServerPlayerEntity>): NPCPartyStore? {
        val party = this.party
        return if (party != null) {
            party
        } else if (npc.party?.isStatic == false) {
            npc.party?.provide(this, level, players)
        } else {
            null
        }
    }

    /** Oi, dev, you no touch this one. This one is for [com.cobblemon.mod.common.api.npc.variation.NPCVariationProvider]s. */
    val variationAspects = mutableSetOf<String>()
    /** You can add to this one if you want, that's ok. */
    val appliedAspects = mutableSetOf<String>()

    override val delegate = if (world.isClientSide) {
        com.cobblemon.mod.common.client.entity.NPCClientDelegate()
    } else {
        NPCServerDelegate()
    }

    var battle: NPCBattleConfiguration? = null
    var behaviour: NPCBehaviourConfiguration? = null

    var interaction: NPCInteractConfiguration? = null

    var data = VariableStruct()
    var config = VariableStruct()

    val aspects: Set<String>
        get() = entityData.get(ASPECTS)

    val battleIds: Set<UUID>
        get() = entityData.get(BATTLE_IDS)

    var actionEffect: ActionEffectContext? = null

    /** Essentially a cached form of what was serialized to make memory reloads still work despite dynamic brain activities on class change. */
    private var brainDynamic: Dynamic<*>? = null


    /* TODO NPC Valuables to add:
     *
     * -- An 'interaction' configuration. This can be loaded from a JSON or API or even a .js (ambitious). Handles what happens
     * -- when you right click. Can be a dialogue tree with some complexity, or provides options to open a shopkeeper GUI,
     * -- that sort of deal. As extensible as we can manage it (and we can manage a lot).
     *
     * A 'party provider' configuration. This is for an NPC that's going to be used as a trainer. A stack of configuration
     * planning has been done by Vera and Design, get it from them and tweak to be clean.
     *
     * A pathing configuration. Another one that could be loaded from JSON or .js or API. Controls AI.
     *
     * npcs should be able to sleep lol
     */

    init {
        delegate.initialize(this)
        addPosableFunctions(struct)
        runtime.environment.query.addFunctions(struct.functions)
        refreshDimensions()
        navigation.setCanFloat(true)
        if (!world.isClientSide) {
            brain = makeBrain(brainDynamic ?: makeEmptyBrainDynamic())
        }
    }

    // This has to be below constructor and entity tracker fields otherwise initialization order is weird and breaks them syncing
    companion object {
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.ATTACK_KNOCKBACK)

        val NPC_CLASS = SynchedEntityData.defineId(NPCEntity::class.java, IdentifierDataSerializer)
        val ASPECTS = SynchedEntityData.defineId(NPCEntity::class.java, StringSetDataSerializer)
        val POSE_TYPE = SynchedEntityData.defineId(NPCEntity::class.java, PoseTypeDataSerializer)
        val BATTLE_IDS = SynchedEntityData.defineId(NPCEntity::class.java, UUIDSetDataSerializer)
        val NPC_PLAYER_TEXTURE = SynchedEntityData.defineId(NPCEntity::class.java, NPCPlayerTextureSerializer)
        val LEVEL = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.INT)


//        val BATTLING = Activity.register("npc_battling")

        val SENSORS: Collection<SensorType<out Sensor<in NPCEntity>>> = listOf(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            SensorType.NEAREST_PLAYERS,
            CobblemonSensors.BATTLING_POKEMON,
            CobblemonSensors.NPC_BATTLING,
            SensorType.VILLAGER_HOSTILES
        )

        val MEMORY_MODULES: List<MemoryModuleType<*>> = ImmutableList.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.IS_PANICKING,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            CobblemonMemories.NPC_BATTLING,
            CobblemonMemories.BATTLING_POKEMON,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.ANGRY_AT,
            MemoryModuleType.ATTACK_COOLING_DOWN,
            CobblemonMemories.DIALOGUES,
            CobblemonMemories.ACTIVE_ACTION_EFFECT,
            MemoryModuleType.NEAREST_HOSTILE
        )

        const val SEND_OUT_ANIMATION = "send_out"
        const val RECALL_ANIMATION = "recall"
        const val LOSE_ANIMATION = "lose"
        const val WIN_ANIMATION = "win"
    }

    override fun brainProvider() = Brain.provider<NPCEntity>(MEMORY_MODULES, SENSORS)
    override fun getBreedOffspring(world: ServerLevel, entity: AgeableMob) = null // No lovemaking! Unless...
    override fun getCurrentPoseType() = this.entityData.get(POSE_TYPE)

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(NPC_CLASS, NPCClasses.classes.first().id)
        builder.define(ASPECTS, emptySet())
        builder.define(POSE_TYPE, PoseType.STAND)
        builder.define(BATTLE_IDS, setOf())
        builder.define(NPC_PLAYER_TEXTURE, NPCPlayerTexture(ByteArray(1), NPCPlayerModelType.NONE))
        builder.define(LEVEL, 1)
    }

    override fun getAddEntityPacket(serverEntity: ServerEntity) = ClientboundCustomPayloadPacket(
        SpawnNPCPacket(
            this,
            super.getAddEntityPacket(serverEntity) as ClientboundAddEntityPacket
        )
    ) as Packet<ClientGamePacketListener>

    override fun makeBrain(dynamic: Dynamic<*>): Brain<NPCEntity> {
        this.brainDynamic = dynamic
        val brain = brainProvider().makeBrain(dynamic)
        this.brain = brain
        if (npc != null) {
            NPCBrain.configure(this, npc, brain)
        }
        return brain
    }

    override fun doHurtTarget(target: Entity): Boolean {
        val source = this.damageSources().mobAttack(this)
        val hurt = target.hurt(source, attributes.getValue(Attributes.ATTACK_DAMAGE).toFloat() * 5F)
        if (hurt) {
            playAttackSound()
        }
        return hurt
    }

    override fun getBrain() = super.getBrain() as Brain<NPCEntity>

    fun updateAspects() {
        entityData.set(ASPECTS, appliedAspects + variationAspects)
    }

    fun isInBattle() = battleIds.isNotEmpty()
    fun getBattleConfiguration() = battle ?: npc.battleConfiguration

    /** Retrieves the battle theme associated with this Pokemon's Species/Form, or the default PVW theme if not found. */
    fun getBattleTheme() = this.npc.battleTheme?.let(BuiltInRegistries.SOUND_EVENT::get) ?: CobblemonSounds.PVN_BATTLE

    override fun tick() {
        super.tick()
        delegate.tick(this)
        schedulingTracker.update(1/20F)
    }

    override fun customServerAiStep() {
        super.customServerAiStep()
        getBrain().tick(level() as ServerLevel, this)
    }

    override fun sendDebugPackets() {
        super.sendDebugPackets()
        DebugPackets.sendEntityBrain(this)
        DebugPackets.sendGoalSelector(level(), this, this.goalSelector)
        DebugPackets.sendPathFindingPacket(level(), this, this.navigation.path, this.navigation.path?.distToTarget ?: 0F)
    }

    override fun saveWithoutId(nbt: CompoundTag): CompoundTag {
        super.saveWithoutId(nbt)
        nbt.put(DataKeys.NPC_DATA, MoLangFunctions.writeMoValueToNBT(data))
        nbt.put(DataKeys.NPC_CONFIG, MoLangFunctions.writeMoValueToNBT(config))
        nbt.put(DataKeys.NPC_LEVEL, IntTag.valueOf(level))
        nbt.putString(DataKeys.NPC_CLASS, npc.id.toString())
        nbt.put(DataKeys.NPC_ASPECTS, ListTag().also { list -> appliedAspects.forEach { list.add(StringTag.valueOf(it)) } })
        nbt.put(DataKeys.NPC_VARIATION_ASPECTS, ListTag().also { list -> variationAspects.forEach { list.add(StringTag.valueOf(it)) } })
        interaction?.let {
            val interactionNBT = CompoundTag()
            interactionNBT.putString(DataKeys.NPC_INTERACT_TYPE, it.type)
            it.writeToNBT(interactionNBT)
            nbt.put(DataKeys.NPC_INTERACTION, interactionNBT)
        }
        val battle = battle
        if (battle != null) {
            val battleNBT = CompoundTag()
            battle.saveToNBT(battleNBT)
            nbt.put(DataKeys.NPC_BATTLE_CONFIGURATION, battleNBT)
        }
        if (skill != null) {
            nbt.putInt(DataKeys.NPC_SKILL, skill ?: 0)
        }
        val party = party
        if (party != null) {
            val partyNBT = CompoundTag()
            party.saveToNBT(partyNBT, registryAccess())
            nbt.put(DataKeys.NPC_PARTY, partyNBT)
        }
        val playerTexture = entityData.get(NPC_PLAYER_TEXTURE)
        if (playerTexture.model != NPCPlayerModelType.NONE) {
            nbt.put(DataKeys.NPC_PLAYER_TEXTURE, CompoundTag().also {
                it.putString(DataKeys.NPC_PLAYER_TEXTURE_MODEL, playerTexture.model.name)
                it.putByteArray(DataKeys.NPC_PLAYER_TEXTURE_TEXTURE, playerTexture.texture)
            })
        }
        val hitbox = hitbox
        if (hitbox != null) {
            nbt.put(DataKeys.NPC_HITBOX, CompoundTag().also {
                it.putFloat(DataKeys.NPC_HITBOX_WIDTH, hitbox.width)
                it.putFloat(DataKeys.NPC_HITBOX_HEIGHT, hitbox.height)
                it.putBoolean(DataKeys.NPC_HITBOX_FIXED, hitbox.fixed)
            })
        }
        val isMovable = isMovable
        if (isMovable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_MOVABLE, isMovable)
        }
        val isInvulnerable = isInvulnerable
        if (isInvulnerable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_INVULNERABLE, isInvulnerable)
        }
        val isLeashable = isLeashable
        if (isLeashable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_LEASHABLE, isLeashable)
        }
        val allowProjectileHits = allowProjectileHits
        if (allowProjectileHits != null) {
            nbt.putBoolean(DataKeys.NPC_ALLOW_PROJECTILE_HITS, allowProjectileHits)
        }
        return nbt
    }

    override fun load(nbt: CompoundTag) {
        npc = NPCClasses.getByIdentifier(Identifier.parse(nbt.getString(DataKeys.NPC_CLASS))) ?: NPCClasses.classes.first()
        entityData.set(LEVEL, nbt.getInt(DataKeys.NPC_LEVEL).takeIf { it != 0 } ?: 1)
        super.load(nbt)
        data = MoLangFunctions.readMoValueFromNBT(nbt.getCompound(DataKeys.NPC_DATA)) as VariableStruct
        config = if (nbt.contains(DataKeys.NPC_CONFIG)) MoLangFunctions.readMoValueFromNBT(nbt.getCompound(DataKeys.NPC_CONFIG)) as VariableStruct else VariableStruct()
        appliedAspects.addAll(nbt.getList(DataKeys.NPC_ASPECTS, Tag.TAG_STRING.toInt()).map { it.asString })
        variationAspects.addAll(nbt.getList(DataKeys.NPC_VARIATION_ASPECTS, Tag.TAG_STRING.toInt()).map { it.asString })
        nbt.getCompound(DataKeys.NPC_INTERACTION).takeIf { !it.isEmpty }?.let { nbt ->
            val type = nbt.getString("type")
            val configType = NPCInteractConfiguration.types[type] ?: return@let
            interaction = configType.clazz.getConstructor().newInstance().also { it.readFromNBT(nbt) }
        }
        val battleNBT = nbt.getCompound(DataKeys.NPC_BATTLE_CONFIGURATION)
        if (!battleNBT.isEmpty) {
            battle = NPCBattleConfiguration().also { it.loadFromNBT(battleNBT) }
        }
        this.skill = if (nbt.contains(DataKeys.NPC_SKILL)) nbt.getInt(DataKeys.NPC_SKILL) else null
        val partyNBT = nbt.getCompound(DataKeys.NPC_PARTY)
        if (!partyNBT.isEmpty) {
            party = NPCPartyStore(this).also { it.loadFromNBT(partyNBT, registryAccess()) }
        }
        if (nbt.contains(DataKeys.NPC_PLAYER_TEXTURE)) {
            val textureNBT = nbt.getCompound(DataKeys.NPC_PLAYER_TEXTURE)
            val model = NPCPlayerModelType.valueOf(textureNBT.getString(DataKeys.NPC_PLAYER_TEXTURE_MODEL))
            val texture = textureNBT.getByteArray(DataKeys.NPC_PLAYER_TEXTURE_TEXTURE)
            entityData.set(NPC_PLAYER_TEXTURE, NPCPlayerTexture(texture, model))
        }
        this.isMovable = if (nbt.contains(DataKeys.NPC_IS_MOVABLE)) nbt.getBoolean(DataKeys.NPC_IS_MOVABLE) else null
        this.isInvulnerable = if (nbt.contains(DataKeys.NPC_IS_INVULNERABLE)) nbt.getBoolean(DataKeys.NPC_IS_INVULNERABLE) else null
        this.isLeashable = if (nbt.contains(DataKeys.NPC_IS_LEASHABLE)) nbt.getBoolean(DataKeys.NPC_IS_LEASHABLE) else null
        this.allowProjectileHits = if (nbt.contains(DataKeys.NPC_ALLOW_PROJECTILE_HITS)) nbt.getBoolean(DataKeys.NPC_ALLOW_PROJECTILE_HITS) else null
        this.hitbox = if (nbt.contains(DataKeys.NPC_HITBOX)) {
            val hitboxNBT = nbt.getCompound(DataKeys.NPC_HITBOX)

            val width = hitboxNBT.getFloat(DataKeys.NPC_HITBOX_WIDTH)
            val height = hitboxNBT.getFloat(DataKeys.NPC_HITBOX_HEIGHT)
            val fixed = hitboxNBT.getBoolean(DataKeys.NPC_HITBOX_FIXED)

            if (fixed) EntityDimensions.fixed(width, height) else EntityDimensions.scalable(width, height)
        } else {
            null
        }
        updateAspects()
    }

    fun loadTextureFromGameProfileName(username: String) {
        val server = server ?: return
        server.profileRepository.findProfilesByNames(arrayOf(username), object : ProfileLookupCallback {
            override fun onProfileLookupSucceeded(profile: GameProfile) {
                val deepProfile = server.sessionService.fetchProfile(profile.id, false)?.profile ?: return Cobblemon.LOGGER.error("Failed to fetch profile for game profile name: $username")
                val textures = server.sessionService.getTextures(deepProfile)
                val skin = textures.skin!!
                val url = skin.url
                val model = NPCPlayerModelType.valueOf((skin.getMetadata("model") ?: "default").uppercase())
                loadTexture(URI(url), model)
            }

            override fun onProfileLookupFailed(profileName: String, exception: Exception) {
                Cobblemon.LOGGER.error("Unable to load texture for game profile name: $username")
            }
        })
    }

    fun loadTexture(uri: URI, model: NPCPlayerModelType) {
        appliedAspects -= "model-default"
        appliedAspects -= "model-slim"
        appliedAspects += "model-${model.name.lowercase()}"
        entityData.set(NPC_PLAYER_TEXTURE, NPCPlayerTexture(uri.toURL().openStream().readBytes(), model))
        updateAspects()
    }

    override fun hasCustomName() = true

    override fun isCustomNameVisible() = true

    override fun isPersistenceRequired(): Boolean {
        return super.isPersistenceRequired() || !npc.canDespawn
    }
    override fun getDimensions(pose: Pose): EntityDimensions = hitbox ?: npc.hitbox

    override fun isPushable(): Boolean {
        return isMovable ?: npc.isMovable
    }

    override fun isInvulnerableTo(source: DamageSource): Boolean {
        return isInvulnerable ?: npc.isInvulnerable
    }

    override fun canBeLeashed(): Boolean {
        return isLeashable ?: npc.isLeashable
    }

    override fun canBeHitByProjectile(): Boolean {
        return allowProjectileHits ?: npc.allowProjectileHits
    }

    fun initialize(level: Int) {
        variationAspects.clear()
        entityData.set(LEVEL, level)
        npc.config.forEach { it.applyDefault(this) }
        npc.variations.values.forEach { this.variationAspects.addAll(it.provideAspects(this)) }
        if (party == null || npc.party != null) {
            party = npc.party?.takeIf { it.isStatic }?.provide(this, level)
        }
        updateAspects()
    }

    override fun mobInteract(player: Player, hand: Hand): InteractionResult {
        if (player is ServerPlayerEntity) {
            if (player.isCreative && player.getStackInHand(hand).item.toString() == CobblemonItems.NPC_EDITOR.toString()) {
                edit(player)
            } else if (hand == Hand.MAIN_HAND) {
                if (player.getBattleState()?.first?.getActor(this) != null) {
                    return InteractionResult.PASS
                }

                (interaction ?: npc.interaction)?.interact(this, player)
//                val battle = getBattleConfiguration()
//                if (battle.canChallenge) {
//                    val provider = battle.party
//                    if (provider != null) {
//                        val party = provider.provide(this, listOf(player))
//                        val result = BattleBuilder.pvn(
//                            player = player,
//                            npcEntity = this
//                        )
//                    }
//                }
            }

        }
        return InteractionResult.SUCCESS
    }

    fun playAnimation(animation: String, expressions: List<String> = emptyList()) {
        val packet = PlayPosableAnimationPacket(
            entityId = id,
            animation = setOf(animation),
            expressions = expressions
        )
        packet.sendToPlayers(level().players().filterIsInstance<ServerPlayerEntity>().filter { it.distanceTo(this) < 256 })
    }

    override fun recalling(pokemonEntity: PokemonEntity): CompletableFuture<Unit> {
        playAnimation(RECALL_ANIMATION, pokemonExpressions(pokemonEntity.pokemon))
        return delayedFuture(seconds = 1.6F)
    }

    override fun sendingOut(pokemon: Pokemon): CompletableFuture<Unit> {
        playAnimation(SEND_OUT_ANIMATION, pokemonExpressions(pokemon))
        return delayedFuture(seconds = 1.6F)
    }

    private fun pokemonExpressions(pokemon: Pokemon): List<String> {
        return listOf(
            "v.actioning_pokemon_name=\"${pokemon.species.name}\";",
            "v.actioning_pokemon_level=${pokemon.level};",
            "v.actioning_pokemon_ball=\"${pokemon.caughtBall.name}\";",
            "v.actioning_pokemon_shiny=\"${pokemon.shiny}\";",
        )
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        if (delegate != null) {
            delegate.onSyncedDataUpdated(data)
        }
    }

    fun edit(player: ServerPlayerEntity) {
        val lastEditing = editingPlayer?.getPlayer()
        if (lastEditing != null) {
            lastEditing.sendPacket(CloseNPCEditorPacket())
        }
        player.sendPacket(OpenNPCEditorPacket(this))
        editingPlayer = player.uuid
    }
}