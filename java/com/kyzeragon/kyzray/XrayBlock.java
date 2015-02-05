package com.kyzeragon.kyzray;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.Tessellator;

public class XrayBlock {

	private int x;
	private int y;
	private int z;
	
	public XrayBlock(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void drawBlock(Tessellator tess, int color)
	{
		GL11.glLineWidth(3.0f);
		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(x, y, z);
		tess.addVertex(x + 1, y, z);
		tess.addVertex(x + 1, y, z + 1);
		tess.addVertex(x, y, z + 1);
		tess.draw();
		
		tess.startDrawing(GL11.GL_LINE_LOOP);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(x, y + 1, z);
		tess.addVertex(x + 1, y + 1, z);
		tess.addVertex(x + 1, y + 1, z + 1);
		tess.addVertex(x, y + 1, z + 1);
		tess.draw();
		
		tess.startDrawing(GL11.GL_LINES);
		tess.setColorRGBA_I(color, 200);
		tess.addVertex(x, y, z);
		tess.addVertex(x, y + 1, z);
		
		tess.addVertex(x + 1, y, z);
		tess.addVertex(x + 1, y + 1, z);
		
		tess.addVertex(x + 1, y, z + 1);
		tess.addVertex(x + 1, y + 1, z + 1);
		
		tess.addVertex(x, y, z + 1);
		tess.addVertex(x, y + 1, z + 1);
		tess.draw();
	}
}
