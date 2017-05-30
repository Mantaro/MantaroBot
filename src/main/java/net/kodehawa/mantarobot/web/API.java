package net.kodehawa.mantarobot.web;

import net.kodehawa.mantarobot.MantaroBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class API {

	public MantaroBot bot;

	public static void main(String[] args) {
		SpringApplication.run(API.class);
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer() {
		return container -> {
			container.setPort(8117);
			container.setDisplayName("MantaroBotAPI");
		};
	}

	public void setBot(MantaroBot callable){
		bot = callable;
	}

	public MantaroBot getBot() {
		return bot;
	}
}
