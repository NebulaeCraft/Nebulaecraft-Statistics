package com.sakana.datacap.network;

import com.sakana.datacap.DataCollectionMod;
import com.sakana.datacap.exit.ExitRegion;
import com.sakana.datacap.exit.ExitRegionIds;
import com.sakana.datacap.selection.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketCreateExitRegion implements IMessage {
    private String id = "";

    public PacketCreateExitRegion() {
    }

    public PacketCreateExitRegion(String id) {
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

    public static class Handler implements IMessageHandler<PacketCreateExitRegion, IMessage> {
        @Override
        public IMessage onMessage(final PacketCreateExitRegion message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handle(message, player);
                }
            });
            return null;
        }

        private void handle(PacketCreateExitRegion message, EntityPlayerMP player) {
            String id = message.id == null ? "" : message.id.trim();
            if (!ExitRegionIds.isValid(id)) {
                DataCollectionMod.sendMessage(player, ExitRegionIds.ID_RULE_MESSAGE);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            Selection selection = DataCollectionMod.SELECTIONS.getSelection(player.getUniqueID());
            if (selection == null || !selection.isComplete()) {
                DataCollectionMod.sendMessage(player, "Select pos1 with left click and pos2 with right click first.");
                NetworkHandler.syncExitRegions(player);
                return;
            }

            ExitRegion region = new ExitRegion(id, selection.getPos1(), selection.getPos2());
            if (!DataCollectionMod.EXIT_REGIONS.add(region)) {
                DataCollectionMod.sendMessage(player, "Exit region already exists: " + id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            if (!save(player)) {
                DataCollectionMod.EXIT_REGIONS.remove(id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            DataCollectionMod.sendMessage(player, "Created exit region " + id + ".");
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
