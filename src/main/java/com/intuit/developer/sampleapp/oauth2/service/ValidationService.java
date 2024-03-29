package com.intuit.developer.sampleapp.oauth2.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intuit.developer.sampleapp.oauth2.domain.OAuth2Configuration;
import com.intuit.developer.sampleapp.oauth2.helper.HttpHelper;

/**
 * Identsustõendi kontrollija
 *
 */
@Service
public class ValidationService {
    
    @Autowired
    public OAuth2Configuration oAuth2Configuration;
    
    @Autowired
    public HttpHelper httpHelper;
    
    private static final CloseableHttpClient CLIENT = HttpClientBuilder.create().build();
    private static final Logger logger = Logger.getLogger(ValidationService.class);
    
    /**
     * Identsustõendi kontrollimine
     * 
     * @param idToken
     * @return
     */
    public boolean isValidIDToken(String idToken,
      HttpSession session) {

        String[] idTokenParts = idToken.split("\\.");
        
        if (idTokenParts.length < 3) {
            logger.debug("invalid idTokenParts length");
            return false;
        }

        String idTokenHeader = base64UrlDecode(idTokenParts[0]);
        String idTokenPayload = base64UrlDecode(idTokenParts[1]);
        byte[] idTokenSignature = base64UrlDecodeToBytes(idTokenParts[2]);

        JSONObject idTokenHeaderJson = new JSONObject(idTokenHeader);
        JSONObject idTokenHeaderPayload = new JSONObject(idTokenPayload);
        
        // Eralda autenditud isiku identifikaator (isikukood)
        String sub = idTokenHeaderPayload.getString("sub");
        // ja salvesta seansihoidjasse
        session.setAttribute("sub", sub);

        // Eralda autenditud isiku profiiliteave
        JSONObject profiiliteave = idTokenHeaderPayload.getJSONObject("profile_attributes");
        String eesnimi = profiiliteave.getString("given_name");
        String perenimi = profiiliteave.getString("family_name");
        // ja salvesta seansihoidlasse
        session.setAttribute("given_name", eesnimi);
        session.setAttribute("family_name", perenimi);

        // Kontrolli tõendi väljaandjat
        String issuer = idTokenHeaderPayload.getString("iss");
        if(!issuer.equalsIgnoreCase(oAuth2Configuration.getIssuer())) {
            logger.debug("Tõendi väljaandja ei klapi");
            return false;
        }

        // Kontrolli tõendi saajat
        String aud = idTokenHeaderPayload.getString("aud"); 
        if(!aud.equalsIgnoreCase(oAuth2Configuration.getClientId())) {
            logger.debug("Tõendi saaja ei klapi");
            return false;
        }

        // Kontrolli, et tõend ei ole aegunud
        Long expirationTimestamp = idTokenHeaderPayload.getLong("exp");
        Long currentTime = Instant.now().getEpochSecond();

        if((expirationTimestamp - currentTime) <= 0) {
            logger.debug("Tõend on aegunud");
            return false;
        }

        JSONObject TARAvoti = hangiTARAvoti();

        /*
        // Kontrolli tõendi allkirja
        HashMap<String,JSONObject> keyMap = getKeyMapFromJWKSUri();
        if (keyMap == null || keyMap.isEmpty()) {
            logger.debug("unable to retrive keyMap from JWKS url");
            return false;
        }

        //first get the kid from the header.
        String keyId = idTokenHeaderJson.getString("kid");
        JSONObject keyDetails = keyMap.get(keyId);
        */

        // Koosta PublicKey
        String exponent = TARAvoti.getString("e");
        String modulo = TARAvoti.getString("n");

        PublicKey publicKey = getPublicKey(modulo, exponent);

        byte[] data = (idTokenParts[0] + "." + idTokenParts[1]).getBytes(StandardCharsets.UTF_8);

        // Kontrolli allkirja
        try {
            boolean isSignatureValid = verifyUsingPublicKey(data, idTokenSignature, publicKey);
            logger.info("Allkiri õige: " + isSignatureValid);
            return isSignatureValid;
        } catch (GeneralSecurityException e) {
            logger.error("Erind identsustõendi allkirja kontrollimisel ", e);
            return false;
        }

    }
    
    private String base64UrlDecode(String input) {
        byte[] decodedBytes = base64UrlDecodeToBytes(input);
        String result = new String(decodedBytes, StandardCharsets.UTF_8);
        return result;
    }

    private byte[] base64UrlDecodeToBytes(String input) {
        Base64 decoder = new Base64(-1, null, true);
        byte[] decodedBytes = decoder.decode(input);

        return decodedBytes;
    }

    /**
     * Hangib HTTP GET päringuga TARA allkirjavõtme
     * 
     * @return - TARA avalik allkirjavõti, JSONObject-na
     */
    private JSONObject hangiTARAvoti() {

        // Valmista HTTP GET päring
        HttpGet TARAKeyRequest =
          new HttpGet(oAuth2Configuration.getTARAKeyEndpoint());

        try {

            CloseableHttpResponse response = CLIENT.execute(TARAKeyRequest);
            StringBuffer result;
            try {

                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.info("TARA allkirjavõtme hankimine ebaõnnestus");
                    return null;
                }

                result = httpHelper.getResult(response);
            } finally {
                response.close();
            }
            logger.debug("Saadud võti: " + result);

            JSONObject jwksPayload = new JSONObject(result.toString());
            JSONArray keysArray = jwksPayload.getJSONArray("keys");

            JSONObject voti = keysArray.getJSONObject(0);

            return voti;
        }
        catch (Exception ex) {
            logger.error("Erind TARA allkirjavõtme hankimisel: ", ex);
            return null;
        }

    }

    private PublicKey getPublicKey(String MODULUS, String EXPONENT) {
        byte[] nb = base64UrlDecodeToBytes(MODULUS);
        byte[] eb = base64UrlDecodeToBytes(EXPONENT);
        BigInteger n = new BigInteger(1, nb);
        BigInteger e = new BigInteger(1, eb);

        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(n, e);
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
            return publicKey;
        } catch (Exception ex) {
            logger.error("Exception while getting public key ", ex);
            throw new RuntimeException("Cant create public key", ex);
        }
    }

    private boolean verifyUsingPublicKey(byte[] data, byte[] signature, PublicKey pubKey)
            throws GeneralSecurityException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKey);
        sig.update(data);
        return sig.verify(signature);
    }

}
