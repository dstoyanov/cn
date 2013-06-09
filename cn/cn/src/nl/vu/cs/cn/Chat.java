package nl.vu.cs.cn;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP.Socket;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class Chat extends Activity {
	
	Socket s1, s2;
	EditText editText1, editText2;
	


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		
//		LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayout1);
//		editText1 = (EditText) ll.findViewById(R.id.textInput1);
		editText1 = (EditText) findViewById(R.id.textInput1);

//		editText1.setText("lalalla1");

		
//		editText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//		    public void onFocusChange(View v, boolean hasFocus) {
//		    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//		    	mgr.showSoftInput(editText1, InputMethodManager.SHOW_IMPLICIT);
//		    }
//		});
		System.out.println("fafa");
		System.out.println("allalal");
		
		
        editText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                editText1.post(new Runnable() {
                    public void run() {
                    	editText1.setText("Focus change");
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                       imm.showSoftInput(editText1, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });

		

		// Connect various GUI components to the networking stack.
	}
	


}
