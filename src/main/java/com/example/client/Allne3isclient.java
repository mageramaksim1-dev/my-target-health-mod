package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class Allne3isclient implements ClientModInitializer {

    private static LivingEntity lastAttackedEntity = null;
    private static long lastAttackTimestamp = 0;
    private static final long DISPLAY_TIMEOUT_MS = 8000;

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof LivingEntity living) {
                lastAttackedEntity = living;
                lastAttackTimestamp = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register(this::renderTargetHealthOverlay);
    }

    private void renderTargetHealthOverlay(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        LivingEntity target = getActiveTarget(client);

        if (target == null || !target.isAlive()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int centerX = screenWidth / 2;
        int startY = screenHeight / 2 + 18;

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float healthPercent = Math.max(0.0f, Math.min(1.0f, health / maxHealth));

        String nameText = target.getDisplayName().getString();
        String healthText = String.format("%.1f / %.1f HP", health, maxHealth);

        TextRenderer font = client.getTextRenderer();
        int barWidth = 100;
        int barHeight = 8;
        int barX = centerX - (barWidth / 2);
        int barY = startY + 12;

        drawContext.fill(barX - 4, startY - 2, barX + barWidth + 4, barY + barHeight + 3, 0x88000000);
        drawContext.drawCenteredTextWithShadow(font, nameText, centerX, startY, 0xFFFFFF);
        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF444444);

        int filledWidth = (int) (barWidth * healthPercent);
        int barColor = getHealthColor(healthPercent);
        drawContext.fill(barX, barY, barX + filledWidth, barY + barHeight, barColor);

        drawContext.drawCenteredTextWithShadow(font, healthText, centerX, barY, 0xFFFFFF);
    }

    private LivingEntity getActiveTarget(MinecraftClient client) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity hitEntity = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (hitEntity instanceof LivingEntity living && living.equals(lastAttackedEntity)) {
                return living;
            }
        }

        if (lastAttackedEntity != null && (System.currentTimeMillis() - lastAttackTimestamp < DISPLAY_TIMEOUT_MS)) {
            return lastAttackedEntity;
        }

        return null;
    }

    private int getHealthColor(float percent) {
        if (percent > 0.66f) {
            return 0xFF55FF55;
        } else if (percent > 0.33f) {
            return 0xFFFFFF55;
        } else {
            return 0xFFFF5555;
        }
    }
}