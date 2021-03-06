package com.mycompany.sample.plumbing.claims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycompany.sample.plumbing.errors.ErrorCodes;
import com.mycompany.sample.plumbing.errors.ErrorFactory;

/*
 * A default implementation that can be overridden
 */
public class CustomClaimsProvider {

    /*
     * Return empty custom claims by default
     */
    @SuppressWarnings(value = "checkstyle:DesignForExtension")
    public CustomClaims getCustomClaims(final TokenClaims token, final UserInfoClaims userInfo) {
        return new CustomClaims();
    }

    /*
     * Do the serialization work before saving to the cache
     */
    public final String serialize(final ApiClaims claims) {

        var mapper = new ObjectMapper();
        var data = mapper.createObjectNode();
        data.set("token", claims.getToken().exportData());
        data.set("userInfo", claims.getUserInfo().exportData());
        data.set("custom", claims.getCustom().exportData());
        return data.toString();
    }

    /*
     * Do the deserialization work to read claims from the cache
     */
    public final ApiClaims deserialize(final String claimsText) {

        try {
            var mapper = new ObjectMapper();
            var data = mapper.readValue(claimsText, ObjectNode.class);

            var token = TokenClaims.importData(data.get("token"));
            var userInfo = UserInfoClaims.importData(data.get("userInfo"));
            var custom = this.deserializeCustomClaims(data.get("custom"));

            return new ApiClaims(token, userInfo, custom);

        } catch (Throwable ex) {

            // Report the error including an error code and exception details
            throw ErrorFactory.createServerError(
                    ErrorCodes.JSON_PARSE_ERROR,
                    "Problem encountered parsing JSON data",
                    ex);
        }
    }

    /*
     * This default implementation can be overridden to manage deserialization
     */
    @SuppressWarnings(value = "checkstyle:DesignForExtension")
    protected CustomClaims deserializeCustomClaims(final JsonNode claimsNode) {
        return CustomClaims.importData(claimsNode);
    }
}
