package com.appiphany.nacc.services;

import java.util.List;
import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Site;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class GuideAdapter extends ArrayAdapter<Site> {
	private final LayoutInflater inflater;
	private List<Site> sites;
	
	public GuideAdapter(Context context, List<Site> sites) {
		super(context, R.layout.site_item, sites);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.sites = sites;
	}
	
	public List<Site> getDatasource(){
		return sites;
	}
	
	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		ViewHolder viewHolder;
		if(convertView == null){
			viewHolder = new ViewHolder();
			convertView = inflater.inflate(R.layout.site_item, null);
			viewHolder.tvSiteName = (TextView) convertView.findViewById(R.id.tvSiteName);
			viewHolder.chkSite = (CheckBox) convertView.findViewById(R.id.chkSite);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}

		String name = getItem(position).getName();
		if(name != null){
			name = name.trim();
			if(name.length() == 1) {
				name = name.toLowerCase();
			}else if(name.length() > 1) {
				name = name.substring(0, 1).toUpperCase() + name.substring(1);
			}
		}

		viewHolder.tvSiteName.setText(name);
		viewHolder.chkSite.setFocusable(false);
		viewHolder.chkSite.setChecked(getItem(position).isSelected());
		viewHolder.chkSite.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getItem(position).toggleSelected();			
			}
		});
		return convertView;
	}
	
	public void toggleCheck(int position, View selectedView){			
		ViewHolder viewHolder = (ViewHolder) selectedView.getTag();
		getItem(position).toggleSelected();
		viewHolder.chkSite.setChecked(getItem(position).isSelected());
	}
	
	private static class ViewHolder{
		public TextView tvSiteName;
		public CheckBox chkSite;
	}

}
