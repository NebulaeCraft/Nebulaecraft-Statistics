package com.sakana.datacap.client.network;

import com.sakana.datacap.client.ExitRegionClientCache;
import com.sakana.datacap.network.PacketSyncExitRegions;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientSyncExitRegionsHandler implements IMessageHandler<PacketSyncExitRegions, IMessage> {
    @Override
    public IMessage onMessage(final PacketSyncExitRegions message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ExitRegionClientCache.setRegionsFromJson(message.getRegionsJson());
            }
        });
        return null;
    }
}
