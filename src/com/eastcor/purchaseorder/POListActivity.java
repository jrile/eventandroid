package com.eastcor.purchaseorder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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

public class POListActivity extends ExpandableListActivity {
	public final static String EXTRA_PO_NUM = "com.eastcor.purchaseorder.PO_NUM";
	private POListTask poTask = null;
	/**
	 * This is adapter for expandable list-view for constructing the group and
	 * child elements.
	 */
	public class ExpAdapter extends BaseExpandableListAdapter {

		private Context myContext;
		public View children[];

		public ExpAdapter(Context context) {

			myContext = context;

			children = new View[getGroupCount()];
			for (int i = 0; i < getGroupCount(); i++) {
				final int temp = i;
				LayoutInflater inflater = (LayoutInflater) myContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			// Log.e("getGroupCount", groupElements.size() + "");
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
	 * strings for group elements
	 */

	// static final ArrayList<String> groupElements = new ArrayList<String>();
	static final ArrayList<PurchaseOrder> groupElements = new ArrayList<PurchaseOrder>();
	DisplayMetrics metrics;
	int width;
	ExpandableListView expList;
	ExpAdapter ea;

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return ea;
	}

	public void save(View v) {
		try {
			String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
			for (int i = 0; i < ea.children.length; i++) {
				RadioButton approve = (RadioButton) ea.children[i]
						.findViewById(R.id.approve);
				RadioButton reject = (RadioButton) ea.children[i]
						.findViewById(R.id.reject);
				PurchaseOrder po = groupElements.get(i);
				if (approve.isChecked()) {
					xml += "<po>\n";
					xml += "\t<number>" + po.getPoNum() + "</number>\n";
					xml += "\t<status>accepted</status>\n</po>\n";
					Log.i("Save Button Pressed", po.getVendorName()
							+ " accepted.");
				} else if (reject.isChecked()) {
					EditText reasonText = (EditText) ea.children[i]
							.findViewById(R.id.rejectReason);
					String reason = reasonText.getText().toString();
					xml += "<po>\n";
					xml += "\t<number>" + po.getPoNum() + "</number>\n";
					xml += "\t<status>rejected</status>\n";
					try {
						xml += "\t<reason>"
								+ URLEncoder.encode(reason, "UTF-8")
								+ "</reason>\n</po>\n";
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.i("Save Button Pressed", po.getVendorName()
							+ " rejected. Reason: " + reason);
				}
			}
			System.out.println(xml);
			URL host = new URL(
					"http://10.0.2.2:8080/FishbowlConnect/query/update");
			HttpPost httppost = new HttpPost(host.toString());
			httppost.setHeader("Content-type", "application/x-www-form-urlencoded");
			ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams
					.add(new BasicNameValuePair("token", LoginActivity.token));
			postParams.add(new BasicNameValuePair("xml", xml));
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
					postParams);
			httppost.setEntity(formEntity);
			HttpClient httpclient = new DefaultHttpClient();
			HttpParams httpParams = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams,
					LoginActivity.HTTP_TIMEOUT);
			Log.i("Save Button Pressed", "Sending updates to server...");
			HttpResponse response = httpclient.execute(httppost);
			Log.i("Save Button Pressed", "Response received. Status code: " + response.getStatusLine().getStatusCode());
			
		} catch (Exception e) {

		}
		

	}
	

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
		expList.setIndicatorBounds(width - GetDipsFromPixel(50), width
				- GetDipsFromPixel(10));
		if (ea == null) {
			// PROCESS POS HERE AND THEN CREATE ADAPTER
			poTask = new POListTask();
			poTask.execute((Void) null);
			try {
				poTask.get(60, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ea = new ExpAdapter(this);
			Log.i("onCreate", "Proceeding with creating adapter");
		}
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

	public int GetDipsFromPixel(float pixels) {
		// Get the screen's density scale
		final float scale = getResources().getDisplayMetrics().density;
		// Convert the dps to pixels, based on density scale
		return (int) (pixels * scale + 0.5f);
	}

	public class POListTask extends AsyncTask<Void, Void, Boolean> {
		private String result;

		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				URL host = new URL(
						"http://10.0.2.2:8080/FishbowlConnect/query/list");
				HttpPost httppost = new HttpPost(host.toString());
				httppost.setHeader("Content-type", "application/json");

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
				parser.require(XmlPullParser.START_TAG, null, "polist"); // make
																			// sure
																			// correct
																			// XML
																			// is
																			// posted
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
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// token has timed out
				e.printStackTrace();
			}
			return true;
		}
	}

}