package com.example.examplemod.network;

import com.example.examplemod.gui.CartographerContainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBuyItem {
    private final int slotIndex;

    public PacketBuyItem(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public static void encode(PacketBuyItem msg, PacketBuffer buf) {
        buf.writeInt(msg.slotIndex);
    }

    public static PacketBuyItem decode(PacketBuffer buf) {
        return new PacketBuyItem(buf.readInt());
    }

    public static void handle(PacketBuyItem msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof CartographerContainer) {
                CartographerContainer container = (CartographerContainer) player.containerMenu;
                if (msg.slotIndex == -1) {
                    // Специальный индекс -1 означает обновление предложений
                    container.refreshOffers();
                } else {
                    container.purchaseItem(msg.slotIndex);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}