package com.se1.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.se1.DropBox.Constants;
import com.se1.DropBox.DropboxDownload;
import com.se1.DropBox.UploadFile;
import com.se1.DropBox.Utils;
import com.se1.dao.DatabaseOperation;
import com.se1.dao.User;
import com.se1.main.MainActivity;
import com.se1.main.R;

import java.io.File;
import java.util.Date;

public class HomeActivity extends SherlockFragmentActivity implements View.OnClickListener {
    private DatabaseOperation datasource;
    /*
    Dropbox variable
     */
    private static final int TAKE_PHOTO = 1;
    private Button btnUpload, boxId;
    private final String DIR = "/";
    private File f;
    private boolean mLoggedIn, onResume;
    private DropboxAPI<AndroidAuthSession> mApi;
    /*
    Dropbox variable
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_activity_home);
        /*DropBox*/
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
         // checkAppKeySetup();
        setLoggedIn(false);




        /*End of NavDrawer code*/

        setContentView(R.layout.activity_home);
        final Button removeSignIn   = (Button)findViewById(R.id.removeSignIn);
        boxId   = (Button)findViewById(R.id.boxId);
        boxId.setOnClickListener(this);
        //btnDownload.setOnClickListener(this)
        datasource = new DatabaseOperation(this);

        datasource.open();
        removeSignIn.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        User user=datasource.getUserDetail();
                        if(user!=null && ((user.getEmailId()!= null || user.getEmailId().equalsIgnoreCase("")) && user.getLoggedIn() == 1 ))
                        {
                            datasource.removeSignIn(user.getEmailId());
                        }
                    }

                });




    }
    //Navigate to registration page
    public void goToResetPassword(View view)
    {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        startActivity(intent);
    }
    //Navigate to registration page
    public void goToLoginPage(View view)
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

        /*Drop box code start */


    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(Constants.DROPBOX_APP_KEY,
                Constants.DROPBOX_APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0],
                    stored[1]);
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE,
                    accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE);
        }

        return session;
    }

    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == boxId) {
            startActivity(new Intent(HomeActivity.this, DropboxDownload.class));
        } else if (v == btnUpload) {
            createDir();
            if (mLoggedIn) {
                logOut();
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            f = new File(Utils.getPath(),new Date().getTime()+".jpg");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            startActivityForResult(intent, TAKE_PHOTO);
        }
    }

    private void logOut() {
        mApi.getSession().unlink();

        clearKeys();
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    private void createDir() {
        File dir = new File(Utils.getPath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO) {
//				f = new File(Utils.getPath() + "/temp.jpg");
                if (Utils.isOnline(HomeActivity.this)) {
                    mApi.getSession().startAuthentication(HomeActivity.this);
                    onResume = true;
                } else {
                    Utils.showNetworkAlert(HomeActivity.this);
                }
            }
        }
    }

    public void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            //instead of f send file from sd card
            UploadFile upload = new UploadFile(HomeActivity.this, mApi, DIR, f);
            upload.execute();
            onResume = false;

        }
    }

    private void storeKeys(String key, String secret) {
        SharedPreferences prefs = getSharedPreferences(
                Constants.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Constants.ACCESS_KEY_NAME, key);
        edit.putString(Constants.ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    @Override
    protected void onResume() {

        AndroidAuthSession session = mApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(onResume);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:"
                        + e.getLocalizedMessage());
            }
        }
        super.onResume();
    }

    /*DropBox code End*/

}
