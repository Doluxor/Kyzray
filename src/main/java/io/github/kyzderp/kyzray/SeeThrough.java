package io.github.kyzderp.kyzray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

public class SeeThrough 
{
	private float distance;
	
	public SeeThrough()
	{
		this.distance = 4.0f;
	}
	
	public void setObliqueNearPlaneClip(float a, float b, float c)
	{
		this.setObliqueNearPlaneClip(a, b, c, -this.distance);
	}
	
	public void setDistance(float dist)
	{
		this.distance = dist;
	}

	
	private void setObliqueNearPlaneClip(float a, float b, float c, float d) 
	{
		float matrix[] = new float[16];
		float x, y, z, w, dot;
		FloatBuffer buf = makeBuffer(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf);
		buf.get(matrix).rewind();
		x = (sgn(a) + matrix[8]) / matrix[0];
		y = (sgn(b) + matrix[9]) / matrix[5];
		z = -1.0F;
		w = (1.0F + matrix[10]) / matrix[14];
		dot = a*x + b*y + c*z + d*w;
		matrix[2] = a * (2f / dot);
		matrix[6] = b * (2f / dot);
		matrix[10] = c * (2f / dot) + 1.0F;
		matrix[14] = d * (2f / dot);
		buf.put(matrix).rewind();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadMatrix(buf);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	
    private float sgn(float f) { return f<0f ? -1f : (f>0f ? 1f : 0f); }
    
    private static FloatBuffer makeBuffer(int length) { return ByteBuffer.allocateDirect(length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); }

//    private static FloatBuffer makeBuffer(float[] array) { return (FloatBuffer)ByteBuffer.allocateDirect(array.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(array).flip(); }
    

}
