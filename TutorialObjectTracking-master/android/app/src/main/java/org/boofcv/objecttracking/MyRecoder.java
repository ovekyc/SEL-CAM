package org.boofcv.objecttracking;

import android.media.MediaRecorder;

import java.io.IOException;

/**
 * Created by chan on 16. 5. 17..
 */
public class MyRecoder extends MediaRecorder{
    public void initRecorder() {
        //setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        //CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        //setProfile(cpHigh);
        //setOutputFile("/storage/sdcard0/videocapture_example.mp4");
        //setMaxDuration(50000); // 50 seconds
        //setMaxFileSize(5000000); // Approximately 5 megabytes
    }

    public void prepareRecorder() {
        //recorder.setPreviewDisplay(holder.getSurface());

        try {
            prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            //finish();
        } catch (IOException e) {
            e.printStackTrace();
            //finish();
        }
    }

}
