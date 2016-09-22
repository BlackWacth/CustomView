package com.hua.customview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioGroup;

import com.hua.customview.widgets.PolyToPolyView;

public class MainActivity extends AppCompatActivity {

    private RadioGroup mRadioGroup;
    private PolyToPolyView mPolyToPolyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRadioGroup = (RadioGroup) findViewById(R.id.rg_point_count_group);
        mPolyToPolyView = (PolyToPolyView) findViewById(R.id.ptpv_poly_to_poly_view);
        mRadioGroup.check(R.id.rb_point_count_4);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_point_count_0 :
                        mPolyToPolyView.setPointCount(0);
                        break;

                    case R.id.rb_point_count_1 :
                        mPolyToPolyView.setPointCount(1);
                        break;

                    case R.id.rb_point_count_2 :
                        mPolyToPolyView.setPointCount(2);
                        break;

                    case R.id.rb_point_count_3 :
                        mPolyToPolyView.setPointCount(3);
                        break;

                    case R.id.rb_point_count_4 :
                        mPolyToPolyView.setPointCount(4);
                        break;
                }
            }
        });
    }
}
