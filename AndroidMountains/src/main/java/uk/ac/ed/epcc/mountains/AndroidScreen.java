package uk.ac.ed.epcc.mountains;

import android.graphics.Bitmap;
import android.graphics.Color;


public class AndroidScreen implements Screen {
	private Bitmap bitmap;
	private int clut[];
	private int scrollbuffer[];
	int width;
	int height;
	public AndroidScreen(int width, int height){
		this.width=width;
		this.height=height;
		bitmap=Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		scrollbuffer = new int[width*height];
	}
	public int getWidth(){
		return width;
	}
	public int getHeight(){
		return height;
	}
	public  Bitmap getBitmap(){
		return bitmap;
	}
	
	@Override
	public void blank_region(int col, int lx, int ly, int hx, int hy)
	{
		int count = (hx-lx+1)*(hy-ly+1);
		int val = clut[col];
		for(int i=0;i<count;i++){
			scrollbuffer[i]=val;
		}
		bitmap.setPixels(scrollbuffer, 0, hx-lx+1, lx, ly, hx-lx+1, hy-ly+1);
	}

	@Override
	public void scroll_screen(int col, int dist) {
		int stride=width-dist;
		if( stride > 0){
			bitmap.getPixels(scrollbuffer, 0, width-dist, dist,0, width-dist, height);
			bitmap.setPixels(scrollbuffer, 0, width-dist, 0, 0, width-dist, height);
			blank_region(col, width-dist, 0, width-1, height-1);
		}
	}

	@Override
	public void plot_pixel(int x, int y, int col) {
		bitmap.setPixel(x, y, clut[col]);
	}

	
	@Override
	public void setClut(int col, float r, float g, float b) {
		int red =  (int)(r * 255.0F);
		int green = (int)(g * 255.0F);
		int blue = (int) (b * 255.0F);
		clut[col] = Color.argb(255,red, green, blue);
	}

	@Override
	public void makeClut(int size) {
		clut = new int[size];
	}

}
