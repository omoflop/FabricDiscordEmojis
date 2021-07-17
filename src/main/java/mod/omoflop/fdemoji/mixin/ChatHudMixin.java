package mod.omoflop.fdemoji.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.omoflop.fdemoji.EmbedCache;
import mod.omoflop.fdemoji.FDEmoji;
import mod.omoflop.fdemoji.accessor.BaseTextAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;
import static net.minecraft.client.gui.DrawableHelper.fill;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

	@Shadow private boolean hasUnreadNewMessages;

	@Shadow private int scrolledLines;

	@Shadow @Final private MinecraftClient client;

	@Shadow @Final private Deque<Text> messageQueue;

	@Shadow
	protected static double getMessageOpacityMultiplier(int age) {
		return 0;
	}

	@Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;

	@Shadow public abstract double getChatScale();

	@Shadow public abstract int getWidth();

	@Shadow protected abstract boolean isChatFocused();

	@Shadow public abstract int getVisibleLineCount();

	@Shadow protected abstract void processMessageQueue();

	@Shadow protected abstract boolean isChatHidden();

	@Shadow public abstract List<String> getMessageHistory();

	@Shadow protected abstract void addMessage(Text message, int messageId);

	@Shadow public abstract void addMessage(Text message);

	@Shadow @Final private List<ChatHudLine<Text>> messages;

	/**
	 * @author
	 */
	@Overwrite
	public void render(MatrixStack matrices, int tickDelta) {

		FDEmoji.renderTest(matrices);


		if (!this.isChatHidden()) {
			this.processMessageQueue();
			int i = this.getVisibleLineCount();
			int j = this.visibleMessages.size();
			if (j > 0) {
				boolean bl = false;
				if (this.isChatFocused()) {
					bl = true;
				}

				float f = (float)this.getChatScale();
				int chatWidth = MathHelper.ceil((float)this.getWidth() / f);
				matrices.push();
				matrices.translate(4.0D, 8.0D, 0.0D);
				matrices.scale(f, f, 1.0F);
				double d = this.client.options.chatOpacity * 0.8999999761581421D + 0.10000000149011612D;
				double e = this.client.options.textBackgroundOpacity;
				double messageHeight = 9.0D * (this.client.options.chatLineSpacing + 1.0D);
				double h = -8.0D * (this.client.options.chatLineSpacing + 1.0D) + 4.0D * this.client.options.chatLineSpacing;
				int l = 0;

				int m;
				int x;
				int aa;
				int ab;

				int lastY = 0;
				int chatShift = 0;
				for(m = 0; m + this.scrolledLines < this.visibleMessages.size() && m < i; ++m) {
					int messageIndex = m + this.scrolledLines;
					ChatHudLine<OrderedText> chatHudLine = this.visibleMessages.get(messageIndex);
					if (chatHudLine != null) {
						x = tickDelta - chatHudLine.getCreationTick();
						if (x < 200 || bl) {
							double o = bl ? 1.0D : this.getMessageOpacityMultiplier(x);
							aa = (int)(255.0D * o * d);
							ab = (int)(255.0D * o * e);
							++l;
							if (aa > 3) {
								double s = (double)(-m) * messageHeight;
								matrices.push();
								matrices.translate(0.0D, 0.0D, 50.0D);


								// Draw text shadow
								int x1 = -4;
								int y1 = (int)(s - messageHeight); // ?

								int width = chatWidth+4;
								int height = (int)messageHeight;

								int imageWidth = 32;
								int imageHeight = 32;

								try {
									Text text = this.messages.get(messageIndex).getText();
									if (text != null) {
										String thisMessage = ((BaseTextAccessor) text).getEmbedURL();
										if (thisMessage != "") {
											EmbedCache.EmbedTexture texture = EmbedCache.getOrLoadImage(thisMessage);

											RenderSystem.enableBlend();
											texture.bindTexture();
											RenderSystem.setShaderTexture(0, texture.getGlId());

											drawTexture(matrices, x1, (int)(s + h + messageHeight), 0, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
											RenderSystem.disableBlend();
										}

									}

								} catch (Exception ex) {
									System.out.println(ex);
								}

								fill(matrices, x1, y1, x1+width, y1+height, ab << 24);

								RenderSystem.enableBlend();
								matrices.translate(0.0D, 0.0D, 50.0D);
								OrderedText chatMessage = chatHudLine.getText();
								this.client.textRenderer.drawWithShadow(matrices, chatMessage, 0.0F, y1+1, 16777215 + (aa << 24));
								//this.client.textRenderer.draw(matrices, String.format("%s | %s => %s", messageIndex, m, this.messages.get(messageIndex).getText().getString()), x1+width, (float)((int)(s + h)), 16777215 + (aa << 24));


								lastY = y1;
								RenderSystem.disableBlend();

								matrices.pop();
							}
						}
					}
				}

				int w;
				if (!this.messageQueue.isEmpty()) {
					m = (int)(128.0D * d);
					w = (int)(255.0D * e);
					matrices.push();
					matrices.translate(0.0D, 0.0D, 50.0D);
					fill(matrices, -2, 0, 4+chatWidth, 9, w << 24);
					RenderSystem.enableBlend();
					matrices.translate(0.0D, 0.0D, 50.0D);
					this.client.textRenderer.drawWithShadow(matrices, new TranslatableText("chat.queue", new Object[]{this.messageQueue.size()}), 0.0F, 1.0F, 16777215 + (m << 24));
					matrices.pop();
					RenderSystem.disableBlend();
				}

				if (bl) {
					Objects.requireNonNull(this.client.textRenderer);
					int v = 9;
					w = j * v;
					x = l * v;
					int y = this.scrolledLines * x / j;
					int z = x * x / w;
					if (w != x) {
						aa = y > 0 ? 170 : 96;
						ab = this.hasUnreadNewMessages ? 13382451 : 3355562;
						matrices.translate(-4.0D, 0.0D, 0.0D);
						fill(matrices, 0, -y, 2, -y - z, ab + (aa << 24));
						fill(matrices, 2, -y, 1, -y - z, 13421772 + (aa << 24));
					}
				}

				matrices.pop();
			}
		}
	}

	private HashMap<Text, String> embedMap = new HashMap<>();

	@Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;)V", cancellable = true)
	public void addMessage(Text message, CallbackInfo ci) {
		String string = message.getString();
		if (string.contains("http")) {
			int i1 = string.indexOf("http");
			int i2 = string.indexOf(" ", i1);
			string = string.substring(i1, (i2 == -1) ? string.length() : i2);

			BaseTextAccessor text = (BaseTextAccessor) new LiteralText(message.getString().replace(string, ""));

			text.setEmbedURL(string);

			embedMap.put((Text)text, string);
			this.addMessage((Text)text, 0);
			this.addMessage(new LiteralText(""), 0);
			this.addMessage(new LiteralText(" "), 0);
			this.addMessage(new LiteralText("   "), 0);
			this.addMessage(new LiteralText("       "), 0);
			ci.cancel();
		}
	}
}
