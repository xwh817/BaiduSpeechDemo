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

public class OnlineSpeechActivity extends AppCompatActivity {

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

		btnStartRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				start();
			}
		});
		btnStopRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel();
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
		asr = EventManagerFactory.create(this, "asr");
		asr.registerListener(new EventListener() {
			@Override
			public void onEvent(String name, String params, byte[] data, int offset, int length) {
				String result = null;
				if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
					result = "引擎准备就绪，可以开始说话";
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)) {
					result = "检测到用户的已经开始说话";
					startSpeakTime = System.currentTimeMillis();
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)) {
					result = "检测到用户的已经停止说话"+ params;
					stopSpeakTime = System.currentTimeMillis();
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
					// 临时识别结果, 长语音模式需要从此消息中取出结果

					try {
						JSONObject jsonObject = new JSONObject(params);
						String resultType = jsonObject.getString("result_type");

						if ("final_result".equals(resultType)) {
							String best_result = jsonObject.getString("best_result");
							result = "最终识别结果：" + best_result;

							tvParseResult.append("解析结果：" + best_result+"\n");

						} else if ("nlu_result".equals(resultType)) {
							String nlu_result = new String(data, offset, length);
							result = "语义解析结果：" + nlu_result;
						} else {
							String best_result = jsonObject.getString("best_result");
							result = "临时识别结果：" + best_result;
						}

						} catch (JSONException e) {
						e.printStackTrace();
					}


				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
					// 识别结束， 最终识别结果或可能的错误
					result = "识别结束" ;
					btnStartRecord.setEnabled(true);
				} else {
					result = "onEvent: " + name;
				}

				printResult(result);

			}
		}); //  EventListener 中 onEvent方法
	}

	private void printResult(String text) {
		tvResult.append(text + "\n\n");
		//Log.i("TEst", text);
	}

	private long startSpeakTime;
	private long stopSpeakTime;

	protected void start() {
		tvResult.setText("");
		tvParseResult.setText("");
		btnStartRecord.setEnabled(false);

		String json = getAsrParams().toString(); // 这里可以替换成你需要测试的json
		asr.send(SpeechConstant.ASR_START, json, null, 0, 0);

		printResult("启动识别，输入参数：" + json);
	}

	protected JSONObject asrParams;
	protected JSONObject getAsrParams() {
		if (asrParams == null) {
			try {
				asrParams = new JSONObject();
				asrParams.put(SpeechConstant.PID, 1536); // 默认1536, 语义15361,输入法模型1537
				asrParams.put(SpeechConstant.DECODER, 0); // 纯在线(默认)
				asrParams.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN); // 语音活动检测
				asrParams.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800); // 开启VAD尾点检测，即静音判断的毫秒数。建议设置800ms-3000ms
				//asrParams.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 0); // VAD_ENDPOINT_TIMEOUT=0 && 输入法模型 开启长语音。
				asrParams.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);// 是否需要语音音频数据回调
				asrParams.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);// 是否需要语音音量数据回调
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return asrParams;
	}

	public void stop() {
		asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
	}


	public void cancel() {
		asr.send(SpeechConstant.ASR_CANCEL, null, null, 0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancel();
	}


	public static void stopSpeech() {

	}
}