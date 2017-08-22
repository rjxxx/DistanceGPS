package com.konstantin.distancegpsviewer;

import java.util.ArrayList;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.String;

public class BoxAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater lInflater;
    ArrayList<PhoneInformation> objects;

    BoxAdapter(Context context, ArrayList<PhoneInformation> item) {
        ctx = context;
        objects = item;
        lInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // кол-во элементов
    @Override
    public int getCount() {
        return objects.size();
    }

    // элемент по позиции
    @Override
    public Object getItem(int position) {
        return objects.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }


    // пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // используем созданные, но не используемые view
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.item, parent, false);
        }
        PhoneInformation p = getItemInformation(position);


        ((TextView) view.findViewById(R.id.name)).setText(p.name);
        ((TextView) view.findViewById(R.id.ip)).setText(p.ip);
        if (p.isEnableGPS.equals("0")) {
            ((TextView) view.findViewById(R.id.isEnableGPS)).setText(ctx.getString(R.string.gpsOff));
            ((TextView) view.findViewById(R.id.isEnableGPS)).setTextColor(ctx.getResources().getColor(R.color.red));
            ((ImageView) view.findViewById(R.id.ivImage)).setImageResource(R.drawable.icon_red);
            p.isTracking = false;
        } else {
            ((TextView) view.findViewById(R.id.isEnableGPS)).setText(ctx.getString(R.string.gpsOn));
            ((TextView) view.findViewById(R.id.isEnableGPS)).setTextColor(ctx.getResources().getColor(R.color.green));
            ((ImageView) view.findViewById(R.id.ivImage)).setImageResource(R.drawable.icon_grey);

        }

        if (p.isTracking) {
            if (p.distance.equals("")) {
                ((TextView) view.findViewById(R.id.distance)).setText(ctx.getString(R.string.gpsFindingCoordinates));
                ((TextView) view.findViewById(R.id.accuracy)).setText("");
                ((ImageView) view.findViewById(R.id.ivImage)).setImageResource(R.drawable.icon_blue);

            } else {
                ((TextView) view.findViewById(R.id.distance)).setText("До цели " + p.distance + " м");
                ((TextView) view.findViewById(R.id.accuracy)).setText("Точность " + p.accuracy + " м");
                ((ImageView) view.findViewById(R.id.ivImage)).setImageResource(R.drawable.icon_green);
            }
        } else {
            ((TextView) view.findViewById(R.id.distance)).setText(ctx.getString(R.string.gpsNoTracking));
            ((TextView) view.findViewById(R.id.accuracy)).setText("");

        }

        view.setTag(position);
        return view;
    }


    PhoneInformation getItemInformation(int position) {
        return ((PhoneInformation) getItem(position));
    }

}