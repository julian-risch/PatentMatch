package de.hpi.fgis.pm;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import static de.hpi.fgis.pm.PatentMinerConstants.ES_HOST;
import static de.hpi.fgis.pm.PatentMinerConstants.ES_PORT;

public class ConnectElasticSearch {
    public static RestClient buildRestClient() {

        RestClient restClient = RestClient.builder(
                new HttpHost(ES_HOST, ES_PORT, "http")).build();
        return restClient;
    }

    public static RestHighLevelClient buildRestHighLevelClient() {

        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                new HttpHost(ES_HOST, ES_PORT, "http")));
        return client;
    }

    

}
