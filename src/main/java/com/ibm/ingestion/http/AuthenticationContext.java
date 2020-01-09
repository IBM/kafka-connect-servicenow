package com.ibm.ingestion.http;

import org.json.JSONObject;

public class AuthenticationContext {

    private String _accessToken;
    private String _refreshToken;

    public AuthenticationContext(JSONObject jwtToken) {
        this._accessToken = jwtToken.getString("access_token");
        this._refreshToken = jwtToken.getString("refresh_token");
    }

    public String getAccessToken() {
        return _accessToken;
    }

    public String getRefreshToken() {
        return _refreshToken;
    }

}
