package xwh.baidu.speech;

import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xwh on 2018/11/12.
 */
public class LocalSpeechActivity extends OnlineSpeechActivity {

	public static final String filePath = "/sdcard/speech.pcm";  // 临时文件
	public static final String IN_FILE_STREAM = "#xwh.baidu.speech.FileAudioInputStream.getVoiceInputStream()";

	@Override
	protected JSONObject getAsrParams() {
		super.getAsrParams();
		try {
			asrParams.put(SpeechConstant.IN_FILE, IN_FILE_STREAM);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return asrParams;
	}

}
