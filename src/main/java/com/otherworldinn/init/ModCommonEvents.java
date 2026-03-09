package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.command.TeamCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModCommonEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TeamCommands.register(event.getDispatcher());
    }
}
