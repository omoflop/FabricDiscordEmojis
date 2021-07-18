package mod.omoflop.fdemoji;

public abstract class ImageLoadThread extends Thread {

    public ImageLoadThread(EmbedCache.EmbedTexture self, String url) {
        this.self = self;
        this.url = url;
    }

    EmbedCache.EmbedTexture self;
    String url;

}
