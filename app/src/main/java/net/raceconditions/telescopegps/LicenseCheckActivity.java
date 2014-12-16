package net.raceconditions.telescopegps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import com.google.android.vending.licensing.*;


/**
 * Created by ubuntu on 10/18/14.
 */
public class LicenseCheckActivity extends Activity{
    static boolean licensed = true;
    static boolean didCheck = false;
    static boolean checkingLicense = false;
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhPt1fngFRHWTh0YzwhlrrSxZgdFxdNN/cdzvzjxOZBk4eDfceGOALj7Ja2xVInrN39BctY6Lsav9gWnUF8zFYZ5jRexHpxyVLil4hdVnv6uRJFD2eX4D94CPnnBiJMqnRO1pjqGu/2qLJHPzB0qNrHwhQLk3ceUNlC6MN3ty93aT+DJKGwaj1kbqt5odGdUTVKulKHzY63aHkQsF7kaF/h9u1izd96kFdiV41oThtAUDgsJd4LX/XVyy+zFyd/WylxilLZN3LPYoqjiIQTkTVDhC2rJo8u33nJcbf6Wx+8dLOuuNTiyXduQWw1dQ4XdrpjLETWZVtN+wh00S10rdqwIDAQAB";

    LicenseCheckerCallback mLicenseCheckerCallback;
    LicenseChecker mChecker;
    Handler mHandler;
    SharedPreferences prefs;
    NexStarSplash.LicenseCheckCallback licenseCheckCallback;

    private static final byte[] SALT = new byte[]{
            -14, -3, 66, 78, -10, 122, 29, 13, -73, -58, 1, -92, -62, 101, 35, -54, -21, -52, -115,
            65
    };

    private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {

                setProgressBarIndeterminateVisibility(false);

            }
        });
    }

    protected void doCheck() {

        didCheck = false;
        checkingLicense = true;
        setProgressBarIndeterminateVisibility(false);

        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    protected void checkLicense(NexStarSplash.LicenseCheckCallback licenseCheckCallback) {

        this.licenseCheckCallback = licenseCheckCallback;

        Log.i("LICENSE", "checkLicense");
        mHandler = new Handler();

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);

        doCheck();
    }

    protected class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        public void allow(int reason) {
            Log.i("LICENSE", "allow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.allow));
            licensed = true;
            checkingLicense = false;
            didCheck = true;

            licenseCheckCallback.onLicenseAccepted();
        }

        public void dontAllow(int reason ) {
            Log.i("LICENSE", "dontAllow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.dont_allow));
            licensed = false;
            checkingLicense = false;
            didCheck = true;

            showDialog(0);
        }

        public void applicationError(int errorCode) {
            Log.i("LICENSE", "error: " + errorCode);
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            licensed = false;
            String result = String.format(getString(R.string.application_error), errorCode);
            checkingLicense = false;
            didCheck = true;

            //displayResult(result);
            showDialog(0);
        }
    }

    protected Dialog onCreateDialog(int id) {
        // We have only one dialog.
        return new AlertDialog.Builder(this)
                .setTitle(R.string.unlicensed_dialog_title)
                .setMessage(R.string.unlicensed_dialog_body)
                .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "http://market.android.com/details?id=" + getPackageName()));
                        startActivity(marketIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })

                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener(){
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        Log.i("License", "Key Listener");
                        finish();
                        return true;
                    }
                })
                .create();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChecker != null) {
            Log.i("License", "destroy checker");
            mChecker.onDestroy();
        }
    }
}
