package com.llamastudio.zoltraakmoves;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** action: 0 = dash, 1 = double jump, 2 = meteor storm */
public record MovePayload(int action) implements CustomPacketPayload {
    public static final int DASH = 0;
    public static final int DOUBLE_JUMP = 1;
    public static final int METEOR = 2;

    public static final Type<MovePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZoltraakMoves.MODID, "move"));
    public static final StreamCodec<ByteBuf, MovePayload> CODEC =
            ByteBufCodecs.VAR_INT.map(MovePayload::new, MovePayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
