package com.abobrinha.caixinha.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class WordPressConn {

    private final static String WORDPRESS_BASE_URL = "https://public-api.wordpress.com/rest/v1.1/sites/";
//    URL para testes de dados inválidos
//    private final static String WORDPRESS_BASE_URL = "http://google.com/ping?";
//    URL para testes de dados vazios
//    private final static String WORDPRESS_BASE_URL = "http://google.com/?";
    private final static String WORDPRESS_ABOBRINHA_ID = "113100833";
    private final static String WORDPRESS_POSTS = "posts";

    private final static String FIELDS_PARAM = "fields";
    private final static String CATEGORY_PARAM = "category";
    private final static String NUMBER_PARAM = "number";

    private final static String CATEGORY_VALUE = "historias-infantis-abobrinha";
    private final static int NUMBER_VALUE = 100;

    /**
     * Contrói a URL para consultar o WordPress API
     */
    private static URL buildUrl() {
        Uri builtUri = Uri.parse(WORDPRESS_BASE_URL).buildUpon()
                .appendPath(WORDPRESS_ABOBRINHA_ID)
                .appendPath(WORDPRESS_POSTS)
                .appendQueryParameter(FIELDS_PARAM, WordPressJson.getJsonHistoryFields())
                .appendQueryParameter(CATEGORY_PARAM, CATEGORY_VALUE)
                .appendQueryParameter(NUMBER_PARAM, Integer.toString(NUMBER_VALUE))
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * Retorna a string JSON com o resultado da consulta ao WordPress.
     */
    public static String getResponseFromAPI() throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) buildUrl().openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}