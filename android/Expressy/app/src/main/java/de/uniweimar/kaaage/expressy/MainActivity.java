package de.uniweimar.kaaage.expressy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, OnMapReadyCallback
{
	public static final CameraPosition TOUR_EIFFEL =
			new CameraPosition.Builder().target(new LatLng(48.8560894, 2.2964442))
					.zoom(17f)
					.bearing(330)
					.tilt(80)
					.build();

	private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
	private GoogleMap mMap;

	private TextView textView;
	private TextView accData;
	private ImageView imageView;

	private ImageView[][] puzzle = new ImageView[3][3];

	private PebbleKit.PebbleDataReceiver mReceiver;

	// The tuple key corresponding to a vector received from the watch
	private static final int PP_KEY_CMD = 128;
	private static final int PP_KEY_X   = 1;
	private static final int PP_KEY_Y   = 2;
	private static final int PP_KEY_Z   = 3;

	@SuppressWarnings("unused")
	private static final int PP_CMD_INVALID = 0;
	private static final int PP_CMD_VECTOR  = 1;

	public static final int VECTOR_INDEX_X  = 0;
	public static final int VECTOR_INDEX_Y  = 1;
	public static final int VECTOR_INDEX_Z  = 2;

	private static int vector[] = new int[3];

	float [] i = new float[3];
	float [] o = new float[3];
	float ALPHA = 0.1f;

//	private float[] lowPass(float x, float y, float z) {
//		float[] filteredValues = new float[3];
//		filteredValues[0] = x * a + filteredValues[0] * (1.0 - a);
//		filteredValues[1] = y * a + filteredValues[1] * (1.0 – a);
//		filteredValues[2] = z * a + filteredValues[2] * (1.0 – a);
//
//		return filteredValues;
//	}
	protected float[] lowPass( float[] input, float[] output ) {
		if ( output == null ) return input;

		for ( int i=0; i<input.length; i++ ) {
//			output[i] = output[i] + ALPHA * (input[i] - output[i]);
			output[i] = (input[i] * ALPHA) + (output[i] * (1.0f - ALPHA));
		}
		return output;
	}
	float dX, dY;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		MySupportMapFragment mapFragment = (MySupportMapFragment) getSupportFragmentManager()
//				.findFragmentById(R.id.map);
//		mapFragment.getMapAsync(this);
		SupportMapFragment mapFragment =
				(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
	}

	/**
	 * Called when a touch event is dispatched to a view. This allows listeners to
	 * get a chance to respond before the target view.
	 *
	 * @param v     The view the touch event has been dispatched to.
	 * @param event The MotionEvent object containing full information about
	 *              the event.
	 * @return True if the listener has consumed the event, false otherwise.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		switch (event.getAction()) {

//			case MotionEvent.ACTION_DOWN:
//				imageView = (ImageView) v;
//				dX = imageView.getX() - event.getRawX();
//				dY = imageView.getY() - event.getRawY();
//				break;
//
//			case MotionEvent.ACTION_MOVE:
//				imageView.animate()
//						.x(event.getRawX() + dX)
//						.y(event.getRawY() + dY)
//						.setDuration(0)
//						.start();
//				break;
//			case MotionEvent.ACTION_UP:
//				imageView = null;
//			default:
//				return false;
		}
		return true;
	}

	/**
	 * Dispatch onResume() to fragments.  Note that for better inter-operation
	 * with older versions of the platform, at the point of this call the
	 * fragments attached to the activity are <em>not</em> resumed.  This means
	 * that in some cases the previous state may still be saved, not allowing
	 * fragment transactions that modify the state.  To correctly interact
	 * with fragments in their proper state, you should instead override
	 * {@link #onResumeFragments()}.
	 */
	@Override
	protected void onResume()
	{
		super.onResume();

		boolean isConnected = PebbleKit.isWatchConnected(this);
		Toast.makeText(this, "Pebble " + (isConnected ? "is" : "is not") + " connected!", Toast.LENGTH_LONG).show();
		if (isConnected)
		{
			// Construct output String
			StringBuilder builder = new StringBuilder();
			builder.append("Pebble Info\n\n");

			// What is the firmware version?
			PebbleKit.FirmwareVersionInfo info = PebbleKit.getWatchFWVersion(this);
			builder.append("Firmware version: ");
			builder.append(info.getMajor()).append(".");
			builder.append(info.getMinor()).append("\n");

			// Is AppMesage supported?
			boolean appMessageSupported = PebbleKit.areAppMessagesSupported(this);
			builder.append("AppMessage supported: " + (appMessageSupported ? "true" : "false"));

//			textView.setText(builder);

			// Get information back from the watchapp
			if (mReceiver == null)
			{
				mReceiver = new PebbleKit.PebbleDataReceiver(UUID.fromString("d3ce4ebe-7724-415f-aa83-d3fa6995b805"))
				{

					@Override
					public void receiveData(Context context, int id, PebbleDictionary dict)
					{
						// Always ACKnowledge the last message to prevent timeouts
						PebbleKit.sendAckToPebble(getApplicationContext(), id);

						final Long cmdValue = dict.getInteger(PP_KEY_CMD);
						if (cmdValue == null)
						{
							return;
						}

						if (cmdValue.intValue() == PP_CMD_VECTOR)
						{

							// Capture the received vector.
							final Long xValue = dict.getInteger(PP_KEY_X);
							if (xValue != null)
							{
								vector[VECTOR_INDEX_X] = xValue.intValue();
							}

							final Long yValue = dict.getInteger(PP_KEY_Y);
							if (yValue != null)
							{
								vector[VECTOR_INDEX_Y] = yValue.intValue();
							}

							final Long zValue = dict.getInteger(PP_KEY_Z);
							if (zValue != null)
							{
								vector[VECTOR_INDEX_Z] = zValue.intValue();
							}
						}

//						int x = vector[VECTOR_INDEX_X];
//						int y = vector[VECTOR_INDEX_Y];
//						int z = vector[VECTOR_INDEX_Z];

						i[0] = vector[VECTOR_INDEX_X];
						i[1] = vector[VECTOR_INDEX_Y];
						i[2] = vector[VECTOR_INDEX_Z];
						o = lowPass(i,o);
						float x = o[0];
						float y = o[1];
						float z = o[2];

						double roll = (Math.atan2(y,z) * 180) / Math.PI;
						double pitch = (Math.atan2(x, Math.sqrt( y*y + z*z )) * 180) / Math.PI;

//						accData.setText("x: " + vector[VECTOR_INDEX_X] + "\ty: " + vector[VECTOR_INDEX_Y] + "\tz:" + vector[VECTOR_INDEX_Z] + "\n");
//						accData.setText("roll: " + roll+180 + "\tpitch: " + pitch + "\n");

						if (mMap != null)
						{
							roll = roll + 180;
							if (5 < roll && roll < 90)
								setBearing(roll);
							else if ((270 < roll && roll < 355))
								setBearing(-(360 - roll));
//							System.out.print("roll: " + roll + " roll+180: " + (roll+180) + " pitch: " + pitch);
							if ((-90 < pitch && pitch < -5) || (5 < pitch && pitch < 90))
								moveForward(pitch);
//							imageView.setRotation((float) roll + 180);
//							imageView.setRotationX((float) pitch);
						}
					}
				};

			}

			// Register the receiver to get data
			PebbleKit.registerReceivedDataHandler(this, mReceiver);

			// Launch the sports app
			PebbleKit.startAppOnPebble(this, UUID.fromString("d3ce4ebe-7724-415f-aa83-d3fa6995b805"));
		}
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap)
	{
		mMap = googleMap;
		mMap.setBuildingsEnabled(true);
		mMap.moveCamera(CameraUpdateFactory.newCameraPosition(TOUR_EIFFEL));
//		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

//		mc.put(mMap.addMarker(new MarkerOptions().position(new LatLng(50.97794,11.028965)).title("Erfurt")),
//				mMap.addCircle(new CircleOptions().center(new LatLng(50.97794,11.028965)).radius(0).strokeColor(Color.RED).strokeWidth(8)));

//		mMap.setOnCameraChangeListener(this);
//		mMap.setOnMapLongClickListener(this);

//		this.moveToMyLocation();
	}

	private void moveToMyLocation()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
		}
		else
		{
			mMap.setMyLocationEnabled(true);

			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();

			Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
			if (location != null)
			{
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						new LatLng(location.getLatitude(), location.getLongitude()), 13));

				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(10).build();
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch (requestCode)
		{
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
			{
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{
					moveToMyLocation();
				}
				else
				{
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
							MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
				}

				return;
			}
		}
	}

	/**
	 * When the map is not ready the CameraUpdateFactory cannot be used. This should be called on
	 * all entry points that call methods on the Google Maps API.
	 */
	private boolean checkReady() {
		if (mMap == null) {
			Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public void setBearing(double angle)
	{
		CameraPosition currentCameraPosition = mMap.getCameraPosition();
		double oldAngle = currentCameraPosition.bearing;
		double newAngle = oldAngle + (angle/5);
		CameraPosition cameraPosition = new CameraPosition.Builder(currentCameraPosition)
				.bearing((float)newAngle).build();
		changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	public void setTilt(double angle)
	{
		CameraPosition currentCameraPosition = mMap.getCameraPosition();
		double newAngle = angle;
		CameraPosition cameraPosition = new CameraPosition.Builder(currentCameraPosition)
				.tilt((float)newAngle).build();
		changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	public void moveForward(double pitch)
	{
		CameraPosition currentCameraPosition = mMap.getCameraPosition();
		float bearing = 360 - currentCameraPosition.bearing;
		LatLng oldlatLng = currentCameraPosition.target;
		double lat = oldlatLng.latitude + 0.000003 * Math.cos(Math.toRadians(bearing)) * pitch;
		double lng = oldlatLng.longitude + 0.000003 * (-Math.sin(Math.toRadians(bearing))) * pitch;
		LatLng newLatLng = new LatLng(lat, lng);
		changeCamera(CameraUpdateFactory.newLatLng(newLatLng));
	}

	/**
	 * Called when the tilt more button (the one with the /) is clicked.
	 */
	public void onTiltMore(View view) {
		if (!checkReady()) {
			return;
		}

		CameraPosition currentCameraPosition = mMap.getCameraPosition();
		float currentTilt = currentCameraPosition.tilt;
		float newTilt = currentTilt + 10;

		newTilt = (newTilt > 90) ? 90 : newTilt;

		CameraPosition cameraPosition = new CameraPosition.Builder(currentCameraPosition)
				.tilt(newTilt).build();

		changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	/**
	 * Called when the tilt less button (the one with the \) is clicked.
	 */
	public void onTiltLess(View view) {
		if (!checkReady()) {
			return;
		}

		CameraPosition currentCameraPosition = mMap.getCameraPosition();

		float currentTilt = currentCameraPosition.tilt;

		float newTilt = currentTilt - 10;
		newTilt = (newTilt > 0) ? newTilt : 0;

		CameraPosition cameraPosition = new CameraPosition.Builder(currentCameraPosition)
				.tilt(newTilt).build();

		changeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	private void changeCamera(CameraUpdate update) {
		changeCamera(update, null);
	}

	/**
	 * Change the camera position by moving or animating the camera depending on the state of the
	 * animate toggle button.
	 */
	private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
		if (false) {
//			if (mCustomDurationToggle.isChecked()) {
//				int duration = mCustomDurationBar.getProgress();
//				// The duration must be strictly positive so we make it at least 1.
//				mMap.animateCamera(update, Math.max(duration, 1), callback);
//			} else {
				mMap.animateCamera(update, callback);
//			}
		} else {
			mMap.moveCamera(update);
		}
	}
}
