package com.vs.anu;

import com.vs.anu.ogl.GLanimation;
import com.vs.anu.player.Bowl;
import android.os.Bundle;
import android.app.Activity;
import android.view.Window;
import com.vs.anu.R;

public class MainActivity extends Activity {
	GLanimation glanim;
	Bowl bowl=new Bowl();
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 				// remove title bar
		setContentView(R.layout.activity_main);
		init();
	}
	private void init() {
		glanim=(GLanimation)findViewById(R.id.glAnuAnimation);
		bowl.setDuration(15);
		bowl.SoundStart();
	}
	@Override protected void onPause()   { 	super.onPause(); 	if (bowl!=null) bowl.SoundStop();	}
	@Override protected void onStop()    { 	super.onStop(); 	if (bowl!=null) bowl.SoundStop();	}
}
