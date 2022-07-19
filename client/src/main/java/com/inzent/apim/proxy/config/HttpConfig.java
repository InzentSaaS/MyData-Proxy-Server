package com.inzent.apim.proxy.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.inzent.apim.proxy.util.MakeCertUtil;

@Component
public class HttpConfig {
	@Value("${server.ssl.trust-store-password}")
	private String trustStorePassword;

	@Value("${server.ssl.trust-store}")
	private Resource trustStore;

	@Value("${server.ssl.key-store-password}")
	private String keyStorePassword;

	@Value("${server.ssl.key-password}")
	private String keyPassword;

	@Value("${server.ssl.key-store}")
	private Resource keyStore;

	@Value("${proxy.ssl.client.validateCertificate:true}")
	private boolean validateCertificate;

	@Value("${proxy.ssl.client.tls-version:}")
	private String tlsVersion;

	@Value("${server.ssl.reloadShell:}")
	private String reloadShell;

	@Value("${proxy.header.filter:host,x-target-host}")
	private String filterHeaders;

	@Value("${proxy.ssl.client.connectTimeout:60}")
	private int connectTimeout = 1 * 60; // seconds (1 minutes)
	@Value("${proxy.ssl.client.connectionRequestTimeout:60}")
	private int connectionRequestTimeout = 1 * 60; // seconds (1 minutes)
	@Value("${proxy.ssl.client.socketTimeout:60}")
	private int socketTimeout = 1 * 60; // seconds (1 minutes)
	@Value("${proxy.httpclient.disableContentCompression:false}")
	private boolean disableContentCompression = false; // show data

	@Value("${proxy.httpclient.maxConnPerRoute:200}")
	private int maxConnPerRoute = 50; //
	@Value("${proxy.httpclient.maxConnTotal:50}")
	private int maxConnTotal = 200; //

	private int MAX_REDIRECT_ATTEMPTS = 3;

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

	public boolean isFilterHeader(String headerName) {
		return filterHeaders.contains(headerName);
	}

	@Bean
	public RestTemplate loadRestTemplate() throws Exception {
		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
//		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
//		if (CollectionUtils.isEmpty(interceptors)) {
//		    interceptors = new ArrayList<>();
//		}
//		interceptors.add(new LoggingInterceptor());
//		restTemplate.setInterceptors(interceptors);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			protected boolean hasError(HttpStatus statusCode) {
				return false;
			}
		});

		return restTemplate;
	}

	private ClientHttpRequestFactory clientHttpRequestFactory() throws Exception {
		return new HttpComponentsClientHttpRequestFactory(httpClient());
	}

	private HttpClient httpClient() throws Exception {
		// Load our keystore and truststore containing certificates that we trust.
		SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray())
				.loadKeyMaterial(keyStore.getURL(), keyStorePassword.toCharArray(), keyPassword.toCharArray()).build();

		// Create a trust manager that does not validate certificate chains
		if (!validateCertificate) {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };
			sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
		}

		SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslcontext,
				ObjectUtils.isEmpty(tlsVersion) ? null : tlsVersion.split(","), null, new NoopHostnameVerifier());

		RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout * 1000)
				.setConnectionRequestTimeout(connectionRequestTimeout * 1000).setSocketTimeout(socketTimeout * 1000)
				.build();

//		setMaxTotal(int max): Set the maximum number of total open connections.
//		setDefaultMaxPerRoute(int max): Set the maximum number of concurrent connections per route, which is 2 by default.
//		setMaxPerRoute(int max): Set the total number of concurrent connections to a specific route, which is 2 by default.
//		https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/apidocs/org/apache/http/impl/conn/PoolingHttpClientConnectionManager.html
		
		HttpClientBuilder customHttpClientBuilder = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory)
				.setMaxConnPerRoute(maxConnPerRoute).setMaxConnTotal(maxConnTotal).setDefaultRequestConfig(config)
//				.setConnectionReuseStrategy(new NoConnectionReuseStrategy())
//				.setConnectionManager(poolingConnManager)
				.setRetryHandler(new HttpRequestRetryHandler() {
					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount > MAX_REDIRECT_ATTEMPTS)
							return false;
						if (exception instanceof org.apache.http.NoHttpResponseException)
							return true;
						return false;
					}
				}).disableRedirectHandling();

		if (disableContentCompression)
			customHttpClientBuilder.disableContentCompression();

		return customHttpClientBuilder.build();
	}

	public void renewCA(String url) throws Exception {
		String host;
		int port;
		char[] passphrase;
		String[] c = url.split(":");
		host = c[0];
		port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
		passphrase = trustStorePassword.toCharArray();

		InputStream in = new FileInputStream(trustStore.getFile());
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(in, passphrase);
		in.close();

		// SSLContext context = SSLContext.getInstance("TLS");
		SSLContext context = SSLContexts.custom()
				.loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray())
				.loadKeyMaterial(keyStore.getURL(), keyStorePassword.toCharArray(), keyPassword.toCharArray()).build();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
		context.init(null, new TrustManager[] { tm }, null);
		SSLSocketFactory factory = context.getSocketFactory();

		logger.info("Opening connection to " + host + ":" + port + "...");
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(10000);
		try {
			logger.info("Starting SSL handshake...");
			socket.startHandshake();
			socket.close();
			logger.info("");
			logger.info("No errors, certificate is already trusted");
			return;
		} catch (SSLException e) {
			logger.info("");
			e.printStackTrace(System.out);
		}

		X509Certificate[] chain = tm.chain;
		if (chain == null) {
			logger.info("Could not obtain server certificate chain");
			return;
		}

		logger.info("");
		logger.info("Server sent " + chain.length + " certificate(s):");
		logger.info("");
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		for (int i = 0; i < chain.length; i++) {
			X509Certificate cert = chain[i];
			logger.info(" " + (i + 1) + " Subject " + cert.getSubjectDN());
			logger.info("   Issuer  " + cert.getIssuerDN());
			sha1.update(cert.getEncoded());
			logger.info("   sha1    " + toHexString(sha1.digest()));
			md5.update(cert.getEncoded());
			logger.info("   md5     " + toHexString(md5.digest()));
			logger.info("");
		}

		logger.info("Enter certificate to add to trusted keystore or 'q' to quit: [1]");

		X509Certificate cert = chain[0];
		String alias = host;
		ks.setCertificateEntry(alias, cert);

		OutputStream out = new FileOutputStream(trustStore.getFile());
		ks.store(out, passphrase);
		out.close();

		if (!StringUtils.isEmpty(reloadShell)) {
			File dir = trustStore.getFile().getParentFile();
			MakeCertUtil.createFile(cert, dir);
			MakeCertUtil.executeCmd(reloadShell, dir);
		}

		logger.info("");
		logger.info(cert.toString());
		logger.info("");
		logger.info("Added certificate to keystore '" + trustStore.getFilename() + "' using alias '" + alias + "'");
	}

	private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

	private static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes) {
			b &= 0xff;
			sb.append(HEXDIGITS[b >> 4]);
			sb.append(HEXDIGITS[b & 15]);
			sb.append(' ');
		}
		return sb.toString();
	}

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;

		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}
}
