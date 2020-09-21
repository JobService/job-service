/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.jobservice.acceptance;

import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * This utility class provides helper methods for the job service caller tests.
 */
public final class JobServiceCallerTestsHelper {

    private static final Logger LOG = LoggerFactory.getLogger(JobServiceCallerTestsHelper.class);

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
     * Parses the input json string and returns the value of the passed in key.
     *
     * @param input - string to parse
     * @return JSONArray - JSONArray representation of input string
     * @throws ParseException
     */
    public static JSONArray parseJsonArray(final String input) throws ParseException {
        if (input == null) {
            return null;
        }
        final JSONParser parser = new JSONParser();
        final Object obj = parser.parse(input);
        return (JSONArray) obj;
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

        LOG.debug("sendPOST: targetURL {}, input {}", targetURL, input);

        StringBuilder sb = new StringBuilder();
        String line;

        URL url;
        HttpURLConnection urlConnection = null;
        try {

            url = new URL(targetURL);

            //  Route through proxy if debugging enabled.
            if (isDebuggingEnabled()) {
                Proxy proxy = getProxy();
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
                        + urlConnection.getResponseCode()
                        + " Error stream : "
                        + IOUtils.toString(urlConnection.getErrorStream(), StandardCharsets.UTF_8)
                        + " Url : "
                        + urlConnection.getURL().toString());
            }

        } catch (Exception e) {
            LOG.error("Error while sending POST request {}. ", e);
            throw e;
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

        LOG.debug("sendGET: targetURL {}", targetURL);

        StringBuilder sb = new StringBuilder();
        String line;

        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(targetURL);

            //  Route through proxy if debugging enabled.
            if (isDebuggingEnabled()) {
                Proxy proxy = getProxy();
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
                        + urlConnection.getResponseCode()
                        + " Error stream : "
                        + IOUtils.toString(urlConnection.getErrorStream(), StandardCharsets.UTF_8)
                        + " Url : "
                        + urlConnection.getURL().toString());
            }

        } catch (Exception e) {
            LOG.error("Error while sending GET request {}. ", e);
            throw e;
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
        LOG.debug("getJobDefinitionContainerName: containerJSON {}, dockerContainersURL {}", containerJSON, dockerContainersURL);

        //  Append "create" to url in order to send request to create the container
        LOG.debug("getJobDefinitionContainerName: Creating container ...");
        String createContainerURL = dockerContainersURL + "create";
        String sendCreateContainerPostResponse = sendPOST(createContainerURL,containerJSON,"application/json", "gzip");

        // Get container id from response object.
        LOG.debug("getJobDefinitionContainerName: Get container id from response ...");
        JSONObject createContainerResponse = parseJson(sendCreateContainerPostResponse);
        String id = (String) createContainerResponse.get("Id");

        //  Use identifier to build up a URL that can be used to return container metadata.
        LOG.debug("getJobDefinitionContainerName: Get container metadata ...");
        String getContainerMetadataURL = dockerContainersURL + id + "/json";
        String sendGetContainerMetadataPostResponse = sendGET(getContainerMetadataURL);

        LOG.debug("getJobDefinitionContainerName: Get container name ...");
        JSONObject getContainerMetadataResponse = parseJson(sendGetContainerMetadataPostResponse);
        String name = (String) getContainerMetadataResponse.get("Name");

        //  Remove first character from container name (i.e. '/').
        return removeFirst(name);
    }

    /**
     * Gets the container link name for job web service container.
     *
     * @param jobServiceImageName - job web service image name.
     * @param jobServiceAdminPort - job web service admin port.
     * @param dockerContainersURL - docker containers URL needed to create the container and access its name.
     * @return String - link name for the job web service container
     * @throws IOException, ParseException
     */
    public static String getJobServiceContainerLinkName(String jobServiceImageName, String jobServiceAdminPort, String dockerContainersURL) throws IOException, ParseException {

        LOG.debug("getJobServiceContainerLinkName: jobServiceImageName {}, jobServiceAdminPort {}, dockerContainersURL {}", jobServiceImageName, jobServiceAdminPort, dockerContainersURL);

        String jobServiceContainerName = "";
        String getContainersMetadataURL = dockerContainersURL + "/json";

        //  Get a list of running containers.
        LOG.debug("getJobDefinitionContainerName: Retrieve a list of running containers ...");
        String sendGetContainersMetadataPostResponse = sendGET(getContainersMetadataURL);
        JSONArray getContainersMetadataResponse = parseJsonArray(sendGetContainersMetadataPostResponse);

        //  Iterate through each running container and identify the job web service image we need matching on the admin port it is using.
        LOG.debug("getJobDefinitionContainerName: Identify job web service image matching on admin port ...");
        for (Object objContainer: getContainersMetadataResponse) {
            JSONObject container = (JSONObject)objContainer;
            String image = (String)container.get("Image");

            //  Only match on the specified job web service image.
            if (image.equalsIgnoreCase(jobServiceImageName)) {
                JSONArray ports = (JSONArray)container.get("Ports");

                //  Iterate through ports and match on the specified admin port.
                for (Object objPort: ports) {
                    JSONObject port = (JSONObject)objPort;

                    final Long publicPort = (Long) port.get("PublicPort");
                    if (publicPort != null) {
                        String portValue = Long.toString(publicPort);
                        if (portValue.equals(jobServiceAdminPort)) {
                            LOG.debug("getJobDefinitionContainerName: Found matching job web service image ...");

                            //  Match found. Use the container Names section to generate a link name.
                            List<String> names = (List<String>) container.get("Names");

                            //  Remove first character from container name (i.e. '/').
                            jobServiceContainerName = removeFirst(names.get(0)) + ":job-service";
                            break;
                        }
                    }
                }
            }

            if (!jobServiceContainerName.isEmpty()){
                break;
            }
        }

        //  Return link name.
        return jobServiceContainerName;
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

    private static Proxy getProxy() {
        String proxyHostName;
        if (!Strings.isNullOrEmpty(System.getenv("DEBUG_HTTP_PROXY_HOST"))) {
            proxyHostName = System.getenv("DEBUG_HTTP_PROXY_HOST");
        } else {
            proxyHostName = "localhost";
        }

        LOG.debug("getProxy: proxyHostName {}", proxyHostName);

        int proxyPort;
        if (!Strings.isNullOrEmpty(System.getenv("DEBUG_HTTP_PROXY_PORT"))) {
            proxyPort = Integer.parseInt(System.getenv("DEBUG_HTTP_PROXY_PORT"));
        } else {
            proxyPort = 8888;
        }

        LOG.debug("getProxy: proxyPort {}", proxyPort);

        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostName, proxyPort));
    }
}
