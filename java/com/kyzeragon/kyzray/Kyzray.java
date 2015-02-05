package com.kyzeragon.kyzray;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class Kyzray {

	private Block blockToFind;
	private int radius;
	private LinkedList<XrayBlock> blockList;
	private boolean displayArea;
	private int minX, minZ, maxX, maxZ;
	
	ChatStyle style;
	ChatComponentText displayMessage;

	public Kyzray()
	{
		this.blockToFind = null;
		this.radius = 32;
		this.displayArea = false;
		
		this.style = new ChatStyle();
		this.style.setColor(EnumChatFormatting.AQUA);
	}

	/**
	 * Draws the blocks being xrayed for
	 */
	public void drawXray()
	{
		if (this.blockToFind == null)
			return;
		Tessellator tess = Tessellator.instance;
		int color = this.blockToFind.getMapColor(0).colorValue;
		for (XrayBlock block: blockList)
		{
			block.drawBlock(tess, color);
		}
	}
	
	/**
	 * Draws a box around the area that had been searched in
	 */
	public void drawArea()
	{
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
		tess.addVertex(this.minX, 255, this.minZ);
		tess.addVertex(this.maxX, 255, this.minZ);
		tess.addVertex(this.maxX, 255, this.maxZ);
		tess.addVertex(this.minX, 255, this.maxZ);
		tess.draw();
		
		tess.startDrawing(GL11.GL_LINES);
		tess.setColorRGBA_F(1.0F, 0.0F, 0.0F, 0.3F);
		tess.addVertex(this.minX, 0, this.minZ);
		tess.addVertex(this.minX, 255, this.minZ);

		tess.addVertex(this.maxX, 0, this.minZ);
		tess.addVertex(this.maxX, 255, this.minZ);

		tess.addVertex(this.maxX, 0, this.maxZ);
		tess.addVertex(this.maxX, 255, this.maxZ);

		tess.addVertex(this.minX, 0, this.maxZ);
		tess.addVertex(this.minX, 255, this.maxZ);
		tess.draw();
	}

	/**
	 * Finds the list of blocks in the radius
	 * @return The list of blocks
	 */
	private LinkedList<XrayBlock> getBlockList()
	{
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().theWorld;
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		this.minX = (int) (player.posX - this.radius);
		this.maxX = (int) (player.posX + this.radius);		
		this.minZ = (int) (player.posZ - this.radius);
		this.maxZ = (int) (player.posZ + this.radius);
		
		for (int x = this.minX; x < this.maxX; x++)
		{
			for (int y = 0; y < 256; y++)
			{
				for (int z = this.minZ; z < this.maxZ; z++)
				{
					Block currentBlock = world.getBlock(x, y, z);
					if (currentBlock.getLocalizedName().equals(this.blockToFind.getLocalizedName()))
					{
						toReturn.add(new XrayBlock(x, y, z));
					}
				}
			}
		}
		this.logMessage("Found " + toReturn.size() + " of " + this.blockToFind.getLocalizedName()
				+ " in " + this.radius + " block radius.");
		return toReturn;
	}

	/**
	 * Attempts to set the block to xray for
	 * @param blockToFind The block to attempt to xray for
	 * @return The message to display to the user depending on if successfully set
	 */
	public String setToFind(String blockToFind)
	{
		if (Block.blockRegistry.containsKey(blockToFind))
		{
			this.blockToFind = (Block)Block.blockRegistry.getObject(blockToFind);
			this.blockList = this.getBlockList();
			return "Now xraying for block " + blockToFind + " aka " + this.blockToFind.getLocalizedName();
		}
		else if (blockToFind.matches("[0-9]+")
				&& Block.blockRegistry.containsID(Integer.parseInt(blockToFind)))
		{
			this.blockToFind = (Block)Block.blockRegistry.getObjectForID(Integer.parseInt(blockToFind));
			this.blockList = this.getBlockList();
			return "Now xraying for block ID " + blockToFind + " aka " + this.blockToFind.getLocalizedName();
		}
		return "No such block ID/name as " + blockToFind;
	}

	/**
	 * "Reloads" the list of blocks to display.
	 */
	public void reload()
	{
		if (this.blockToFind != null)
			this.blockList = this.getBlockList();
	}

	/**
	 * Setter for the radius
	 * @param theRadius The radius to xray in
	 * @return The error/success message to display to the user
	 */
	public String setRadius(int theRadius) 
	{ 
		if (theRadius < 1)
			return "You can't have a radius that small, uDerp.";
		if (theRadius > 64)
			return theRadius + " is too large! Maximum allowed radius is 64.";
		this.radius = theRadius;
		this.reload();
		return "Xray radius set to " + theRadius;
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
	public String getToFind() { return this.blockToFind.getLocalizedName(); }

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
		this.displayMessage = new ChatComponentText(message);
		this.displayMessage.setChatStyle(style);
		Minecraft.getMinecraft().thePlayer.addChatComponentMessage(displayMessage);
	}
	
	public boolean getAreaDisplay() { return this.displayArea; }
}
