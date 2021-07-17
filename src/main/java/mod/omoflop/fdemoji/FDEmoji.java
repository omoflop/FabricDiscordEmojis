package mod.omoflop.fdemoji;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;

public class FDEmoji implements ModInitializer {
	@Override
	public void onInitialize() {
		//sup
		//i love you omo <3
	}

	private static TextureManager textureManager;
	public static TextureManager getTextureManager() {
		if (textureManager == null) textureManager = MinecraftClient.getInstance().getTextureManager();
		return textureManager;
	}
}
