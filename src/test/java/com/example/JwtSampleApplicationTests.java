package com.example;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class JwtSampleApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	
	private SignedJWT signedJWT;
	
	private JWSSigner signer;
	
	/**
	 * 
	 * @throws Exception
	 */
	public JwtSampleApplicationTests() throws Exception {
		String signingKey = new String(Files.readAllBytes(ResourceUtils.getFile("src/test/resources/private-key.txt").toPath()));
		RsaSigner rsaSigner = new RsaSigner(signingKey);
		Field privateKey = ReflectionUtils.findField(RsaSigner.class, "key");
		privateKey.setAccessible(true);
		this.signer = new RSASSASigner((java.security.PrivateKey) privateKey.get(rsaSigner));
	}
	
	/**
	 * 
	 * @param claimsSet
	 * @return
	 * @throws Exception
	 */
	private String generateToken(JWTClaimsSet claimsSet) throws Exception {
		this.signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
		this.signedJWT.sign(this.signer);
		return this.signedJWT.serialize();
	}
	
	@Test
	public void queryWithNoToken() {
		ResponseEntity<UserInfoContext> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, null, UserInfoContext.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.UNAUTHORIZED));
	}
	
	@Test
	public void queryWithAnInvalidToken()  {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Authorization", "Bearer " + UUID.randomUUID().toString());
		ResponseEntity<String> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.UNAUTHORIZED));
		Assert.assertThat(exchange.getBody(), CoreMatchers.containsString("invalid_token"));
	}
	
	@Test
	public void assertValidUserInfoRequest() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
	
		requestHeaders.add("Authorization", "Bearer " + generateToken(new JWTClaimsSet.Builder()
			      .claim("id", "123456879")
			      .claim("facilities", new String[]{"10", "11", "12"})
			      .build()));
		
		ResponseEntity<UserInfoContext> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, new HttpEntity<>(requestHeaders), UserInfoContext.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.OK));
		Assert.assertThat(exchange.getBody().getId(), CoreMatchers.is("123456879"));
	}
	
	@Test
	public void anonymousAccess() {
		/**
		 * The security filter doesn't impact authorized anonymous access
		 */
		ResponseEntity<String> exchange = this.restTemplate.exchange("/anonymous", HttpMethod.GET, null, String.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.OK));
		Assert.assertThat(exchange.getBody(), CoreMatchers.is("anonymous"));
	}
	
	@Test
	public void assertValidPingRequest() throws Exception {
		/**
		 * The /ping endpoint is secure but no userId path variable considerations
		 */
		HttpHeaders requestHeaders = new HttpHeaders();
	
		requestHeaders.add("Authorization", "Bearer " + generateToken(new JWTClaimsSet.Builder()
			      .claim("id", "123456879")
			      .claim("facilities", new String[]{"10", "11", "12"})
			      .build()));
		
		ResponseEntity<String> exchange = this.restTemplate.exchange("/ping", HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.OK));
		Assert.assertThat(exchange.getBody(), CoreMatchers.is("pong"));
	}
	
	@Test
	public void assertInvalidUserId() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Authorization", "Bearer " + generateToken(new JWTClaimsSet.Builder()
			      .claim("id", String.valueOf(System.currentTimeMillis()))
			      .claim("facilities", new String[]{"10", "11", "12"})
			      .build()));
		ResponseEntity<UserInfoContext> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, new HttpEntity<>(requestHeaders), UserInfoContext.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.BAD_REQUEST));
	}
	
	@Test
	public void assertJwtMissingUserId() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Authorization", "Bearer " + generateToken(new JWTClaimsSet.Builder()
				  .claim("facilities", new String[]{"10", "11", "12"})
			      .build()));
		ResponseEntity<UserInfoContext> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, new HttpEntity<>(requestHeaders), UserInfoContext.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.UNAUTHORIZED));
	}
	
	@Test
	public void assertJwtMissingFacilities() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Authorization", "Bearer " + generateToken(new JWTClaimsSet.Builder()
				.claim("id", "123456879")
			    .build()));
		ResponseEntity<UserInfoContext> exchange = this.restTemplate.exchange("/users/123456879/info", HttpMethod.GET, new HttpEntity<>(requestHeaders), UserInfoContext.class);
		Assert.assertThat(exchange.getStatusCode(), CoreMatchers.is(HttpStatus.UNAUTHORIZED));
	}
}
