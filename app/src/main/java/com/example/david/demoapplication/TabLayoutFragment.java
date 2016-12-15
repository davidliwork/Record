package com.example.david.demoapplication;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by david on 2016/11/21.
 */

public class TabLayoutFragment extends Fragment {

    public static String TABLAYOUT_FRAGMENT = "tab_fragment";
    private TextView txt;
    private int type;

    public static TabLayoutFragment newInstance(int type) {
        TabLayoutFragment fragment = new TabLayoutFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(TABLAYOUT_FRAGMENT, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = (int) getArguments().getSerializable(TABLAYOUT_FRAGMENT);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tablayout, container, false);
        initView(view);
        return view;
    }

    protected void initView(View view) {
        RecyclerView listView = (RecyclerView) view.findViewById(R.id.list_view);
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add("测试数据：" + i);
        }
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(View.inflate(getContext(),R.layout.textview,null)){

                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            }

            @Override
            public int getItemCount() {
                return 1000;
            }
        });
//            listView.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, list));
//        txt = (TextView) view.findViewById(R.id.tab_txt);
//        switch (type) {
//            case 0:
//                txt.setText("这是综艺Fragment");
//                break;
//            case 1:
//                txt.setText("这是体育Fragment");
//                break;
//            case 2:
//                txt.setText("这是新闻Fragment");
//                break;
//            case 3:
//                txt.setText("这是热点Fragment");
//                break;
//            case 4:
//                txt.setText("这是头条Fragment");
//                break;
//            case 5:
//                txt.setText("这是军事Fragment");
//                break;
//            case 6:
//                txt.setText("这是历史Fragment");
//                break;
//            case 7:
//                txt.setText("这是科技Fragment");
//                break;
//            case 8:
//                txt.setText("这是人文Fragment");
//                break;
//            case 9:
//                txt.setText("这是地理Fragment");
//                break;
    }

}
