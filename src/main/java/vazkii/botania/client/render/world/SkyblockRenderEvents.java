/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.render.world;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.world.WorldTypeSkyblock;

public final class SkyblockRenderEvents {

	public static void onRender(RenderWorldLastEvent event) {
		World world = Minecraft.getInstance().world;
		if (ConfigHandler.CLIENT.enableFancySkybox.get()
				&& world.getDimension().getType() == DimensionType.OVERWORLD
				&& (ConfigHandler.CLIENT.enableFancySkyboxInNormalWorlds.get()
						|| WorldTypeSkyblock.isWorldSkyblock(world))) {
			if (!(world.getDimension().getSkyRenderer() instanceof SkyblockSkyRenderer)) {
				world.getDimension().setSkyRenderer(new SkyblockSkyRenderer());
			}
		}
	}

}
