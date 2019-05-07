package de.hpi.fgis.pm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Base64;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;

import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.common.settings.Settings;

public class ElasticSearchHelpers implements PatentMinerConstants{
    public static Map<String, Object> getDocById(String docNo) throws IOException {

    	RestHighLevelClient restClient1= null;
        RestClient lowLevelRestClient1 =null;
        try {
        	lowLevelRestClient1 = ElasticSearchHelpers.buildRestClient();
        	restClient1 = new RestHighLevelClient(lowLevelRestClient1);
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | KeyStoreException | IOException e) {
			e.printStackTrace();
		}
        
        GetRequest request = new GetRequest(INDEX_NAME, PATENT_TYPE_NAME, docNo).version(2);
        GetResponse getResponse = restClient1.get(request);
        lowLevelRestClient1.close();
        return getResponse.getSource();
    }
    public static RestClient buildRestClient() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, KeyManagementException{

    
    KeyStore truststore = KeyStore.getInstance("jks");
    try (InputStream is = new FileInputStream("/etc/elasticsearch/truststore.jks")) {
        truststore.load(is, "changeit".toCharArray());
    }
    SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);
    final SSLContext sslContext = sslBuilder.build();
     
    	final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    	credentialsProvider.setCredentials(AuthScope.ANY,
    	        new UsernamePasswordCredentials("admin", "admin"));
    	
    	RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "https"), new HttpHost("localhost", 9201, "https"))
    	        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
    	            @Override
    	            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
    	                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setSSLContext(sslContext);
    	            }
    	        });
    	return builder.build();
    }
    @SuppressWarnings("resource")
	public static Client buildClient() {
    	TransportClient client = null;
    	 try {
			client = new PreBuiltTransportClient(Settings.EMPTY)
			        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ES_HOST), ES_PORT));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	        
        client.threadPool().getThreadContext().putHeader("Authorization", "Basic "+Base64.getEncoder().encode("admin:admin".getBytes(StandardCharsets.UTF_8)));
        
        return client;
    }
}
