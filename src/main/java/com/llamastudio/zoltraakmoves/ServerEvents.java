package com.llamastudio.zoltraakmoves;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerEvents {
    /** Sat thuong co ban cua Zoltraak Staff (danh thuong) = 10 atk. Nguoi choi co san 1 -> cong them 9. */
    private static final double STAFF_ATTACK_DAMAGE = 10.0;
    private static final double STAFF_ATTACK_SPEED = 1.6; // nhu kiem

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        Item staff = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("zoltraak", "zoltraak_staff"));
        if (!event.getItemStack().is(staff)) return;
        event.addModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ZoltraakMoves.MODID, "staff_damage"),
                        STAFF_ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ATTACK_SPEED,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ZoltraakMoves.MODID, "staff_speed"),
                        STAFF_ATTACK_SPEED - 4.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (sp.onGround()) MoveLogic.onGround(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MeteorSkill.tickAll();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MoveLogic.forget(event.getEntity().getUUID());
    }
}
