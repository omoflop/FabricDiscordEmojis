package mod.omoflop.fdemoji.mixin;

import com.google.common.base.Splitter;
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

import static mod.omoflop.fdemoji.ModConfig.IMAGE_SIZE;
import static net.minecraft.client.gui.DrawableHelper.*;

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
	 *  * Fix style not being preserved on messages
	 *  * Clear image cache when not visible anymore
	 *  * Fix images getting cropped too aggressively
	 */

	@Inject(at = @At(value = "TAIL"), method = "render")
	public void render(MatrixStack matrices, int tickDelta, CallbackInfo ci) {
		int j = this.visibleMessages.size();

		if (this.isChatHidden() || j <= 0)
			return;

		boolean fade = this.isChatFocused();

		double h = -8.0D * (this.client.options.chatLineSpacing + 1.0D) + 4.0D * this.client.options.chatLineSpacing;
		double messageHeight = 9.0D * (this.client.options.chatLineSpacing + 1.0D);

		//render images
		for (int m = 0, extra = 0; m < this.messages.size() && extra - this.scrolledLines < this.getVisibleLineCount(); ++m) {

			Text line = this.messages.get(m).getText();
			if (line == null) continue;

			//break lines
			List<OrderedText> lineList = ChatMessages.breakRenderedChatMessageLines(line, MathHelper.floor((double) this.getWidth() / this.getChatScale()), this.client.textRenderer);
			extra += lineList.size();

			double s = (double) (-extra + 2 + this.scrolledLines) * messageHeight;
			int y = (int) (s + h + messageHeight);

			if (y > 0) continue;

			//skip if it doesn't matter
			String embedURL = ((BaseTextAccessor) line).getEmbedURL();
			if (embedURL == null) continue;

			//draw stuff

			int x = tickDelta - this.messages.get(m).getCreationTick();
			if (fade || x < 200) {

				double opacity = fade ? 1.0f : getMessageOpacityMultiplier(x);

				EmbedCache.EmbedTexture texture = EmbedCache.getOrLoadImage(embedURL);
				RenderSystem.setShaderTexture(0, texture.getGlId());
				int width = (int) Math.round(texture.ratio * IMAGE_SIZE);
				int height = IMAGE_SIZE;

				RenderSystem.setShaderColor(1, 1, 1, (float) opacity);

				int cropDown = 0;
				if (y + height - messageHeight > 0) {
					cropDown = y + height;
				}

				RenderSystem.enableBlend();
				drawTexture(matrices, 4, y, 0, 0, 0, width, height-cropDown, height, width);

				RenderSystem.disableBlend();
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;)V", cancellable = true)
	public void addMessage(Text message, CallbackInfo ci) {
		String string = message.getString();

		if (string.contains("http") && (string.endsWith("png") || string.endsWith("jpg") || string.endsWith("jpeg") || string.endsWith("gif"))) {
			int i1 = string.indexOf("http");
			int i2 = string.indexOf(" ", i1);

			boolean haveExtraText = i2 != -1;
			string = string.substring(i1, haveExtraText ? i2 : string.length());

			if (EmbedCache.getOrLoadImage(string) != null) {
				BaseTextAccessor text = (BaseTextAccessor) new LiteralText((message.getString() + " ").replace(string, haveExtraText ? "\n\n\n\n\n" : "\n\n\n\n"));
				text.setEmbedURL(string);

				this.addMessage((Text) text, 0);

				ci.cancel();
			}
		}
	}
}
