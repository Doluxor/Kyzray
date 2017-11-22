package io.github.kyzderp.kyzray;

import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;

import org.lwjgl.opengl.GL11;

import com.mumfrey.liteloader.gl.GL;

public class Kyzray {

	private LinkedList<Block> blocksToFind;
	private int radius;
	private LinkedList<XrayBlock> blockList;
	private boolean displayArea;
	private int minX, minZ, maxX, maxZ, minY, maxY;

	Style style;
	TextComponentString displayMessage;

	public Kyzray()
	{
		this.blocksToFind = new LinkedList<Block>();
		this.radius = 32;
		this.displayArea = false;

		this.style = new Style();
	}

	/**
	 * Draws the blocks being xrayed for
	 */
	public void drawXray()
	{
		if (this.blockList == null)
			return;
		Tessellator tess = Tessellator.getInstance();
		if (tess == null)
			return;
		int totalBlocks = 0;
		for (XrayBlock block: blockList)
		{
			if (block == null)
				return;
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
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder vbuf = tess.getBuffer();
		GL11.glLineWidth(5.0f);
		GL.glColor4f(1F, 0F, 0F, 0.5F);
		
		vbuf.begin(GL11.GL_LINE_LOOP, GL.VF_POSITION);
		vbuf.pos(this.minX, this.minY, this.minZ);
		vbuf.pos(this.maxX, this.minY, this.minZ);
		vbuf.pos(this.maxX, this.minY, this.maxZ);
		vbuf.pos(this.minX, this.minY, this.maxZ);
		tess.draw();

		vbuf.begin(GL11.GL_LINE_LOOP, GL.VF_POSITION);
		vbuf.pos(this.minX, this.maxY, this.minZ);
		vbuf.pos(this.maxX, this.maxY, this.minZ);
		vbuf.pos(this.maxX, this.maxY, this.maxZ);
		vbuf.pos(this.minX, this.maxY, this.maxZ);
		tess.draw();

		vbuf.begin(GL.GL_LINE, GL.VF_POSITION);
		vbuf.pos(this.minX, this.minY, this.minZ);
		vbuf.pos(this.minX, this.maxY, this.minZ);

		vbuf.pos(this.maxX, this.minY, this.minZ);
		vbuf.pos(this.maxX, this.maxY, this.minZ);

		vbuf.pos(this.maxX, this.minY, this.maxZ);
		vbuf.pos(this.maxX, this.maxY, this.maxZ);

		vbuf.pos(this.minX, this.minY, this.maxZ);
		vbuf.pos(this.minX, this.maxY, this.maxZ);
		tess.draw();
	}

	private LinkedList<XrayBlock> findLagWater()
	{
		LiteModKyzray.logMessage("Looking for 'L' shaped water... this may take a while.", true);
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().world;
		Block water = Block.getBlockFromName("water");
		int blue = 0x0000FF;
		int red = 0xAA0000;
		int orange = 0xFFAA00;
		int yellow = 0xFFFF55;
		int numLag = 0;
		int dLag = 0;
		int idkLag = 0;
		if (this.maxY > 254)
			this.maxY = 254;

		for (int x = this.minX; x < this.maxX; x++)
		{
			for (int z = this.minZ; z < this.maxZ; z++)
			{
				for (int y = this.minY; y < this.maxY; y++)
				{
					Block currentBlock = world.getBlockState(new BlockPos(x, y, z)).getBlock();

					if (currentBlock.equals(water))
					{
						boolean aboveWater = true;
						for (int i = 1; i < 10; i++) // check if above 9 blocks are water
						{
							if (!world.getBlockState(new BlockPos(x, y + 10 - i, z)).getBlock().equals(water))
							{ // check top-down: faster!
								aboveWater = false;
								break;
							}
						}
						if (aboveWater) // check if above is water
						{
							boolean isL = false; // TODO: null pointers?
							if (world.getBlockState(new BlockPos(x - 1, y, z)).getBlock().equals(water) 
									&& !world.getBlockState(new BlockPos(x - 1, y + 1, z)).getBlock().equals(water))
							{
								toReturn.add(new XrayBlock(x - 1, y, z, blue, true));
								isL = true;
							}
							if (world.getBlockState(new BlockPos(x + 1, y, z)).getBlock().equals(water)
									&& !world.getBlockState(new BlockPos(x + 1, y + 1, z)).getBlock().equals(water))
							{
								toReturn.add(new XrayBlock(x + 1, y, z, blue, true));
								isL = true;
							}
							if (world.getBlockState(new BlockPos(x, y, z - 1)).getBlock().equals(water)
									&& !world.getBlockState(new BlockPos(x, y + 1, z - 1)).getBlock().equals(water))
							{
								toReturn.add(new XrayBlock(x, y, z - 1, blue, true));
								isL = true;
							}
							if (world.getBlockState(new BlockPos(x, y, z + 1)).getBlock().equals(water)
									&& !world.getBlockState(new BlockPos(x, y + 1, z + 1)).getBlock().equals(water))
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
									if (world.getBlockState(new BlockPos(x, i, z)).getBlock().equals(water))
										toReturn.add(new XrayBlock(x, i, z, blue, true));
									else
										break;
								}
							}
							else
							{
								// now check for same-level diagonal
								boolean isIDKL = false; // idk if lag :D
								if (world.getBlockState(new BlockPos(x - 1, y, z - 1)).getBlock().equals(water)
										&& !world.getBlockState(new BlockPos(x - 1, y + 1, z - 1)).getBlock().equals(water))
								{
									toReturn.addFirst(new XrayBlock(x - 1, y, z - 1, yellow, false));
									isIDKL = true;
								}
								if (world.getBlockState(new BlockPos(x - 1, y, z + 1)).getBlock().equals(water)
										&& !world.getBlockState(new BlockPos(x - 1, y + 1, z + 1)).getBlock().equals(water))
								{
									toReturn.addFirst(new XrayBlock(x - 1, y, z + 1, yellow, false));
									isIDKL = true;
								}
								if (world.getBlockState(new BlockPos(x + 1, y, z - 1)).getBlock().equals(water)
										&& !world.getBlockState(new BlockPos(x + 1, y + 1, z - 1)).getBlock().equals(water))
								{
									toReturn.addFirst(new XrayBlock(x + 1, y, z - 1, yellow, false));
									isIDKL = true;
								}
								if (world.getBlockState(new BlockPos(x + 1, y, z + 1)).getBlock().equals(water)
										&& !world.getBlockState(new BlockPos(x + 1, y + 1, z + 1)).getBlock().equals(water))
								{
									toReturn.addFirst(new XrayBlock(x + 1, y, z + 1, yellow, false));
									isIDKL = true;
								}

								if (isIDKL)
								{
									idkLag++;
									toReturn.addFirst(new XrayBlock(x, y, z, yellow, false));
									for (int i = y + 1; i < 256; i++)
									{
										if (world.getBlockState(new BlockPos(x, i, z)).getBlock().equals(water))
											toReturn.addFirst(new XrayBlock(x, i, z, blue, true));
										else
											break;
									}
								}

								if (y > 0) // now check for diagonal lag water 1 level under
								{
									if (!world.getBlockState(new BlockPos(x, y - 1, z)).getBlock().equals(water))
									{ // below must be water
										boolean isDL = false;
										for (int ix = -1; ix <= 1; ix++)
										{
											for (int iz = -1; iz <= 1; iz++)
											{ // check the 9 just below the water, don't care about center
												if (world.getBlockState(new BlockPos(x + ix, y - 1, z + iz)).getBlock().equals(water)
														&& !world.getBlockState(new BlockPos(x + ix, y, z + iz)).getBlock().equals(water))
												{ // block above it can't be water
													isDL = true;
													toReturn.addFirst(new XrayBlock(x + ix, y - 1, z + iz, orange, false));
												}
											}
										}
										if (isDL)
										{
											toReturn.addFirst(new XrayBlock(x, y, z, orange, false));
											dLag++;
											for (int i = y + 1; i < 256; i++)
												if (world.getBlockState(new BlockPos(x, i, z)).getBlock().equals(water))
													toReturn.addFirst(new XrayBlock(x, i, z, blue, true));
												else
													break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		LiteModKyzray.logMessage("Found in " + this.radius + " block radius from Y=" + this.minY + " to Y=" + this.maxY + ":", true); 
		LiteModKyzray.logMessage("      \u00A78- \u00A74" + numLag + " \u00A7apossible connected L water", false);
		LiteModKyzray.logMessage("      \u00A78- \u00A76" + dLag + " \u00A7apossible lower-level unconnected L water", false);
		LiteModKyzray.logMessage("      \u00A78- \u00A7e" + idkLag + " \u00A7apossible same-level unconnected L water", false);
		return toReturn;
	}


	/**
	 * Finds the list of blocks in the radius
	 * @return The list of blocks
	 */
	@SuppressWarnings("deprecation")
	private LinkedList<XrayBlock> getBlockList()
	{
		LinkedList<XrayBlock> toReturn = new LinkedList<XrayBlock>();
		WorldClient world = Minecraft.getMinecraft().world;

		int[] blockCounts = new int[this.blocksToFind.size()];
		for (int i = 0; i < this.blocksToFind.size(); i++)
			blockCounts[i] = 0;

		if (this.blocksToFind.contains(Block.getBlockById(0)) && this.blocksToFind.size() > 1)
		{
			LiteModKyzray.logError("Omitting air from multi-block xray search. Xray for air alone to find underground structures.");
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
						BlockPos pos = new BlockPos(x, y, z);
						Block currentBlock = world.getBlockState(pos).getBlock();
						if (currentBlock.equals(this.blocksToFind.get(i)))
							//						if (currentBlock.getLocalizedName().equals(this.blocksToFind.get(i).getLocalizedName()))
						{
							int color = currentBlock.getMapColor(currentBlock.getDefaultState(), world, pos).colorValue;
							toReturn.add(new XrayBlock(x, y, z, color, false));
							blockCounts[i]++;
						}
					}
				}
			}
		}
		String message = "Found";
		for (int i = 0; i < this.blocksToFind.size(); i++)
			message += " " + blockCounts[i] + " " + this.blocksToFind.get(i).getLocalizedName() + ",";
		LiteModKyzray.logMessage(message.substring(0, message.length() - 1) 
				+ " in " + this.radius + " block radius from Y=" + this.minY + " to Y=" + this.maxY, true);
		if (toReturn.size() > 15000)
		{
			LiteModKyzray.logError("Too many blocks! Displaying more will lag.");
			LiteModKyzray.logError("Displayed 15,000 blocks out of " + toReturn.size() + ".");
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
			{
				this.blocksToFind.add(Block.getBlockFromName("wall_sign"));
				this.blocksToFind.add(Block.getBlockFromName("standing_sign"));
				toReturn += " Sign";
			}
			else if (Block.getBlockFromName(findBlock) != null)
			{
				if (!this.blocksToFind.contains(Block.getBlockFromName(findBlock)))
				{
					this.blocksToFind.add(Block.getBlockFromName(findBlock));
					toReturn += " " + this.blocksToFind.getLast().getLocalizedName();
				}
			}
			else if (findBlock.matches("[0-9]+")
					&& Block.REGISTRY.getObjectById(Integer.parseInt(findBlock)) != null)
			{
				if (!this.blocksToFind.contains((Block)Block.REGISTRY.getObjectById(Integer.parseInt(findBlock))))
				{
					this.blocksToFind.add((Block)Block.REGISTRY.getObjectById(Integer.parseInt(findBlock)));
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
		// TODO
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
		EntityPlayerSP player = Minecraft.getMinecraft().player;
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
			LiteModKyzray.logError("You can't have a radius that small, uDerp.");
		else if (theRadius > 128)
			LiteModKyzray.logError(theRadius + " is too large! Maximum allowed radius is 128.");
		else
		{
			this.radius = theRadius;
			LiteModKyzray.logMessage("Xray radius set to " + theRadius, true);
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
}
