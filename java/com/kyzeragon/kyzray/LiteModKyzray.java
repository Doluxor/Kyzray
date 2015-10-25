package com.kyzeragon.kyzray;

import java.io.File;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;

public class LiteModKyzray implements PostRenderListener, OutboundChatFilter 
{

	private boolean xrayOn;
	private Kyzray kyzray;

	@Override
	public String getName() { return "Kyzray"; }

	@Override
	public String getVersion() { return "1.1.1"; }

	@Override
	public void init(File configPath) 
	{
		this.xrayOn = false;
		this.kyzray = new Kyzray();
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

		Tessellator tess = Tessellator.getInstance();
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
	public boolean onSendChatMessage(String message) 
	{
		// TODO: /kr sign -> search words?
		// TODO: use x,z params? or pos?
		String[] tokens = message.split(" ");
		if (tokens.length > 0 && tokens[0].equalsIgnoreCase("/kr"))
		{
			if (tokens.length > 1)
			{
				if (tokens[1].equalsIgnoreCase("on"))
				{
					this.xrayOn = true;
					this.logMessage("Xray: ON", true);
				}
				else if (tokens[1].equalsIgnoreCase("off"))
				{
					this.xrayOn = false;
					this.logMessage("Xray: OFF", true);
				}
				else if (tokens[1].equalsIgnoreCase("clear"))
				{
					this.kyzray.setToFind(null);
					this.kyzray.clearBlockList();
					this.kyzray.reload(false);
					this.logMessage("Xray display cleared", true);
				}
				else if (tokens[1].equalsIgnoreCase("reload") || tokens[1].equalsIgnoreCase("update"))
				{
					this.logMessage("Reloading Kyzray...", true);
					this.kyzray.reload(false);
				}
				else if (tokens[1].equalsIgnoreCase("radius") || tokens[1].equalsIgnoreCase("r")) // set radius
				{
					if (tokens.length == 2)
						this.logMessage("Xray radius is currently " + this.kyzray.getRadius(), true);
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
						this.logMessage("Area display toggled: " + this.kyzray.getAreaDisplay(), true);
					}
					else
					{
						if (tokens[2].equalsIgnoreCase("on"))
						{
							this.kyzray.setAreaDisplay(true);
							this.logMessage("Area display: ON", true);
						}
						else if (tokens[2].equalsIgnoreCase("off"))
						{
							this.kyzray.setAreaDisplay(false);
							this.logMessage("Area display: OFF", true);
						}
						else
							this.logError("Usage: /kr area [on|off]");
					}
				}
				else if (tokens[1].equalsIgnoreCase("lag"))
				{
					this.kyzray.reload(true);
				}
				else if (tokens[1].equalsIgnoreCase("block"))
				{
					this.logError("Usage: /kr <block[,block]> [y1 y2]");
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
					this.logMessage(this.getName() + " §8[§2v" + this.getVersion() + "§8] §acommands", false);
					for (String command: commands)
						this.logMessage("/kr " + command, false);
					IChatComponent link = new ChatComponentText("Click here for the wiki!");
					ChatStyle linkStyle = new ChatStyle();
					linkStyle.setColor(EnumChatFormatting.DARK_GREEN);
					linkStyle.setChatClickEvent(new ClickEvent(Action.OPEN_URL, "https://github.com/Kyzderp/Kyzray/wiki"));
					link.setChatStyle(linkStyle);
					Minecraft.getMinecraft().thePlayer.addChatComponentMessage(link);
				}
				else // set the block to xray for
				{
					String result = this.kyzray.setToFind(tokens[1]);
					this.logMessage(result, true);
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
				this.logMessage("Kyzray §8[§2v" + this.getVersion() + "§8] §aby Kyzeragon", false);
				this.logMessage("Type §2/kr help §afor commands", false);
			}
			return false;
		}
		return true;
	}

	/**
	 * Logs the message to the user
	 * @param message The message to log
	 * @param addPrefix Whether to add the mod-specific prefix or not
	 */
	public static void logMessage(String message, boolean addPrefix)
	{// "§8[§2Kyzray§8] §a"
		if (addPrefix)
			message = "§8[§2Kyzray§8] §a" + message;
		ChatComponentText displayMessage = new ChatComponentText(message);
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message)
	{
		ChatComponentText displayMessage = new ChatComponentText("§8[§4!§8] §c" + message + " §8[§4!§8]");
		displayMessage.setChatStyle((new ChatStyle()).setColor(EnumChatFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}
}
