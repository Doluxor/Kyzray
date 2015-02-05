package com.kyzeragon.kyzray;

import java.io.File;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;

public class LiteModKyzray implements PostRenderListener, OutboundChatListener {

	private boolean xrayOn;
	private Kyzray kyzray;
	
	ChatStyle style;
	ChatComponentText displayMessage;

	@Override
	public String getName() { return "Kyzray"; }

	@Override
	public String getVersion() { return "0.9.0"; }

	@Override
	public void init(File configPath) 
	{
		this.xrayOn = false;
		this.kyzray = new Kyzray();
		
		this.style = new ChatStyle();
		this.style.setColor(EnumChatFormatting.AQUA);
	}

	@Override
	public void upgradeSettings(String version, File configPath,
			File oldConfigPath) {}

	/**
	 * Draw the xrayed blocks.
	 */
	@Override
	public void onPostRenderEntities(float partialTicks) {
		if (!this.xrayOn)
			return;
		
		RenderHelper.disableStandardItemLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_DEPTH_TEST);

		boolean foggy = GL11.glIsEnabled(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_FOG);

		GL11.glPushMatrix();

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		GL11.glTranslated(-(player.prevPosX + (player.posX - player.prevPosX) * partialTicks),
				-(player.prevPosY + (player.posY - player.prevPosY) * partialTicks),
				-(player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks));
		
		Tessellator tess = Tessellator.instance;
		this.kyzray.drawXray();
		if (this.kyzray.getAreaDisplay())
		this.kyzray.drawArea();

		GL11.glPopMatrix();

		// Only re-enable fog if it was enabled before we messed with it.
		// Or else, fog is *all* you'll see with Optifine.
		if (foggy)
			GL11.glEnable(GL11.GL_FOG);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);

		RenderHelper.enableStandardItemLighting();
	}

	@Override
	public void onPostRender(float partialTicks) {}

	/**
	 * Handles player's /kr commands
	 */
	@Override
	public void onSendChatMessage(C01PacketChatMessage packet, String message) {
		// see https://github.com/totemo/watson/blob/0.7.0-1.7.2_02/src/watson/LiteModWatson.java
		String[] tokens = message.split(" ");
		if (tokens.length > 0 && tokens[0].equalsIgnoreCase("/kr"))
		{
			if (tokens.length > 1)
			{
				if (tokens[1].equalsIgnoreCase("on"))
				{
					this.xrayOn = true;
					this.logMessage("Xray: ON");
				}
				else if (tokens[1].equalsIgnoreCase("off"))
				{
					this.xrayOn = false;
					this.logMessage("Xray: OFF");
				}
				else if (tokens[1].equalsIgnoreCase("reload") || tokens[1].equalsIgnoreCase("update"))
				{
					this.logMessage("Reloading Kyzray...");
					this.kyzray.reload();
				}
				else if (tokens[1].equalsIgnoreCase("radius") || tokens[1].equalsIgnoreCase("r")) // set radius
				{
					if (tokens.length == 2)
						this.logMessage("Xray radius is currently " + this.kyzray.getRadius());
					else
					{
						if (!tokens[2].matches("[0-9]+"))
							this.logMessage("\"" + tokens[2] + "\" is not a valid integer!");
						else
							this.logMessage(this.kyzray.setRadius(Integer.parseInt(tokens[2])));
					}
				}
				else if (tokens[1].equalsIgnoreCase("area"))
				{
					if (tokens.length == 2)
					{
						this.kyzray.setAreaDisplay(!this.kyzray.getAreaDisplay());
						this.logMessage("Area display toggled: " + this.kyzray.getAreaDisplay());
					}
					else
					{
						if (tokens[2].equalsIgnoreCase("on"))
						{
							this.kyzray.setAreaDisplay(true);
							this.logMessage("Area display: ON");
						}
						else if (tokens[2].equalsIgnoreCase("off"))
						{
							this.kyzray.setAreaDisplay(false);
							this.logMessage("Area display: OFF");
						}
						else
							this.logMessage("Your parameter makes no sense. Fix pl0x.");
					}
				}
				else if (tokens[1].equalsIgnoreCase("help"))
				{
					String[] commands = {"<on> - Turn on xraying. Duh.", 
							"<off> - Turn off xraying.",
							"<reload|update> - Displays new xray area.",
							"<radius|r> [radius] - Displays current radius or sets new radius.",
							"<help> - This help message. Hurrdurr.",
							"<area> [on|off] - Toggle/on/off the display for xray area"};
					this.logMessage("Kyzray [v" + this.getVersion() + "] commands");
					for (String command: commands)
					{
						this.logMessage("/kr " + command);
					}
				}
				else // set the block to xray for
					this.logMessage(this.kyzray.setToFind(tokens[1]));
			}
			else // display version and help command
			{
				this.logMessage("Kyzray [v" + this.getVersion() + "]");
				this.logMessage("Type /kr help for commands");
			}
		}
	}
	
	/**
	 * Helper to log a message to the user
	 * @param message Message to be logged
	 */
	private void logMessage(String message)
	{
		this.displayMessage = new ChatComponentText(message);
		this.displayMessage.setChatStyle(style);
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}
}
