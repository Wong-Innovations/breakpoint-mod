package com.wonginnovations.timeloop;

import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;

@Mod(modid = Timeloop.ID, name = Timeloop.NAME, version = Timeloop.VERSION)
@Mod.EventBusSubscriber(modid = Timeloop.ID)
public class Timeloop {

    public static final String ID = "timeloop";
    public static final String NAME = "Time Loop";
    public static final String VERSION = "1.0.0-a";
    public static final Logger LOGGER = LogManager.getLogger(ID);
    public static final ModConfig CONFIG = new ModConfig();

    private boolean loopEnded = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CONFIG.loadConfig(new File (event.getModConfigurationDirectory(), "timeloop.json"));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartingEvent event) {
        BackupManager.backupDimensions();
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (BackupManager.RESTORE_ON_CLOSE) {
            BackupManager.restoreDimensions();
            Timeloop.LOGGER.info("Reset to beginning of loop!");
            BackupManager.RESTORE_ON_CLOSE = false;
//            FMLCommonHandler.instance().getMinecraftServerInstance().run();
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!BackupManager.HAS_BACKED_UP) BackupManager.backupDimensions();
        if (CONFIG.hasDimensionConfig(event.world.provider.getDimension())
                && event.world.getWorldTime() == CONFIG.getDimensionConfig(event.world.provider.getDimension()).loopEnd
                && Timeloop.CONFIG.getDimensionConfig(event.world.provider.getDimension()).doLoop)
        {
            Timeloop.LOGGER.info("End of loop!");
            event.world.setWorldTime(CONFIG.getDimensionConfig(event.world.provider.getDimension()).loopStart);
            BackupManager.RESTORE_ON_CLOSE = true;
            loopEnded = true;
            FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        } else if (event.world instanceof WorldServer && ((WorldServer) event.world).areAllPlayersAsleep()) {
            long currentTime = event.world.getWorldTime();
            long timeAfterSleep = (currentTime + 24000L) - (currentTime + 24000L) % 24000L;
            if (CONFIG.getDimensionConfig(event.world.provider.getDimension()).loopEnd < timeAfterSleep) {
                Timeloop.LOGGER.info("End of loop!");
                event.world.setWorldTime(CONFIG.getDimensionConfig(event.world.provider.getDimension()).loopStart);
                BackupManager.RESTORE_ON_CLOSE = true;
                loopEnded = true;
                FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
            }
        }
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent event) {
        if (loopEnded && event.getGui() instanceof GuiDisconnected) {
            try {
                Field reason = GuiDisconnected.class.getDeclaredField("reason");
                reason.setAccessible(true);
                reason.set(event.getGui(), CONFIG.serverDisconnectMsg);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
    }

}
