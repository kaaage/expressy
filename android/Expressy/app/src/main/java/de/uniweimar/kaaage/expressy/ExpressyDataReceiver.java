package de.uniweimar.kaaage.expressy;

import android.content.Context;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Created by kaaage on 25/09/16.
 */
public abstract class ExpressyDataReceiver extends PebbleKit.PebbleDataReceiver
{
	private PebbleKit.PebbleDataReceiver mReceiver;
	private static int vector[] = new int[3];
	private Context context;

	public ExpressyDataReceiver(String appId, Context context)
	{
		super(UUID.fromString(appId));
		this.context = context;

		boolean isConnected = PebbleKit.isWatchConnected(context);
		Toast.makeText(context, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();

//		// Construct output String
//		StringBuilder builder = new StringBuilder();
//		builder.append("Pebble Info\n\n");
//
//		// What is the firmware version?
//		PebbleKit.FirmwareVersionInfo info = PebbleKit.getWatchFWVersion(this);
//		builder.append("Firmware version: ");
//		builder.append(info.getMajor()).append(".");
//		builder.append(info.getMinor()).append("\n");
//
//		// Is AppMesage supported?
//		boolean appMessageSupported = PebbleKit.areAppMessagesSupported(this);
//		builder.append("AppMessage supported: " + (appMessageSupported ? "true" : "false"));

		// Register the receiver to get data
		PebbleKit.registerReceivedDataHandler(context, this);

		// Launch the sports app
		PebbleKit.startAppOnPebble(context, UUID.fromString(appId));
	}

	@Override
	public void receiveData(Context context, int id, PebbleDictionary dict)
	{
		// Always ACKnowledge the last message to prevent timeouts
		PebbleKit.sendAckToPebble(this.context, id);

		final Long cmdValue = dict.getInteger(128);
		if (cmdValue == null)
		{
			return;
		}

		if (cmdValue.intValue() == 1)
		{

			// Capture the received vector.
			final Long xValue = dict.getInteger(1);
			if (xValue != null)
			{
				vector[0] = xValue.intValue();
			}

			final Long yValue = dict.getInteger(2);
			if (yValue != null)
			{
				vector[1] = yValue.intValue();
			}

			final Long zValue = dict.getInteger(3);
			if (zValue != null)
			{
				vector[2] = zValue.intValue();
			}
		}

		int x = vector[0];
		int y = vector[1];
		int z = vector[2];

		double roll = (Math.atan2(y,z) * 180) / Math.PI;
		double pitch = (Math.atan2(x, Math.sqrt( y*y + z*z )) * 180) / Math.PI;

//		accData.setText("x: " + vector[INDEX_X] + "\ty: " + vector[INDEX_Y] + "\tz:" + vector[INDEX_Z] + "\n");
//		accData.setText("roll: " + roll+180 + "\tpitch: " + pitch + "\n");

		onReceiveData(roll, pitch);
	}

	public abstract void onReceiveData(double roll, double pitch);
}
