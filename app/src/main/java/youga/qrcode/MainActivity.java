package youga.qrcode;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import zxing.core.BarcodeResult;
import zxing.core.CaptureManager;
import zxing.core.CompoundBarcodeView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    CompoundBarcodeView mBarcodeView;
    CaptureManager mCaptureManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBarcodeView = (CompoundBarcodeView) findViewById(R.id.compoundBarcodeView);


        mCaptureManager = new CaptureManager(this, mBarcodeView, new CaptureManager.CaptureCallBack() {
            @Override
            public void onResult(BarcodeResult rawResult) {

                Log.i(TAG, rawResult.toString());
            }
        });

        mCaptureManager.initializeFromIntent(getIntent(), savedInstanceState);
        mCaptureManager.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCaptureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCaptureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCaptureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCaptureManager.onSaveInstanceState(outState);
    }
}
