package net.sora.betterms;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class BetterMS implements ClientModInitializer {

    private int lastSlot = -1;
    private boolean isEnabled = true;
    private static KeyBinding toggleKeyBinding;

    @Override
    public void onInitializeClient() {
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.betterms.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F12,
                "category.betterms"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (toggleKeyBinding.wasPressed()) {
                isEnabled = !isEnabled;
                
                if (isEnabled) {
                    client.player.sendMessage(Text.literal("[Better MS] ").formatted(Formatting.GOLD)
                            .append(Text.literal("ENABLED").formatted(Formatting.GREEN)), true);
                } else {
                    client.player.sendMessage(Text.literal("[Better MS] ").formatted(Formatting.GOLD)
                            .append(Text.literal("DISABLED").formatted(Formatting.RED)), true);
                }
            }

            if (isEnabled) {
                int currentSlot = client.player.getInventory().selectedSlot;

                if (currentSlot != lastSlot) {
                    ItemStack currentItem = client.player.getMainHandStack();

                    if (isWeapon(currentItem)) {
                        performInstantAttack(client);
                    }
                    lastSlot = currentSlot;
                }
            }
        });
    }

    private boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof AxeItem || stack.isOf(Items.MACE);
    }

    private void performInstantAttack(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        client.player.resetLastAttackedTicks();

        Entity targetEntity = getTargetEntity(client, 3.0D);

        if (targetEntity != null) {
            client.interactionManager.attackEntity(client.player, targetEntity);
        }

        client.player.swingHand(Hand.MAIN_HAND);
    }

    private Entity getTargetEntity(MinecraftClient client, double reachDistance) {
        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null || client.world == null) return null;

        Vec3d eyePos = cameraEntity.getEyePos();
        Vec3d lookVec = cameraEntity.getRotationVec(1.0F);
        Vec3d reachVec = eyePos.add(lookVec.x * reachDistance, lookVec.y * reachDistance, lookVec.z * reachDistance);

        Box box = cameraEntity.getBoundingBox().stretch(lookVec.multiply(reachDistance)).expand(1.0D);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(
                cameraEntity,
                eyePos,
                reachVec,
                box,
                entity -> !entity.isSpectator() && entity.canHit(),
                reachDistance * reachDistance
        );

        return entityHitResult != null ? entityHitResult.getEntity() : null;
    }
}
