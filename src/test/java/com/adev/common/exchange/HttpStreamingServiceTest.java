package com.adev.common.exchange;

import com.adev.common.exchange.http.domain.RequestParam;
import com.adev.common.exchange.http.service.HttpStreamingService;
import io.reactivex.disposables.Disposable;

public class HttpStreamingServiceTest {
    public static void main(String[] args) throws InterruptedException {
        HttpStreamingService httpStreamingService=new HttpStreamingService("baidu");
        httpStreamingService.connect();
        RequestParam param = new RequestParam();
        param.setUrl("https://api.coinone.co.kr/ticker/?currency=btc");
        Disposable disposable=httpStreamingService.pollingRestApi(param).subscribe(e->{
            System.out.println(e);
        });
//        Thread.sleep(20000L);
//        disposable.dispose();
//        httpStreamingService.disconnect();
    }
}
