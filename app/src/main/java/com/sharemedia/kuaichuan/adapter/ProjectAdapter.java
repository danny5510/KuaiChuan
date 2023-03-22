package com.sharemedia.kuaichuan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sharemedia.kuaichuan.R;
import com.sharemedia.kuaichuan.entities.Project;

import java.util.Random;

public class ProjectAdapter extends BaseAdapter implements View.OnClickListener {
    private final Context context;
    private final int layout;
    private Project[] listItems;
    private Callback mCallback;
    public interface Callback{
        public void click(View v);
    }
    public ProjectAdapter(Context context, int layout, Project[] listItems ,Callback callback) {
        this.context = context;
        this.layout = layout;
        this.listItems = listItems;
        mCallback = callback;
    }

    @Override
    public int getCount() {
        return listItems.length;
    }

    @Override
    public Object getItem(int position) {
        return listItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            /*LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view=inflater.inflate(layout,parent,false);*/
            view = LayoutInflater.from(context).inflate(R.layout.listview_item_project, null);

        }
        view.setOnClickListener(this);
        view.setTag(position);
        TextView text1 = (TextView) view.findViewById(R.id.project_name);
        TextView text2 = (TextView) view.findViewById(R.id.project_address);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.project_percent);
        Project project = listItems[position];
        text1.setText(listItems[position].name + "    完成" + project.qty_finished + "/" + project.qty_required);
        text2.setText(listItems[position].address);
        Random random = new Random();
        progressBar.setProgress(random.nextInt(100));

        return view;
    }

    @Override
    public void onClick(View v) {
        mCallback.click(v);
    }
}
