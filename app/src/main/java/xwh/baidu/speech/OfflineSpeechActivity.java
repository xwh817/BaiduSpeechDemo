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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 离线命令词识别
 *
 * 1. 自定义语义设置： http://yuyin.baidu.com/asr 生成bsg文件后放入assets下面
 *  词条：
 *   action = 开,关,打开,关闭
	 device = 灯,电灯,电视,空调,窗帘,主灯,灯带,射灯,夜灯
	 room = 客厅,厨房,卧室,卫生间
	 air_action = 制冷,制热,恒温,调高,调低
 *
 *  语义：
 *   device.action = <action><device>
	 device.action_room = <action><room><device>
	 device.air_action = <device><air_action>
	 device.air_action_room = <room><device><air_action>

 * 2. 应用包名和控制台包名一致
 *
 */
public class OfflineSpeechActivity extends AppCompatActivity {

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

		loadOfflineEngine();
	}


	/**
	 * 检测离线引擎是否可以加载成功
	 */
	private void loadOfflineEngine() {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put(SpeechConstant.DECODER, 2);
		params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
		asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
	}


	private void initView() {
		tvResult = (TextView) findViewById(R.id.tvResult);
		tvParseResult = (TextView) findViewById(R.id.tvParseResult);
		btnStartRecord = (Button) findViewById(R.id.btnStartRecord);
		btnStopRecord = (Button) findViewById(R.id.btnStopRecord);
		btnStopRecord.setVisibility(View.GONE);

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
					result = "检测到用户的已经停止说话"+ params + ", during:"+getDuring();
					stopSpeakTime = System.currentTimeMillis();
				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
					// 临时识别结果, 长语音模式需要从此消息中取出结果

					try {
						JSONObject jsonObject = new JSONObject(params);
						String resultType = jsonObject.getString("result_type");

						if ("final_result".equals(resultType)) {
							String best_result = jsonObject.getString("best_result");
							result = "最终识别结果：" + best_result+", json:" + params + ", during:"+getDuring();

							long endTime = System.currentTimeMillis();
							tvParseResult.setText("解析结果：" + best_result + "\n" + "录音结束到解析，耗时：" + (endTime - stopSpeakTime));

						} else if ("nlu_result".equals(resultType)) {
							String nlu_result = new String(data, offset, length);
							result = "语义解析结果：" + nlu_result + ", during:"+getDuring();
						} else {
							result = "临时识别结果：" + params + ", during:"+getDuring();
						}

						} catch (JSONException e) {
						e.printStackTrace();
					}


				} else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
					// 识别结束， 最终识别结果或可能的错误
					result = "识别结束" + params + ", during:"+getDuring();
					btnStartRecord.setEnabled(true);
					//asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
				} else {
					result = "onEvent: " + name;
				}

				printResult(result);

			}
		}); //  EventListener 中 onEvent方法
	}

	private long getDuring() {
		return System.currentTimeMillis() - startSpeakTime;
	}

	private void printResult(String text) {
		tvResult.append(text + "\n\n");
		//Log.i("TEst", text);
	}

	private long startSpeakTime;
	private long stopSpeakTime;

	private void start() {
		tvResult.setText("");
		btnStartRecord.setEnabled(false);

		String json = getOfflineParams().toString(); // 这里可以替换成你需要测试的json
		asr.send(SpeechConstant.ASR_START, json, null, 0, 0);

		printResult("启动识别，输入参数：" + json);
	}

	private JSONObject offlineParams;
	private JSONObject getOfflineParams() {
		if (offlineParams == null) {
			try {
				offlineParams = new JSONObject();
				offlineParams.put(SpeechConstant.PID, 1536); // 默认1536
				offlineParams.put(SpeechConstant.DECODER, 2); //
				offlineParams.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets:///baidu_speech_grammar.bsg");
				offlineParams.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN); // 语音活动检测
				offlineParams.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800); // 不开启长语音。开启VAD尾点检测，即静音判断的毫秒数。建议设置800ms-3000ms
				offlineParams.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);// 是否需要语音音频数据回调
				offlineParams.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);// 是否需要语音音量数据回调
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return offlineParams;
	}

	private void stop() {
		asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
	}


}