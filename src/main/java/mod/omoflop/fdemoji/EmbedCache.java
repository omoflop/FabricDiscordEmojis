package mod.omoflop.fdemoji;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import mod.omoflop.fdemoji.accessor.BaseTextAccessor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class EmbedCache {

    private static final HashMap<String, EmbedTexture> IMAGE_CACHE = new HashMap<>();
    public static void clearCache() {
        IMAGE_CACHE.clear();
    }


    public static class EmbedTexture extends ResourceTexture {

        public byte[] data;
        public Identifier id;
        public boolean isDone = false;
        public double ratio;

        public EmbedTexture() {
            super(new Identifier("minecraft:textures/entity/steve.png"));
        }

        public void loadFromURL(String url) {
            Thread loadThread = new ImageLoadThread(this, url) {
                public void run() {
                    try {
                        InputStream stream = new URL(url).openStream();
                        self.data = IOUtils.toByteArray(stream);
                        BufferedImage buf = ImageIO.read(new ByteArrayInputStream(data));
                        int w = buf.getWidth();
                        int h = buf.getHeight();
                        self.ratio = (double)w/h;

                        stream.close();
                        uploadUsingData();
                    } catch (IOException e) {
                        data = null;
                        e.printStackTrace();
                    }
                }
            };
            loadThread.start();
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

    public static String parseURL(String url) {
        if (url == null) return null;

        for(String s : url.split(" ")) {
            if (s.startsWith("http")) {
                int i1 = s.indexOf("http");
                int i2 = s.indexOf(" ", i1);

                boolean haveExtraText = i2 != -1;
                return s.substring(i1, haveExtraText ? i2 : s.length());
            }
        }

        return null;
    }

    public static EmbedTexture getOrLoadImage(String imageURL) {
       if (!IMAGE_CACHE.containsKey(imageURL)) {
           Identifier texture_id = new Identifier("fdemoji", ""+imageURL.hashCode());

           EmbedTexture texture = new EmbedTexture();
           texture.id = texture_id;
           FDEmoji.getTextureManager().registerTexture(texture.id, texture);

           IMAGE_CACHE.put(imageURL, texture);
           texture.loadFromURL(imageURL);
           return texture;
       }

       return IMAGE_CACHE.get(imageURL);
    }

}
