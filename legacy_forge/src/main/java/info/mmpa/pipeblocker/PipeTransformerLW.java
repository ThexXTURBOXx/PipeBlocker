package info.mmpa.pipeblocker;

import net.minecraft.launchwrapper.IClassTransformer;

public class PipeTransformerLW implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        return basicClass;
    }
}
