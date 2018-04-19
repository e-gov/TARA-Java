package com.intuit.developer.sampleapp.oauth2.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * @author dderose
 *
 */
@Configuration
@PropertySource(value="classpath:/application.properties", ignoreResourceNotFound=true)
public class OAuth2Configuration {
	
	@Autowired
    Environment env;

    public String getTARAAuthorizationEndpoint() {
    	return env.getProperty("TARAAuthorizationEndpoint");
    }

    public String getTARATokenEndpoint() {
    	return env.getProperty("TARATokenEndpoint");
    }
    
    public String getTARAKeyEndpoint() {
    	return env.getProperty("TARAKeyEndpoint");
    }
    
	public String getSimpleScope() {
		return env.getProperty("SimpleScope");
	}
	
    public String getClientId() {
    	return env.getProperty("ClientId");
    }

    public String getClientSecret() {
        return System.getenv("CLIENT_SECRET");
    }
    
    public String getRedirectUri() {
        return env.getProperty("RedirectUri");
	}
	
	public String getIssuer() {
		return env.getProperty("Issuer");
	}

}
