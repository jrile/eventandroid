package com.eastcor.purchaseorder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

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
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
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
		private View children[];

		
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
			Log.e("getChild [g#, c#]", groupPosition + " " + childPosition);
			return children[childPosition];
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Log.e("getChildView", groupPosition + " " + children.length + " "
					+ childPosition);
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
			//Log.e("getGroupCount", groupElements.size() + "");
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
			tvGroupName.setText(groupElements.get(groupPosition));

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

	static final ArrayList<String> groupElements = new ArrayList<String>();
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

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String errorMsg = "", result;
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
			// adapter depends on how many items are in groupElements.
			poTask = new POListTask();
			poTask.execute((Void) null);
			
			groupElements.add("748: Mecsoft Corporation");
			groupElements
					.add("747: Custom Welding & Fabrication test test test test");
			groupElements.add("745: Lowes");
			groupElements.add("681: Amazon");
			ea = new ExpAdapter(this);
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
				Log.e("onGroupExpand", "OK");
			}
		});

		expList.setOnGroupCollapseListener(new OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				Log.e("onGroupCollapse", "OK");
			}
		});

		expList.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Log.e("OnChildClickListener", "OK");
				return false;
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
		private String result, errorMsg = "";
		
		
		@Override
		protected Boolean doInBackground(Void... arg0) {
			try {
				URL host = new URL("http://10.0.2.2:8080/FishbowlConnect/query/list");
				HttpPost httppost = new HttpPost(host.toString());
				httppost.setHeader("Content-type", "application/json");

				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("token", LoginActivity.token));
				UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParams);
				httppost.setEntity(formEntity);
				HttpClient httpclient = new DefaultHttpClient();	
				HttpParams httpParams = httpclient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, LoginActivity.HTTP_TIMEOUT);
				InputStream is = null;
				Log.i("poList onCreate", "Sending request for PO List");
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				Log.i("Response recieved", "Status code: "
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
					String text = parser.getText();
					Log.i("PO List XML Parse", name + " " + text);
					if (name != null && name.equals("purchaseorder")) {
						parser.next();
						System.out.println(parser.getName() + parser.getText());
						
					}
					else if (name != null && name.equals("error")) {
						parser.next();
						errorMsg += parser.getText();
					}
					parser.next();
				}
				is.close();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		
		@Override
		protected void onCancelled() {
			
		}
		
		@Override
		protected void onPostExecute(final Boolean success) {
			
		}
		

		
	}

}