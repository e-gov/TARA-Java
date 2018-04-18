package com.intuit.developer.sampleapp.oauth2.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.developer.sampleapp.oauth2.domain.IDTokenResponse;
import com.intuit.developer.sampleapp.oauth2.domain.OAuth2Configuration;
import com.intuit.developer.sampleapp.oauth2.helper.HttpHelper;
import com.intuit.developer.sampleapp.oauth2.service.ValidationService;

/**
 * @author dderose
 *
 */
@Controller
public class CallbackController {
    
    @Autowired
    public OAuth2Configuration oAuth2Configuration;
    
    @Autowired
    public ValidationService validationService;
    
    @Autowired
    public HttpHelper httpHelper;
    
    private static final HttpClient CLIENT =
      HttpClientBuilder
        .create()
        .build();
    private static ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(CallbackController.class);
    
    /**
     * Tagasipöördumise käsitleja
     *      
     * @param auth_code - volituskood
     * @param state     - 
     * @param session   - seansiandmete hoidmise struktuur
     * @return
     */
    @RequestMapping("/Callback")
    public String callBackFromOAuth(
        @RequestParam("code")
         String authCode,
        @RequestParam("state")
         String state,
        HttpSession session) {   
        logger.debug("Volituskoodi töötlemisel");
        
        String csrfToken = (String) session
          .getAttribute("csrfToken");
        if (csrfToken.equals(state)) {
            session.setAttribute("auth_code", authCode);

            /* Päri identsustõend */
            IDTokenResponse IDTokenResponse = requestIDToken(authCode, session);  
            
            /*
             * save token to session
             * In real usecase, this is where tokens would have to be persisted (to a SQL DB, for example). 
             * Update your Datastore here with user's AccessToken and RefreshToken along with the realmId
            */
            /* session.setAttribute("access_token", IDTokenResponse.getAccessToken());
            session.setAttribute("refresh_token", IDTokenResponse.getRefreshToken()); */
         
            /* 
             * However, in case of OpenIdConnect, when you request OpenIdScopes during authorization,
             * you will also receive IDToken from Intuit. You first need to validate that the IDToken actually came from Intuit.
             */
            if (StringUtils.isNotBlank(IDTokenResponse.getIdToken())) {
               if(validationService.isValidIDToken(IDTokenResponse.getIdToken())) {
                   logger.info("Identsustõend kontrollitud");
                   //get user info
                   // saveUserInfo(IDTokenResponse.getAccessToken(), session);
               }
            }
            
            return "autenditud";
        }
        logger.info("Viga: State väärtus ei klapi" );
        return null;
    }

    private IDTokenResponse requestIDToken(String auth_code, HttpSession session) {
        logger.info("inside bearer tokens");

        HttpPost post = new HttpPost(oAuth2Configuration.getTARATokenEndpoint());

        // Lisa päis
        post = httpHelper.addHeader(post);
        List<NameValuePair> urlParameters = httpHelper.getUrlParameters(session, "");

        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            HttpResponse response = CLIENT.execute(post);

            logger.info("Response Code : "+ response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.info("Viga identsustõendi pärimisel");
                return null;
            }

            StringBuffer result = httpHelper.getResult(response);
            logger.debug("Saadud identsustõend: " + result);

            return mapper.readValue(result.toString(), IDTokenResponse.class);
            
        } catch (Exception ex) {
            logger.error("Viga identsustõendi pärimisel", ex);
        }
        return null;
    }

    /*
    private void saveUserInfo(String accessToken, HttpSession session) {
        //Ideally you would fetch the realmId and the accessToken from the data store based on the user account here.
        HttpGet userInfoReq = new HttpGet(oAuth2Configuration.getUserProfileApiHost());
        userInfoReq.setHeader("Accept", "application/json");
        userInfoReq.setHeader("Authorization","Bearer "+accessToken);

        try {
            HttpResponse response = CLIENT.execute(userInfoReq);

            logger.info("Response Code : "+ response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() == 200) {
                
                StringBuffer result = httpHelper.getResult(response);
                logger.debug("raw result for user info= " + result);

                //Save the UserInfo here.
                JSONObject userInfoPayload = new JSONObject(result.toString());
                session.setAttribute("sub", userInfoPayload.get("sub"));
                session.setAttribute("givenName", userInfoPayload.get("givenName"));
                session.setAttribute("email", userInfoPayload.get("email"));
                
            } else {
                logger.info("failed getting user info");
            }

            
        }
        catch (Exception ex) {
            logger.error("Exception while retrieving user info ", ex);
        }
    }

    */

}
