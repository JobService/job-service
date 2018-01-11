/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.services.admin;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public class HttpServletResponseForTesting implements HttpServletResponse
{
    private byte[] content;

    /**
     * Method unused during testing.
     * @param cookie
     */
    @Override
    public void addCookie(Cookie cookie)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @return
     */
    @Override
    public boolean containsHeader(String name)
    {
        return false;
    }

    /**
     * Method unused during testing.
     * @param url
     * @return
     */
    @Override
    public String encodeURL(String url)
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @param url
     * @return
     */
    @Override
    public String encodeRedirectURL(String url)
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @param url
     * @return
     */
    @Override
    public String encodeUrl(String url)
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @param url
     * @return
     */
    @Override
    public String encodeRedirectUrl(String url)
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @param sc
     * @param msg
     * @throws IOException
     */
    @Override
    public void sendError(int sc, String msg) throws IOException
    {

    }

    /**
     * Method unused during testing.
     * @param sc
     * @throws IOException
     */
    @Override
    public void sendError(int sc) throws IOException
    {

    }

    /**
     * Method unused during testing.
     * @param location
     * @throws IOException
     */
    @Override
    public void sendRedirect(String location) throws IOException
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param date
     */
    @Override
    public void setDateHeader(String name, long date)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param date
     */
    @Override
    public void addDateHeader(String name, long date)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param value
     */
    @Override
    public void setHeader(String name, String value)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param value
     */
    @Override
    public void addHeader(String name, String value)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param value
     */
    @Override
    public void setIntHeader(String name, int value)
    {

    }

    /**
     * Method unused during testing.
     * @param name
     * @param value
     */
    @Override
    public void addIntHeader(String name, int value)
    {

    }

    /**
     * Method unused during testing.
     * @param sc
     */
    @Override
    public void setStatus(int sc)
    {

    }

    /**
     * Method unused during testing.
     * @param sc
     * @param sm
     */
    @Override
    public void setStatus(int sc, String sm)
    {

    }

    /**
     * Method unused during testing.
     * @return
     */
    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @return
     */
    @Override
    public String getContentType()
    {
        return null;
    }

    /**
     * Return the content.
     * @return
     */
    public byte[] getContent()
    {
        return content;
    }

    /**
     * Returns an instance of ServletOutputStream whose write(byte b[]) method has been overridden to save b[] as the
     * content which can then be retrieved with the getContent() method.
     * @return ServletOutputStream whose write(byte b[]) method saves the byte array as content.
     * @throws IOException
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return new ServletOutputStream()
        {

            @Override
            public void write(int b) throws IOException
            {

            }

            @Override
            public void write(byte b[]) throws IOException
            {
                content = b;
            }
        };
    }

    /**
     * Method unused during testing.
     * @return
     * @throws IOException
     */
    @Override
    public PrintWriter getWriter() throws IOException
    {
        return null;
    }

    /**
     * Method unused during testing.
     * @param charset
     */
    @Override
    public void setCharacterEncoding(String charset)
    {

    }

    /**
     * Method unused during testing.
     * @param len
     */
    @Override
    public void setContentLength(int len)
    {

    }

    /**
     * Method unused during testing.
     * @param type
     */
    @Override
    public void setContentType(String type)
    {

    }

    /**
     * Method unused during testing.
     * @param size
     */
    @Override
    public void setBufferSize(int size)
    {

    }

    /**
     * Method unused during testing.
     * @return
     */
    @Override
    public int getBufferSize()
    {
        return 0;
    }

    /**
     * Method unused during testing.
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException
    {

    }

    /**
     * Method unused during testing.
     */
    @Override
    public void resetBuffer()
    {

    }

    /**
     * Method unused during testing.
     * @return
     */
    @Override
    public boolean isCommitted()
    {
        return false;
    }

    /**
     * Method unused during testing.
     */
    @Override
    public void reset()
    {

    }

    /**
     * Method unused during testing.
     * @param loc
     */
    @Override
    public void setLocale(Locale loc)
    {

    }

    /**
     * Method unused during testing.
     * @return
     */
    @Override
    public Locale getLocale()
    {
        return null;
    }
}
