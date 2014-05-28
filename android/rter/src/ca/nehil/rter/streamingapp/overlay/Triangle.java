package ca.nehil.rter.streamingapp.overlay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Triangle {
	private FloatBuffer vertexBuffer; // Buffer for vertex-array
	public static enum Colour {
		RED, GREEN, BLUE
	}
	private Colour currentColour = Colour.GREEN;

	private float[] vertices = { // Vertices for the frame
			-1.0f, -1.0f / 1.73f, 0.0f,
			1.0f, -1.0f / 1.73f, 0.0f,
			0.0f, 2.0f / 1.73f, 0.0f
	};
	
	public Triangle() {		
		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder()); // Use native byte order
		vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
	}
	
	public void draw(GL10 gl){
		draw(gl, false);
	}
	
	// Render the shape
	public void draw(GL10 gl, boolean fill) {
		// Enable vertex-array and define its buffer
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

		// Set the color for each of the faces
		switch (this.currentColour) {
		case RED:
			gl.glColor4f(0.9f, 0.0f, 0.0f, 1.0f);
			break;
		case GREEN:
			gl.glColor4f(0.0f, 0.9f, 0.0f, 1.0f);
			break;
		case BLUE:
			gl.glColor4f(0.0f, 0.0f, 0.9f, 1.0f);
			break;
		}

		// Draw the primitive from the vertex-array directly
		if(fill){
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 3);
		} else {
			gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 3);
		}
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
	
	/**
	 * not thread safe
	 * @param colour
	 */
	public void colour(Colour colour){
		this.currentColour = colour;
	}
}
