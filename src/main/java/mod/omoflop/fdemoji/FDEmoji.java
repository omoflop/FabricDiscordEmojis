package mod.omoflop.fdemoji;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.image.BufferedImage;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

public class FDEmoji implements ModInitializer {
	@Override
	public void onInitialize() {
		//sup
	}

	public static void renderTest(MatrixStack matrices) {

	}

	private static TextureManager textureManager;
	public static TextureManager getTextureManager() {
		if (textureManager == null) textureManager = MinecraftClient.getInstance().getTextureManager();
		return textureManager;
	}


}
