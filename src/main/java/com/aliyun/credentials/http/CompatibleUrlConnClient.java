package com.aliyun.credentials.http;

import com.aliyun.credentials.exception.CredentialException;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.*;
import java.util.Map.Entry;

import static com.aliyun.credentials.configure.Config.USER_AGENT_PREFIX;

public class CompatibleUrlConnClient implements Closeable {

    protected static final String ACCEPT_ENCODING = "Accept-Encoding";
    protected static final String CONTENT_TYPE = "Content-Type";
    protected static final String USER_AGENT = "User-Agent";
    private static final String DEFAULT_USER_AGENT;

    static {
        Properties sysProps = System.getProperties();
        String version = "";
        Properties props = new Properties();
        try {
            props.load(CompatibleUrlConnClient.class.getClassLoader().getResourceAsStream("version.properties"));
            version = props.getProperty("sdk.credentials.version");
        } catch (IOException e) {
            e.printStackTrace();
        }
        DEFAULT_USER_AGENT = String.format(USER_AGENT_PREFIX + " (%s; %s) Java/%s Credentials/%s TeaDSL/1", sysProps.getProperty("os.name"), sysProps
                .getProperty("os.arch"), sysProps.getProperty("java.runtime.version"), version);
    }

    public CompatibleUrlConnClient() {

    }

    public static HttpResponse compatibleGetResponse(HttpRequest request) {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        HttpResponse response = client.syncInvoke(request);
        client.close();
        return response;
    }

    public HttpResponse syncInvoke(HttpRequest request) {
        InputStream content = null;
        HttpResponse response = null;
        HttpURLConnection httpConn = buildHttpConnection(request);

        try {
            httpConn.connect();
            if (request.getHttpContent() != null) {
                DataOutputStream dos = new DataOutputStream(httpConn.getOutputStream());
                dos.write(request.getHttpContent());
                dos.flush();
                dos.close();
            }
            content = httpConn.getInputStream();
            response = new HttpResponse(httpConn.getURL().toString());
            parseHttpConn(response, httpConn, content, null);
            return response;
        } catch (IOException e) {
            content = httpConn.getErrorStream();
            response = new HttpResponse(httpConn.getURL().toString());
            parseHttpConn(response, httpConn, content, e);
            return response;
        } finally {
            if (content != null) {
                try {
                    content.close();
                } catch (IOException e) {
                    throw new CredentialException(e.getMessage(), e);
                }
            }
            httpConn.disconnect();
        }
    }


    private SSLSocketFactory createSSLSocketFactory(boolean ignoreSSLCert) {
        try {
            X509TrustManagerImp compositeX509TrustManager;
            if (ignoreSSLCert) {
                compositeX509TrustManager = new X509TrustManagerImp(true);
            } else {
                // get trustManager using default certification from jdk
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
                List<TrustManager> trustManagerList = new ArrayList<TrustManager>(Arrays.asList(tmf.getTrustManagers()));
                final List<X509TrustManager> finalTrustManagerList = new ArrayList<X509TrustManager>();
                for (TrustManager tm : trustManagerList) {
                    if (tm instanceof X509TrustManager) {
                        finalTrustManagerList.add((X509TrustManager) tm);
                    }
                }
                compositeX509TrustManager = new X509TrustManagerImp(finalTrustManagerList);
            }
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{compositeX509TrustManager},
                    new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    private HostnameVerifier createHostnameVerifier(boolean ignoreSSLCert) {
        return DefaultHostnameVerifier.getInstance(ignoreSSLCert);
    }

    public void checkHttpRequest(HttpRequest request) {
        String strUrl = request.getSysUrl();
        if (null == strUrl) {
            throw new IllegalArgumentException("URL is null for HttpRequest.");
        }
        if (null == request.getSysMethod()) {
            throw new IllegalArgumentException("Method is not set for HttpRequest.");
        }
    }


    public HttpURLConnection initHttpConnection(URL url, HttpRequest request) {
        try {
            HttpURLConnection httpConn = null;

            if ("https".equalsIgnoreCase(url.getProtocol())) {
                SSLSocketFactory sslSocketFactory = createSSLSocketFactory(false);
                HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
                httpsConn.setSSLSocketFactory(sslSocketFactory);
                HostnameVerifier hostnameVerifier = createHostnameVerifier(false);
                httpsConn.setHostnameVerifier(hostnameVerifier);
                httpConn = httpsConn;
            }

            if (httpConn == null) {
                httpConn = (HttpURLConnection) url.openConnection();
            }

            httpConn.setRequestMethod(request.getSysMethod().toString());
            httpConn.setInstanceFollowRedirects(false);
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.setUseCaches(false);
            setConnectionTimeout(httpConn, request);
            httpConn.setRequestProperty(ACCEPT_ENCODING, "identity");
            httpConn.setRequestProperty(USER_AGENT, DEFAULT_USER_AGENT);
            Map<String, String> mappedHeaders = request.getSysHeaders();
            for (Entry<String, String> entry : mappedHeaders.entrySet()) {
                httpConn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (request.getHttpContent() != null) {
                httpConn.setRequestProperty(CONTENT_TYPE, request.getSysHeaders().get(CONTENT_TYPE));
            }
            return httpConn;
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public void setConnectionTimeout(HttpURLConnection httpConn, HttpRequest request) {
        httpConn.setConnectTimeout(request.getSysConnectTimeout());
        httpConn.setReadTimeout(request.getSysReadTimeout());
    }

    public HttpURLConnection buildHttpConnection(HttpRequest request) {
        checkHttpRequest(request);
        String strUrl = request.getSysUrl();
        try {
            URL url = new URL(strUrl);

            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            HttpURLConnection httpConn = initHttpConnection(url, request);
            return httpConn;
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public void parseHttpConn(HttpResponse response, HttpURLConnection httpConn, InputStream content, Exception e) {
        byte[] buff;
        try {
            if (null != content) {
                buff = readContent(content);
            } else {
                response.setResponseMessage(e.getMessage());
                return;
            }
            response.setResponseCode(httpConn.getResponseCode());
            response.setResponseMessage(httpConn.getResponseMessage());
            Map<String, List<String>> headers = httpConn.getHeaderFields();
            for (Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (null == key) {
                    continue;
                }
                List<String> values = entry.getValue();
                StringBuilder builder = new StringBuilder(values.get(0));
                for (int i = 1; i < values.size(); i++) {
                    builder.append(",");
                    builder.append(values.get(i));
                }
                response.putHeaderParameter(key, builder.toString());
            }
            String type = response.getHeaderValue("Content-Type");
            if (null != buff && null != type) {
                response.setSysEncoding("UTF-8");
                String[] split = type.split(";");
                response.setHttpContentType(FormatType.mapAcceptToFormat(split[0].trim()));
                if (split.length > 1 && split[1].contains("=")) {
                    String[] codings = split[1].split("=");
                    response.setSysEncoding(codings[1].trim().toUpperCase());
                }
            }
            response.setHttpContent(buff, response.getSysEncoding(), response.getHttpContentType());
        } catch (Exception exception) {
            throw new CredentialException(exception.getMessage(), exception);
        }
    }

    public byte[] readContent(InputStream content) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        try {
            while (true) {
                final int read = content.read(buff);
                if (read == -1) {
                    break;
                }
                outputStream.write(buff, 0, read);
            }
        } catch (IOException e) {
            throw new CredentialException(e.getMessage(), e);
        }
        return outputStream.toByteArray();
    }

    @Override
    public void close() {
        // do nothing
    }


}
