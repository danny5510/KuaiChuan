package com.sharemedia.kuaichuan.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sharemedia.kuaichuan.KuaiChuanContext;
import com.sharemedia.kuaichuan.R;
import com.sharemedia.kuaichuan.adapter.ProjectAdapter;
import com.sharemedia.kuaichuan.databinding.FragmentDashboardBinding;
import com.sharemedia.kuaichuan.entities.Project;

public class DashboardFragment extends Fragment implements ProjectAdapter.Callback {

    private FragmentDashboardBinding binding;
    private ListView listView1;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        listView1 = (ListView) root.findViewById(R.id.ListViewProject);
        Project[] listItems = new Project[]{
                new Project("金嘉" , "金嘉的地址"),
                new Project("招商", "招商的地址"),
                new Project("左右", "左右的地址"),
        };
        listView1.setAdapter(new ProjectAdapter(KuaiChuanContext.applicationContext, R.id.listview_item_project, listItems, this));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void click(View v) {
        Integer position=(Integer)v.getTag();
        Toast.makeText(getContext(), "点击了" + position, Toast.LENGTH_SHORT).show();
    }
}