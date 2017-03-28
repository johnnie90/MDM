package com.formiik.formiikmdm;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.List;
import java.util.Map;

/**
 * Created by jonathan on 30/03/16.
 */
public class AdapterApps extends BaseAdapter {

    Context                  context;
    List<Map<String,String>> apps;

    public AdapterApps(Context context,List<Map<String,String>> apps){
        this.context = context;
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {

        final ViewHolder viewHolder;

        LayoutInflater inflater     = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.item_app, viewGroup, false);

        viewHolder = new ViewHolder();

        viewHolder.checkBox_app = (CheckBox) view.findViewById(R.id.checkBox_app);

        view.setTag(viewHolder);

        final Map<String,String> itemApp = apps.get(position);

        viewHolder.checkBox_app.setText(itemApp.get("name"));


        if(itemApp.get("block").equalsIgnoreCase("true")) viewHolder.checkBox_app.setChecked(true);
        else viewHolder.checkBox_app.setChecked(false);

        viewHolder.checkBox_app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = ServiceBlockApp.sharedPref.edit();

                if(isChecked){
                    ServiceBlockApp.appsList.get(position).put("block","true");
                    editor.putString(itemApp.get("name"), "true");
                }
                else{
                    ServiceBlockApp.appsList.get(position).put("block","false");
                    editor.putString(itemApp.get("name"), "false");
                }

                editor.commit();
            }
        });

        return view;
    }

    class ViewHolder{
        CheckBox checkBox_app;
    }
}
