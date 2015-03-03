package com.kyzeragon.kyzray;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;

public class Kyzray {

	private LinkedList<Block> blocksToFind;
	private int radius;
	private LinkedList<XrayBlock> blockList;
	private boolean displayArea;
	private int minX, minZ, maxX, maxZ, minY, maxY;

	ChatStyle style;
	ChatComponentText displayMessage;

	public Kyzray()
	{
		this.blocksToFind = new LinkedList<Block>();
		this.radius = 32;
		this.displayArea = false;

		this.style = new ChatStyle();
	}

	/**
	 * Draws the blocks being xrayed for
	 */
	public void drawXray()
	{
		if (this.blockList == null)
			return;
		Tessellator tess = Tessellator.instance;
		int totalBlocks = 0;
		for (XrayBlock block: blockList)
		{
			block.drawBlock(tess);
			totalBlocks++;
			if (totalBlocks > 15000)
				return;
		}
	}

	/**
	 * Draws a box around the area that had been searched in
	 */
	public void drawArea()
	{
		if (this.blocksToFind.size() == 0)
			return;
		Tessellator tess = Tessellator.instance;
		GL11.glLineWidth(5.0f);
		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_F(1.0F, 0.0F, 0.0F, 0.3F);
		tess.addVertex(this.minX, this.minY, this.minZ);
		tess.addVertex(this.maxX, this.minY, this.minZ);
		tess.addVertex(this.maxX, this.minY, this.maxZ);
		tess.addVertex(this.minX, this.minY, this.maxZ);
		tess.draw();

		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_F(1.0F, 0.0F, 0.0F, 0.3F);
		tess.addVertex(this.minX, this.maxY, this.minZ);
		tess.addVertex(this.maxX, this.maxY, this.minZ);
		tess.addVertex(this.maxX, this.maxY, this.maxZ);
		tess.addVertex(this.minX, this.maxY, this.maxZ);
		tess.draw();

		tess.startDrawing(GL11.GL_LINES);
		tess.setColorRGBA_F(1.0F, 0.0F, 0.0F, 0.3F);
		tess.addVertex(this.minX, this.minY, this.minZ);
		tess.addVertex(this.minX, this.maxY, this.minZ);

		tess.addVertex(this.maxX, this.minY, this.minZ);
		tess.addVertex(this.maxX, this.maxY, this.minZ);

		tess.addVertex(this.maxX, this.minY, this.maxZ);
		tess.addVertex(this.maxX, this.maxY, this.maxZ);

		tess.addVertex(this.minX, this.minY, this.maxZ);
		tess.addVertex(this.minX, this.maxY, this.maxZ);
		tess.draw();
	}

	private LinkedList<XrayBlock> findLagWater()
	{
		this.logMessage("Looking for 'L' shaped water... this may take a while.");
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().theWorld;
		Block water = Block.getBlockFromName("water");
		int blue = water.getMapColor(0).colorValue;
		int red = 0xFF0000;
		int numLag = 0;
		if (this.maxY > 254)
			this.maxY = 254;

		for (int x = this.minX; x < this.maxX; x++)
		{
			for (int z = this.minZ; z < this.maxZ; z++)
			{
				for (int y = this.minY; y < this.maxY; y++)
				{
					Block currentBlock = world.getBlock(x, y, z);

					if (currentBlock.equals(water))
					{
						boolean aboveWater = true;
						for (int i = 1; i < 10; i++) // check if above 9 blocks are water
						{
							if (!world.getBlock(x, y + i, z).equals(water))
							{
								aboveWater = false;
								break;
							}
						}
						if (aboveWater) // check if above is water
						{
							boolean isL = false; // TODO: null pointers?
							if (world.getBlock(x - 1, y, z).equals(water) 
									&& !world.getBlock(x - 1, y + 1, z).equals(water))
							{
								toReturn.add(new XrayBlock(x - 1, y, z, blue, true));
								isL = true;
							}
							if (world.getBlock(x + 1, y, z).equals(water)
									&& !world.getBlock(x + 1, y + 1, z).equals(water))
							{
								toReturn.add(new XrayBlock(x + 1, y, z, blue, true));
								isL = true;
							}
							if (world.getBlock(x, y, z - 1).equals(water)
									&& !world.getBlock(x, y + 1, z - 1).equals(water))
							{
								toReturn.add(new XrayBlock(x, y, z - 1, blue, true));
								isL = true;
							}
							if (world.getBlock(x, y, z + 1).equals(water)
									&& !world.getBlock(x, y + 1, z + 1).equals(water))
							{
								toReturn.add(new XrayBlock(x, y, z + 1, blue, true));
								isL = true;
							}

							if (isL == true)
							{
								numLag++;
								toReturn.add(new XrayBlock(x, y, z, red, false));
								for (int i = y + 1; i < 256; i++)
								{
									if (world.getBlock(x, i, z).equals(water))
										toReturn.add(new XrayBlock(x, i, z, blue, true));
									else
										break;
								}
							}
						}
					}
				}
			}
		}
		this.logMessage("Found " + numLag + " possible lag sources in " 
				+ this.radius + " block radius from Y=" + this.minY + " to Y=" + this.maxY);
		return toReturn;
	}


	/**
	 * Finds the list of blocks in the radius
	 * @return The list of blocks
	 */
	private LinkedList<XrayBlock> getBlockList()
	{
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().theWorld;

		int[] blockCounts = new int[this.blocksToFind.size()];
		for (int i = 0; i < this.blocksToFind.size(); i++)
			blockCounts[i] = 0;

		if (this.blocksToFind.contains(Block.getBlockById(0)) && this.blocksToFind.size() > 1)
		{
			this.logError("Omitting air from multi-block xray search. Xray for air alone to find underground structures.");
			this.blocksToFind.remove(Block.getBlockById(0));
		}
		for (int x = this.minX; x < this.maxX; x++)
		{
			for (int z = this.minZ; z < this.maxZ; z++)
			{
				for (int y = this.minY; y < this.maxY; y++)
				{
					for (int i = 0; i < this.blocksToFind.size(); i++)
					{
						Block currentBlock = world.getBlock(x, y, z);
						if (currentBlock.equals(this.blocksToFind.get(i)))
							//						if (currentBlock.getLocalizedName().equals(this.blocksToFind.get(i).getLocalizedName()))
						{
							toReturn.add(new XrayBlock(x, y, z, currentBlock.getMapColor(0).colorValue, false));
							blockCounts[i]++;
						}
					}
				}
			}
		}
		String message = "Found";
		for (int i = 0; i < this.blocksToFind.size(); i++)
			message += " " + blockCounts[i] + " " + this.blocksToFind.get(i).getLocalizedName() + ",";
		this.logMessage(message.substring(0, message.length() - 1) 
				+ " in " + this.radius + " block radius from Y=" + this.minY + " to Y=" + this.maxY);
		if (toReturn.size() > 15000)
		{
			this.logError("Too many blocks! Displaying more will lag.");
			this.logError("Displayed 15,000 blocks out of " + toReturn.size() + ".");
		}
		return toReturn;
	}

	/**
	 * Attempts to set the block to xray for
	 * @param blockToFind The block to attempt to xray for
	 * @return The message to display to the user depending on if successfully set
	 */
	public String setToFind(String blockString)
	{
		this.blocksToFind.clear();
		if (blockString == null)
			return "";
		blockString = blockString.toLowerCase();
		String[] blocks = blockString.split(",");
		String toReturn = "Now xraying for blocks:";
		for (String findBlock: blocks)
		{
			if (findBlock.equals("sign"))
				findBlock = "wall_sign";
			if (Block.blockRegistry.containsKey(findBlock))
			{
				if (!this.blocksToFind.contains((Block)Block.blockRegistry.getObject(findBlock)))
				{
					this.blocksToFind.add((Block)Block.blockRegistry.getObject(findBlock));
					toReturn += " " + this.blocksToFind.getLast().getLocalizedName();
				}
			}
			else if (findBlock.matches("[0-9]+")
					&& Block.blockRegistry.containsID(Integer.parseInt(findBlock)))
			{
				if (!this.blocksToFind.contains((Block)Block.blockRegistry.getObjectForID(Integer.parseInt(findBlock))))
				{
					this.blocksToFind.add((Block)Block.blockRegistry.getObjectForID(Integer.parseInt(findBlock)));
					toReturn += " " + this.blocksToFind.getLast().getLocalizedName();
				}
			}
			else
				return "No such block ID/name as \"" + findBlock + "\"";
		}
		return toReturn;
	}

	public void searchSign()
	{
		this.setToFind("sign");

	}

	/**
	 * "Reloads" the list of blocks to display.
	 */
	public void reload(boolean lag)
	{
		if (this.blocksToFind.size() < 1 && !lag)
			return;
		this.reload(0, 255, lag);
	}

	/**
	 * "Reloads" the list of blocks to display, with y parameters.
	 * @param y1 y coordinate 1
	 * @param y2 y coordinate 2
	 */
	public void reload(int y1, int y2, boolean lag)
	{
		if (this.blocksToFind.size() < 1 && !lag)
			return;
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		this.reload((int)(player.posX - this.radius), (int)(player.posX + this.radius),
				y1, y2, (int)(player.posZ - this.radius), (int)(player.posZ + this.radius), lag);
	}

	/**
	 * "Reloads the list of blocks to display, with full parameters.
	 * @param x1 x coord 1
	 * @param x2 x coord 2
	 * @param y1 y coord 1
	 * @param y2 y coord 2
	 * @param z1 z coord 1
	 * @param z2 z coord 2
	 */
	public void reload(int x1, int x2, int y1, int y2, int z1, int z2, boolean lag)
	{
		if (this.blocksToFind.size() < 1 && !lag)
			return;
		this.minX = Math.min(x1, x2);
		this.maxX = Math.max(x1, x2);
		this.minY = Math.min(y1, y2);
		this.maxY = Math.max(y1, y2);
		this.minZ = Math.min(z1, z2);
		this.maxZ = Math.max(z1, z2);
		if (lag)
			this.blockList = this.findLagWater();
		else
			this.blockList = this.getBlockList();
	}

	/**
	 * Setter for the radius
	 * @param theRadius The radius to xray in
	 * @return The error/success message to display to the user
	 */
	public void setRadius(int theRadius) 
	{ 
		if (theRadius < 1)
			this.logError("You can't have a radius that small, uDerp.");
		else if (theRadius > 128)
			this.logError(theRadius + " is too large! Maximum allowed radius is 128.");
		else
		{
			this.radius = theRadius;
			this.logMessage("Xray radius set to " + theRadius);
		}
	}

	/**
	 * Sets whether area display is on or not
	 * @param isOn If area display is on
	 */
	public void setAreaDisplay(boolean isOn) { this.displayArea = isOn; }

	/**
	 * Getter for the radius to look in
	 * @return radius being xrayed in
	 */
	public int getRadius() { return this.radius; }

	/**
	 * Getter for if the area should be displayed
	 * @return whether the area should be displayed
	 */
	public boolean getAreaDisplay() { return this.displayArea; }
	
	/**
	 * Clears the blockList
	 */
	public void clearBlockList() { this.blockList.clear(); }


	///// PRIVATE METHODS /////

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
