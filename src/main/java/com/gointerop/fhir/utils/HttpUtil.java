package com.gointerop.fhir.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@Component
public class HttpUtil {
    @Autowired
    SSLUtil sslUtil;

    public String get(String urlString, String accept) throws MalformedURLException, IOException, ProtocolException {
        String retVal = null;                       

        sslUtil.disableSslVerification();

        HttpURLConnection http = configureHeaders(urlString, HttpMethod.GET, accept, null);

        int code = http.getResponseCode();

        if (code == 404) throw new ResourceNotFoundException("Documento not found");
        
        StringBuffer response = readBufferedResponse(http, code);

        retVal = response.toString();        

        return retVal;
    }

    public String post(String urlString, String accept, String content) throws MalformedURLException, IOException, ProtocolException {
        String retVal = null;                       

        sslUtil.disableSslVerification();

        HttpURLConnection http = configureHeaders(urlString, HttpMethod.POST, accept, null);
        
        try( BufferedOutputStream bos = new BufferedOutputStream( http.getOutputStream())) {
            bos.write(content.getBytes("UTF-8"));
        }

        int code = http.getResponseCode();

        if (code == 404) throw new ResourceNotFoundException("Documento not found");
        
        StringBuffer response = readBufferedResponse(http, code);

        retVal = response.toString();        

        return retVal;
    }

    private HttpURLConnection configureHeaders(String urlString, HttpMethod method, String accept, String authorization)
            throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(urlString);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod(method.toString());        
        http.setDoOutput(true);
        if(accept != null) http.setRequestProperty("Accept", accept);
        if(authorization != null) http.setRequestProperty("Authorization", "Bearer " + authorization);
        http.setRequestProperty("Content-Type", "application/fhir+json;fhirVersion=4.0");        
        return http;
    }

    private StringBuffer readBufferedResponse(HttpURLConnection http, int code) throws IOException {
        StringBuffer response = new StringBuffer();
        String inputLine;
        if (code != 200 && code != 201) {
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getErrorStream(), "UTF-8"));
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            http.disconnect();
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF-8"));
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            http.disconnect();
        }
        return response;
    }
}