package com.kyzeragon.kyzray;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.Tessellator;

public class XrayBlock {

	private int x;
	private int y;
	private int z;
	private int color;
	private boolean smaller;
	
	public XrayBlock(int x, int y, int z, int color, boolean smaller)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = color;
		this.smaller = smaller;
	}
	
	public void drawBlock(Tessellator tess)
	{
		if (this.smaller)
			this.drawBlock(tess, x + 0.1, y + 0.1, z + 0.1, x + 0.9, y + 0.9, z + 0.9);
		else
			this.drawBlock(tess, x, y, z, x + 1, y + 1, z + 1);
	}
	
	private void drawBlock(Tessellator tess, double minX, double minY, double minZ, 
			double maxX, double maxY, double maxZ)
	{
		GL11.glLineWidth(3.0f);
		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(minX, minY, minZ);
		tess.addVertex(maxX, minY, minZ);
		tess.addVertex(maxX, minY, maxZ);
		tess.addVertex(minX, minY, maxZ);
		tess.draw();
		
		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(minX, maxY, minZ);
		tess.addVertex(maxX, maxY, minZ);
		tess.addVertex(maxX, maxY, maxZ);
		tess.addVertex(minX, maxY, maxZ);
		tess.draw();
		
		tess.startDrawing(GL11.GL_LINES);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(minX, minY, minZ);
		tess.addVertex(minX, maxY, minZ);
		
		tess.addVertex(maxX, minY, minZ);
		tess.addVertex(maxX, maxY, minZ);
		
		tess.addVertex(maxX, minY, maxZ);
		tess.addVertex(maxX, maxY, maxZ);
		
		tess.addVertex(minX, minY, maxZ);
		tess.addVertex(minX, maxY, maxZ);
		tess.draw();
	}
}
