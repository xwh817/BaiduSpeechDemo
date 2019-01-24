package xwh.baidu.speech;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
 * 唤醒并设备命令
 * <p>
 * 1. 自定义唤醒词： http://yuyin.baidu.com/wake 生成bsg文件后放入assets下面
 * 2. 应用包名和控制台包名一致
 */
public class WakeupAndRecogActivity extends BaseActivity {

	private Button btnStartRecord;
	private Button btnStopRecord;
	private TextView tvResult;
	private TextView tvParseResult;

	private EventManager mWakeuper;

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
				startWakeup();
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
		mWakeuper = EventManagerFactory.create(this, "wp");
		mWakeuper.registerListener(new EventListener() {
			@Override
			public void onEvent(String name, String params, byte[] data, int offset, int length) {
				String result = null;
				if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_STARTED)) {
					result = "进入唤醒";
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_READY)) {
					result = "已准备好唤醒";
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS)) {

					try {
						JSONObject json = new JSONObject(params);
						String word = json.getString("word");
						result = "唤醒成功： " + word;
					} catch (JSONException e) {
						e.printStackTrace();
					}

					mWakeuper.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
					startAsr();

				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED)) {
					result = "停止唤醒";
				} else {
					result = "onEvent: " + name + ", mParamsWakeup:" + params;
				}

				printResult(result);

			}
		}); //  EventListener 中 onEvent方法
	}

	private void printResult(String text) {
		tvResult.append(text + "\n");
		//Log.i("TEst", text);
	}


	private void startWakeup() {
		tvResult.setText("");
		//tvParseResult.setText("");
		btnStartRecord.setEnabled(false);

		String json = getWakeupParams().toString(); // 这里可以替换成你需要测试的json
		mWakeuper.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
	}

	/**
	 * 唤醒词说完后，然后接句子，是否回溯音频流。（推荐4个字 1500ms， 根据唤醒词测试调整）
	 * backTrackInMs=0，不回溯音频流，可能出现截断
	 * backTrackInMs>0，回溯音频流，可能出现多余字符
	 * <p>
	 * backTrackInMs 时间回溯，SDK有15s的录音缓存。如设置为(System.currentTimeMillis() - 1500),表示回溯1.5s的音频。
	 * https://ai.baidu.com/forum/topic/show/497353
	 */
	private int backTrackInMs = 400;


	private JSONObject mParamsWakeup;

	private JSONObject getWakeupParams() {
		if (mParamsWakeup == null) {
			try {
				mParamsWakeup = new JSONObject();
				mParamsWakeup.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");   // params里 "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return mParamsWakeup;
	}


	private JSONObject asrParams;
	private JSONObject getAsrParams() {
		try {
			if (asrParams == null) {
				asrParams = new JSONObject();
				asrParams.put(SpeechConstant.PID, 1536); // 默认1536
				asrParams.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 1000); // 开启VAD尾点检测，即静音判断的毫秒数。建议设置800ms-3000ms
				asrParams.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN); // 语音活动检测
				asrParams.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);// 是否需要语音音频数据回调
				asrParams.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);// 是否需要语音音量数据回调
			}

			//if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
				asrParams.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
			//}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return asrParams;
	}


	private EventManager asr;

	private void startAsr() {

		if (asr == null) {
			asr = EventManagerFactory.create(this, "asr");
			asr.registerListener(new EventListener() {
				boolean hasResult = false;
				@Override
				public void onEvent(String name, String params, byte[] data, int offset, int length) {
					String result = null;
					if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
						result = "引擎准备就绪，可以开始说话";
						hasResult = false;
					} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)) {
						result = "检测到用户的已经开始说话";
					} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)) {
						result = "检测到用户的已经停止说话";
					} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
						// 临时识别结果, 长语音模式需要从此消息中取出结果

						try {
							JSONObject jsonObject = new JSONObject(params);
							String resultType = jsonObject.getString("result_type");

							if ("final_result".equals(resultType)) {
								String best_result = jsonObject.getString("best_result");
								result = "最终识别结果：" + best_result;

								long endTime = System.currentTimeMillis();
								tvParseResult.append("解析结果：" + best_result+"\n");
								hasResult = true;

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
						result = "识别结束";
						btnStartRecord.setEnabled(true);
						//asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);

						/*if (hasResult) {    // 如果有人在说话，就接着识别，如果没有就进入唤醒状态
							startAsr();
						} else {
							startWakeup();
						}*/
						startWakeup();

					} else {
						result = "onEvent: " + name;
					}

					printResult(result);

				}
			}); //  EventListener 中 onEvent方法
		}

		asrParams = getAsrParams(); // 更新参数里面的时间

		asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
		asr.send(SpeechConstant.ASR_START, asrParams.toString(), null, 0, 0);
	}


	private void stop() {
		btnStartRecord.setEnabled(true);
		mWakeuper.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
		asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop();
	}


}