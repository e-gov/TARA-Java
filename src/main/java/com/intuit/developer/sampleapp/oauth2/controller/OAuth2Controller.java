package com.intuit.developer.sampleapp.oauth2.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import com.intuit.developer.sampleapp.oauth2.domain.OAuth2Configuration;
import com.intuit.developer.sampleapp.oauth2.helper.HttpHelper;
import com.intuit.developer.sampleapp.oauth2.service.ValidationService;

/**
 * Autentimispäringu saatja
 *
 */
@Controller
public class OAuth2Controller {

	private static final Logger logger = Logger.getLogger(OAuth2Controller.class);

	@Autowired
	public OAuth2Configuration oAuth2Configuration;

	@Autowired
	public ValidationService validationService;

	@Autowired
	public HttpHelper httpHelper;

	/*
		Avaleht
		*/
	@RequestMapping("/")
	public String home() {
		System.out.println("*** TARA-Java ***");
		return "home";
	}

	@RequestMapping("/autenditud")
	public String connected() {
		return "autenditud";
	}

	/**
	 * Autentimispäringu saatmine
	 * @return
	 */
	@RequestMapping("/autentimisele")
	public View sendAuthRequest(HttpSession session) {

		logger.info("Autentimispäringu saatmine");

		return new RedirectView(
			prepareUrl(
				oAuth2Configuration.getSimpleScope(),
		 		generateCSRFToken(session)),
				true,
				true,
				false);
	}

	/*
	 Moodustab autentimispäringu
	 
	  @param scope Päringusse pandav skoop või skoobid
	  @param csrfToken Võltspäringuründe vastane turvaväärtus
	  @return Autentimispäring, URLencoded vormingus
	  */
	private String prepareUrl(String scope, String csrfToken) {
		try {
			return oAuth2Configuration.getTARAAuthorizationEndpoint() +
				"?client_id=" +
				oAuth2Configuration.getClientId() +
				"&response_type=code&scope=" +
				URLEncoder.encode(scope, "UTF-8") +
				"&redirect_uri=" +
				URLEncoder.encode(oAuth2Configuration.getRedirectUri(), "UTF-8") +
				"&state=" +
				csrfToken;
		} catch (UnsupportedEncodingException e) {
			logger.error("Exception while preparing url for redirect ", e);
		}
		return null;
	}

	/*
		Moodustab võltspäringuründe vastase turvatokeni
		*/
	private String generateCSRFToken(HttpSession session) {
		String csrfToken = UUID.randomUUID().toString();
		session.setAttribute("csrfToken", csrfToken);
		return csrfToken;
	}

}
