package info.mmpa.pipeblocker;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import sun.misc.ObjectInputFilter;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.Name("PipeBlocker")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class PipeLoadingPlugin implements IFMLLoadingPlugin {

    public PipeLoadingPlugin() {
        ObjectInputFilter.Config.setSerialFilter(new ObjectInputFilter() {
            @Override
            public Status checkInput(FilterInfo filterInfo) {
                // TODO actually implement
                return Status.REJECTED;
            }
        });
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "info.mmpa.pipeblocker.PipeTransformer" };
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
}