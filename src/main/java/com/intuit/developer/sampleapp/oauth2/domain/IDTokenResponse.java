package com.intuit.developer.sampleapp.oauth2.domain;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
/* @JsonPropertyOrder({
        "expires_in",
        "id_token",
        "refresh_token",
        "x_refresh_token_expires_in",
        "access_token",
        "token_type"
}) */
@Component
public class IDTokenResponse {

    @JsonProperty("id_token")
    private String idToken;
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The idToken
     */
    @JsonProperty("id_token")
    public String getIdToken() {
        return idToken;
    }

    /**
     *
     * @param idToken
     * The id_token
     */
    @JsonProperty("id_token")
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }


    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}