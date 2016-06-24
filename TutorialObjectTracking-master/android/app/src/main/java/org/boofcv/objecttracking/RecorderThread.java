package org.boofcv.objecttracking;

import android.os.AsyncTask;

/**
 * Created by chan on 16. 5. 17..
 */
public class RecorderThread extends AsyncTask<Object, Object, Object> {
    private static final String TAG = "RecoderThread";
    public MyRecoder recoder;

    @Override
    protected void onPreExecute() {
        recoder = new MyRecoder();
        recoder.initRecorder();
        recoder.prepareRecorder();
    }

    protected Object doInBackground(Object...params){
        recoder.start();
        return null;
    }

    @Override
    protected void onCancelled() {
        recoder.stop();
    }

}//end of thread