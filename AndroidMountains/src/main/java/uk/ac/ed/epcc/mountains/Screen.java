package uk.ac.ed.epcc.mountains;

public interface Screen {
	/** set a rectangle to the specified colout
	 * 
	 * @param col color
	 * @param lx lower x value
	 * @param ly lower y value
	 * @param hx highest x value
	 * @param hy highest y valuye
	 */
	public void blank_region(int col,int lx,int ly,int hx,int hy);
	/** Scroll image dist pixels to the left
	 * 
	 * @param col
	 * @param dist
	 */
	public void scroll_screen(int col,int dist);
	public void plot_pixel(int x, int y, int col);
	/** Set position col in the CLUT to r,g,b value
	 * 
	 * @param col
	 * @param r
	 * @param g
	 * @param b
	 */
	public void setClut(int col, float r, float g, float b);
	/** Allocate a clut of a certain size
	 * 
	 * @param size
	 */
	public void makeClut(int size);
}
