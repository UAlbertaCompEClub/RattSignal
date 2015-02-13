package ca.jviau.rattsignal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import java.io.IOException;
import java.io.InputStream;


public class SignalActivity extends Activity {

    public static final String DEBUG_TAG = "SignalActivity";
    public static final String SENDER_ID = "652901739903";
    public static final String RESPONDING_KEY = "responding";

    public static MobileServiceClient mClient;
    private MobileServiceTable<SignalResponder> mTable;
    private boolean isResponding;
    private SignalFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SignalFragment())
                    .commit();
        }

        Log.d(DEBUG_TAG, "Getting isResponding from shared preferences");
        SharedPreferences sp = getSharedPreferences(SignalActivity.class.getSimpleName(), MODE_PRIVATE);
        isResponding = sp.getBoolean(RESPONDING_KEY, false);
    }

    public void onFragmentReady(SignalFragment fragment) {
        this.mFragment = fragment;
        setUpMobileService();
        setUpOnClicks();
        mFragment.setResponding(isResponding);
        refreshRespondingCount();
    }

    private void setUpMobileService() {
        try {
            Log.d(DEBUG_TAG, "Initialising azure mobile service...");
            JsonObject azure_info = parseJsonFile(R.raw.mobile_services);
            mClient = new MobileServiceClient(
                    azure_info.get("url").getAsString(),
                    azure_info.get("key").getAsString(),
                    this)
                    .withFilter(new ProgressFilter(this, mFragment.getProgressBar()));
            mTable = mClient.getTable(SignalResponder.class);
            NotificationsManager.handleNotifications(this, SENDER_ID, RattSignalHandler.class);
        } catch (Exception e) {
            e.printStackTrace();
            createAndShowDialog(new Exception("There was an error connecting to the mobile service."), "Error");
        }
    }

    private void setUpOnClicks() {
        mFragment.getSirenImageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respondToSignal(!getResponding());
            }
        });
        mFragment.getCountTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshRespondingCount();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(DEBUG_TAG, "Storing isResponding to shared preferences");
        SharedPreferences sp = getSharedPreferences(SignalActivity.class.getSimpleName(), MODE_PRIVATE);
        sp.edit().putBoolean(RESPONDING_KEY, isResponding).apply();
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 0);
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

    private void refreshRespondingCount() {
        Log.d(DEBUG_TAG, "refreshing responders count");

        new AsyncTask<Void, Void, Void>() {
          @Override
        public Void doInBackground(Void... params) {
              try {
                  MobileServiceList<SignalResponder> results = mTable.where().field("responding").eq(true).execute().get();
                  final int count = results.size();
                  Log.d(DEBUG_TAG, String.format("Retrieved count of %d responders.", count));

                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          if (count > 0) {
                              mFragment.getCountTextView().setText(String.format("%d responding", count));
                          } else {
                              mFragment.getCountTextView().setText("signal off");
                          }
                      }
                  });
              } catch (Exception e) {
                  e.printStackTrace();
                  createAndShowDialogOnUi(e, "Error");
              }
              return null;
          }
        }.execute();
    }

    /**
     * Sets the responding state of this device in the mobile service
     *
     * @param responding
     *              Boolean of if this device is responding or not
     */
    private void respondToSignal(final boolean responding) {
        Log.d(DEBUG_TAG, "Responding to signal: " + responding);

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    SignalResponder signal = new SignalResponder(Installation.id(SignalActivity.this), responding);
                    Log.d(DEBUG_TAG, "Putting value " + signal + " into mobile service");
                    mTable.insert(signal).get();
                    setResponding(responding);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFragment.setResponding(responding);
                        }
                    });
                    refreshRespondingCount();
                } catch (Exception e) {
                    e.printStackTrace();
                    createAndShowDialogOnUi(e, "Error");
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
        isResponding = responding;
    }

    /**
     * Gets if this device is responding to the call of RATT
     *
     * @return
     *          Signal responding state
     */
    private boolean getResponding() {
        return isResponding;
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogOnUi(final Exception exception, final String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialogOnUi(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialogOnUi(final String message, final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SignalActivity.this);

                builder.setMessage(message);
                builder.setTitle(title);
                builder.create().show();
            }
        });
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

        private ImageView mSirenImageView;
        private ImageView mRespondingImageView;
        private TextView mCountTextView;
        private ProgressBar mProgressBar;


        public SignalFragment() {
        }

        public ImageView getSirenImageView() {
            return mSirenImageView;
        }

        public ImageView getRespondingImageView() {
            return mRespondingImageView;
        }

        public TextView getCountTextView() {
            return mCountTextView;
        }

        public ProgressBar getProgressBar() {
            return mProgressBar;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.fragment_signal, container, false);
            this.mSirenImageView = (ImageView) view.findViewById(R.id.sirenImageView);
            this.mRespondingImageView = (ImageView) view.findViewById(R.id.respondingImageView);
            this.mCountTextView = (TextView) view.findViewById(R.id.countTextView);
            this.mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            mProgressBar.setVisibility(ProgressBar.GONE);
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Activity activity = getActivity();
            if (activity != null) {
                ((SignalActivity) activity).onFragmentReady(this);
            }
        }

        public void setResponding(boolean responding) {
            if (responding) {
                mRespondingImageView.setVisibility(View.VISIBLE);
            } else {
                mRespondingImageView.setVisibility(View.INVISIBLE);
            }
        }
    }
}
