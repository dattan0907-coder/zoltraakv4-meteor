package com.llamastudio.zoltraakmoves;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientMoves {
    /** Phim Dash, mac dinh R (doi duoc trong Options > Controls). */
    public static final KeyMapping DASH = new KeyMapping(
            "key.zoltraak_moves.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.zoltraak_moves");

    private static boolean jumpWasDown = false;

    private ClientMoves() {}

    static void init(IEventBus modBus) {
        modBus.addListener(ClientMoves::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientMoves::onTick);
        NeoForge.EVENT_BUS.addListener(ClientMoves::onInteract);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DASH);
    }

    /** Ctrl + Chuot phai (khi cam gay) = Meteor Storm; chan don chuot phai thuong cua gay. */
    private static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || !MoveLogic.holdingStaff(p)) return;

        long w = mc.getWindow().getWindow();
        boolean ctrl = InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL);
        if (!ctrl) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            PacketDistributor.sendToServer(new MovePayload(MovePayload.METEOR));
        }
    }

    private static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        boolean usable = mc.screen == null && MoveLogic.holdingStaff(p);

        // R = Dash
        while (DASH.consumeClick()) {
            if (usable) PacketDistributor.sendToServer(new MovePayload(MovePayload.DASH));
        }

        // Double Jump: nhan Space lan 2 khi dang o tren khong (cam gay)
        boolean jumpDown = mc.options.keyJump.isDown();
        if (usable && jumpDown && !jumpWasDown && !p.onGround()
                && !p.isInWater() && !p.isFallFlying() && !p.getAbilities().flying) {
            PacketDistributor.sendToServer(new MovePayload(MovePayload.DOUBLE_JUMP));
        }
        jumpWasDown = jumpDown;
    }
}
