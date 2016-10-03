package io.github.kyzderp.kyzray;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.PreRenderListener;
import com.mumfrey.liteloader.core.LiteLoader;

public class LiteModKyzray implements PostRenderListener, OutboundChatFilter, PreRenderListener 
{
	private boolean xrayOn;
	private Kyzray kyzray;
	private SeeThrough seeThrough;
	private static KeyBinding seeThroughBinding;

	@Override
	public String getName() { return "Kyzray"; }

	@Override
	public String getVersion() { return "1.4.0"; }

	@Override
	public void init(File configPath) 
	{
		this.xrayOn = false;
		this.kyzray = new Kyzray();
		this.seeThrough = new SeeThrough();
		LiteModKyzray.seeThroughBinding = new KeyBinding("key.kyzray.seethrough", Keyboard.CHAR_NONE, "key.categories.litemods");
		LiteLoader.getInput().registerKeyBinding(LiteModKyzray.seeThroughBinding);
	}

	@Override
	public void upgradeSettings(String version, File configPath,
			File oldConfigPath) {}

	/**
	 * Draw the xrayed blocks.
	 */
	@Override
	public void onPostRenderEntities(float partialTicks) 
	{
		
		
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
	public void onRenderTerrain(float partialTicks, int pass) 
	{
		if (Minecraft.getMinecraft().currentScreen == null 
				&& Keyboard.isKeyDown(LiteModKyzray.seeThroughBinding.getKeyCode()))
		{
			this.seeThrough.setObliqueNearPlaneClip(0.0f, 0.0f, -1.0f);
		}
	}
	


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
					LiteModKyzray.logMessage("Xray: ON", true);
				}
				else if (tokens[1].equalsIgnoreCase("off"))
				{
					this.xrayOn = false;
					LiteModKyzray.logMessage("Xray: OFF", true);
				}
				else if (tokens[1].equalsIgnoreCase("clear"))
				{
					this.kyzray.setToFind(null);
					this.kyzray.clearBlockList();
					this.kyzray.reload(false);
					LiteModKyzray.logMessage("Xray display cleared", true);
				}
				else if (tokens[1].equalsIgnoreCase("reload") || tokens[1].equalsIgnoreCase("update"))
				{
					LiteModKyzray.logMessage("Reloading Kyzray...", true);
					this.kyzray.reload(false);
				}
				else if (tokens[1].equalsIgnoreCase("radius") || tokens[1].equalsIgnoreCase("r")) // set radius
				{
					if (tokens.length == 2)
						LiteModKyzray.logMessage("Xray radius is currently " + this.kyzray.getRadius(), true);
					else
					{
						if (!tokens[2].matches("[0-9]+"))
							LiteModKyzray.logError("\"" + tokens[2] + "\" is not a valid integer!");
						else
							this.kyzray.setRadius(Integer.parseInt(tokens[2]));
					}
				}
				else if (tokens[1].equalsIgnoreCase("area"))
				{
					if (tokens.length == 2)
					{
						this.kyzray.setAreaDisplay(!this.kyzray.getAreaDisplay());
						LiteModKyzray.logMessage("Area display toggled: " + this.kyzray.getAreaDisplay(), true);
					}
					else
					{
						if (tokens[2].equalsIgnoreCase("on"))
						{
							this.kyzray.setAreaDisplay(true);
							LiteModKyzray.logMessage("Area display: ON", true);
						}
						else if (tokens[2].equalsIgnoreCase("off"))
						{
							this.kyzray.setAreaDisplay(false);
							LiteModKyzray.logMessage("Area display: OFF", true);
						}
						else
							LiteModKyzray.logError("Usage: /kr area [on|off]");
					}
				}
				else if (tokens[1].equalsIgnoreCase("lag"))
				{
					this.kyzray.reload(true);
				}
				else if (tokens[1].equalsIgnoreCase("block"))
				{
					LiteModKyzray.logError("Usage: /kr <block[,block]> [y1 y2]");
				}
				else if (tokens[1].equalsIgnoreCase("dist"))
				{
					try {
						this.seeThrough.setDistance(Float.parseFloat(tokens[2]));
						LiteModKyzray.logMessage("See-through distance set to " + tokens[2] + " blocks.", true);
					} catch (Exception e) {
						LiteModKyzray.logError("Second argument must be a positive number!");
					}
				}
				else if (tokens[1].equalsIgnoreCase("help"))
				{
					String[] commands = {"on - Turn on xraying. Duh.", 
							"off - Turn off xraying.",
							"clear - Clear the display but keep xray on.",
							"<block[,block]> - Blocks to xray for, see /kr block for more.",
							"reload|update - Displays new xray area.",
							"radius|r [radius] - Displays radius or sets new radius.",
							"area [on|off] - Toggle/on/off the display for xray area.",
							"lag - Look for possible lag sources.",
							"dist <#> - The distance to see through",
							"help - This help message. Hurrdurr."};
					LiteModKyzray.logMessage(this.getName() + " \u00A78[\u00A72v" + this.getVersion() + "\u00A78] \u00A7acommands", false);
					for (String command: commands)
						LiteModKyzray.logMessage("/kr " + command, false);
				}
				else // set the block to xray for
				{
					String result = this.kyzray.setToFind(tokens[1]);
					LiteModKyzray.logMessage(result, true);
					if (result.matches("Now xraying for.*"))
					{
						if (tokens.length == 4)
						{
							if (tokens[2].matches("-?[0-9]*") && tokens[3].matches("-?[0-9]*"))
								this.kyzray.reload(Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), false);
							else
								LiteModKyzray.logError("Invalid integer. Usage: /kr <block(s)> [y1] [y2]");
						}
						else
							this.kyzray.reload(false);
					}
				}
			}
			else // display version and help command
			{
				LiteModKyzray.logMessage("Kyzray \u00A78[\u00A72v" + this.getVersion() + "\u00A78] \u00A7aby Kyzeragon", false);
				LiteModKyzray.logMessage("Type \u00A72/kr help \u00A7afor commands", false);
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
	{// "\u00A78[\u00A72Kyzray\u00A78] \u00A7a"
		if (addPrefix)
			message = "\u00A78[\u00A72Kyzray\u00A78] \u00A7a" + message;
		TextComponentString displayMessage = new TextComponentString(message);
		displayMessage.setStyle((new Style()).setColor(TextFormatting.GREEN));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	/**
	 * Logs the error message to the user
	 * @param message The error message to log
	 */
	public static void logError(String message)
	{
		TextComponentString displayMessage = new TextComponentString("\u00A78[\u00A74!\u00A78] \u00A7c" + message + " \u00A78[\u00A74!\u00A78]");
		displayMessage.setStyle((new Style()).setColor(TextFormatting.RED));
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}

	@Override
	public void onRenderWorld(float partialTicks) {}
	@Override
	public void onSetupCameraTransform(float partialTicks, int pass, long timeSlice) {}
	@Override
	public void onRenderSky(float partialTicks, int pass) {}
	@Override
	public void onRenderClouds(float partialTicks, int pass, RenderGlobal renderGlobal) {}
	@Override
	public void onPostRender(float partialTicks) {}

}
