/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.interactive

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.criterion.CastPokeRodContext
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.fishing.BaitConsumedEvent
import com.cobblemon.mod.common.api.events.fishing.BaitSetEvent
import com.cobblemon.mod.common.api.events.fishing.PokerodCastEvent
import com.cobblemon.mod.common.api.events.fishing.PokerodReelEvent
import com.cobblemon.mod.common.api.fishing.FishingBait
import com.cobblemon.mod.common.api.fishing.FishingBaits
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity
import com.cobblemon.mod.common.item.RodBaitComponent
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.enchantmentRegistry
import com.cobblemon.mod.common.util.itemRegistry
import com.cobblemon.mod.common.util.playSoundServer
import com.cobblemon.mod.common.util.toEquipmentSlot
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.util.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

class PokerodItem(val pokeRodId: Identifier, settings: Properties) : FishingRodItem(settings) {

    companion object {
        fun getBaitOnRod(stack: ItemStack): FishingBait? {
            return stack.components.get(CobblemonItemComponents.BAIT)?.bait
        }

        fun getBaitStackOnRod(stack: ItemStack): ItemStack {
            return stack.components.get(CobblemonItemComponents.BAIT)?.stack ?: ItemStack.EMPTY
        }

        fun setBait(stack: ItemStack, bait: ItemStack) {
            CobblemonEvents.BAIT_SET.postThen(BaitSetEvent(stack, bait), { event -> }, {
                if (bait.isEmpty) {
                    stack.set<RodBaitComponent>(CobblemonItemComponents.BAIT, null)
                    return
                }
                val fishingBait = FishingBaits.getFromBaitItemStack(bait) ?: return
                stack.set(CobblemonItemComponents.BAIT, RodBaitComponent(fishingBait, bait))
                // add a new component that stores the itemStack as a component? Yes!
            })
        }

        fun consumeBait(stack: ItemStack) {
            CobblemonEvents.BAIT_CONSUMED.postThen(BaitConsumedEvent(stack), { event -> }, {
                val baitStack = getBaitStackOnRod(stack)
                val baitCount = baitStack.count
                if (baitCount == 1) {
                    stack.set<RodBaitComponent>(CobblemonItemComponents.BAIT, null)
                    return
                }
                if (baitCount > 1) {
                    val fishingBait = FishingBaits.getFromBaitItemStack(baitStack) ?: return
                    stack.set<RodBaitComponent>(
                        CobblemonItemComponents.BAIT,
                        RodBaitComponent(fishingBait, ItemStack(baitStack.item, baitCount - 1))
                    )
                }
            })
        }

        fun getBaitEffects(stack: ItemStack): List<FishingBait.Effect> {
            return getBaitOnRod(stack)?.effects ?: return emptyList()
        }
    }


    // Fishing Rod: Bundle edition
    override fun overrideOtherStackedOnMe(
        itemStack: ItemStack,
        itemStack2: ItemStack,
        slot: Slot,
        clickAction: ClickAction,
        player: Player,
        slotAccess: SlotAccess
    ): Boolean {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player))
            return false

        val baitStack = getBaitStackOnRod(itemStack)

        CobblemonEvents.BAIT_SET_PRE.postThen(BaitSetEvent(itemStack, itemStack2), { event ->
            return event.isCanceled
        }, {

            // If not holding an item on cursor
            if (itemStack2.isEmpty) {
                // Retrieve bait onto cursor
                if (baitStack != ItemStack.EMPTY) {
                    playDetachSound(player)
                    setBait(itemStack, ItemStack.EMPTY)
                    slotAccess.set(baitStack.copy())
                    return true
                }
            }
            // If holding item on cursor
            else {

                // If item on cursor is a valid bait
                if (FishingBaits.getFromBaitItemStack(itemStack2) != null) {

                    // Add as much as possible
                    if (baitStack != ItemStack.EMPTY) {
                        if (baitStack.item == itemStack2.item) {

                            playAttachSound(player)
                            // Calculate how much bait to add
                            val diff = (baitStack.maxStackSize - baitStack.count).coerceIn(0, itemStack2.count)
                            itemStack2.shrink(diff)
                            baitStack.grow(diff)
                            setBait(itemStack, baitStack)
                            return true
                        }

                        // If Item on rod is different from cursor item, swap them
                        playAttachSound(player)
                        setBait(itemStack, itemStack2.copy())
                        slotAccess.set(baitStack.copy())
                        return true
                    }

                    // If no bait currently on rod, add all
                    playAttachSound(player)
                    setBait(itemStack, itemStack2.copy())
                    itemStack2.shrink(itemStack2.count)
                    return true
                }
            }
        })
        return false
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {

        val itemStack = user.getItemInHand(hand)
        val offHandItem = user.getItemInHand(InteractionHand.OFF_HAND)
        val offHandBait = FishingBaits.getFromBaitItemStack(offHandItem)

        var baitOnRod = getBaitOnRod(itemStack)

        // Check if offhand item is valid bait and the rod is not in use, if so then apply bait from offhand
        if (!world.isClientSide && user.fishing == null && offHandBait != null) {
            CobblemonEvents.BAIT_SET_PRE.postThen(BaitSetEvent(itemStack, offHandItem), { event ->
                return InteractionResultHolder.fail(itemStack)
            }, {
                playAttachSound(user)

                // if there is bait on the rod already then drop it on the ground before applying offhand bait
                val baitStack = baitOnRod?.toItemStack(world.itemRegistry)

                if (baitStack != null && offHandItem.item != baitStack.item) {
                    if (!baitStack.isEmpty) {
                        baitStack.count = getBaitStackOnRod(itemStack).count
                        user.drop(baitStack, true) // Drop the full stack
                    }

                    // apply single bait item from offhand
                    val singleBait = offHandItem.copy()
                    singleBait.count = 1
                    setBait(itemStack, singleBait)
                    offHandItem.shrink(1)
                }
            })
        }

        var i: Int
        if (user.fishing != null) { // if the bobber is out yet
            if (!world.isClientSide) {
                CobblemonEvents.POKEROD_REEL.postThen(
                    PokerodReelEvent(itemStack),
                    { event -> return InteractionResultHolder.fail(itemStack) },
                    { event ->
                        i = user.fishing!!.retrieve(itemStack)
                        itemStack.hurtAndBreak(i, user, hand.toEquipmentSlot())
                        world.playSoundServer(
                            Vec3(user.x,
                                user.y,
                                user.z),
                            CobblemonSounds.FISHING_ROD_REEL_IN,
                            SoundSource.PLAYERS,
                            1.0f,
                            1.0f / (world.getRandom().nextFloat() * 0.4f + 0.8f)
                        )
                    }
                )
            }


            world.playSound(null as Player?, user.x, user.y, user.z, CobblemonSounds.FISHING_ROD_REEL_IN, SoundSource.PLAYERS, 1.0f, 1.0f)
            user.gameEvent(GameEvent.ITEM_INTERACT_FINISH)
        } else { // if the bobber is not out yet

            if (!world.isClientSide) {
                val lureLevel = world.enchantmentRegistry.getHolder(Enchantments.LURE).map { EnchantmentHelper.getItemEnchantmentLevel(it, itemStack) }.orElse(0)
                val luckLevel = world.enchantmentRegistry.getHolder(Enchantments.LUCK_OF_THE_SEA).map { EnchantmentHelper.getItemEnchantmentLevel(it, itemStack) }.orElse(0)

                val bobberEntity = PokeRodFishingBobberEntity(
                    user,
                    pokeRodId,
                    getBaitOnRod(itemStack)?.toItemStack(world.itemRegistry) ?: ItemStack.EMPTY,
                    world,
                    luckLevel,
                    lureLevel,
                    itemStack
                )
                CobblemonEvents.POKEROD_CAST_PRE.postThen(
                    PokerodCastEvent.Pre(itemStack, bobberEntity, getBaitStackOnRod(itemStack)),
                    { event -> return InteractionResultHolder.fail(itemStack) },
                    { event ->
                        world.addFreshEntity(bobberEntity)
                        var baitId = getBaitOnRod(itemStack)?.item ?: cobblemonResource("empty_bait")
                        CobblemonCriteria.CAST_POKE_ROD.trigger(user as ServerPlayer, CastPokeRodContext(baitId))

                        CobblemonEvents.POKEROD_CAST_POST.post(
                            PokerodCastEvent.Post(itemStack, bobberEntity, getBaitStackOnRod(itemStack))
                        )
                    }
                )
            }

            user.awardStat(Stats.ITEM_USED.get(this))
            user.gameEvent(GameEvent.ITEM_INTERACT_START)
        }
        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide)
    }

    override fun getEnchantmentValue(): Int {
        return 1
    }

    override fun getDescriptionId(): String {
        return "item.cobblemon.poke_rod"
    }

    private fun playAttachSound(entity: Entity) {
        entity.playSound(CobblemonSounds.FISHING_BAIT_ATTACH, 1F, 1F)
    }

    private fun playDetachSound(entity: Entity) {
        entity.playSound(CobblemonSounds.FISHING_BAIT_DETACH, 1F, 1F)
    }

}
