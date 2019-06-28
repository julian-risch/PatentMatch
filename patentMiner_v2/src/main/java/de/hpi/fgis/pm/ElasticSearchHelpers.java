package de.hpi.fgis.pm;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


public class ElasticSearchHelpers implements PatentMinerConstants{

    public static RestClient buildRestClient() {

        RestClientBuilder builder = RestClient.builder(
                new HttpHost("localhost", 9200, "http"));
        builder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(Node node) {
                System.out.println("Error: Node Failed");
            }
        });
        RestClient restClient = builder.build();
        return restClient;
    }


    public static void checkHealth() throws IOException {
        Request request = new Request("GET", "/_cluster/health");
        request.setEntity(new NStringEntity(
                "{\"json\":\"text\"}",
                ContentType.APPLICATION_JSON));
        RestClient restClient = buildRestClient();
        Response response = restClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        System.out.println(responseBody);

    }


}