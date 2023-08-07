package info.mmpa.pipeblocker;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

// we support multiple minecraft version, so no @MCVersion
@IFMLLoadingPlugin.Name("PipeBlocker")
// naming clash, so fully qualified name
@cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name("PipeBlocker")
public class PipeLoadingPlugin implements IFMLLoadingPlugin, cpw.mods.fml.relauncher.IFMLLoadingPlugin {

    public PipeLoadingPlugin() {
        PipeBlocker.apply();
        PipeBlockerMod.LOGGER.info("Pipes being blocked.");
    }

    @Override
    public String[] getASMTransformerClass() {
        List<String> transformers = new ArrayList<>();
        if (classExists("net.minecraft.launchwrapper.IClassTransformer")) {
            PipeBlockerMod.LOGGER.info("Registering LaunchWrapper Class Transformer...");
            transformers.add("info.mmpa.pipeblocker.PipeTransformerLW");
        }
        if (classExists("cpw.mods.fml.relauncher.IClassTransformer")) {
            PipeBlockerMod.LOGGER.info("Registering CPW Class Transformer...");
            transformers.add("info.mmpa.pipeblocker.PipeTransformerCPW");
        }
        if (transformers.isEmpty()) {
            PipeBlockerMod.LOGGER.warn("No supported Class Transformer found!");
        }
        if (transformers.size() > 1) {
            PipeBlockerMod.LOGGER.warn("Multiple supported Class Transformers found! "
                    + "Expect ClassCastExceptions. Just ignore them, everything will still work fine!");
        }
        return transformers.toArray(new String[0]);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public String[] getLibraryRequestClass() {
        return null;
    }
}
