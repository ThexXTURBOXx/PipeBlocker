package info.mmpa.pipeblocker;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.Mod;
import info.mmpa.pipeblocker.logger.PipeBlockerLogger;

@Mod(modid = "pipeblocker", name = "PipeBlocker", version = Tags.VERSION, acceptableRemoteVersions = "*")
public class PipeBlockerMod {

    public static final PipeBlockerLogger LOGGER = PipeBlocker.detectLogger();

    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLInitializationEvent ev) {
        PipeBlocker.chooseBestLogger();
    }
}
