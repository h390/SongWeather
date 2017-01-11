package com.study.szj.songweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.study.szj.songweather.gson.Forecast;
import com.study.szj.songweather.gson.Weather;
import com.study.szj.songweather.util.HttpUtil;
import com.study.szj.songweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public static final String keyvalue = "cdffee1ebec64009bbbd946df0f6abbf";
    private TextView title_city;
    private TextView title_update_time;
    private TextView drgree_text;
    private TextView weather_info_text;
    private LinearLayout forecast_layout;
    private TextView aqi_text;
    private TextView pm25_text;
    private TextView comfort_text;
    private TextView car_wash_text;
    private TextView sport_text;
    private ScrollView weather_layout;
    private ImageView bing_pic_imag;
    private LinearLayout forecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 21) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        initView();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = sp.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器请求weather数据
            String weather_id = getIntent().getStringExtra("weather_id");
            //隐藏weather_layout
            weather_layout.setVisibility(View.INVISIBLE);
            //请求数据
            requestWeather(weather_id);
        }
        //尝试从缓存当中读取图片的链接，如果有，则加载图片，如果没有，则重新请求图片网络地址
        String bingPic = sp.getString("image", null);
        if (bingPic != null) {
            //加载图片
            Glide.with(this).load(bingPic).into(bing_pic_imag);
        } else {
            //请求图片网络地址
            requestImgUrl();
        }
    }

    /**
     * 请求网络图片地址
     */
    private void requestImgUrl() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String url = response.body().string();
                //存入到缓存当中
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("image", url);
                edit.apply();
                //请求加载图片
                Glide.with(WeatherActivity.this).load(url).into(bing_pic_imag);
            }

            @Override
            public void onFailure(Call call, IOException e) {

            }
        });
    }

    /**
     * 根据天气id请求天气信息。
     *
     * @param weather_id
     */
    private void requestWeather(String weather_id) {
        String weatherUrl = "https://api.heweather.com/x3/weather?cityid=" + weather_id + "&key=" + keyvalue;
        //请求网络
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //获得字符串
                final String reponseText = response.body().string();
                //转换成weather类
                final Weather weather = Utility.handleWeatherResponse(reponseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //进行weather类的非空判断和状态判断
                        if (weather != null && "ok".equals(weather.status)) {
                            //缓存到sp文件当中
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", reponseText);
                            //使生效
                            editor.apply();
                            //显示天气
                            showWeatherInfo(weather);
                        } else {
                            //弹出来吐司，获取消息失败
                            Toast.makeText(WeatherActivity.this, "获取天气消息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this, "获取天气消息失败", Toast.LENGTH_SHORT).show();

            }
        });
        requestImgUrl();
    }

    /**
     * 处理并展示weather实体类中的数据
     *
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        title_city.setText(cityName);
        title_update_time.setText(updateTime);
        drgree_text.setText(degree);
        weather_info_text.setText(weatherInfo);
        forecast_layout.removeAllViews();

        //下面是预报具体信息的更新
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecast_layout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecast_layout.addView(view);
        }
        if (weather.aqi != null) {
            aqi_text.setText(weather.aqi.city.aqi);
            pm25_text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运行建议：" + weather.suggestion.sport.info;
        comfort_text.setText(comfort);
        car_wash_text.setText(carWash);
        sport_text.setText(sport);
        weather_layout.setVisibility(View.VISIBLE);

    }

    private void initView() {
        title_city = (TextView) findViewById(R.id.title_city);
        title_update_time = (TextView) findViewById(R.id.title_update_time);
        drgree_text = (TextView) findViewById(R.id.drgree_text);
        weather_info_text = (TextView) findViewById(R.id.weather_info_text);
        forecast_layout = (LinearLayout) findViewById(R.id.forecast);
        aqi_text = (TextView) findViewById(R.id.aqi_text);
        pm25_text = (TextView) findViewById(R.id.pm25_text);
        comfort_text = (TextView) findViewById(R.id.comfort_text);
        car_wash_text = (TextView) findViewById(R.id.car_wash_text);
        sport_text = (TextView) findViewById(R.id.sport_text);
        weather_layout = (ScrollView) findViewById(R.id.weather_layout);

        bing_pic_imag = (ImageView) findViewById(R.id.bing_pic_imag);
        forecast = (LinearLayout) findViewById(R.id.forecast);
    }
}
