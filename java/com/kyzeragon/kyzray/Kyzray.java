package com.kyzeragon.kyzray;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class Kyzray {

	private LinkedList<Block> blocksToFind;
	private int radius;
	private LinkedList<XrayBlock> blockList;
	private boolean displayArea;
	private int minX, minZ, maxX, maxZ, maxY;

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
		if (this.blocksToFind.size() == 0)
			return;
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
		tess.addVertex(this.minX, 0, this.minZ);
		tess.addVertex(this.maxX, 0, this.minZ);
		tess.addVertex(this.maxX, 0, this.maxZ);
		tess.addVertex(this.minX, 0, this.maxZ);
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
		tess.addVertex(this.minX, 0, this.minZ);
		tess.addVertex(this.minX, this.maxY, this.minZ);

		tess.addVertex(this.maxX, 0, this.minZ);
		tess.addVertex(this.maxX, this.maxY, this.minZ);

		tess.addVertex(this.maxX, 0, this.maxZ);
		tess.addVertex(this.maxX, this.maxY, this.maxZ);

		tess.addVertex(this.minX, 0, this.maxZ);
		tess.addVertex(this.minX, this.maxY, this.maxZ);
		tess.draw();
	}

	/**
	 * Finds the list of blocks in the radius
	 * @return The list of blocks
	 */
	private LinkedList<XrayBlock> getBlockList()
	{
		//		this.logMessage("Seeing through the world...");
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().theWorld;
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		this.minX = (int) (player.posX - this.radius);
		this.maxX = (int) (player.posX + this.radius);		
		this.minZ = (int) (player.posZ - this.radius);
		this.maxZ = (int) (player.posZ + this.radius);
		this.maxY = 255;
		int[] blockCounts = new int[this.blocksToFind.size()];
		for (int i = 0; i < this.blocksToFind.size(); i++)
			blockCounts[i] = 0;

		if (this.blocksToFind.contains(Block.getBlockById(0)))
		{
			if (this.blocksToFind.size() == 1)
				this.maxY = 63;
			else
			{
				this.logError("Omitting air from multi-block xray search. Xray for air alone to find underground structures.");
				this.blocksToFind.remove(Block.getBlockById(0));
			}
		}
		for (int x = this.minX; x < this.maxX; x++)
		{
			for (int y = 0; y < this.maxY; y++)
			{
				for (int z = this.minZ; z < this.maxZ; z++)
				{
					for (int i = 0; i < this.blocksToFind.size(); i++)
					{
						Block currentBlock = world.getBlock(x, y, z);
						if (currentBlock.getLocalizedName().equals(this.blocksToFind.get(i).getLocalizedName()))
						{
							toReturn.add(new XrayBlock(x, y, z, currentBlock.getMapColor(0).colorValue));
							blockCounts[i]++;
						}
					}
				}
			}
		}
		if (this.blocksToFind.size() == 1
				&& Block.isEqualTo(this.blocksToFind.get(0), Block.getBlockById(0))) // if air
		{
			this.logMessage("Found " + toReturn.size() + " air blocks in " + this.radius
					+ " block radius from bedrock to sea level. Cuz I assume you're not xraying the sky.");
		}
		else
		{
			String message = "Found";
			for (int i = 0; i < this.blocksToFind.size(); i++)
				message += " " + blockCounts[i] + " " + this.blocksToFind.get(i).getLocalizedName() + ",";
			this.logMessage(message.substring(0, message.length() - 1) 
					+ " in " + this.radius + " block radius from bedrock to sky.");
		}
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

	/**
	 * "Reloads" the list of blocks to display.
	 */
	public void reload()
	{
		if (this.blocksToFind.size() > 0)
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
		else if (theRadius > 64)
			this.logError(theRadius + " is too large! Maximum allowed radius is 64.");
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
	public void setAreaDisplay(boolean isOn)
	{
		this.displayArea = isOn;
	}

	/**
	 * Getter for the block to find
	 * @return name of the block being xrayed for
	 */
	//	public String getToFind() { return this.blockToFind.getLocalizedName(); }

	/**
	 * Getter for the radius to look in
	 * @return radius being xrayed in
	 */
	public int getRadius() { return this.radius; }

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

	public boolean getAreaDisplay() { return this.displayArea; }
}
