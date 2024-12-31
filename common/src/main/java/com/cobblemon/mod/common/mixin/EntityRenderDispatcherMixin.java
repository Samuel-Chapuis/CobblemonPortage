/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.render.MatrixWrapper;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PosableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow private Map<PlayerSkin.Model, EntityRenderer<? extends Player>> playerRenderers;

    @Inject(
        method = "onResourceManagerReload",
        at = @At(value = "TAIL")
    )
    public void resourceManagerReloadHook(ResourceManager resourceManager, CallbackInfo ci) {
        CobblemonClient.INSTANCE.onAddLayer(this.playerRenderers);
    }

    @Inject(
        method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V",
        at = @At(value = "TAIL")
    )
    private static void renderLocators(
        PoseStack poseStack,
        VertexConsumer buffer,
        Entity entity,
        float red,
        float green,
        float blue,
        float alpha,
        CallbackInfo ci
    ) {
        if (entity instanceof PosableEntity posableEntity) {
            if (posableEntity.getDelegate() instanceof PosableState state) {
                state.getLocatorStates().forEach((locator, matrix) -> {
                    poseStack.pushPose();
                    Vec3 pos = matrix.getOrigin().subtract(entity.position());
                    LevelRenderer.renderLineBox(
                        poseStack,
                        buffer,
                        AABB.ofSize(pos, 0.25, 0.25, 0.25),
                        0F,
                        1F,
                        0F,
                        1F
                    );
                    poseStack.popPose();
                });

            }
        }
    }
}