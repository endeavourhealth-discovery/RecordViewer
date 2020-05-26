package org.endeavourhealth.recordviewer.api.endpoints;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.http.HttpStatus;
import org.endeavourhealth.core.database.dal.usermanager.caching.ApplicationPolicyCache;
import org.endeavourhealth.core.database.dal.usermanager.caching.UserCache;
import org.endeavourhealth.core.database.dal.usermanager.models.JsonApplicationPolicyAttribute;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class HL7APIGatewayAuthorizerHandler implements RequestHandler<TokenAuthorizerContext, AuthPolicy> {

    @Override
    public AuthPolicy handleRequest(TokenAuthorizerContext input, Context context) {

        String headerAuthToken = input.getAuthorizationToken();
        String userID = "";

        /*KeycloakPrincipal kp = (KeycloakPrincipal)sc.getUserPrincipal();
        String headerAuthToken = kp.getKeycloakSecurityContext().getTokenString();*/

        // validate the incoming authorization token by calling dev keycloak and produce the principal user identifier associated with the token
        Client client = ClientBuilder.newClient();
        String url = "https://devauth.discoverydataservice.net/";
        String path = "auth/realms/endeavour2/protocol/openid-connect/userinfo";

        WebTarget target = client.target(url).path(path);

        Boolean foundFHIRPolicy = false;

        try {
            Response response = target
                    .request()
                    .header("Authorization", "Bearer "+headerAuthToken)
                    .get();

            if (response.getStatus() == HttpStatus.SC_OK) { // user is authorized in keycloak, so get the user record and ID associated with the token
                Map<String, String> users = response.readEntity(Map.class);

                for (Map.Entry<String, String> user: users.entrySet()) {
                    if (user.getKey().equals("sub")) {
                        userID = user.getValue();
                        break;
                    }
                }

                // call user manager with the user ID to get the user's authorized applications and policies
                String applicationPolicyId = UserCache.getUserApplicationPolicyId(userID);
                List<JsonApplicationPolicyAttribute> jsonApplicationPolicyAttributes = ApplicationPolicyCache.getApplicationPolicyAttributes(applicationPolicyId);

                for (JsonApplicationPolicyAttribute policyAttribute: jsonApplicationPolicyAttributes) {
                    String accessProfileName = policyAttribute.getApplicationAccessProfileName();
                    if (accessProfileName.equals("record-viewer:fhir")) { // check that the user has an application policy for accessing FHIR records
                        foundFHIRPolicy = true;
                        break;
                    }
                }

            } else { // user is not authorized with this token
                throw new RuntimeException("Unauthorized"); // Not authorized so send 401/403 Unauthorized response to the client
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unauthorized"); // Not authorized so send 401/403 Unauthorized response to the client
        }

        if (!foundFHIRPolicy) {
            throw new RuntimeException("Unauthorized"); // Not authorized so send 401 Unauthorized response to the client
        }

        // if the token is valid, a policy is generated which will allow or deny access to the client
        // if access is allowed, API Gateway will proceed with the back-end integration configured on the method that was called

        String methodArn = input.getMethodArn();
        String[] arnPartials = methodArn.split(":");
        String region = arnPartials[3];
        String awsAccountId = arnPartials[4];
        String[] apiGatewayArnPartials = arnPartials[5].split("/");
        String restApiId = apiGatewayArnPartials[0];
        String stage = apiGatewayArnPartials[1];
        String httpMethod = apiGatewayArnPartials[2];
        String resource = ""; // root resource
        if (apiGatewayArnPartials.length == 4) {
            resource = apiGatewayArnPartials[3];
        }

        return new AuthPolicy(userID, AuthPolicy.PolicyDocument.getAllowAllPolicy(region, awsAccountId, restApiId, stage));
        // return new AuthPolicy(userID, AuthPolicy.PolicyDocument.getDenyAllPolicy(region, awsAccountId, restApiId, stage));
    }

}
