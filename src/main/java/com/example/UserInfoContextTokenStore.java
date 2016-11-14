package com.example;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserInfoContextTokenStore extends JwtTokenStore {

	private static Logger LOGGER = LoggerFactory.getLogger(UserInfoContextTokenStore.class);
	
	private ObjectMapper  claimsSetMapper = new ObjectMapper();
	
	private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
		
	/**
	 * 
	 * @param jwtTokenEnhancer
	 */
	public UserInfoContextTokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
		super(jwtTokenEnhancer);
	}

	@Override
	public OAuth2Authentication readAuthentication(String token) {
		OAuth2Authentication authentication = super.readAuthentication(token);

		if (authentication == null ) {
			return null;
		}
		
		try {
			UserInfoContext userInfoContext = this.claimsSetMapper.readValue(JwtHelper.decode(token).getClaims().getBytes(), UserInfoContext.class);
			LOGGER.debug("Jwt UserInfoContext : {}", ToStringBuilder.reflectionToString(userInfoContext, ToStringStyle.JSON_STYLE));
			
			Set<ConstraintViolation<UserInfoContext>> violations = 
						this.validatorFactory.getValidator().validate(userInfoContext);
			
			if (! violations.isEmpty()) {
				violations.stream().forEach(violation -> {
					LOGGER.error("Jwt token validation error, '{}' {}", new Object[] { violation.getPropertyPath(), violation.getMessage()});
				});
				
				return null;
			}
			
			authentication.setDetails(userInfoContext);
			
		} catch (Exception e) {
			LOGGER.error("Error while getting the jwt claims set", e);
			return null;
		}

		return authentication;
	}
}