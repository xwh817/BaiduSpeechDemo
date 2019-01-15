package xwh.baidu.speech;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Created by xwh on 2019/1/14.
 */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "MusicIntentReceiver";
	private Context mContext;

	private static EventObserver mEventObserver;
	public interface EventObserver{
		void onEvent(KeyEvent keyEvent);
	}
	public static void setEventObserver(EventObserver observer) {
		mEventObserver = observer;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
			Log.i(LOG_TAG, "ACTION_MEDIA_BUTTON!");

			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
			if (mEventObserver != null) {
				mEventObserver.onEvent(keyEvent);
			}
			switch (keyEvent.getKeyCode()) {
				case KeyEvent.KEYCODE_HEADSETHOOK:
					Toast.makeText(context, "hook",Toast.LENGTH_SHORT).show();
					break;

				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					Toast.makeText(context, "PLAY_PAUSE",Toast.LENGTH_SHORT).show();
					break;

				case KeyEvent.KEYCODE_MEDIA_PLAY:
					Toast.makeText(context, "PLAY",Toast.LENGTH_SHORT).show();
					Log.d(LOG_TAG, "KEYCODE_MEDIA_PLAY!");
					break;

				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					Toast.makeText(context, "PAUSE", Toast.LENGTH_SHORT).show();
					Log.d(LOG_TAG, "KEYCODE_MEDIA_PAUSE!");
					break;

				case KeyEvent.KEYCODE_MEDIA_STOP:
					Toast.makeText(context, "STOP",Toast.LENGTH_SHORT).show();
					break;

				case KeyEvent.KEYCODE_MEDIA_NEXT:
					Toast.makeText(context, "NEXT",Toast.LENGTH_SHORT).show();
					break;

				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					Toast.makeText(context, "PREVIOUS",Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}
}
