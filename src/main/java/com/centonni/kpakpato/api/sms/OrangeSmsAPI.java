/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.centonni.kpakpato.api.sms;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public final class OrangeSmsAPI implements SmsAPI {

    public final static String BASE_URL = "https://api.orange.com";
    public final static String TOKEN_URL = "oauth/v2/token";
    private String clientId;
    private String clientSecret;
    private AuthenticationToken token;

    private OrangeSmsAPI() {

    }

    public OrangeSmsAPI(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        token = getToken();
    }

    @Override
    public AuthenticationToken getToken() {

        String clientInfos = clientId + ":" + clientSecret;
        String body = "grant_type=client_credentials";
        AuthenticationToken authenticationToken = null;

        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(OrangeSmsAPI.BASE_URL + "/" + OrangeSmsAPI.TOKEN_URL);
            postRequest.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(clientInfos.getBytes()));
            StringEntity input = new StringEntity(body);
            input.setContentType("application/x-www-form-urlencoded");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

            final ObjectMapper objectMapper = new ObjectMapper();
            authenticationToken = objectMapper.readValue(response.getEntity().getContent(), AuthenticationToken.class);

        } catch (JsonParseException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return authenticationToken;
    }

    @Override
    public boolean sendSms(MessageContext message, String receiverAdress) {

        String path = BASE_URL + "/" + "smsmessaging/v1/outbound/" + message.getSenderAdress() + "/requests";

        String body = bodyToJSON(createMessageBody(message, receiverAdress)); 
        
        boolean state=false;
        
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(path);
            postRequest.setHeader("Authorization", token.getAuthorization());
            StringEntity input = new StringEntity(body);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);
            if (response.getStatusLine().getStatusCode() != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }else{
                state=true;
                final ObjectMapper objectMapper = new ObjectMapper();
            MessageBody messageBody = objectMapper.readValue(response.getEntity().getContent(), MessageBody.class);
                System.out.println(" $$$$ "+messageBody);

            }

        } catch (JsonParseException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return state;
    }

    @Override
    public boolean sendSms(MessageContext message, String... receiverAdress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private MessageBody createMessageBody(MessageContext message, String receiverAdress) {

        OutboundSMSMessageRequest messageRequest = new OutboundSMSMessageRequest();
        messageRequest.setAddress(receiverAdress);
        messageRequest.setSenderAddress(message.getSenderAdress());
        messageRequest.setSenderName(message.getSenderName());

        OutboundSMSTextMessage textMessage = new OutboundSMSTextMessage();
        textMessage.setMessage(message.getMessage());

        messageRequest.setOutboundSMSTextMessage(textMessage);

        MessageBody body = new MessageBody(messageRequest);

        return body;
    }

    private String bodyToJSON(MessageBody body) {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{}";
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(OrangeSmsAPI.class.getName()).log(Level.SEVERE, null, ex);
        }

        return json;
    }
}