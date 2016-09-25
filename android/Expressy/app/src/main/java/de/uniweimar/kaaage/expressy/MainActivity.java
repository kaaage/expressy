/**
 * Created by kaaage on 28/05/16.
 */

package de.uniweimar.kaaage.expressy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{
	private TextView textView;
	private TextView accData;
	private ImageView imageView;
	private float rotation = 0;
	private float rotationX = 0;
	private ExpressyDataReceiver mReceiver;
	private ImageView[][] puzzle = new ImageView[3][3];
	float dX, dY;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GridLayout puzzleLayout = (GridLayout) findViewById(R.id.gridLayoutPuzzle);

		Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.android_logo);

		Bitmap[][] bitmaps = createBitmaps(img);

		for(int i=0;i<3;i++){
			for(int j=0;j<3;j++){
				puzzle[i][j] = new ImageView(this);
				puzzle[i][j].setScaleType(ImageView.ScaleType.CENTER);
				puzzle[i][j].setImageDrawable(new BitmapDrawable(bitmaps[i][j]));
				puzzle[i][j].setOnTouchListener(this);

				GridLayout.LayoutParams param =new GridLayout.LayoutParams();
				param.height = GridLayout.LayoutParams.WRAP_CONTENT;
				param.width = GridLayout.LayoutParams.WRAP_CONTENT;
				param.rightMargin = 5;
				param.topMargin = 5;
				param.rowSpec = GridLayout.spec(i);
				param.columnSpec = GridLayout.spec(j);
				puzzle[i][j].setLayoutParams(param);
				puzzleLayout.addView(puzzle[i][j]);
			}
		}
	}

	public Bitmap[][] createBitmaps(Bitmap source) {
		Bitmap[][] bmp = new Bitmap[3][3];
		int k=0;
		int width=source.getWidth();
		int height=source.getHeight();
		for(int i=0;i<3;i++){
			for(int j=0;j<3;j++){
				bmp[i][j]= Bitmap.createBitmap(source, j*width/3, i*height/3, width/3, height/3);
			}
		}
		return bmp;
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

			case MotionEvent.ACTION_DOWN:
				imageView = (ImageView) v;
				rotation = imageView.getRotation();
				rotationX = imageView.getRotationX();
				dX = imageView.getX() - event.getRawX();
				dY = imageView.getY() - event.getRawY();
				break;

			case MotionEvent.ACTION_MOVE:
				imageView.animate()
						.x(event.getRawX() + dX)
						.y(event.getRawY() + dY)
						.setDuration(0)
						.start();
				break;
			case MotionEvent.ACTION_UP:
				imageView = null;
				rotation = 0;
				rotationX = 0;
			default:
				return false;
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

		// Get information back from the watchapp
		if (mReceiver == null)
		{
			mReceiver = new ExpressyDataReceiver("d3ce4ebe-7724-415f-aa83-d3fa6995b805", getApplicationContext())
			{
				@Override
				public void onReceiveData(double roll, double pitch)
				{
					if (imageView != null)
					{
						imageView.setRotation(rotation + (float) roll + 180);
						imageView.setRotationX(rotationX + (float) pitch);
					}

				}
			};
		}
	}

}
