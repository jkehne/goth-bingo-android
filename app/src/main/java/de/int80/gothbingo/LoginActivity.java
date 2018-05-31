package de.int80.gothbingo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    static final String PLAYER_NAME_KEY = LoginActivity.class.getName() + ".PLAYER_NAME";
    static final String GAME_ID_KEY = LoginActivity.class.getName() + ".GAME_ID";

    private static LoginActivity currentInstance;

    static LoginActivity getCurrentInstance() {
        return currentInstance;
    }

    private void setEventListeners() {
        Button signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignIn();
            }
        });

        EditText gameIDField = findViewById(R.id.gameID);
        gameIDField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                handleSignIn();
                return true;
            }
        });
    }

    private void restoreLoginInfo() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        EditText playerNameField = findViewById(R.id.playerName);
        playerNameField.setText(prefs.getString(PLAYER_NAME_KEY, ""));

        EditText gameIDField = findViewById(R.id.gameID);
        gameIDField.setText(prefs.getString(GAME_ID_KEY, ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setEventListeners();

        restoreLoginInfo();

        new UpdateChecker(getApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Nullable
    private String extractFieldContents(int fieldID) {
        EditText field = findViewById(fieldID);

        if (field == null)
            return null;

        return field.getText().toString().trim();
    }

    private void handleSignIn() {
        currentInstance = null;

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(PLAYER_NAME_KEY, extractFieldContents(R.id.playerName));
        intent.putExtra(GAME_ID_KEY, extractFieldContents(R.id.gameID));
        startActivity(intent);
    }

    private void saveLoginInfo(String playerName, String gameID) {
        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();

        if (playerName != null)
            prefs.putString(PLAYER_NAME_KEY, playerName);

        if (gameID != null)
            prefs.putString(GAME_ID_KEY, gameID);

        prefs.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentInstance = this;
    }

    @Override
    protected void onStop() {
        super.onStop();

        currentInstance = null;

        saveLoginInfo(extractFieldContents(R.id.playerName), extractFieldContents(R.id.gameID));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_activity_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ShareButton:
                handleShare();
                return true;
            case R.id.GithubButton:
                handleGithub();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleGithub() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(getString(R.string.github_url)));
        startActivity(browserIntent);
    }

    private void handleShare() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_url));

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
    }
}