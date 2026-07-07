package com.sakana.datacap.client.network;

import com.sakana.datacap.client.ExitRegionClientCache;
import com.sakana.datacap.client.gui.GuiExitRegionManager;
import com.sakana.datacap.network.PacketOpenExitRegionGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientOpenExitRegionGuiHandler implements IMessageHandler<PacketOpenExitRegionGui, IMessage> {
    @Override
    public IMessage onMessage(final PacketOpenExitRegionGui message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ExitRegionClientCache.setSelection(message.getPos1(), message.getPos2());
                ExitRegionClientCache.setRegionsFromJson(message.getRegionsJson());
                Minecraft.getMinecraft().displayGuiScreen(new GuiExitRegionManager());
            }
        });
        return null;
    }
}
