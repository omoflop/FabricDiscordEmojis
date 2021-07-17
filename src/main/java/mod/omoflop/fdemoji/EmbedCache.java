package mod.omoflop.fdemoji;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryUtil;
import org.w3c.dom.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class EmbedCache {

    private static HashMap<String, EmbedTexture> imageCache = new HashMap<>();

    public static void clearCache() {
        imageCache.clear();
    }

    public static class EmbedTexture extends ResourceTexture {

        public byte[] data;
        public Identifier id;
        public boolean isDone = false;

        public EmbedTexture() {
            super(new Identifier("minecraft:textures/entity/steve.png"));
        }

        public void loadFromURL(String url) {
            try {
                InputStream stream = new URL(url).openStream();
                data = IOUtils.toByteArray(stream);
                stream.close();
                uploadUsingData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void registerTexture(){
            FDEmoji.getTextureManager().registerTexture(id, this);
        }

        public void uploadUsingData() {
            registerTexture();
            try {
                ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                wrapper.put(data);
                wrapper.rewind();
                NativeImage image = NativeImage.read(wrapper);

                RenderSystem.recordRenderCall(() -> {
                    uploadTexture(image);

                    isDone = true;
                    System.out.println("Loaded embed texture");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void uploadTexture(NativeImage image) {
            TextureUtil.prepareImage(this.getGlId(), image.getWidth(), image.getHeight());
            image.upload(0, 0, 0, true);
            this.isDone = true;
        }
    }

    public static EmbedTexture getOrLoadImage(String imageURL) {
       if (!imageCache.containsKey(imageURL)) {
           Identifier texture_id = new Identifier("fdemoji", ""+imageURL.hashCode());

           EmbedTexture texture = new EmbedTexture();
           texture.id = texture_id;
           FDEmoji.getTextureManager().registerTexture(texture.id, texture);

           texture.loadFromURL(imageURL);
           imageCache.put(imageURL, texture);
           return texture;
       }

       return imageCache.get(imageURL);
    }

}
