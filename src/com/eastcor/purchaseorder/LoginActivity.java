package com.eastcor.purchaseorder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.eastcor.purchaseorder.USER";
	public static final String EXTRA_KEY = "com.eastcor.purchaseorder.KEY";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	public static final int IA_KEY = 8675309;
	public static String ticketKey;
	public static final String EXTRA_USER = "com.eastcor.purchaseorder.USER";
	public static final int HTTP_TIMEOUT = 10000;
	public static URL host;
	public static String token = null;

	// Values for email and password at the time of the login attempt.
	private String user;
	private String pass;

	// UI references.
	private Button mLoginButton;
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	public static final String PREFS = "LoginPrefs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Intent intent = getIntent();
		boolean timeout = intent.getBooleanExtra(POListActivity.EXTRA_TIMEOUT, false);
		if(timeout) {
		    Toast.makeText(this, "Your login has expired.", Toast.LENGTH_LONG).show();
		}
		// Set up the login form.

		mLoginButton = (Button) findViewById(R.id.sign_in_button);
		mUsernameView = (EditText) findViewById(R.id.username);
		mUsernameView.setText(user);
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							mLoginButton.setPressed(true);
							mLoginButton.performClick();
							return true;
						}
						return false;
					}
				});
		mPasswordView.setOnKeyListener(new View.OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_ENTER:
					mLoginButton.setPressed(true);
					mLoginButton.performClick();
					return true;
				}
				return false;
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.refresh_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		user = mUsernameView.getText().toString();
		pass = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(pass)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(user)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and start a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			hideSoftKeyboard(this);
			showProgress(true);

			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}


	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		private boolean connectionError = false;
		private String errorMsg;

		@Override
		protected Boolean doInBackground(Void... params) {
			String result = null;
			try {
				host = new URL("http://10.0.2.2:8080/FishbowlConnect/login");
				
				HttpPost httppost = new HttpPost(host.toString());
				httppost.setHeader("Content-type", "application/json");
				
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("user", user));
				postParams.add(new BasicNameValuePair("pass", encodePassword(pass)));
				UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParams);
				httppost.setEntity(formEntity);
				HttpClient httpclient = new DefaultHttpClient();	
				HttpParams httpParams = httpclient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, HTTP_TIMEOUT);
				InputStream is = null;
				Log.i("LoginActivity", "Login sent...");
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				Log.i("LoginActivity", "Response received. Status code: "
						+ response.getStatusLine().getStatusCode());
				is = entity.getContent();
				BufferedReader r = new BufferedReader(new InputStreamReader(is,
						"UTF-8"), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = r.readLine()) != null) {
					sb.append(line + "\n");
				}
				
				result = sb.toString();
				// parse result, see if login was successful:
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(new StringReader(result));

				parser.next();
				while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
					String name = parser.getName();
					if (name != null && name.equals("token")) {
						parser.next();
						String t = parser.getText();
						if(t!=null) {
							token = t;
							Log.i("LoginActivity", "Retrieved valid token.");
							return true;
						}
						
					}
					parser.next();
				}
				is.close();

			} catch (MalformedURLException e) {
				Log.e("LoginActivity", "Malformed URL: " + e.getMessage());
				connectionError = true;
				errorMsg = e.getMessage();
			} catch (IOException e) {
				Log.e("LoginActivity", "Error connecting to server: " + e.getMessage());
				connectionError = true;
				errorMsg = e.getMessage();
			} catch (XmlPullParserException e) {
				Log.e("LoginActivity", "Error reading XML: " + e.getMessage());
				connectionError = true;
				errorMsg = e.getMessage();
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);
			if (success) {
				finish();
			} else {
				if(connectionError) {
					String temp = (errorMsg==null) ? "" : errorMsg +"\n\n";
					temp += "Please try again later.";
					Toast toast = Toast.makeText(getApplicationContext(), temp, Toast.LENGTH_LONG);
					toast.show();
				} else {
					mPasswordView
							.setError("Invalid username/password combination.");
					mPasswordView.requestFocus();
				}
			}
			
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}

	}

	@Override
	public void finish() {
		Intent intent = new Intent(this, POListActivity.class);
		startActivity(intent);
		super.finish();
	}
	
	public static void hideSoftKeyboard(Activity activity) {
	    InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	private static String encodePassword(String password) {
		MessageDigest algorithm;
		try {
			algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(password.getBytes());
			byte[] encrypted = algorithm.digest();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStream encoder = MimeUtility.encode(out, "base64");
			encoder.write(encrypted);
			encoder.flush();
			encoder.close();
			return new String(out.toByteArray());
		} catch (NoSuchAlgorithmException e) {
			return "Bad Encryption";
		} catch (MessagingException e) {
			return "Bad Encryption";
		} catch (IOException e) {
			return "Bad Encryption";
		}
	}
}
