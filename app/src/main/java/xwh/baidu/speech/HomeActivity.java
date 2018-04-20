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
}
