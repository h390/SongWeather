package com.study.szj.songweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.study.szj.songweather.db.City;
import com.study.szj.songweather.db.County;
import com.study.szj.songweather.db.Province;
import com.study.szj.songweather.util.HttpUtil;
import com.study.szj.songweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/1/8.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    //当前选中的级别
    private int current_level;
    private static final String Tag = "ChooseAreaFragment";

    //控件的初始化
    private TextView mTextView;
    //listview
    private ListView mListView;
    //adapter
    private ArrayAdapter<String> mAdapter;
    //集合
    private List<String> mList = new ArrayList<>();
    //butoon
    private Button mButton;
    //进度对话框
    private ProgressDialog mDialog;

    //三个集合
    private List<Province> mProvinces;
    //市的集合
    private List<City> mCities;
    //县的集合
    private List<County> mCounties;
    //选中的省
    private Province mSelectProvince;
    //选中的市
    private City mSelectCity;

    public static final String ADDRESS = "http://guolin.tech/api/china/";

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                {
                    //关闭进度条
                    closeProgressDialog();
                    //根据类型进行不同的查询
                    if ("province".equals(msg.obj)) {
                        queryProvinces();
                    } else if ("city".equals(msg.obj)) {
                        queryCities();
                    } else if ("county".equals(msg.obj)) {
                        queryCountries();
                    }
                }
            }
        }
    };
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mTextView = (TextView) view.findViewById(R.id.title_text);
        mListView = (ListView) view.findViewById(R.id.list_view);
        mButton = (Button) view.findViewById(R.id.back_button);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mList);
        mListView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //设置listview的点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (current_level == LEVEL_PROVINCE) {
                    //获得省，通过省查询市
                    mSelectProvince = mProvinces.get(position);
                    queryCities();
                } else if (current_level == LEVEL_CITY) {
                    mSelectCity = mCities.get(position);
                    //通过市查询县
                    queryCountries();
                } else if (current_level == LEVEL_COUNTY) {
                    //将当前地区的weather_id传过去
                    String weather_id = mCounties.get(0).getWeatherId();
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);
                    //intent附加信息
                    intent.putExtra("weather_id", weather_id);
                    //开启
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        //设置按钮的点击事件
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果当前状态是市，回退到省，如果当前状态是县，回退到市
                if (current_level == LEVEL_COUNTY) {
                    queryCities();
                } else if (current_level == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库当中查，如果没有再从网络获取
     */
    private void queryProvinces() {
        //将标题栏设置为中国，回退按钮隐藏，获得包含省的集合，并将集合放在
        //需要在adapter中加载数据的mList当中
        mTextView.setText("中国");
        mButton.setVisibility(View.GONE);
        mProvinces = DataSupport.findAll(Province.class);
        //遍历这个集合，将数据放在datalist当中
        if (mProvinces.size() > 0) {
            mList.clear();
            for (Province province : mProvinces) {
                mList.add(province.getProvinceName());

            }
            //通知数据发生改变
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            //当前的状态也重新赋值
            current_level = LEVEL_PROVINCE;
        } else {
            queryFromServer(ADDRESS, "province");
        }
    }

    private void queryCities() {
        //设置标题名字为省的名字
        mTextView.setText(mSelectProvince.getProvinceName());
        //按钮设置为显示
        mButton.setVisibility(View.VISIBLE);
        //获得城市列表
        mCities = DataSupport.where("provinceid = ?", String.valueOf(mSelectProvince.getId())).find(City.class);
        if (mCities.size() > 0) {
            //将城市的数据库加载到集合当中
            mList.clear();
            for (City city : mCities) {
                mList.add(city.getCityName());

            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            current_level = LEVEL_CITY;
        } else {
            int mSelectProvinceId = mSelectProvince.getProvinceCode();
            String address = ADDRESS+mSelectProvinceId;
            queryFromServer(address,"city");
        }
    }

    private void queryCountries() {
        mTextView.setText(mSelectCity.getCityName());
        mButton.setVisibility(View.VISIBLE);
        mCounties = DataSupport.where("cityid = ?",String.valueOf(mSelectCity.getId())).find(County.class);
        if (mCounties.size() > 0) {
            //集合清零
            mList.clear();
            //循环这个集合并添加到mlist当中去
            for (County county : mCounties) {
                mList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            current_level = LEVEL_COUNTY;
        } else {
            //地址再加上城市码和省码
            String address = ADDRESS + mSelectProvince.getProvinceCode() + "/" + mSelectCity.getCityCode();
            //请求服务器
            queryFromServer(address,"county");
        }
    }

    /**
     * 根据传入的地址和类型请求服务器获得数据
     * @param address 请求的地址
     * @param type 请求的类型
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        //请求服务器数据
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //获得请求的内容
             String responseText = response.body().string();
                //设置一个布尔值
                boolean result = false;
                if ("province".equals(type)) {
                    //说明请求的是省的数据
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, mSelectProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, mSelectCity.getId());

                }
                if (result) {
                    Message message = Message.obtain();
                    message.obj = type;
                    message.what = 100;
                    mHandler.sendMessage(message);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //弹出来加载失败的toast
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示加载进度框
     */
    private void showProgressDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage("正在加载...");
            mDialog.setCanceledOnTouchOutside(false);
        }
        mDialog.show();
    }
    /**
     * 不显示进度框
     */
    private void closeProgressDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }
}

