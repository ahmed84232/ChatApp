package com.ahmedy.chat;

import com.ahmedy.chat.config.TrustAllSSL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableCaching
public class ChatApplication {

	public static void main(String[] args) {
		TrustAllSSL.enable();
		SpringApplication.run(ChatApplication.class, args);
	}

}
