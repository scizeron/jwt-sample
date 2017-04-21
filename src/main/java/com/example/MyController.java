package com.example;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * 
 * @author stéphane
 *
 */
@RestController
@RequestMapping(produces={MediaType.APPLICATION_JSON_VALUE})
public class MyController {

	@RequestMapping(value="/anonymous", produces={MediaType.TEXT_PLAIN_VALUE})
	public String anonymous() {
		return "anonymous";
	}
	
	@RequestMapping(value="/ping", produces={MediaType.TEXT_PLAIN_VALUE})
	public String ping() {
		return "pong";
	}
	
	@RequestMapping("/users/{userId}/info")
	public UserInfoContext getUserInfo(UserInfoContext userInfoContext) {
		return userInfoContext;
	}
}
