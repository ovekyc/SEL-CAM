package org.boofcv.objecttracking;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.ConfigTld;
import boofcv.abst.tracker.MeanShiftLikelihoodType;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoDisplayActivity;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Activity which opens a camera and lets the user select objects to track and switch between
 * different trackers.  The user selects an object by clicking and dragging until the drawn
 * rectangle fills the object.  To select a new object click reset.
 */
public class ObjectTrackerActivity extends VideoDisplayActivity
        implements AdapterView.OnItemSelectedListener, View.OnTouchListener
{

    private MySocket mSocket;
    private BluetoothThread bluetoothThread;
    Spinner spinnerView;
    //private RecorderThread recorderThread;
    private MediaRecorder recorder;
    private static String FILEPATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    private static String FILENAME = "ObjectTracking.mp4";
    private File output;

    int mode = 0;

    // size of the minimum square which the user can select
    final static int MINIMUM_MOTION = 20;

    Point2D_I32 click0 = new Point2D_I32();
    Point2D_I32 click1 = new Point2D_I32();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout controls = (LinearLayout)inflater.inflate(R.layout.objecttrack_controls,null);

        LinearLayout parent = getViewContent();
        parent.addView(controls);

        FrameLayout iv = getViewPreview();
        iv.setOnTouchListener(this);

        spinnerView = (Spinner)controls.findViewById(R.id.spinner_algs);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tracking_objects, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerView.setAdapter(adapter);
        spinnerView.setOnItemSelectedListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        startObjectTracking(spinnerView.getSelectedItemPosition());

        mSocket = ((MySocket) getApplicationContext());
        bluetoothThread = new BluetoothThread(getApplicationContext(), mSocket.getSocket());
        //recorderThread = new RecorderThread();
        if(output == null){
            output = new File(FILEPATH, FILENAME);
        }
        if(recorder == null){

            recorder = new MediaRecorder();
            recorder.setCamera(mCamera);

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

            recorder.setOutputFile(output.getAbsolutePath());

            recorder.setMaxDuration(10000);

            Log.e("FILE", output.getAbsolutePath());
        }


        // uncomment the line below to visually show the FPS.
        // The FPS is affected by the camera speed and processing power.  The camera speed
        // is often determined by ambient lighting conditions
//        setShowFPS(true);
    }
    
    public void recordVideo( View view ) {
        if(output == null){
            output = new File(FILEPATH, FILENAME);
        }
        //recorderThread = new RecorderThread();
        if(recorder == null){
            
            mCamera.unlock();
            recorder = new MediaRecorder();
            recorder.setCamera(mCamera);
            
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            
            recorder.setOutputFile(output.getAbsolutePath());
            
            recorder.setMaxDuration(10000);
            try {
                recorder.prepare();
                recorder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            
        }
    }
    public void finishRec(View view){
        recorder.stop();
        recorder.release();
    }


    @Override
    protected Camera openConfigureCamera( Camera.CameraInfo cameraInfo )
    {
        Camera mCamera = UtilVarious.selectAndOpenCamera(cameraInfo, this);
        Camera.Parameters param = mCamera.getParameters();

        // Select the preview size closest to 320x240
        // Smaller images are recommended because some computer vision operations are very expensive
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(UtilVarious.closest(sizes, 320, 240));
        param.setPreviewSize(s.width,s.height);
        mCamera.setParameters(param);




        return mCamera;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id ) {
        startObjectTracking(pos);
    }

    private void startObjectTracking(int pos) {
        TrackerObjectQuad tracker = null;
        ImageType imageType = null;

        switch (pos) {
            case 0:
                imageType = ImageType.single(ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.circulant(null, ImageUInt8.class);
                break;

            case 1:
                imageType = ImageType.ms(3, ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(false),imageType);
                break;

            case 2:
                imageType = ImageType.ms(3, ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),imageType);
                break;

            case 3:
                imageType = ImageType.ms(3, ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,256, MeanShiftLikelihoodType.HISTOGRAM,imageType);
                break;

            case 4:{
                imageType = ImageType.single(ImageUInt8.class);
                SfotConfig config = new SfotConfig();
                config.numberOfSamples = 10;
                config.robustMaxError = 30;
                tracker = FactoryTrackerObjectQuad.sparseFlow(config,ImageUInt8.class,null);
            }break;

            case 5:
                imageType = ImageType.single(ImageUInt8.class);
                tracker = FactoryTrackerObjectQuad.tld(new ConfigTld(false),ImageUInt8.class);
                break;

            default:
                throw new RuntimeException("Unknown tracker: "+pos);
        }
        setProcessing(new TrackingProcessing(tracker,imageType) );
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if( mode == 0 ) {
            if(MotionEvent.ACTION_DOWN == motionEvent.getActionMasked()) {
                click0.set((int) motionEvent.getX(), (int) motionEvent.getY());
                click1.set((int) motionEvent.getX(), (int) motionEvent.getY());
                mode = 1;
            }
        } else if( mode == 1 ) {
            if(MotionEvent.ACTION_MOVE == motionEvent.getActionMasked()) {
                click1.set((int)motionEvent.getX(),(int)motionEvent.getY());
            } else if(MotionEvent.ACTION_UP == motionEvent.getActionMasked()) {
                click1.set((int)motionEvent.getX(),(int)motionEvent.getY());
                mode = 2;
            }
        }
        return true;
    }

    public void resetPressed( View view ) {
        mode = 0;
        if(recorder != null){
            recorder.reset();
            recorder.release();
        }
    }

    protected class TrackingProcessing<T extends ImageBase> extends VideoImageProcessing<MultiSpectral<ImageUInt8>>
    {

        T input;
        ImageType<T> inputType;

        int beforeX,beforeY;
        long prevTime;

        TrackerObjectQuad tracker;
        boolean visible;

        Quadrilateral_F64 location = new Quadrilateral_F64();

        Paint paintSelected = new Paint();
        Paint paintLine0 = new Paint();
        Paint paintLine1 = new Paint();
        Paint paintLine2 = new Paint();
        Paint paintLine3 = new Paint();
        private Paint textPaint = new Paint();

        protected TrackingProcessing(TrackerObjectQuad tracker , ImageType<T> inputType) {
            super(ImageType.ms(3,ImageUInt8.class));
            this.inputType = inputType;

            if( inputType.getFamily() == ImageType.Family.SINGLE_BAND ) {
                input = inputType.createImage(1,1);
            }

            mode = 0;
            this.tracker = tracker;

            paintSelected.setColor(Color.argb(0xFF / 2, 0xFF, 0, 0));

            paintLine0.setColor(Color.RED);
            paintLine0.setStrokeWidth(3f);
            paintLine1.setColor(Color.MAGENTA);
            paintLine1.setStrokeWidth(3f);
            paintLine2.setColor(Color.BLUE);
            paintLine2.setStrokeWidth(3f);
            paintLine3.setColor(Color.GREEN);
            paintLine3.setStrokeWidth(3f);

            // Create out paint to use for drawing
            textPaint.setARGB(255, 200, 0, 0);
            textPaint.setTextSize(60);

            beforeX = -1000;
            beforeY = -1000;
            prevTime = System.currentTimeMillis();
        }

        @Override
        protected void process(MultiSpectral<ImageUInt8> input, Bitmap output, byte[] storage)
        {
            updateTracker(input);
            visualize(input, output, storage);
        }

        private void updateTracker(MultiSpectral<ImageUInt8> color) {
            if( inputType.getFamily() == ImageType.Family.SINGLE_BAND ) {
                input.reshape(color.width,color.height);
                ConvertImage.average(color, (ImageUInt8) input);
            } else {
                input = (T)color;
            }

            if( mode == 2 ) {
                imageToOutput(click0.x, click0.y, location.a);
                imageToOutput(click1.x, click1.y, location.c);

                // make sure the user selected a valid region
                makeInBounds(location.a);
                makeInBounds(location.c);

                if( movedSignificantly(location.a,location.c) ) {
                    // use the selected region and start the tracker
                    location.b.set(location.c.x, location.a.y);
                    location.d.set( location.a.x, location.c.y );

                    tracker.initialize(input, location);
                    visible = true;
                    mode = 3;

                    try {
                        recorder.prepare();
                    } catch (IOException e) {
                        Log.e("Recorder", e.toString());
                    }
                    recorder.start();


                } else {
                    // the user screw up. Let them know what they did wrong
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ObjectTrackerActivity.this, "Drag a larger region", Toast.LENGTH_SHORT).show();
                        }
                    });
                    mode = 0;
                }
            } else if( mode == 3 ) {
                int px = (int) (location.getA().getX() + location.getC().getX());
                int py = (int) (location.getA().getY() + location.getC().getY());
                px /= 2;
                py /= 2;
                long curTime = System.currentTimeMillis();
                if(curTime - prevTime > 300 && Math.abs(px+py-beforeX-beforeY) > 5) {
                    bluetoothThread.run(px, py);
                    beforeY = py;
                    beforeX = px;
                    prevTime = curTime;
                }
                visible = tracker.process(input, location);
            }
        }

        private void visualize(MultiSpectral<ImageUInt8> color, Bitmap output, byte[] storage) {
            ConvertBitmap.multiToBitmap(color, output, storage);
            Canvas canvas = new Canvas(output);

            if( mode == 1 ) {
                Point2D_F64 a = new Point2D_F64();
                Point2D_F64 b = new Point2D_F64();

                imageToOutput(click0.x, click0.y, a);
                imageToOutput(click1.x, click1.y, b);

                canvas.drawRect((int)a.x,(int)a.y,(int)b.x,(int)b.y,paintSelected);
            } else if( mode >= 2 ) {
                if( visible ) {
                    Quadrilateral_F64 q = location;

                    drawLine(canvas,q.a,q.b,paintLine0);
                    drawLine(canvas,q.b,q.c,paintLine1);
                    drawLine(canvas,q.c,q.d,paintLine2);
                    drawLine(canvas,q.d,q.a,paintLine3);
                } else {
                    canvas.drawText("?",color.width/2,color.height/2,textPaint);
                }
            }
        }

        private void drawLine( Canvas canvas , Point2D_F64 a , Point2D_F64 b , Paint color ) {
            canvas.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y,color);
        }

        private void makeInBounds( Point2D_F64 p ) {
            if( p.x < 0 ) p.x = 0;
            else if( p.x >= input.width )
                p.x = input.width - 1;

            if( p.y < 0 ) p.y = 0;
            else if( p.y >= input.height )
                p.y = input.height - 1;

        }

        private boolean movedSignificantly( Point2D_F64 a , Point2D_F64 b ) {
            if( Math.abs(a.x-b.x) < MINIMUM_MOTION )
                return false;
            if( Math.abs(a.y-b.y) < MINIMUM_MOTION )
                return false;

            return true;
        }
    }
}
