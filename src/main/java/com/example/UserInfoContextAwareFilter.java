package com.example;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * 
 * @author stéphane
 *
 */
public class UserInfoContextAwareFilter implements Filter {

	private static Logger LOGGER = LoggerFactory.getLogger(UserInfoContextAwareFilter.class);

	private final String regex;

	private final int userIdGroup;

	/**
	 * 
	 * @param regex
	 * @param userIdGroup
	 */
	public UserInfoContextAwareFilter(String regex, int userIdGroup) {
		this.regex = regex;
		this.userIdGroup = userIdGroup;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;

		String url = request.getServletPath();
		String pathInfo = request.getPathInfo();
		String query = request.getQueryString();

		if (pathInfo != null || query != null) {
			StringBuilder sb = new StringBuilder(url);

			if (pathInfo != null) {
				sb.append(pathInfo);
			}

			if (query != null) {
				sb.append('?').append(query);
			}
			url = sb.toString();
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			chain.doFilter(request, response);
			return;
		}
		
		OAuth2Authentication oauth2Authentication = (OAuth2Authentication) authentication;
		UserInfoContext infoContext = (UserInfoContext) ((OAuth2AuthenticationDetails) oauth2Authentication.getDetails()).getDecodedDetails();
		request.setAttribute(UserInfoContext.class.getName(), infoContext);

		Matcher matcher = Pattern.compile(this.regex).matcher(url);
		
		if (matcher.matches()) {

			if (matcher.groupCount() < this.userIdGroup) {
				LOGGER.warn("Bad request, impossible to get the userId from the request");
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return;
			}

			String userIdPathVariable = matcher.group(this.userIdGroup);

			if (!userIdPathVariable.equals(infoContext.getId())) {
				LOGGER.warn("Bad request, user ids inconsistency ['{}' != '{}']",
						new Object[] { userIdPathVariable, infoContext.getId() });
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return;
			}
		}
		
		chain.doFilter(request, response);
	}
}