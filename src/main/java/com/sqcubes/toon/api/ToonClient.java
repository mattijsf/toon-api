package com.sqcubes.toon.api;

import com.google.gson.Gson;
import com.sqcubes.toon.api.exception.ToonAuthenticationFailedException;
import com.sqcubes.toon.api.exception.ToonLoginFailedException;
import com.sqcubes.toon.api.exception.ToonNotAuthenticatedException;
import com.sqcubes.toon.api.model.ToonLoginResponse;
import com.sqcubes.toon.api.model.ToonResponse;
import com.sqcubes.toon.api.model.ToonSchemeState;
import com.sqcubes.toon.api.persistence.ToonMemoryPersistenceHandler;
import com.sqcubes.toon.api.persistence.ToonPersistenceHandler;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class ToonClient {
    public final static int MIN_TEMPERATURE = 6;
    public final static int MAX_TEMPERATURE = 30;
    public final static Float[] SUPPORTED_TEMPERATURES;

    /* Toon Program-states */
    private final static int PROGRAM_MANUAL = 0;

    /* API URI / Paths */
    private static final String DEFAULT_BASE_STRING = "https://toonopafstand.eneco.nl";
    private static final URL DEFAULT_BASE_URL;
    private static final String LOGIN_PATH = "/toonMobileBackendWeb/client/login";
    private final URI LOGIN_URI;
    private static final String AGREEMENT_PATH = "/toonMobileBackendWeb/client/auth/start";
    private final URI AGREEMENT_URI;
    private static final String TEMP_SET_PATH = "/toonMobileBackendWeb/client/auth/setPoint";
    private final URI TEMP_SET_URI;
    private static final String CHANGE_SCHEME_STATE_PATH = "/toonMobileBackendWeb/client/auth/schemeState";
    private final URI CHANGE_SCHEME_STATE_URI;
    //    private static final String LOGIN_CHECK_PATH = "/toonMobileBackendWeb/client/checkIfLoggedIn";
    //    private final URI LOGIN_CHECK_URI;
    //    private static final String UPDATE_PATH = "/toonMobileBackendWeb/client/auth/retrieveToonState";
    //    private static final String LOGOUT_PATH = "/toonMobileBackendWeb/client/auth/logout";

    /* Persistence keys */
    protected static final String PERSISTENCE_KEY_USERNAME = "username";
    protected static final String PERSISTENCE_KEY_PASSWORD_HASH = "hashedPassword";
    protected static final String PERSISTENCE_KEY_CLIENT_ID = "clientId";
    protected static final String PERSISTENCE_KEY_CLIENT_ID_CHECKSUM = "clientIdChecksum";

    private final Gson gson = new Gson();
    private final HttpClient client;
    private final ToonPersistenceHandler persistenceHandler;

    private HttpHost proxy;

    static {
        URL defaultEndpointUrl;
        try {
            defaultEndpointUrl = new URL(DEFAULT_BASE_STRING);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        DEFAULT_BASE_URL = defaultEndpointUrl;

        // load supported temperatures
        List<Float> temperatures = new ArrayList<Float>();
        float temperature = MIN_TEMPERATURE;
        while(temperature <= MAX_TEMPERATURE){
            temperatures.add(temperature);
            temperature += 0.5;
        }


        SUPPORTED_TEMPERATURES = temperatures.toArray(new Float[temperatures.size()]);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ToonClient(@NotNull HttpClient client) {
        this(client, new ToonMemoryPersistenceHandler());
    }

    public ToonClient(@NotNull HttpClient client,@NotNull URL baseUrl) {
        this(client, new ToonMemoryPersistenceHandler(), baseUrl);
    }

    public ToonClient(@NotNull HttpClient client, @NotNull ToonPersistenceHandler persistenceHandler){
        this(client, persistenceHandler, DEFAULT_BASE_URL);
    }

    public ToonClient(@NotNull HttpClient client, @NotNull ToonPersistenceHandler persistenceHandler, @NotNull URL baseUrl) {
        this.client = client;
        this.persistenceHandler = persistenceHandler;
        try {
            LOGIN_URI = new URL(baseUrl, LOGIN_PATH).toURI();
            AGREEMENT_URI = new URL(baseUrl, AGREEMENT_PATH).toURI();
            TEMP_SET_URI = new URL(baseUrl, TEMP_SET_PATH).toURI();
            CHANGE_SCHEME_STATE_URI = new URL(baseUrl, CHANGE_SCHEME_STATE_PATH).toURI();


        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean hasCredentials(){
        try {
            assertCredentials();
            return true;
        } catch (ToonNotAuthenticatedException e) {
            return false;
        }
    }

    public boolean authenticate(@NotNull String username, @NotNull String password) throws ToonAuthenticationFailedException {
        try {
            return login(username, password, false);
        } catch (ToonLoginFailedException e) {
            throw new ToonAuthenticationFailedException(e);
        }
    }

    public boolean setSchemeState(ToonSchemeState state) throws ToonLoginFailedException {
        assertCredentials();

        try{
                Map<String, String> params = new HashMap<String, String>(randomParam());
                params.putAll(authenticationParams());
                params.put("state", String.valueOf(PROGRAM_MANUAL));
                params.put("temperatureState", String.valueOf(state.schemeStateCode()));

                ToonResponse response = get(CHANGE_SCHEME_STATE_URI, params, ToonResponse.class);
                return isResponseSuccess(response);
        }
        catch (ToonUnauthorizedException e) {
            if (login()){
                return setSchemeState(state);
            }
            else{
                throw new ToonLoginFailedException(e);
            }
        }
    }

    public boolean setTemperature(@NotNull float temperatureInCelsius) throws ToonLoginFailedException {
        assertCredentials();

        try {

            if (temperatureInCelsius >= 6 && temperatureInCelsius <= 30) {

                Map<String, String> params = new HashMap<String, String>();
                params.putAll(authenticationParams());
                params.putAll(randomParam());
                params.put("value", String.valueOf((int)(temperatureInCelsius * 100)));

                ToonResponse response = get(TEMP_SET_URI, params, ToonResponse.class);
                return isResponseSuccess(response);
            }
        } catch (ToonUnauthorizedException e) {
            if (login()){
                return setTemperature(temperatureInCelsius);
            }
            else{
                throw new ToonLoginFailedException(e);
            }
        }
        return false;
    }

    private boolean login() throws ToonLoginFailedException {
        String username = persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_USERNAME);
        String passwordHash = persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_PASSWORD_HASH);

        assertCredentials(username, passwordHash);

        if (!login(username, passwordHash, true))
            throw new ToonLoginFailedException("Login attempt did not succeed for an unknown reason.");

        return true;
    }

    private boolean login(String username, String password, boolean hashed) throws ToonLoginFailedException {
        try{
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", username);
            params.put(hashed ? "hashedPassword" : "password", password);

            ToonLoginResponse loginResponse = post(LOGIN_URI, params, ToonLoginResponse.class);

            if (!isResponseSuccess(loginResponse)){
                throw new ToonLoginFailedException("Initial login attempt failed, see response.", loginResponse);
            }

            if (isEmpty(loginResponse.getClientId(), loginResponse.getClientIdChecksum(), loginResponse.getPasswordHash())){
                throw new ToonLoginFailedException("Initial login attempt failed, expected information was not provided.");
            }

            if (!confirmLoginAgreement(loginResponse)) {
                throw new ToonLoginFailedException("Initial login attempt succeeded but confirming the agreement failed.");
            }

            persistenceHandler.persistKeyValuePair(PERSISTENCE_KEY_USERNAME, username);
            persistenceHandler.persistKeyValuePair(PERSISTENCE_KEY_PASSWORD_HASH, loginResponse.getPasswordHash());
            persistenceHandler.persistKeyValuePair(PERSISTENCE_KEY_CLIENT_ID, loginResponse.getClientId());
            persistenceHandler.persistKeyValuePair(PERSISTENCE_KEY_CLIENT_ID_CHECKSUM, loginResponse.getClientIdChecksum());
            return true;

        } catch (ToonUnauthorizedException e) {
            throw new ToonLoginFailedException("Re-login was requested during the login procedure itself. This is unexpected and indicates an error during the login attempt", e);
        }
    }

    private boolean confirmLoginAgreement(ToonLoginResponse login) throws ToonUnauthorizedException {
        Map<String, String> params = new HashMap<String, String>(randomParam());
        params.put("clientId", login.getClientId());
        params.put("clientIdChecksum", login.getClientIdChecksum());
        params.put("agreementId", login.getAgreements().get(0).getAgreementId());
        params.put("agreementIdChecksum", login.getAgreements().get(0).getAgreementIdChecksum());

        ToonResponse response = get(AGREEMENT_URI, params, ToonResponse.class);
        return isResponseSuccess(response);
    }

    private void assertCredentials() throws ToonNotAuthenticatedException {
        String username = persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_USERNAME);
        String passwordHash = persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_PASSWORD_HASH);
        assertCredentials(username, passwordHash);
    }
    private void assertCredentials(String username, String passwordHash) throws ToonNotAuthenticatedException {
        if (isEmpty(username, passwordHash))
            throw new ToonNotAuthenticatedException("Missing username and/or password(hash). Did you forgot to authenticate()?");
    }

    private <T extends ToonResponse> T post(URI uri, Map<String, String> params, Class<T> classOfT) throws ToonUnauthorizedException {
        UrlEncodedFormEntity formEntity = formEntityFromParameters(params);

        HttpPost method = new HttpPost(uri);
        method.setEntity(formEntity);
        return executeForObject(method, classOfT);
    }

    private <T extends ToonResponse> T get(URI uri, Map<String, String> queryParams, Class<T> classOfT) throws ToonUnauthorizedException {
        uri = appendParametersToURI(uri, queryParams);

        HttpGet method = new HttpGet(uri);
        return executeForObject(method, classOfT);
    }

    private <T extends ToonResponse> T executeForObject(HttpRequestBase method, Class<T> classOfT) throws ToonUnauthorizedException {
        try {
            applyRequestProxy(method);

            HttpResponse response = client.execute(method);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200){
                Reader reader = new InputStreamReader(entity.getContent());
                return gson.fromJson(reader, classOfT);
            }
            else if (response.getStatusLine().getStatusCode() == 500){
                T errorResponse = tryExtractDefunctHtmlAsJson(classOfT, entity);
                if (!errorResponse.getSuccess()){
                    // Might have to check response message content to verify it is an ACTUAL failed login???
                    throw new ToonUnauthorizedException();
                }
                else{
                    throw new IllegalStateException("Got a success acknowledgement while statusCode 500 was received");
                }
            }
            else{
                throw new IllegalStateException("Unhandled statusCode " + response.getStatusLine().getStatusCode());
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error while performing request to " + method.getURI(), e);
        }
    }

    private <T> T tryExtractDefunctHtmlAsJson(Class<T> classOfT, HttpEntity entity) throws IOException {
        String responseString = EntityUtils.toString(entity, "UTF-8");
        String html = StringEscapeUtils.unescapeHtml(responseString);
        if (!isEmpty(html)){
            int openBracket = html.indexOf('{');
            int closeBracket = html.lastIndexOf('}');
            if (closeBracket > openBracket){
                String json = html.substring(openBracket, closeBracket + 1);
                return gson.fromJson(json, classOfT);
            }
        }

        throw new IllegalStateException("Received error from server: " + responseString);
    }

    private void applyRequestProxy(HttpRequestBase method) {
        if (proxy != null){
            RequestConfig.Builder requestConfigBuilder = method.getConfig() != null
                            ? RequestConfig.copy(method.getConfig())
                            : RequestConfig.custom();

            RequestConfig proxyConfig = requestConfigBuilder.setProxy(proxy).build();
            method.setConfig(proxyConfig);
        }
    }

    private UrlEncodedFormEntity formEntityFromParameters(Map<String, String> params){
        List<NameValuePair> valuePairs = parametersAsNameValuePair(params);

        UrlEncodedFormEntity urlEncodedFormEntity = null;
        try {
            urlEncodedFormEntity = new UrlEncodedFormEntity(valuePairs, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
        }

        return urlEncodedFormEntity;
    }

    private List<NameValuePair> parametersAsNameValuePair(Map<String, String> params) {
        List<NameValuePair> valuePairs = new ArrayList<NameValuePair>();
        for (String key : params.keySet()) {
            valuePairs.add(new BasicNameValuePair(key, params.get(key)));
        }
        return valuePairs;
    }

    private Map<String, String> authenticationParams(){
        Map<String, String> params = new HashMap<String, String>(2);
        params.put("clientId", persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_CLIENT_ID));
        params.put("clientIdChecksum", persistenceHandler.getPersistedKeyValue(PERSISTENCE_KEY_CLIENT_ID_CHECKSUM));
        return params;
    }

    private Map<String, String> randomParam() {
        Map<String, String> params = new HashMap<String, String>(2);
        params.put("random", UUID.randomUUID().toString());
        return params;
    }

    private URI appendParametersToURI(URI uri, Map<String, String> params) {
        if (params == null){
            return uri;
        }

        List<NameValuePair> valuePairs = parametersAsNameValuePair(params);
        List<NameValuePair> existingValuePairs = URLEncodedUtils.parse(uri, "UTF-8");
        List<NameValuePair> combinedValuePairs = new ArrayList<NameValuePair>();
        combinedValuePairs.addAll(existingValuePairs);
        combinedValuePairs.addAll(valuePairs);

        String query = URLEncodedUtils.format(combinedValuePairs, "UTF-8");

        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query, uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    boolean isResponseSuccess(ToonResponse response){
        return response != null
                && response.getSuccess();
    }

    private boolean isEmpty(String... str) {
        for (String s : str) {
            if (s == null || s.isEmpty())
                    return true;
        }

        return false;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setProxy(HttpHost proxy) {
        this.proxy = proxy;
    }

//    public boolean checkLogin() throws ToonLoginRequiredException {
//        Map<String, String> params = new HashMap<String, String>(randomParam());
//        params.putAll(authenticationParams());
//
//        LoginCheckResponse response = get(LOGIN_CHECK_URI, params, LoginCheckResponse.class);
//        return isResponseSuccess(response) && isTrue(response.getLoggedIn());
//    }

    private class ToonUnauthorizedException extends Exception {
    }
}
