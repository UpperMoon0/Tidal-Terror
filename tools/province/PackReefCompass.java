import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Pack generated dial/needle assets into native compass model frames. */
public final class PackReefCompass {
    private static BufferedImage resize(Path path)throws Exception {
        var input=ImageIO.read(path.toFile());
        if(!input.getColorModel().hasAlpha())throw new AssertionError("Generated sprite lost alpha");
        var out=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(input,0,0,64,64,null);g.dispose();return out;
    }
    public static void main(String[] args)throws Exception {
        var dial=resize(Path.of(args[0]));var needle=resize(Path.of(args[1]));
        var root=Path.of(args[2]);var textures=root.resolve("textures/item");var models=root.resolve("models/item");
        Files.createDirectories(textures);Files.createDirectories(models);
        StringBuilder overrides=new StringBuilder();
        for(int i=0;i<32;i++) {
            var out=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(dial,0,0,null);g.translate(31.5,34.5);g.rotate(i*Math.PI*2/32);g.translate(-31.5,-32.5);
            g.drawImage(needle,0,0,null);g.dispose();
            String name=String.format(java.util.Locale.ROOT,"reef_compass_%02d",i);
            ImageIO.write(out,"png",textures.resolve(name+".png").toFile());
            Files.writeString(models.resolve(name+".json"),"{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"tidalterror:item/"+name+"\"}}\n");
            if(i>0)overrides.append(',');
            overrides.append("{\"predicate\":{\"angle\":").append(i/32.0).append("},\"model\":\"tidalterror:item/").append(name).append("\"}");
        }
        Files.writeString(models.resolve("reef_compass.json"),"{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"tidalterror:item/reef_compass_00\"},\"overrides\":["+overrides+"]}\n");
        System.out.println("Packed 32 generated-art compass frames, 64x64 RGBA");
    }
}
