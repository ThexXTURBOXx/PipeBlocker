package cpw.mods.fml.relauncher;

public interface IClassTransformer
{
    public byte[] transform(String name, String transformedName, byte[] basicClass);
    public byte[] transform(String name, byte[] bytes);
}
