package com.daepamarket.daepa_market_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class DaepaMarketBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaepaMarketBackEndApplication.class, args);
    }

}
