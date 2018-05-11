package xwh.baidu.speech;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 唤醒
 *
 * 1. 自定义唤醒词： http://yuyin.baidu.com/wake 生成bsg文件后放入assets下面
 * 2. 应用包名和控制台包名一致
 *
 */
public class WakeupActivity extends AppCompatActivity {

	private Button btnStartRecord;
	private Button btnStopRecord;
	private TextView tvResult;
	private TextView tvParseResult;

	private EventManager asr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		initPermission();
		initListener();

	}


	private void initView() {
		tvResult = (TextView) findViewById(R.id.tvResult);
		tvParseResult = (TextView) findViewById(R.id.tvParseResult);
		btnStartRecord = (Button) findViewById(R.id.btnStartRecord);
		btnStopRecord = (Button) findViewById(R.id.btnStopRecord);

		btnStartRecord.setText(R.string.start_wakeup);
		btnStopRecord.setText(R.string.stop_wakeup);

		btnStartRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				start();
			}
		});
		btnStopRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stop();
			}
		});
	}

	/**
	 * android 6.0 以上需要动态申请权限
	 */
	private void initPermission() {
		String permissions[] = {Manifest.permission.RECORD_AUDIO,
				Manifest.permission.ACCESS_NETWORK_STATE,
				Manifest.permission.INTERNET,
				Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
		};

		ArrayList<String> toApplyList = new ArrayList<String>();

		for (String perm : permissions) {
			if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
				toApplyList.add(perm);
				//进入到这里代表没有权限.

				printResult("Android 没有授权");
			}
		}
		String tmpList[] = new String[toApplyList.size()];
		if (!toApplyList.isEmpty()) {
			ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// 此处为android 6.0以上动态授权的回调，用户自行实现。
	}


	private void initListener() {
		asr = EventManagerFactory.create(this, "wp");
		asr.registerListener(new EventListener() {
			@Override
			public void onEvent(String name, String params, byte[] data, int offset, int length) {
				String result;
				if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_STARTED)) {
					result = "进入唤醒";
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_READY)) {
					result = "已准备好唤醒";
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS)) {
					result = "唤醒成功： "+ params;
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED)) {
					result = "停止唤醒";
				} else {
					result = "onEvent: " + name + ", params:" + params;
				}

				printResult(result);

			}
		}); //  EventListener 中 onEvent方法
	}

	private void printResult(String text) {
		tvResult.append(text + "\n\n");
		//Log.i("TEst", text);
	}


	private void start() {
		tvResult.setText("");
		tvParseResult.setText("");
		btnStartRecord.setEnabled(false);

		String json = getWakeupParams().toString(); // 这里可以替换成你需要测试的json
		asr.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);

		printResult("启动识别，输入参数：" + json);
	}

	private JSONObject params;
	private JSONObject getWakeupParams() {
		if (params == null) {
			try {
				params = new JSONObject();
				//params.put(SpeechConstant.PID, 1536); // 默认1536
				params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");   // params里 "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

				/*params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800); // 不开启长语音。开启VAD尾点检测，即静音判断的毫秒数。建议设置800ms-3000ms
				params.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);// 是否需要语音音频数据回调
				params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);// 是否需要语音音量数据回调*/
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return params;
	}

	private void stop() {
		btnStartRecord.setEnabled(true);
		asr.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop();
	}


}