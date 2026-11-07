package com.llamastudio.zoltraakmoves;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class MoveLogic {
    private static final int DASH_COOLDOWN_TICKS = 50; // 2.5s
    private static final double DASH_SPEED = 1.8;
    private static final double DASH_LIFT = 0.25;
    private static final double JUMP_SPEED = 0.8;

    private static final Map<UUID, Long> DASH_READY = new HashMap<>();
    private static final Set<UUID> JUMP_USED = new HashSet<>();

    private MoveLogic() {}

    public static boolean holdingStaff(Player p) {
        Item staff = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("zoltraak", "zoltraak_staff"));
        return p.getMainHandItem().is(staff) || p.getOffhandItem().is(staff);
    }

    public static void handle(ServerPlayer p, int action) {
        if (p.isSpectator() || !holdingStaff(p)) return;
        if (action == MovePayload.DASH) dash(p);
        else if (action == MovePayload.DOUBLE_JUMP) doubleJump(p);
        else if (action == MovePayload.METEOR) MeteorSkill.cast(p);
    }

    private static void dash(ServerPlayer p) {
        if (!p.onGround()) return;
        long now = p.level().getGameTime();
        if (now < DASH_READY.getOrDefault(p.getUUID(), 0L)) return;

        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) return;
        flat = flat.normalize().scale(DASH_SPEED);

        DASH_READY.put(p.getUUID(), now + DASH_COOLDOWN_TICKS);
        p.setDeltaMovement(flat.x, DASH_LIFT, flat.z);
        p.hurtMarked = true;

        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 15, 0.4, 0.1, 0.4, 0.1);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.9f);
        }
    }

    private static void doubleJump(ServerPlayer p) {
        if (p.onGround() || p.isInWater() || p.isFallFlying() || p.getAbilities().flying) return;
        if (!JUMP_USED.add(p.getUUID())) return;

        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x, JUMP_SPEED, dm.z);
        p.hurtMarked = true;
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false, false));

        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 15, 0.5, 0.1, 0.5, 0.15);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5f, 1.7f);
        }
    }

    static void onGround(UUID id) { JUMP_USED.remove(id); }

    static void forget(UUID id) {
        JUMP_USED.remove(id);
        DASH_READY.remove(id);
        MeteorSkill.forget(id);
    }
}
