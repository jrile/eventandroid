package com.eastcor.purchaseorder;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;

public class POListActivity extends ExpandableListActivity {

	/**
	 * This is adapter for expandable list-view for constructing the group and
	 * child elements.
	 */
	public class ExpAdapter extends BaseExpandableListAdapter {

		private Context myContext;

		public ExpAdapter(Context context) {
			myContext = context;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) myContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.child_row, parent, false);
			}

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			//return arrChildelements[groupPosition].length;
			return 1; // set to 4: report link, approve button, reject reason + button 
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
				convertView = inflater.inflate(R.layout.group_row,parent,false);
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
	static final String arrGroupelements[] = { "748: Mecsoft Corporation", "747: Custom Welding & Fabrication test test test test", "745: Lowes",
			"681: Amazon" };

	/**
	 * strings for child elements
	 */
	static final String arrChildelements[][] = {
			{ "Sachin Tendulkar", "Raina", "Dhoni", "Yuvi" },
			{ "Ponting", "Adam Gilchrist", "Michael Clarke" },
			{ "Andrew Strauss", "kevin Peterson", "Nasser Hussain" },
			{ "Graeme Smith", "AB de villiers", "Jacques Kallis" } };

	DisplayMetrics metrics;
	int width;
	ExpandableListView expList;
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		setContentView(R.layout.activity_poview);

		expList = getExpandableListView();

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		width = metrics.widthPixels;
		// this code for adjusting the group indicator into right side of the
		// view
		expList.setIndicatorBounds(width - GetDipsFromPixel(50), width
				- GetDipsFromPixel(10));
		expList.setAdapter(new ExpAdapter(this));

		expList.setOnGroupExpandListener(new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
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
	
	
	public void rejectPressed(View v) {
	
		TextView rejectReason = (TextView) findViewById(R.id.rejectReason);
		rejectReason.setVisibility(View.VISIBLE);
	}
	
	public void approvePressed(View v) {
		TextView rejectReason = (TextView) findViewById(R.id.rejectReason);
		rejectReason.setVisibility(View.GONE);
	}

}