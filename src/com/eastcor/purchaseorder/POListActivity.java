package com.eastcor.purchaseorder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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

import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity that appears after login. Displays 
 * all items in the Fishbowl Inventory that are marked
 * as "Waiting for Approval."
 * 
 * @author jrile
 *
 */
public class POListActivity extends ExpandableListActivity {
	public final static String EXTRA_PO_NUM = "com.eastcor.purchaseorder.PO_NUM";
	public final static String EXTRA_TIMEOUT = "com.eastcor.purchaseorder.TIMEOUT";
	private POListTask poTask = null;
	static ArrayList<PurchaseOrder> groupElements = new ArrayList<PurchaseOrder>();
	DisplayMetrics metrics;
	int width;
	ExpandableListView expList;
	ExpAdapter ea;

	/**
	 * Back button acts as the home button in this case. 
	 * Implemented so users don't have to login if they
	 * want to send the application to the background.
	 */
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	/**
	 * Retains the list adapter with all the information already
	 * retrieved from the database.
	 * @return ExpList adapter
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		return ea;
	}

	/**
	 * Creates a new POListTask to refresh information from database.
	 * @param v unused
	 */
	public void refresh(View v) {
		POListTask task = new POListTask();
		task.execute();
	}

	/**
	 * Creates an xml document to send to the server containing 
	 * approval/rejection information.
	 * @param v unused
	 */
	public void save(View v) {
		try {
			String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<polist>\n";
			for (int i = 0; i < ea.children.length; i++) {
				RadioButton approve = (RadioButton) ea.children[i]
						.findViewById(R.id.approve);
				RadioButton reject = (RadioButton) ea.children[i]
						.findViewById(R.id.reject);
				PurchaseOrder po = groupElements.get(i);
				if (approve.isChecked()) {
					xml += "<po>";

					xml += "<number>" + po.getPoNum() + "</number>";
					xml += "<status>Approved</status></po>";
					Log.i("Save Button Pressed",
							po.getPoNum() + ": " + po.getVendorName()
									+ " accepted.");
				} else if (reject.isChecked()) {
					EditText reasonText = (EditText) ea.children[i]
							.findViewById(R.id.rejectReason);
					String reason = reasonText.getText().toString();
					xml += "<po>";
					xml += "<number>" + po.getPoNum() + "</number>";
					xml += "<status>Rejected</status>";
					try {
						xml += "<reason>" + URLEncoder.encode(reason, "UTF-8")
								+ "</reason></po>";
					} catch (UnsupportedEncodingException e) {
						Toast t = Toast
								.makeText(
										getApplicationContext(),
										"Encoding error, can't send your rejection reason.",
										Toast.LENGTH_SHORT);
						t.show();
					}
					Log.i("Save Button Pressed",
							po.getPoNum() + ": " + po.getVendorName()
									+ " rejected. Reason: " + reason);
				}
			}
			xml += "</polist>";
			Log.i("Save Button Pressed", "Sending updates to server...");
			UpdateTask update = new UpdateTask(xml);
			update.execute((Void) null);
		} catch (IndexOutOfBoundsException e) {
			// nothing to save... ignore
		}
	}

	/**
	 * Checks to see if there is an existing adapter. If not, create 
	 * a new task to fetch information from database and create one.
	 * If there is, load it into the view.
	 * @param savedInstanceState Saved instance from before creation (screen orientation)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ea = (ExpAdapter) getLastNonConfigurationInstance();
		setContentView(R.layout.activity_poview);
		expList = getExpandableListView();
		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		width = metrics.widthPixels;
		expList.setIndicatorBounds(width - getDipsFromPixel(50), width
				- getDipsFromPixel(10));
		if (ea == null) {
			// process POs here.
			poTask = new POListTask();
			poTask.execute((Void) null);
			Log.i("onCreate", "Proceeding with creating adapter");
		} else {
			newAdapter(ea);
		}

	}

	/**
	 * Sets a new adapter and refreshes the views contents.
	 * @param e The adapter containing the purchase orders.
	 */
	public void newAdapter(ExpAdapter e) {
		ea = e;
		Log.i("onCreate", "Proceeding with creating adapter");
		expList.setAdapter(ea);
		expList.setOnGroupExpandListener(new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				for (int i = 0; i < expList.getChildCount(); i++) {
					if (i != groupPosition) {
						expList.collapseGroup(i);
					}
				}
			}
		});
	}

	/**
	 * Get screen's density scale and convert to pixels based on density scale.
	 * @param pixels 
	 * @return 
	 */
	public int getDipsFromPixel(float pixels) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (pixels * scale + 0.5f);
	}

	/**
	 * Purchase Order List Task, retrieves purchase order list from server.
	 * @author jrile
	 *
	 */
	public class POListTask extends AsyncTask<Void, Void, Boolean> {
		private String result;
		private ProgressDialog dialog;
		private boolean loginIntent = false;

		/**
		 * Default constructor, creates a dialog.
		 */
		public POListTask() {
			dialog = new ProgressDialog(POListActivity.this);
		}

		/**
		 * Shows a dialog message stating information is being fetched.
		 */
		@Override
		protected void onPreExecute() {
			this.dialog.setMessage("Fetching information from the database...");
			this.dialog.show();
			groupElements = new ArrayList<PurchaseOrder>();
		}

		/**
		 * Dismisses dialog, if there was an error connecting to the server, 
		 * display an error message. If the user's token has expired, inform 
		 * them of that and redirect to the login screen.
		 * @param success If the task was executed successfully.
		 */
		@Override
		protected void onPostExecute(final Boolean success) {
			ea = new ExpAdapter(POListActivity.this);
			newAdapter(ea);
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			if (!success) {
				if (loginIntent) {
					Intent i = new Intent(getBaseContext(), LoginActivity.class);
					i.putExtra(EXTRA_TIMEOUT, true);
					finish();
					startActivity(i);
				} else {
					Toast msg = Toast
							.makeText(
									getApplicationContext(),
									"There was an error connecting to the database. Please try again.",
									Toast.LENGTH_LONG);
					msg.show();
				}
			}
		}

		/**
		 * Create new post attempt to retrieve purchase order list
		 * from server.
		 * @param arg0 unused
		 * @return Boolean If the task completed successfully.
		 */
		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				HttpPost httppost = new HttpPost(LoginActivity.host+"query/list");
				httppost.setHeader("Content-type", "application/xml");
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("token",
						LoginActivity.token));
				UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
						postParams);
				httppost.setEntity(formEntity);
				HttpClient httpclient = new DefaultHttpClient();
				HttpParams httpParams = httpclient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams,
						LoginActivity.HTTP_TIMEOUT);
				InputStream is = null;
				Log.i("POList", "Sending request for PO List...");
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				if (entity == null) {
					throw new ServerException("Time out");
				}
				Log.i("POList", "Response recieved. Status code: "
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
				r.close();
				is.close();
				// parse result, create the PO List
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(new StringReader(result));
				parser.next();
				parser.require(XmlPullParser.START_TAG, null, "polist");
				int type, poNum = -1;
				float cost = 0;
				String vendorName = null, vendorAddress = null, vendorCity = null, vendorZip = null, shipToName = null, shipToAddress = null, shipToCity = null, shipToZip = null, buyer = null, dateissued = null, shipterms = null, carrier = null, paymentTerms = null, fob = null, desc = null, tag = "";
				HashMap<String, Float> partList = new LinkedHashMap<String, Float>();
				while ((type = parser.getEventType()) != XmlPullParser.END_DOCUMENT) {
					if (type == XmlPullParser.END_TAG) {
						if (parser.getName().equals("purchaseorder")) {
							// parsed all data for a single PO, add to list.
							PurchaseOrder po = new PurchaseOrder(poNum,
									vendorName, vendorAddress, vendorCity,
									vendorZip, shipToName, shipToAddress,
									shipToCity, shipToZip, buyer, dateissued,
									shipterms, carrier, paymentTerms, fob,
									partList);
							groupElements.add(po);
							partList = new HashMap<String, Float>();
						}
						tag = "";
					} else if (type == XmlPullParser.START_TAG) {
						tag = parser.getName();
					} else if (type == XmlPullParser.TEXT) {
						if (tag.equals("ponum")) {
							poNum = Integer.parseInt(parser.getText());
						} else if (tag.equals("vendorname")) {
							vendorName = parser.getText();
						} else if (tag.equals("vendoraddress")) {
							vendorAddress = parser.getText();
						} else if (tag.equals("vendorcity")) {
							vendorCity = parser.getText();
						} else if (tag.equals("vendorzip")) {
							vendorZip = parser.getText();
						} else if (tag.equals("shiptoname")) {
							shipToName = parser.getText();
						} else if (tag.equals("shiptoaddress")) {
							shipToAddress = parser.getText();
						} else if (tag.equals("shiptocity")) {
							shipToCity = parser.getText();
						} else if (tag.equals("shiptozip")) {
							shipToZip = parser.getText();
						} else if (tag.equals("buyer")) {
							buyer = parser.getText();
						} else if (tag.equals("dateissued")) {
							dateissued = parser.getText();
						} else if (tag.equals("shipterms")) {
							shipterms = parser.getText();
						} else if (tag.equals("carrier")) {
							carrier = parser.getText();
						} else if (tag.equals("paymentterms")) {
							paymentTerms = parser.getText();
						} else if (tag.equals("fob")) {
							fob = parser.getText();
						} else if (tag.equals("desc")) {
							desc = parser.getText();
						} else if (tag.equals("cost")) {
							cost = Float.parseFloat(parser.getText());
							partList.put(desc, cost);
						}
					}
					parser.next();
				}
			} catch (IOException e) {
				return false;
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				// if the XML contains unexpected tokens, it's
				// an error message indicating token expired.
				loginIntent = true;
				return false;
			} catch (ServerException e) {
				return false;
			}
			return true;
		}
	}

	/**
	 * This is adapter for expandable list-view for constructing the group and
	 * child elements.
	 */
	public class ExpAdapter extends BaseExpandableListAdapter {
		private Context myContext;
		public View children[];

		/**
		 * Constructor for a new expandable list adapter. Assumes the element
		 * 'groupElements' is already full from the POListTask. 
		 * @param context Context sent from
		 */
		public ExpAdapter(Context context) {
			myContext = context;
			children = new View[getGroupCount()];
			for (int i = 0; i < getGroupCount(); i++) {
				final int temp = i;
				LayoutInflater inflater = (LayoutInflater) myContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				// recreate the same expand view for each child
				children[i] = inflater.inflate(R.layout.child_row, null);
				children[i].findViewById(R.id.approve).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								TextView rejectReason = (TextView) children[temp]
										.findViewById(R.id.rejectReason);
								rejectReason.setVisibility(View.GONE);
							}
						});
				children[i].findViewById(R.id.reject).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								TextView rejectReason = (TextView) children[temp]
										.findViewById(R.id.rejectReason);
								rejectReason.setVisibility(View.VISIBLE);
								children[temp].findViewById(R.id.rejectReason)
										.requestFocus();
							}
						});
				children[i].findViewById(R.id.download_pdf).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(myContext,
										PdfDownloadActivity.class);
								intent.putExtra(EXTRA_PO_NUM, temp);
								startActivity(intent);
								Log.e("downloadPdf", String.valueOf(temp));
							}
						});
			}
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return children[childPosition];
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			convertView = children[groupPosition];
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return groupElements.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) myContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.group_row, parent,
						false);
			}
			TextView tvGroupName = (TextView) convertView
					.findViewById(R.id.tvGroupName);
			tvGroupName
					.setText(groupElements.get(groupPosition).getListTitle());
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	/**
	 * Task to send information to the server to be updated.
	 * @author jrile
	 *
	 */
	public class UpdateTask extends AsyncTask<Void, Void, Boolean> {
		private String xml;
		boolean error = false;

		/**
		 * 
		 * @param xml An XML document, should start with <polist> and have 
		 * each separate purchase order under <po>.
		 */
		public UpdateTask(String xml) {
			super();
			this.xml = xml;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				HttpPost httppost = new HttpPost(LoginActivity.host+"query/update");
				httppost.setHeader("Content-type", "application/xml");
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("token",
						LoginActivity.token));
				postParams.add(new BasicNameValuePair("xml", xml));
				UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
						postParams);
				httppost.setEntity(formEntity);
				HttpClient httpclient = new DefaultHttpClient();
				HttpParams httpParams = httpclient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams,
						LoginActivity.HTTP_TIMEOUT);
				HttpResponse response = httpclient.execute(httppost);
				Log.i("Save Button Pressed", "Response received. Status code: "
						+ response.getStatusLine().getStatusCode());
				if (response.getStatusLine().getStatusCode() != 200) {
					return false;
				}
				return true;
			} catch (IOException e) {
				return false;
			}

		}

		@Override
		public void onPostExecute(Boolean result) {
			if (result) {
				Toast msg = Toast.makeText(getApplicationContext(),
						"Purchase Order(s) successfully saved.",
						Toast.LENGTH_LONG);
				msg.show();
			} else {
				Toast msg = Toast.makeText(getApplicationContext(),
						"There was an error saving! Please try again.",
						Toast.LENGTH_LONG);
				msg.show();
			}
		}
	}

}