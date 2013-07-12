package com.eastcor.purchaseorder;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
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
								Intent intent = new Intent(myContext, PdfDownloadActivity.class);
								intent.putExtra(EXTRA_PO_NUM, temp);
								
								startActivity(intent);
								
								Log.e("downloadPdf", String.valueOf(temp));
								downloadPdf(temp);
							}
						});

			}

		}
		
		public void downloadPdf(int index) {
			
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
			return arrGroupelements.length;
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
			tvGroupName.setText(arrGroupelements[groupPosition]);

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

	static View childElements[];

	/**
	 * strings for group elements
	 */
	static final String arrGroupelements[] = { "748: Mecsoft Corporation",
			"747: Custom Welding & Fabrication test test test test",
			"745: Lowes", "681: Amazon" };

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
		ea = (ExpAdapter) getLastNonConfigurationInstance();
		setContentView(R.layout.activity_poview);
		expList = getExpandableListView();

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		width = metrics.widthPixels;
		expList.setIndicatorBounds(width - GetDipsFromPixel(50), width
				- GetDipsFromPixel(10));
		if (ea == null) {
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

}