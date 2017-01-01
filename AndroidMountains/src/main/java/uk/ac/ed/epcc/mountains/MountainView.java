package uk.ac.ed.epcc.mountains;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MountainView extends SurfaceView implements SurfaceHolder.Callback {


	private MountainThread thread;
	private SurfaceHolder mSurfaceHolder;
	private AndroidScreen screen;
	private Artist artist;
	private SharedPreferences prefs;
	public synchronized MountainThread getThread() {
		return thread;
	}
	private Mountain mountain;

	public MountainView(Context context, AttributeSet attrs) {
		super(context, attrs);
		  // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        Log.w(getClass().getName(),"MountainView created");
        thread = new MountainThread();
        setFocusable(true); // make sure we get key events
	}

	public synchronized void setMountain(SharedPreferences prefs,Mountain m){
		if( m == mountain){
			return;
		}
		this.prefs=prefs;
		this.mountain=m;
		if( artist != null){
			artist = null;
			screen=null;
		}
	}

	public class MountainThread extends Thread{
		
		public MountainThread( ) {
			super();
			//mLinePaint = new Paint();
            //mLinePaint.setAntiAlias(true);
            //mLinePaint.setARGB(255, 0, 255, 0);
		}
		
		
		private boolean running=false;
		private boolean paused=false;

        private synchronized boolean isPaused(){
            //Log.w(getClass().getName(), "paused="+paused);

            return paused;
        }
        private synchronized boolean isRunning(){
            return running;
        }
        //private Paint mLinePaint;
		private boolean turbo=false;
		long little_sleep=0;
		long long_sleep=5000;
		@Override
		public void run() {
			//Log.w(getClass().getName(),"Thread running");
			long snooze=0;
			long target_time;
			while(isRunning()){
				target_time  = System.currentTimeMillis();
				snooze=0;

					if( isRunning() && ! isPaused()){
						
							// attempt to subtract the update time from the sleep time
							// to give a constant update rate. If the update takes too long
							// things will of course be slower.
							if( doPlot() ){
								//snooze=long_sleep;
								snooze=target_time + long_sleep - System.currentTimeMillis();
							}else{
								//snooze=little_sleep;
								snooze=target_time + little_sleep - System.currentTimeMillis();
							}
					}

				if( ! turbo ){
					if( isPaused() ){

                            try {
                                if (isRunning()) {
                                    //Log.w(getClass().getName(), "paused sleep");
                                    sleep(1000L);
                                }
                            } catch (InterruptedException e) {
                                Log.w(getClass().getName(), "interrupted sleep");
                            }

					}else{
						if( snooze > 0 ){
							//Log.w(getClass().getName(),"snooze for "+snooze);
							try{sleep(snooze);}catch (InterruptedException e){}
						}else{
							//Log.w(getClass().getName(),"yield");
							// let other threads have a go.
							yield();
						}
					}
				}else{
					// Don't hog everything.
					//Log.w(getClass().getName(),"yield non turbo");
					yield();
				}
			}
			Log.w(getClass().getName(),"Exit running loop");
		}

		public synchronized void setRunning() {
			if( ! running ){
				running=true;
				start();
			}
		}
		public synchronized void stopRunning(){
			running=false;
			unpause();
		}
		public  void togglePaused() {
			if(isPaused()){
				unpause();
			}else{
				pause();
			}
		}
		
		public synchronized void pause() {
			if( running ){
                Log.w(getClass().getName(), "set paused");
				paused = true;
			}
		}
		public synchronized void unpause(){
			if( paused ){
                Log.w(getClass().getName(), "call unpause");

				paused=false;
			}
		}

		public synchronized void turbo() {
			turbo = ! turbo;
            Log.w(getClass().getName(), "toggle turbo");
			unpause();
		}
	}


	@Override
	public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		//Log.w(getClass().getName()," surface changed "+width+" "+height);
		mSurfaceHolder=holder;
		setSurfaceSize(width, height);
		if( thread ==null){
			thread = new MountainThread();
		}
		
	}
	public synchronized void mountainChanged(){
		if( artist != null){
			if( artist.isIntialised()){
				artist.init_artist_variables();
			}
		}
	}
	public synchronized void setSurfaceSize(int width, int height) {
		if( artist == null || artist.getWidth() != width || artist.getHeight() != height){
			screen=new AndroidScreen(width, height);
			artist=new Artist(width, height, screen, mountain);
			setPrefs(prefs);
		}
	}

	public synchronized void setPrefs(SharedPreferences prefs){
		if( artist != null && prefs != null){
			
		   boolean changed = artist.setMap(prefs.getBoolean("map", false));
		   changed = changed || artist.setReflec(prefs.getBoolean("reflect", true));
		   int repeat = MountainActivity.parseInt(prefs.getString("repeat",null),  - artist.getWidth(),artist.getWidth(), 20);
		   changed = changed || (artist.getRepeat() != repeat);
		   artist.setRepeat(repeat);
		   if(changed && artist.isIntialised()){
			   artist.init_artist_variables();
		   }
		}
	}
	public synchronized boolean doPlot(){
		if( artist != null && mSurfaceHolder != null){
			artist.plot_column();
			// draw image
			synchronized (getClass()) {
				Canvas c = mSurfaceHolder.lockCanvas();
				//c.drawLine(0.0F, 0.0F, (float)screen.getWidth()-1, (float)(i%screen.getHeight()), mLinePaint);
				c.drawBitmap(screen.getBitmap(), 0.0F, 0.0F, null);
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
			return artist.getScroll() != 0;
		}
		return false;
	}
	@Override
	public synchronized void surfaceCreated(SurfaceHolder holder) {
		//Log.w(getClass().getName(),"surface created ");
		// we make a new thread for each surface created
		mSurfaceHolder=holder;
		if( thread == null ){
			thread = new MountainThread();
		}
        thread.setRunning();
	}

	@Override
	public synchronized void surfaceDestroyed(SurfaceHolder holder) {
		  // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mSurfaceHolder=null;
        MountainThread mythread = thread;
        thread=null;
        mythread.stopRunning();
        // have to wait outside sync method as run might need to take lock
        while (retry) {
            try {
                mythread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
	}
	@Override
    public synchronized boolean onKeyDown(int keyCode, KeyEvent msg) {
		Log.w(getClass().getName(),"key down");
		if( thread != null ){
			thread.togglePaused();
		}
        return true;
    }

}
