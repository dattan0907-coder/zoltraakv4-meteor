package com.llamastudio.zoltraakmoves;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;

@Mod(ZoltraakMoves.MODID)
public class ZoltraakMoves {
    public static final String MODID = "zoltraak_moves";

    public ZoltraakMoves(IEventBus modBus, Dist dist) {
        modBus.addListener(ZoltraakMoves::registerPayloads);
        NeoForge.EVENT_BUS.register(ServerEvents.class);
        if (dist == Dist.CLIENT) {
            ClientMoves.init(modBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MovePayload.TYPE, MovePayload.CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        MoveLogic.handle(sp, payload.action());
                    }
                }));
    }
}
