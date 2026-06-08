package com.blog.filter;

import com.blog.utils.XssUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XssFilter implements Filter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(httpRequest);
        chain.doFilter(wrapper, response);
    }

    static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private byte[] body;
        private boolean bodyRead = false;

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return XssUtil.clean(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = XssUtil.clean(values[i]);
            }
            return cleanValues;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> originalMap = super.getParameterMap();
            Map<String, String[]> cleanMap = new HashMap<>();
            for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
                String[] values = entry.getValue();
                String[] cleanValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleanValues[i] = XssUtil.clean(values[i]);
                }
                cleanMap.put(entry.getKey(), cleanValues);
            }
            return cleanMap;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (!bodyRead) {
                readAndCleanBody();
            }
            return new CachedServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (!bodyRead) {
                readAndCleanBody();
            }
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }

        private void readAndCleanBody() throws IOException {
            bodyRead = true;
            String contentType = getContentType();
            
            // 读取原始请求体
            InputStream inputStream = super.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            String originalBody = buffer.toString(StandardCharsets.UTF_8.name());
            
            // 如果是JSON请求，递归清理JSON内容
            if (contentType != null && contentType.contains("application/json") && !originalBody.isEmpty()) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(originalBody);
                    JsonNode cleanedNode = cleanJsonNode(jsonNode);
                    body = objectMapper.writeValueAsBytes(cleanedNode);
                } catch (JsonProcessingException e) {
                    // JSON解析失败，使用原始内容
                    body = originalBody.getBytes(StandardCharsets.UTF_8);
                }
            } else {
                body = originalBody.getBytes(StandardCharsets.UTF_8);
            }
        }

        private JsonNode cleanJsonNode(JsonNode node) {
            if (node == null || node.isNull()) {
                return node;
            }
            
            if (node.isTextual()) {
                // 对字符串值进行XSS清理
                return new TextNode(XssUtil.clean(node.asText()));
            }
            
            if (node.isObject()) {
                ObjectNode objectNode = objectMapper.createObjectNode();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    objectNode.set(field.getKey(), cleanJsonNode(field.getValue()));
                }
                return objectNode;
            }
            
            if (node.isArray()) {
                ArrayNode arrayNode = objectMapper.createArrayNode();
                for (JsonNode element : node) {
                    arrayNode.add(cleanJsonNode(element));
                }
                return arrayNode;
            }
            
            // 其他类型（数字、布尔等）直接返回
            return node;
        }
    }

    static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}
