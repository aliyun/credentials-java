package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.MethodType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class ParameterHelper {
    private final static String TIME_ZONE = "UTC";
    private final static String FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private final static String SEPARATOR = "&";
    public static final String ENCODING = "UTF-8";
    private static final String ALGORITHM_NAME = "HmacSHA1";
    private static AtomicLong seqId = new AtomicLong(0);
    private static final long processStartTime = System.currentTimeMillis();

    public static String getUniqueNonce() {
        // thread id
        long threadId = Thread.currentThread().getId();
        // timestamp: ms
        long currentTime = System.currentTimeMillis();
        // sequence number
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long seq = seqId.getAndIncrement();
        long rand = random.nextLong();

        StringBuffer sb = new StringBuffer();
        sb.append(processStartTime).append('-')
                .append(threadId).append('-')
                .append(currentTime).append('-')
                .append(seq).append('-')
                .append(rand);
        try {
            // hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // hex
            byte[] msg = sb.toString().getBytes();
            sb.setLength(0);
            for (byte b : digest.digest(msg)) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() < 2) {
                    sb.append(0);
                }
                sb.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new CredentialException(e.getMessage(), e);
        }
        return sb.toString();
    }


    public static String getISO8601Time(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(FORMAT_ISO8601);
        df.setTimeZone(new SimpleTimeZone(0, TIME_ZONE));
        return df.format(date);
    }

    public static Date getUTCDate(String date) {
        SimpleDateFormat df = new SimpleDateFormat(FORMAT_ISO8601);
        df.setTimeZone(new SimpleTimeZone(0, TIME_ZONE));
        try {
            return df.parse(date);
        } catch (ParseException e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public static String getTimeString(long milliseconds) {
        Date date = new Date(milliseconds);
        SimpleDateFormat df = new SimpleDateFormat(FORMAT_ISO8601);
        return df.format(date);
    }

    public String composeStringToSign(MethodType method, Map<String, String> queries) {
        String[] sortedKeys = queries.keySet().toArray(new String[]{});
        Arrays.sort(sortedKeys);
        StringBuilder canonicalizedQueryString = new StringBuilder();

        for (String key : sortedKeys) {
            canonicalizedQueryString.append("&")
                    .append(AcsURLEncoder.percentEncode(key)).append("=")
                    .append(AcsURLEncoder.percentEncode(queries.get(key)));
        }
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(method.toString());
        stringToSign.append(SEPARATOR);
        stringToSign.append(AcsURLEncoder.percentEncode("/"));
        stringToSign.append(SEPARATOR);
        stringToSign.append(AcsURLEncoder.percentEncode(
                canonicalizedQueryString.toString().substring(1)));

        return stringToSign.toString();
    }

    public String signString(String stringToSign, String accessKeySecret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM_NAME);
            mac.init(new SecretKeySpec(accessKeySecret.getBytes(ENCODING), ALGORITHM_NAME));
            byte[] signData = mac.doFinal(stringToSign.getBytes(ENCODING));
            return Base64.encodeBase64String(signData);
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }

    }

    public String composeUrl(String endpoint, Map<String, String> queries, String protocol) {
        Map<String, String> mapQueries = queries;
        StringBuilder urlBuilder = new StringBuilder("");
        urlBuilder.append(protocol);
        urlBuilder.append("://").append(endpoint);
        urlBuilder.append("/?");
        StringBuilder builder = new StringBuilder("");
        for (Map.Entry<String, String> entry : mapQueries.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (val == null) {
                continue;
            }
            builder.append(AcsURLEncoder.encode(key));
            builder.append("=").append(AcsURLEncoder.encode(val));
            builder.append("&");
        }

        int strIndex = builder.length();
        builder.deleteCharAt(strIndex - 1);
        String query = builder.toString();
        return urlBuilder.append(query).toString();
    }
}
