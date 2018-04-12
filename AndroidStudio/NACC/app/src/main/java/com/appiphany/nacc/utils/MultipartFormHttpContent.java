package com.appiphany.nacc.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.http.AbstractHttpContent;

public class MultipartFormHttpContent extends AbstractHttpContent {
    private String boundary = "*****";
    private String postHeader = "--";
    private static final String CONTENT_TYPE = "multipart/form-data; boundary=*****";
    private Map<String, Object> paramMaps = new HashMap<String, Object>();
    private long mContentLength = 0;

    @Override
    public long getLength() throws IOException {
        return mContentLength;
    }

    public MultipartFormHttpContent() {
        super(CONTENT_TYPE);
        mContentLength = 11;
    }

    public void addParam(String key, String value) {
        if (key != null && value != null) {
            String encodedValue;
            try {
                encodedValue = URLEncoder.encode(value, "utf-8");
                paramMaps.put(key, encodedValue);
                mContentLength += 9 + 39 + 4 + key.length() + encodedValue.length() + 2;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

    }

    public void addParamWithoutEncode(String key, String value) {
        paramMaps.put(key, value);
        mContentLength += 9 + 39 + 4 + key.length() + value.length() + 2;
    }

    public void addParam(String key, String filename, String fileType, File fileData) {
        paramMaps.put(key, new FileBody(filename, fileType, fileData));
        if (fileData != null) {
            mContentLength += 9 + 39 + key.length() + 13 + filename.length() + 2 + 14 + fileType.length() + 4
                    + fileData.length() + 2;
        }
    }

    public Map<String, Object> getParamMaps() {
        return paramMaps;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, getCharset()));

        for (String key : paramMaps.keySet()) {

            Object value = paramMaps.get(key);
            if (value != null) {
                writer.write(postHeader);
                writer.write(boundary);
                writer.write("\r\n");
                if (value instanceof FileBody) {
                    FileBody fileBody = (FileBody) value;
                    writer.write("Content-Disposition: form-data; name=\"" + key + "\"; filename=\""
                            + fileBody.getFilename() + "\"\r\n");
                    writer.write("Content-Type: " + fileBody.getFileType());
                    writer.write("\r\n");
                    writer.write("\r\n");
                    writer.flush();

                    final InputStream in = new FileInputStream(fileBody.getData());
                    try {
                        byte[] tmp = new byte[4096];
                        int l;
                        while ((l = in.read(tmp)) != -1) {
                            out.write(tmp, 0, l);
                        }
                        out.flush();
                    } finally {
                        in.close();
                    }
                } else {
                    writer.write("Content-Disposition: form-data; name=\"" + key + "\"");
                    writer.write("\r\n");
                    writer.write("\r\n");
                    writer.flush();
                    writer.write(value.toString());

                }
                writer.write("\r\n");
            }
        }
        writer.write(postHeader);
        writer.write(boundary);
        writer.write(postHeader + "\r\n");
        writer.flush();
    }

    private class FileBody {
        private String mFilename;
        private String mFileType;
        private File mData;

        public FileBody(String filename, String fileType, File data) {
            setFilename(filename);
            setFileType(fileType);
            setData(data);
        }

        public String getFilename() {
            return mFilename;
        }

        public void setFilename(String filename) {
            mFilename = filename;
        }

        public String getFileType() {
            return mFileType;
        }

        public void setFileType(String fileType) {
            mFileType = fileType;
        }

        public File getData() {
            return mData;
        }

        public void setData(File data) {
            mData = data;
        }
    }

}
