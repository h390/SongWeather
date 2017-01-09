package com.study.szj.songweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Administrator on 2017/1/7.
 */

public class HttpUtil {
    public static void sendOkHttpRequest(String addrss, okhttp3.Callback callback) {
        //创建client
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(addrss).build();
        //请求加入client当中
        client.newCall(request).enqueue(callback);

    }
}
