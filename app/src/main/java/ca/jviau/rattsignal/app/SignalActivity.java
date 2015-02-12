package ca.jviau.rattsignal.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ProgressBar;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.io.IOException;
import java.io.InputStream;


public class SignalActivity extends ActionBarActivity {

    public static final String SENDER_ID = "113";
    public static final String RESPONDING_KEY = "responding";

    public static MobileServiceClient mClient;
    private MobileServiceTable<SignalResponder> mTable;

    private ImageView mSirenImageView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SignalFragment())
                    .commit();
        }



        this.mSirenImageView = (ImageView) findViewById(R.id.sirenImageView);
        this.mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        setUpMobileService();
        setUpImageViewOnClick();
    }

    private void setUpMobileService() {
        try {
            JsonObject azure_info = parseJsonFile(R.raw.mobile_services);
            mClient = new MobileServiceClient(
                    azure_info.get("url").getAsString(),
                    azure_info.get("key").getAsString(),
                    this)
                    .withFilter(new ProgressFilter(this, mProgressBar));

            mTable = mClient.getTable(SignalResponder.class);
            NotificationsManager.handleNotifications(this, SENDER_ID, RattSignalHandler.class);
        } catch (Exception e) {
            createAndShowDialog(new Exception("There was an error connecting to the mobile service."), "Error");
        }
    }

    private void setUpImageViewOnClick() {
        mSirenImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respondToSignal(!getResponding());
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_signal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Parses the JSON file identified by the Android R.raw id.
     *
     * @param rawFileId
     *          android R.raw id of the file
     * @return
     *          JsonObject of the parsed file
     * @throws IOException
     *          Thrown if there is an error opening/reading the raw resource
     */
    private JsonObject parseJsonFile(final int rawFileId) throws IOException {
        InputStream is = getResources().openRawResource(rawFileId);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return (JsonObject) new JsonParser().parse(new String(buffer, "UTF-8"));
    }

    /**
     * Sets the responding state of this device in the mobile service
     *
     * @param responding
     *              Boolean of if this device is responding or not
     */
    private void respondToSignal(final boolean responding) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {

                try {
                    mTable.insert(new SignalResponder(Installation.id(SignalActivity.this), responding)).get();
                    setResponding(responding);
                } catch (Exception e) {
                    createAndShowDialog(e, "Error");
                }

                return null;
            }

        }.execute();
    }

    /**
     * Updates shared preferences with if this device is responding or not
     *
     * @param responding
     *              If this device is responding to the call of RATT
     */
    private void setResponding(final boolean responding) {
        SharedPreferences sp = getSharedPreferences(SignalActivity.class.getSimpleName(), MODE_PRIVATE);
        sp.edit().putBoolean(RESPONDING_KEY, responding).apply();
    }

    /**
     * Gets if this device is responding to the call of RATT
     *
     * @return
     *          Signal responding state
     */
    private boolean getResponding() {
        SharedPreferences sp = getSharedPreferences(SignalActivity.class.getSimpleName(), MODE_PRIVATE);
        return sp.getBoolean(RESPONDING_KEY, false);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SignalFragment extends Fragment {

        public SignalFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_signal, container, false);
        }
    }
}
