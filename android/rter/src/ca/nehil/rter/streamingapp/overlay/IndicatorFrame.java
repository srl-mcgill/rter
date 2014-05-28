package ca.nehil.rter.streamingapp.overlay;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class IndicatorFrame {
	private FloatBuffer vertexBuffer; // Buffer for vertex-array

	public static enum Colour {
		RED, GREEN, BLUE
	}
	private Colour currentColour = Colour.BLUE;

	private float[] vertices = { // Vertices for the frame
			-1.0f, 1.0f, 0.0f,
			1.0f, 1.0f, 0.0f,
			1.0f, -1.0f, 0.0f,
			-1.0f, -1.0f, 0.0f
	};

	public IndicatorFrame() {		
		// Setup vertex array buffer. Vertices in float. A float has 4 bytes
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder()); // Use native byte order
		vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
	}

	// Render the shape
	public void draw(GL10 gl) {
		// Enable vertex-array and define its buffer
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

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
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
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
