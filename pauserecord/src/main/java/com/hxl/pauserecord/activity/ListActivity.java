package com.hxl.pauserecord.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.hxl.pauserecord.R;
import com.hxl.pauserecord.adapter.FileListAdapter;
import com.hxl.pauserecord.record.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HXL on 16/8/11.
 */
public class ListActivity extends Activity {
    ListView listView;
    List<File> list = new ArrayList<>();
    FileListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        listView = (ListView) findViewById(R.id.listView);
        TextView textView = (TextView) findViewById(R.id.text_view);
        if ("pcm".equals(getIntent().getStringExtra("type"))) {
            list = FileUtils.getPcmFiles();
        } else {
            list = FileUtils.getWavFiles();
        }

        if (list == null || list.size() <= 0) {
            textView.setVisibility(View.VISIBLE);
        } else {
            adapter = new FileListAdapter(this, list);
            listView.setAdapter(adapter);
        }

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                return true;
            }
        });

    }
}
