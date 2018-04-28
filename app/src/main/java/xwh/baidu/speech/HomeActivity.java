package xwh.baidu.speech;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class HomeActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	public void onlineSpeech(View view) {
		Intent intent = new Intent(this, OnlineSpeechActivity.class);
		startActivity(intent);
	}

	public void offlineSpeech(View view) {
		Intent intent = new Intent(this, OfflineSpeechActivity.class);
		startActivity(intent);
	}

	public void wakeup(View view) {
		Intent intent = new Intent(this, WakeupActivity.class);
		startActivity(intent);
	}

	public void wakeupAndRecog(View view) {
		Intent intent = new Intent(this, WakeupAndRecogActivity.class);
		startActivity(intent);
	}
}
