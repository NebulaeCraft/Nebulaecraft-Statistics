package com.sakana.datacap.network;

import com.sakana.datacap.DataCollectionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketDeleteExitRegion implements IMessage {
    private String id = "";

    public PacketDeleteExitRegion() {
    }

    public PacketDeleteExitRegion(String id) {
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, id);
    }

    public static class Handler implements IMessageHandler<PacketDeleteExitRegion, IMessage> {
        @Override
        public IMessage onMessage(final PacketDeleteExitRegion message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handle(message, player);
                }
            });
            return null;
        }

        private void handle(PacketDeleteExitRegion message, EntityPlayerMP player) {
            String id = message.id == null ? "" : message.id;
            if (!DataCollectionMod.EXIT_REGIONS.remove(id)) {
                DataCollectionMod.sendMessage(player, "Unknown exit region: " + id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            if (!save(player)) {
                NetworkHandler.syncExitRegions(player);
                return;
            }

            DataCollectionMod.sendMessage(player, "Removed exit region " + id + ".");
            NetworkHandler.syncExitRegions(player);
        }

        private boolean save(EntityPlayerMP player) {
            MinecraftServer server = player.getServer();
            if (server == null) {
                DataCollectionMod.sendMessage(player, "Failed to save exit regions: server is missing.");
                return false;
            }

            try {
                DataCollectionMod.EXIT_REGION_STORAGE.save(server);
                return true;
            } catch (IOException exception) {
                DataCollectionMod.sendMessage(player, "Failed to save exit regions: " + exception.getMessage());
                return false;
            }
        }
    }
}
