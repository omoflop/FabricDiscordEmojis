package mod.omoflop.fdemoji.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.omoflop.fdemoji.EmbedCache;
import mod.omoflop.fdemoji.accessor.BaseTextAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

	@Shadow private int scrolledLines;
	@Shadow @Final private MinecraftClient client;
	@Shadow @Final private List<ChatHudLine<Text>> messages;
	@Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;

	@Shadow public abstract int getWidth();
	@Shadow public abstract double getChatScale();
	@Shadow public abstract int getVisibleLineCount();
	@Shadow protected abstract boolean isChatHidden();
	@Shadow protected abstract boolean isChatFocused();
	@Shadow protected abstract void addMessage(Text message, int messageId);
	@Shadow private static double getMessageOpacityMultiplier(int age) {
		return 0;
	}
	/* TODO:
	 *  * Image stretching
	 *  * Fix style not being preserved on messages
	 *  * Clear image cache when not visible anymore
	 */

	@Inject(at = @At(value = "TAIL"), method = "render")
	public void render(MatrixStack matrices, int tickDelta, CallbackInfo ci) {
		int j = this.visibleMessages.size();

		if (this.isChatHidden() || j <= 0)
			return;

		boolean fade = this.isChatFocused();

		double messageHeight = 9.0D * (this.client.options.chatLineSpacing + 1.0D);
		double h = -8.0D * (this.client.options.chatLineSpacing + 1.0D) + 4.0D * this.client.options.chatLineSpacing;

		//render images
		for(int m = 0, extra = 0; m + this.scrolledLines < this.messages.size() && m < this.getVisibleLineCount(); ++m) {
			Text line = this.messages.get(m).getText();
			if (line == null) continue;

			//break lines
			List<OrderedText> lineList = ChatMessages.breakRenderedChatMessageLines(line, MathHelper.floor((double)this.getWidth() / this.getChatScale()), this.client.textRenderer);
			extra += lineList.size();

			//skip if it doesnt matter
			String embedURL = ((BaseTextAccessor) line).getEmbedURL();
			if (embedURL == null) continue;

			//draw stuff
			double s = (double) (-extra + 2) * messageHeight;

			int imageWidth = 32;
			int imageHeight = 32;

			int x = tickDelta - this.messages.get(m).getCreationTick();
			if (fade || x < 200) {
				double opacity = fade ? 1.0f : getMessageOpacityMultiplier(x);

				EmbedCache.EmbedTexture texture = EmbedCache.getOrLoadImage(embedURL);
				RenderSystem.setShaderTexture(0, texture.getGlId());

				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1, 1, 1, (float) opacity);
				drawTexture(matrices, 4, (int) (s + h + messageHeight), 0, 0, 0, imageWidth, imageHeight, imageHeight, imageWidth);
				RenderSystem.disableBlend();
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;)V", cancellable = true)
	public void addMessage(Text message, CallbackInfo ci) {
		String string = message.getString();

		if (string.contains("http")) {
			int i1 = string.indexOf("http");
			int i2 = string.indexOf(" ", i1);

			boolean haveExtraText = i2 != -1;
			string = string.substring(i1, haveExtraText ? i2 : string.length());

			if (EmbedCache.getOrLoadImage(string).data != null) {
				BaseTextAccessor text = (BaseTextAccessor) new LiteralText((message.getString() + " ").replace(string, haveExtraText ? "\n\n\n\n\n" : "\n\n\n\n"));
				text.setEmbedURL(string);

				this.addMessage((Text) text, 0);

				ci.cancel();
			}
		}
	}
}
