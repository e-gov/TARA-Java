package com.intuit.developer.sampleapp.oauth2.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
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
 * Tagasipöördumispäringu töötleja
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
    
    private static final CloseableHttpClient CLIENT =
      HttpClientBuilder
        .create()
        .build();

    private static ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(CallbackController.class);
    
    /**
     * Tagasipöördumispäringu käsitleja
     *      
     * @param auth_code - volituskood
     * @param state     - võltspäringuründe vastane turvatoken
     * @param session   - seansiandmete hoidja
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
            
            // Eralda vastusest identsustõend
            String IDToken = IDTokenResponse.getIdToken();

            // Kui identsustõend ei ole tühi,
            if (StringUtils.isNotBlank(IDToken)) {
               // siis kontrolli identsustõendit
               if(validationService.isValidIDToken(IDToken, session)) {
                   logger.info("Identsustõend kontrollitud");
               }
            }
            
            // Tagasta sirvikusse leht "autenditud"
            return "autenditud";
        }
        logger.info("Viga: State väärtus ei klapi" );
        return null;
    }

    /**
     * Pärib identsustõendi
     * 
     * @param auth_code - volituskood
     * @param session   - seansiandmete hoidja
     * @return          - päringu vastus - JSON teisendatud POJO-ks
     */
    private IDTokenResponse requestIDToken(String auth_code, HttpSession session) {
        logger.info("inside bearer tokens");

        HttpPost post = new HttpPost(oAuth2Configuration.getTARATokenEndpoint());

        // Lisa päis
        post = httpHelper.addHeader(post);
        List<NameValuePair> urlParameters = httpHelper.getUrlParameters(session, "");

        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            CloseableHttpResponse response = CLIENT.execute(post);

            StringBuffer result;
            try {
                logger.info("Response Code : "+ response.getStatusLine().getStatusCode());
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.info("Viga identsustõendi pärimisel");
                    return null;
                }

                result = httpHelper.getResult(response);
            } finally {
                response.close();
            }
            logger.debug("Identsustõendi päringu vastus: " + result);

            return mapper.readValue(result.toString(), IDTokenResponse.class);
            
        } catch (Exception ex) {
            logger.error("Viga identsustõendi pärimisel", ex);
        }
        return null;
    }

}
