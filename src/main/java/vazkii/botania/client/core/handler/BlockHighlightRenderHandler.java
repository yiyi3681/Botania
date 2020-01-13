/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Mar 31, 2015, 3:16:02 PM (GMT)]
 */
package vazkii.botania.client.core.handler;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.TileEntitySpecialFlower;
import vazkii.botania.common.Botania;
import vazkii.botania.common.core.helper.PlayerHelper;
import vazkii.botania.common.entity.EntityMagicLandmine;
import vazkii.botania.common.item.ItemTwigWand;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.lib.LibMisc;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = LibMisc.MOD_ID)
public final class BlockHighlightRenderHandler {
	private static final RenderType RECTANGLE;
	private static final RenderType CIRCLE;
	static {
		// todo 1.15 check GL state, AT's for the fields
		RenderState.TransparencyState translucentTransparency = ObfuscationReflectionHelper.getPrivateValue(RenderState.class, null, "field_228515_g_");
		RenderState.CullState disableCull = ObfuscationReflectionHelper.getPrivateValue(RenderState.class, null, "field_228491_A_");
		RenderType.State glState = RenderType.State.builder().transparency(translucentTransparency).cull(disableCull).build(false);
		RECTANGLE = RenderType.of("botania:rectangle_highlight", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, false, true, glState);
		CIRCLE = RenderType.of("botania:circle_highlight", DefaultVertexFormats.POSITION_COLOR, GL11.GL_TRIANGLE_FAN, 256, false, true, glState);
	}

	private BlockHighlightRenderHandler() {}

	@SubscribeEvent
	public static void onWorldRenderLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getInstance();
		RayTraceResult pos = mc.objectMouseOver;
		MatrixStack ms = event.getMatrixStack();
		IRenderTypeBuffer.Impl buffers = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuffer());

		ms.push();

		if (Botania.proxy.isClientPlayerWearingMonocle() && pos != null && pos.getType() == RayTraceResult.Type.BLOCK) {
			BlockPos bPos = ((BlockRayTraceResult) pos).getPos();

			ItemStack stackHeld = PlayerHelper.getFirstHeldItem(mc.player, ModItems.twigWand);
			if (!stackHeld.isEmpty() && ItemTwigWand.getBindMode(stackHeld)) {
				Optional<BlockPos> coords = ItemTwigWand.getBindingAttempt(stackHeld);
				if (coords.isPresent())
					bPos = coords.get();
			}

			TileEntity tile = mc.world.getTileEntity(bPos);
			if (tile instanceof TileEntitySpecialFlower) {
				TileEntitySpecialFlower subtile = (TileEntitySpecialFlower) tile;
				RadiusDescriptor descriptor = subtile.getRadius();
				if (descriptor != null) {
					if (descriptor.isCircle())
						renderCircle(ms, buffers, descriptor.getSubtileCoords(), descriptor.getCircleRadius());
					else renderRectangle(ms, buffers, descriptor.getAABB(), true, null, (byte) 32);
				}
			}
		}

		double offY = -1.0 / 16 + 0.005;
		for(Entity e : mc.world.getAllEntities())
			if(e instanceof EntityMagicLandmine) {
				BlockPos bpos = e.getPosition();
				AxisAlignedBB aabb = new AxisAlignedBB(bpos).offset(0, offY, 0).grow(2.5, 0, 2.5);

				float gs = (float) (Math.sin(ClientTickHandler.total / 20) + 1) * 0.2F + 0.6F;
				int r = (int) (105 * gs);
				int g = (int) (25 * gs);
				int b = (int) (145 * gs);
				Color color = new Color(r, g, b);

				int alpha = 32;
				if(e.ticksExisted < 8)
					alpha *= Math.min((e.ticksExisted + event.getPartialTicks()) / 8F, 1F);
				else if(e.ticksExisted > 47)
					alpha *= Math.min(1F - (e.ticksExisted - 47 + event.getPartialTicks()) / 8F, 1F);

				renderRectangle(ms, buffers, aabb, false, color, (byte) alpha);
				offY += 0.001;
			}

		ms.pop();
		buffers.draw();
	}

	private static void renderRectangle(MatrixStack ms, IRenderTypeBuffer buffers, AxisAlignedBB aabb, boolean inner, @Nullable Color color, byte alpha) {
		double renderPosX = Minecraft.getInstance().getRenderManager().info.getProjectedView().getX();
		double renderPosY = Minecraft.getInstance().getRenderManager().info.getProjectedView().getY();
		double renderPosZ = Minecraft.getInstance().getRenderManager().info.getProjectedView().getZ();

		ms.push();
		ms.translate(aabb.minX - renderPosX, aabb.minY - renderPosY, aabb.minZ - renderPosZ);

		if(color == null)
			color = Color.getHSBColor(ClientTickHandler.ticksInGame % 200 / 200F, 0.6F, 1F);

		float f = 1F / 16F;
		float x = (float) (aabb.maxX - aabb.minX - f);
		float z = (float) (aabb.maxZ - aabb.minZ - f);

		IVertexBuilder buffer = buffers.getBuffer(RECTANGLE);
		Matrix4f mat = ms.peek().getModel();
		buffer.vertex(mat, x, f, f).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
		buffer.vertex(mat, f, f, f).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
		buffer.vertex(mat, f, f, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
		buffer.vertex(mat, x, f, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();

		if(inner) {
			x += f;
			z += f;
			float f1 = f + f / 4F;
			alpha *= 2;
			buffer.vertex(mat, x, f1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
			buffer.vertex(mat, 0, f1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
			buffer.vertex(mat, 0, f1, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
			buffer.vertex(mat, x, f1, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).endVertex();
		}

		ms.pop();
	}

	private static void renderCircle(MatrixStack ms, IRenderTypeBuffer buffers, BlockPos center, double radius) {
		double renderPosX = Minecraft.getInstance().getRenderManager().info.getProjectedView().getX();
		double renderPosY = Minecraft.getInstance().getRenderManager().info.getProjectedView().getY();
		double renderPosZ = Minecraft.getInstance().getRenderManager().info.getProjectedView().getZ();

		ms.push();
		double x = center.getX() + 0.5;
		double y = center.getY();
		double z = center.getZ() + 0.5;
		ms.translate(x - renderPosX, y - renderPosY, z - renderPosZ);
		int color = Color.HSBtoRGB(ClientTickHandler.ticksInGame % 200 / 200F, 0.6F, 1F);
		Color colorRGB = new Color(color);

		int alpha = 32;
		float f = 1F / 16F;

		int totalAngles = 360;
		int drawAngles = 360;
		int step = totalAngles / drawAngles;

		radius -= f;
		IVertexBuilder buffer = buffers.getBuffer(CIRCLE);
		Matrix4f mat = ms.peek().getModel();
		buffer.vertex(mat, 0, f, 0).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();
		for(int i = 0; i < totalAngles + 1; i += step) {
			double rad = (totalAngles - i) * Math.PI / 180.0;
			float xp = (float) (Math.cos(rad) * radius);
			float zp = (float) (Math.sin(rad) * radius);
			buffer.vertex(mat, xp, f, zp).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();
		}
		buffer.vertex(mat, 0, f, 0).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();

		radius += f;
		float f1 = f + f / 4F;
		alpha = 64;
		buffer.vertex(mat, 0, f1, 0).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();
		for(int i = 0; i < totalAngles + 1; i += step) {
			double rad = (totalAngles - i) * Math.PI / 180.0;
			float xp = (float) (Math.cos(rad) * radius);
			float zp = (float) (Math.sin(rad) * radius);
			buffer.vertex(mat, xp, f1, zp).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();
		}
		buffer.vertex(mat, 0, f1, 0).color(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue(), alpha).endVertex();
		ms.pop();
	}

}
