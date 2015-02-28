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
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;

public class LiteModKyzray implements PostRenderListener, OutboundChatListener, ChatFilter {

	private boolean xrayOn;
	private Kyzray kyzray;
	private boolean sentCmd;

	private ChatStyle style;
	private ChatComponentText displayMessage;

	@Override
	public String getName() { return "Kyzray"; }

	@Override
	public String getVersion() { return "1.0.0"; }

	@Override
	public void init(File configPath) 
	{
		this.xrayOn = false;
		this.kyzray = new Kyzray();
		this.sentCmd = false;

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
		// TODO: /kr sign -> search words?
		// TODO: use x,z params? or pos?
		String[] tokens = message.split(" ");
		if (tokens.length > 0 && tokens[0].equalsIgnoreCase("/kr"))
		{
			this.sentCmd = true;
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
				else if (tokens[1].equalsIgnoreCase("clear"))
				{
					this.kyzray.setToFind(null);
					this.kyzray.reload(false);
					this.logMessage("Xray display cleared");
				}
				else if (tokens[1].equalsIgnoreCase("reload") || tokens[1].equalsIgnoreCase("update"))
				{
					this.logMessage("Reloading Kyzray...");
					this.kyzray.reload(false);
				}
				else if (tokens[1].equalsIgnoreCase("radius") || tokens[1].equalsIgnoreCase("r")) // set radius
				{
					if (tokens.length == 2)
						this.logMessage("Xray radius is currently " + this.kyzray.getRadius());
					else
					{
						if (!tokens[2].matches("[0-9]+"))
							this.logError("\"" + tokens[2] + "\" is not a valid integer!");
						else
							this.kyzray.setRadius(Integer.parseInt(tokens[2]));
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
							this.logError("Usage: /kr area [on|off]");
					}
				}
				else if (tokens[1].equalsIgnoreCase("lag"))
				{
					this.kyzray.reload(true);
					this.kyzray.drawXray();
				}
				else if (tokens[1].equalsIgnoreCase("block"))
				{
					this.logMessage("Usage: /kr <block[,block]> [y1 y2]");
				}
				else if (tokens[1].equalsIgnoreCase("help"))
				{
					String[] commands = {"on - Turn on xraying. Duh.", 
							"off - Turn off xraying.",
							"clear - Clear the display but keep xray on.",
							"block[,block] - Blocks to xray for, see /kr block for more.",
							"reload|update - Displays new xray area.",
							"radius|r [radius] - Displays radius or sets new radius.",
							"area [on|off] - Toggle/on/off the display for xray area.",
							"lag - Look for possible lag sources.",
							"help - This help message. Hurrdurr."};
					this.logMessage(this.getName() + " [v" + this.getVersion() + "] commands");
					for (String command: commands)
						this.logMessage("/kr " + command);
				}
				else // set the block to xray for
				{
					String result = this.kyzray.setToFind(tokens[1]);
					this.logMessage(result);
					if (result.matches("Now xraying for.*"))
					{
						if (tokens.length == 4)
						{
							if (tokens[2].matches("-?[0-9]*") && tokens[3].matches("-?[0-9]*"))
								this.kyzray.reload(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), false);
							else
								this.logError("Invalid integer. Usage: /kr <block(s)> [y1] [y2]");
						}
						else
							this.kyzray.reload(false);
					}
				}
			}
			else // display version and help command
			{
				this.logMessage("Kyzray [v" + this.getVersion() + "] by Kyzeragon");
				this.logMessage("Type /kr help for commands");
			}
		}
	}

	/**
	 * Chat filter to not get Unknown command error... bleh
	 */
	@Override
	public boolean onChat(S02PacketChat chatPacket, IChatComponent chat,
			String message) {
		if (message.matches(".*nknown.*ommand.*") && this.sentCmd)
		{
			this.sentCmd = false;
			return false;
		}
		return true;
	}

	/**
	 * Helper to log a message to the user
	 * @param message Message to be logged
	 */
	private void logMessage(String message)
	{
		this.style.setColor(EnumChatFormatting.AQUA);
		this.displayMessage = new ChatComponentText(message);
		this.displayMessage.setChatStyle(style);
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Helper to log error to user in red text
	 * @param message Message to be logged
	 */
	private void logError(String message)
	{
		this.style.setColor(EnumChatFormatting.RED);
		this.displayMessage = new ChatComponentText(message);
		this.displayMessage.setChatStyle(style);
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}


}
