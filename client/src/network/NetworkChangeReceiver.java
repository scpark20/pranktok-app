package network;

import com.scpark.prankcallclient.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import session.Session;
import voice.AudioPlayer;
import voice.AudioRecorder;

import voice.VoiceEncoder;

public class NetworkChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//String status = NetworkUtil.getConnectivityStatusString(context);
		//Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
		int status = NetworkUtil.getConnectivityStatus(context);
		SessionManager.getInstance().reconnect();
		//Session.getInstance().OnNetworkChanged();
	}

}
