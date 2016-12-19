package com.hpe.caf.jobservice.acceptance;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * This utility class provides helper methods for the job service caller tests.
 */
public final class JobServiceCallerTestsHelper {

    private JobServiceCallerTestsHelper() {
    }

    /**
     * Parses the input json string and returns the value of the passed in key.
     *
     * @param input - string to parse
     * @return JSONObject - JSONObject representation of input string
     * @throws ParseException
     */
    public static JSONObject parseJson(final String input) throws ParseException {
        if (input == null) {
            return null;
        }
        final JSONParser parser = new JSONParser();
        final Object obj = parser.parse(input);
        return (JSONObject) obj;
    }

    /**
     * Send POST URL request.
     *
     * @param targetURL - target URL
     * @param input - JSON contents to send in POST body
     * @param contentType - MIME type of the body of the request
     * @param acceptEncoding - list of acceptable encodings
     * @return String - http response
     * @throws IOException
     */
    public static String sendPOST(String targetURL, String input, String contentType, String acceptEncoding) throws IOException {

        StringBuilder sb = new StringBuilder();
        String line;

        URL url;
        HttpURLConnection urlConnection = null;
        try {

            url = new URL(targetURL);

            //  Route through proxy if debugging enabled.
            if (isDebuggingEnabled()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888));
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", contentType);
            urlConnection.setRequestProperty("Accept-Encoding", acceptEncoding);

            urlConnection.setDoOutput(true);

            //  Send request.
            DataOutputStream wr = new DataOutputStream (
                    urlConnection.getOutputStream ());
            wr.writeBytes (input);
            wr.flush ();
            wr.close ();

            //  Get response.
            int httpResult = urlConnection.getResponseCode();

            if(httpResult == HttpURLConnection.HTTP_OK || httpResult == HttpURLConnection.HTTP_CREATED || httpResult == HttpURLConnection.HTTP_NO_CONTENT) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (urlConnection.getInputStream())));

                sb = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            } else {
                throw new RuntimeException("Failed : HTTP error code : "
                        + urlConnection.getResponseCode());
            }

        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        return sb.toString();
    }

    /**
     * Send GET URL request.
     *
     * @param targetURL - target URL
     * @return String - http response
     * @throws IOException
     */
    public static String sendGET(String targetURL) throws IOException {

        StringBuilder sb = new StringBuilder();
        String line;

        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(targetURL);

            //  Route through proxy if debugging enabled.
            if (isDebuggingEnabled()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888));
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }

            urlConnection.setRequestMethod("GET");

            //  Get response.
            int httpResult = urlConnection.getResponseCode();

            if(httpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (urlConnection.getInputStream())));

                sb = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            } else {
                throw new RuntimeException("Failed : HTTP error code : "
                        + urlConnection.getResponseCode());
            }

        } finally {
            if (urlConnection != null) {
                try {
                    urlConnection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        return sb.toString();
    }

    /**
     * Gets the container name of the job definition data container.
     *
     * @param containerJSON - container JSON needed to create the container
     * @param dockerContainersURL - docker containers URL needed to create the container and access its name.
     * @return String - name of the container
     * @throws IOException, ParseException
     */
    public static String getJobDefinitionContainerName(String containerJSON, String dockerContainersURL) throws IOException, ParseException {

        //  Append "create" to url in order to send request to create the container
        String createContainerURL = dockerContainersURL + "create";
        String sendCreateContainerPostResponse = sendPOST(createContainerURL,containerJSON,"application/json", "gzip");

        // Get container id from response object.
        JSONObject createContainerResponse = parseJson(sendCreateContainerPostResponse);
        String id = (String) createContainerResponse.get("Id");

        //  Use identifier to build up a URL that can be used to return container metadata.
        String getContainerMetadataURL = dockerContainersURL + id + "/json";
        String sendGetContainerMetadataPostResponse = sendGET(getContainerMetadataURL);

        JSONObject getContainerMetadataResponse = parseJson(sendGetContainerMetadataPostResponse);
        String name = (String) getContainerMetadataResponse.get("Name");

        //  Remove first character from container name (i.e. '/').
        return removeFirst(name);
    }

    /**
     * Returns the JSON content for the specified file.
     *
     * @param filename - name of file
     * @return String - JSON content
     */
    public static String getJSONFromFile(String filename) throws FileNotFoundException {

        StringBuilder result = new StringBuilder("");

        //  Read JSON file from resources folder.
        ClassLoader classLoader = JobServiceCallerTestsHelper.class.getClassLoader();

        if (filename != null) {
            File file = new File(classLoader.getResource(filename).getFile());

            Assert.assertNotNull(file);
            Assert.assertTrue(file.exists());

            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
            scanner.close();
        }

        return result.toString();
    }

    private static String removeFirst(String s){
        return s.substring(1);
    }

    private static Boolean isDebuggingEnabled() {
        Boolean isEnabled = false;

        String isDebuggingEnabled = System.getenv("DEBUG_HTTP_PROXY");
        if (isDebuggingEnabled != null) {
            if (!isDebuggingEnabled.isEmpty() && (isDebuggingEnabled.equals("1") || isDebuggingEnabled.equals("true") || isDebuggingEnabled.equals("TRUE"))) {
                isEnabled =  true;
            }
        }

        return isEnabled;
    }

}
