package uk.ac.ed.epcc.mountains;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import uk.ac.ed.epcc.mountains.MountainView.MountainThread;
public class MountainActivity extends
        AppCompatActivity implements OnSharedPreferenceChangeListener{

	public static final String FDIM_PREF = "fdim";

	public static final String STOP_PREF = "stop";

	public static final String LEVELS_PREF = "levels";

	private static final String MOUNTAIN_KEY = "MountainKey";
	
	private Mountain mountain;
	private SharedPreferences prefs;
	private MountainView getMountainView(){
		return (MountainView)  findViewById(R.id.MountainView);
	}
	public static int parseInt(String s,int min, int max,int def){
		
		int val=def;
		if( s != null ){
			try{
				val = Integer.parseInt(s);
			}catch(NumberFormatException e){
				val = def;
			}
		}
		if( val < min ){
			return min;
		}
		if( val > max){
			return max;
		}
		return val;
	}
public static double parseDouble(String s,double min, double max,double def){
		
		double val=def;
		if( s != null ){
			try{
				val = Double.parseDouble(s);
			}catch(NumberFormatException e){
				val = def;
			}
		}
		if( val < min ){
			return min;
		}
		if( val > max){
			return max;
		}
		return val;
	}
	public void setPrefs(Mountain m){
	     int len = parseInt(prefs.getString(LEVELS_PREF, null), 2, 20, Mountain.DEFAULT_LEVELS);
	     int stop = parseInt(prefs.getString(STOP_PREF, null),0,len-1,Mountain.DEFAULT_STOP);
	     boolean reset = m.set_size(len, stop);
	     m.setForceval(parseDouble(prefs.getString("force", null), -2.0, 2.0, -0.5));
	     m.set_cross(prefs.getBoolean("cross", true));
	     reset = reset || m.set_rg(prefs.getBoolean("rg1", false), prefs.getBoolean("rg2", false), prefs.getBoolean("rg3", true));
	     m.set_fdim(parseDouble(prefs.getString(FDIM_PREF, null), 0.5, 1.0, Mountain.DEFAULT_FDIM));
	     m.set_front(parseInt(prefs.getString("front",null), 0, len, 1));
	     m.set_back(parseInt(prefs.getString("back",null), 0, len, 1));
	     
	    	 MountainView mountainView = getMountainView();
	    	 if( mountainView != null){
	    		 mountainView.setPrefs(prefs);
	    		 if( reset ){
	    			 mountainView.mountainChanged();
	    		 }
	    	 }
	     
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//prefs.registerOnSharedPreferenceChangeListener(this);
        if( savedInstanceState == null){
        	mountain=new Mountain();
        	setPrefs(mountain);
            mountain.init();
        }else{
        	mountain = (Mountain) savedInstanceState.getSerializable(MOUNTAIN_KEY);
        	if( mountain ==  null){
        		mountain=new Mountain();
        		setPrefs(mountain);
        		mountain.init();
        	}
        }
        setContentView(R.layout.activity_mountain);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);


    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        outState.putSerializable(MOUNTAIN_KEY, mountain);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_mountain, menu);
        return true;
    }
    /**
     * Invoked when the user selects an item from the Menu.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	MountainThread mThread = getMountainView().getThread();
    	Log.w(getClass().getName(),"menu "+item);
    	if( mThread != null ){
            Log.w(getClass().getName(),"mthread is not null "+item);


            switch (item.getItemId()) {
    		case R.id.button_start_stop:
                //Log.w(getClass().getName(),"start stop matched "+item);

    			mThread.togglePaused();
    			return true;
    		case R.id.button_turbo:
                //Log.w(getClass().getName(),"turbo matched "+item);


                mThread.turbo();
    			return true;
    		case R.id.menu_settings:
    			mThread.pause();
    			startActivity(new Intent(this,SettingsActivity.class));
    			return true;
    		}
    	}else{
            Log.w(getClass().getName(),"thread is null "+item);

            switch(item.getItemId()){
    		case R.id.menu_settings:
    			startActivity(new Intent(this,SettingsActivity.class));
    			return true;
    		}
    	}
        return false;
    }
    
    
    protected void onPause(){
    	super.onPause();
    	MountainThread mThread = getMountainView().getThread();
    	if( mThread != null){
    		mThread.pause();
    	}
    	//prefs.unregisterOnSharedPreferenceChangeListener(this);
    }
	@Override
	protected void onResume() {
		super.onResume();
		//prefs.registerOnSharedPreferenceChangeListener(this);
		setPrefs(mountain);
		MountainView mountainView = getMountainView();
		
		MountainThread mThread = mountainView.getThread();
		if( mThread != null){
			mThread.unpause();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		getMountainView().setMountain(prefs,mountain);
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
			setPrefs(mountain);
			getMountainView().setPrefs(prefs);
		
	}
  
}
