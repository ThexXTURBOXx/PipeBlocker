package info.mmpa.pipeblocker;

import cpw.mods.fml.relauncher.IClassTransformer;

public class PipeTransformerCPW implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        return basicClass;
    }
    @Override
    public byte[] transform(String name, byte[] basicClass) {
        return basicClass;
    }
}
