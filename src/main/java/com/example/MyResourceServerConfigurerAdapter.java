package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

/**
 * 
 * @author stéphane
 *
 */
@Configuration
@EnableResourceServer
public class MyResourceServerConfigurerAdapter extends ResourceServerConfigurerAdapter {

	@Autowired
	private JwtAccessTokenConverter jwtAccessTokenConverter;
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
			.antMatchers(HttpMethod.GET, "/users/**").authenticated()
			.antMatchers(HttpMethod.GET, "/ping").authenticated()
			.antMatchers(HttpMethod.GET, "/anonymous").anonymous()
			.anyRequest().denyAll()
			.and().addFilterAfter(new UserInfoContextAwareFilter("(.*)(/users/)([a-zA-Z_0-9]+)/(.*)", 3), FilterSecurityInterceptor.class)
	 	;
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resourceServerSecurityConfigurer) throws Exception {
		DefaultTokenServices services = new DefaultTokenServices();
		services.setTokenStore(new UserInfoContextTokenStore(this.jwtAccessTokenConverter));
		resourceServerSecurityConfigurer.tokenServices(services);
	}
}