package com.blog.utils;

import org.springframework.web.util.HtmlUtils;

public class XssUtil {
    
    public static String clean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return HtmlUtils.htmlEscape(value);
    }
}
