package io.github.kyzderp.kyzray;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;

import com.mumfrey.liteloader.gl.GL;

public class XrayBlock {

	private int x;
	private int y;
	private int z;
	private float colorR;
	private float colorG;
	private float colorB;
	private float colorA;
	private boolean smaller;
	
	public XrayBlock(int x, int y, int z, int color, boolean smaller)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.colorR = ((color >> 16) & 255) / 255;
		this.colorG = ((color >> 8) & 255) / 255;
		this.colorB = (color & 255) / 255;
		this.colorA = 0.8F;
		
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
		GL.glLineWidth(3.0F);
		GL.glColor4f(this.colorR, this.colorG, this.colorB, this.colorA);
		
		BufferBuilder vbuf = tess.getBuffer();
		
		vbuf.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
		vbuf.pos(minX, minY, minZ).endVertex();
		vbuf.pos(maxX, minY, minZ).endVertex();
		vbuf.pos(maxX, minY, maxZ).endVertex();
		vbuf.pos(minX, minY, maxZ).endVertex();
		tess.draw();
		
		vbuf.begin(GL.GL_LINE_LOOP, GL.VF_POSITION);
		vbuf.pos(minX, maxY, minZ).endVertex();
		vbuf.pos(maxX, maxY, minZ).endVertex();
		vbuf.pos(maxX, maxY, maxZ).endVertex();
		vbuf.pos(minX, maxY, maxZ).endVertex();
		tess.draw();
		
		vbuf.begin(GL.GL_LINES, GL.VF_POSITION);
		vbuf.pos(minX, minY, minZ).endVertex();
		vbuf.pos(minX, maxY, minZ).endVertex();
		
		vbuf.pos(maxX, minY, minZ).endVertex();
		vbuf.pos(maxX, maxY, minZ).endVertex();
		
		vbuf.pos(maxX, minY, maxZ).endVertex();
		vbuf.pos(maxX, maxY, maxZ).endVertex();
		
		vbuf.pos(minX, minY, maxZ).endVertex();
		vbuf.pos(minX, maxY, maxZ).endVertex();
		tess.draw();
	}
}
