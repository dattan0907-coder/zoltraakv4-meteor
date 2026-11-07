package com.llamastudio.zoltraakmoves;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ctrl + Chuot phai: trieu hoi mua sao bang / thien thach (AoE) tai diem ngam,
 * ben duoi co vong ma thuat. Toan bo chay phia server, ve bang particle
 * (khong them entity moi nen khong can renderer phia client).
 */
public final class MeteorSkill {
    // ---- thong so (chinh tai day) ----
    private static final double RANGE = 40.0;            // tam ngam toi da
    private static final int COOLDOWN_TICKS = 400;       // 20s
    private static final int DURATION_TICKS = 120;       // vong phep ton tai 6s
    private static final double CIRCLE_RADIUS = 7.0;
    private static final int SPAWN_INTERVAL = 3;         // moi 3 tick roi 1 vien
    private static final double SPAWN_HEIGHT = 22.0;
    private static final double FALL_SPEED = 1.1;        // block / tick
    private static final double IMPACT_RADIUS = 3.0;     // ban kinh AoE moi vien
    private static final float DAMAGE = 10.0f;           // 10 atk moi vien

    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.65f, 0.2f, 1.0f), 1.2f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.4f, 0.9f, 1.0f), 1.0f);

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Storm> STORMS = new HashMap<>();

    private static final class Meteor {
        Vec3 pos;
        final double targetY;
        final boolean ice;
        Meteor(Vec3 pos, double targetY, boolean ice) {
            this.pos = pos;
            this.targetY = targetY;
            this.ice = ice;
        }
    }

    private static final class Storm {
        final ServerLevel level;
        final UUID owner;
        final ServerPlayer caster;
        final Vec3 center;
        final long start;
        final List<Meteor> meteors = new ArrayList<>();
        int spawned = 0;
        Storm(ServerLevel level, ServerPlayer caster, Vec3 center, long start) {
            this.level = level;
            this.caster = caster;
            this.owner = caster.getUUID();
            this.center = center;
            this.start = start;
        }
    }

    private MeteorSkill() {}

    // ------------------------------------------------------------ cast

    static void cast(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        Long ready = COOLDOWN.get(p.getUUID());
        if (ready != null && now < ready) {
            p.displayClientMessage(Component.translatable("message.zoltraak_moves.meteor_cd", (ready - now + 19) / 20), true);
            return;
        }
        COOLDOWN.put(p.getUUID(), now + COOLDOWN_TICKS);

        Vec3 center = findTarget(p, sl);
        STORMS.put(p.getUUID(), new Storm(sl, p, center, now));
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0f, 0.6f);
    }

    private static Vec3 findTarget(ServerPlayer p, ServerLevel sl) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(RANGE));
        BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 point = end;
        if (hit.getType() != HitResult.Type.MISS) {
            point = hit.getLocation().add(hit.getDirection().getStepX() * 0.2,
                    hit.getDirection().getStepY() * 0.2, hit.getDirection().getStepZ() * 0.2);
        }
        // neu ngam len troi / tuong: tha xuong mat dat
        BlockHitResult down = sl.clip(new ClipContext(point, point.add(0, -30, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (down.getType() != HitResult.Type.MISS) point = down.getLocation();
        return point;
    }

    // ------------------------------------------------------------ tick

    static void tickAll() {
        if (STORMS.isEmpty()) return;
        Iterator<Storm> it = STORMS.values().iterator();
        while (it.hasNext()) {
            if (!tickStorm(it.next())) it.remove();
        }
    }

    /** @return false khi storm da ket thuc. */
    private static boolean tickStorm(Storm s) {
        long age = s.level.getGameTime() - s.start;

        if (age <= DURATION_TICKS) {
            if (age % 5 == 0) drawCircle(s, age);
            if (age % SPAWN_INTERVAL == 0) spawnMeteor(s);
        }

        Iterator<Meteor> mi = s.meteors.iterator();
        while (mi.hasNext()) {
            Meteor m = mi.next();
            m.pos = m.pos.add(0, -FALL_SPEED, 0);
            trail(s.level, m);
            if (m.pos.y <= m.targetY) {
                impact(s, m);
                mi.remove();
            }
        }
        return age <= DURATION_TICKS || !s.meteors.isEmpty();
    }

    private static void spawnMeteor(Storm s) {
        double ang = s.level.random.nextDouble() * Math.PI * 2;
        double r = CIRCLE_RADIUS * 0.95 * Math.sqrt(s.level.random.nextDouble());
        double x = s.center.x + Math.cos(ang) * r;
        double z = s.center.z + Math.sin(ang) * r;

        // tim mat dat tai (x, z) de thien thach khong lot xuong duoi dat
        double targetY = s.center.y;
        BlockHitResult g = s.level.clip(new ClipContext(new Vec3(x, s.center.y + 1.5, z), new Vec3(x, s.center.y - 8, z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, s.caster));
        if (g.getType() != HitResult.Type.MISS) targetY = g.getLocation().y;

        boolean ice = (s.spawned++ % 2) == 0; // xen ke sao bang / thien thach lua
        s.meteors.add(new Meteor(new Vec3(x, s.center.y + SPAWN_HEIGHT, z), targetY, ice));
    }

    // ------------------------------------------------------------ visuals

    private static void drawCircle(Storm s, long age) {
        ServerLevel l = s.level;
        Vec3 c = s.center;
        double rot = age * 0.04;

        ring(l, c, CIRCLE_RADIUS, 40, 0, PURPLE);
        ring(l, c, CIRCLE_RADIUS - 0.6, 32, 0, PURPLE);
        ring(l, c, 2.0, 10, -rot, PURPLE);

        // hexagram quay (2 tam giac long nhau)
        double r = CIRCLE_RADIUS - 1.0;
        for (int t = 0; t < 2; t++) {
            double base = rot + t * Math.PI;
            for (int k = 0; k < 3; k++) {
                double a1 = base + k * 2 * Math.PI / 3;
                double a2 = base + (k + 1) * 2 * Math.PI / 3;
                line(l, c, r, a1, a2, 5, CYAN);
            }
        }
    }

    private static void ring(ServerLevel l, Vec3 c, double r, int pts, double off, ParticleOptions o) {
        for (int i = 0; i < pts; i++) {
            double a = off + 2 * Math.PI * i / pts;
            l.sendParticles(o, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void line(ServerLevel l, Vec3 c, double r, double a1, double a2, int pts, ParticleOptions o) {
        double x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
        double x2 = Math.cos(a2) * r, z2 = Math.sin(a2) * r;
        for (int i = 0; i < pts; i++) {
            double f = (double) i / pts;
            l.sendParticles(o, c.x + x1 + (x2 - x1) * f, c.y + 0.1, c.z + z1 + (z2 - z1) * f, 1, 0, 0, 0, 0);
        }
    }

    private static void trail(ServerLevel l, Meteor m) {
        if (m.ice) {
            l.sendParticles(ParticleTypes.END_ROD, m.pos.x, m.pos.y, m.pos.z, 2, 0.15, 0.15, 0.15, 0.01);
            l.sendParticles(ParticleTypes.SNOWFLAKE, m.pos.x, m.pos.y + 0.5, m.pos.z, 3, 0.3, 0.3, 0.3, 0.02);
        } else {
            l.sendParticles(ParticleTypes.FLAME, m.pos.x, m.pos.y, m.pos.z, 4, 0.25, 0.25, 0.25, 0.02);
            l.sendParticles(ParticleTypes.LARGE_SMOKE, m.pos.x, m.pos.y + 0.6, m.pos.z, 1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    // ------------------------------------------------------------ impact

    private static void impact(Storm s, Meteor m) {
        ServerLevel l = s.level;
        double cx = m.pos.x, cy = m.targetY, cz = m.pos.z;

        l.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.5, cz, 1, 0, 0, 0, 0);
        if (m.ice) {
            l.sendParticles(ParticleTypes.SNOWFLAKE, cx, cy + 0.5, cz, 40, 1.2, 0.6, 1.2, 0.12);
            l.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.5, cz, 15, 1.0, 0.5, 1.0, 0.1);
            l.playSound(null, cx, cy, cz, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        } else {
            l.sendParticles(ParticleTypes.FLAME, cx, cy + 0.5, cz, 40, 1.2, 0.6, 1.2, 0.12);
            l.sendParticles(ParticleTypes.LAVA, cx, cy + 0.5, cz, 10, 1.0, 0.3, 1.0, 0.0);
            l.playSound(null, cx, cy, cz, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        final Player owner = l.getPlayerByUUID(s.owner);
        DamageSource src = owner != null ? l.damageSources().playerAttack(owner) : l.damageSources().magic();
        AABB box = new AABB(cx - IMPACT_RADIUS, cy - 1.0, cz - IMPACT_RADIUS, cx + IMPACT_RADIUS, cy + 3.0, cz + IMPACT_RADIUS);

        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && e != owner)) {
            double dx = e.getX() - cx, dz = e.getZ() - cz;
            if (dx * dx + dz * dz > IMPACT_RADIUS * IMPACT_RADIUS) continue;
            if (e.hurt(src, DAMAGE)) {
                if (m.ice) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    e.setTicksFrozen(e.getTicksRequiredToFreeze() + 40);
                } else {
                    e.igniteForSeconds(3);
                }
            }
        }
    }

    static void forget(UUID id) {
        COOLDOWN.remove(id);
        STORMS.remove(id);
    }
}
