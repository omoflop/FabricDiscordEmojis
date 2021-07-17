package mod.omoflop.fdemoji.mixin;

import mod.omoflop.fdemoji.accessor.BaseTextAccessor;
import net.minecraft.text.BaseText;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseText.class)
public class BaseTextMixin implements BaseTextAccessor {
    private String embed$messageURL = null;

    @Override
    public void setEmbedURL(String url) {
        embed$messageURL = url;
    }

    @Override
    public String getEmbedURL() {
        return embed$messageURL;
    }
}
