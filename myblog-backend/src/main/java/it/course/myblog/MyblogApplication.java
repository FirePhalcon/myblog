package it.course.myblog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
//import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
//@PropertySource("file:${myPath}/application.properties")//quando o properties está fora do projeto
//@PropertySource("classPath:application.properties")//quando o properties está dentro do projeto - vai poder usar somente o getProperty
public class MyblogApplication {
	
	@Value("${app.test.profile}")
	String profile;
	
	@Autowired
	Environment env;

	public static void main(String[] args) {
		SpringApplication.run(MyblogApplication.class, args);
	}
	
	@Profile("dev")
	@Bean
	public String testProfile() {
		
		String y = env.getProperty("app.test.profile").toLowerCase(); //substitui o @value - é mais antigo
		
		log.info("The used profile is: " + y);	
		
		return y;
	}

}