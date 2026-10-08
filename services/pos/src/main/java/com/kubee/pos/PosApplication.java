package com.kubee.pos;

import com.kubee.pos.common.time.ShopTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PosApplication {

	public static void main(String[] args) {
		ShopTime.useShopZone();
		SpringApplication.run(PosApplication.class, args);
	}

}
