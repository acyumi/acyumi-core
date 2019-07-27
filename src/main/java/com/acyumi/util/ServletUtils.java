package com.acyumi.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet工具类
 *
 * @author Mr.XiHui
 * @date 2019/7/26
 */
public abstract class ServletUtils {

    /**
     * 代理请求头名数组，用于获取经过代理的客户端ip.
     */
    private static final String[] PROXY_HEADER_NAMES = {
            "X-Forwarded-For",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP"
    };


    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是 <br>
     * 1、我们的服务器使用了nginx等反射代理工具 <br>
     * 2、可能用户使用了代理软件方式避免真实IP地址（如在nginx中不传递客户端ip） <br> <br>
     * <p>
     * 使用了nginx的时候对于server端来说，他接到的请求都是来自nginx服务器的， <br>
     * 此时server端默认获取到的ip则是nginx服务器的ip。 <br>
     * 这并不是我们想要的。这个时候就需要添加如下配置： <br> <br>
     * </p>
     * proxy_set_header X-Real-IP $remote_addr; <br>
     * proxy_set_header X-Real-Port $remote_port; <br>
     * proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; <br> <br>
     * <p>
     * 三个header分别表示： <br>
     * X-Real-IP            客户端ip <br>
     * X-Real-Port          客户端或上一级端口 <br>
     * X-Forwarded-For      包含了客户端和各级代理ip的完整ip链路 <br> <br>
     * </p>
     * 其中X-Real-IP是必需的，后两项选填。当只存在一级nginx代理的时候X-Real-IP和X-Forwarded-For是一致的， <br>
     * 而如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？ <br>
     * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。 <br>
     * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130, 192.168.1.100 <br>
     * 用户真实IP为： 192.168.1.110
     *
     * @param request Http请求对象
     * @return String ip
     */
    public static String getIpAddress(HttpServletRequest request) {

        String ip = null;
        for (int i = 0; i < PROXY_HEADER_NAMES.length; i++) {
            ip = request.getHeader(PROXY_HEADER_NAMES[i]);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                //根据网卡取本机配置的IP
                InetAddress inetAddress = InetAddress.getLoopbackAddress();
                ip = inetAddress.getHostAddress();
            }
        }

        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.split("\\.").length > 4) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip;
    }


    /**
     * 从request中获取请求头信息的Map.
     *
     * @param request Http请求对象
     * @return 请求头信息的Map
     */
    public Map<String, String> getRequestHeaderMap(HttpServletRequest request) {
        Map<String, String> requestHeaderMap = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String header = request.getHeader(headerName);
            requestHeaderMap.put(headerName, header);
        }
        return requestHeaderMap;
    }

    /**
     * 从response中获取响应头信息的Map.
     *
     * @param response http响应对象
     * @return 响应头信息的Map
     */
    public Map<String, String> getResponseHeaderMap(HttpServletResponse response) {
        Map<String, String> requestHeaderMap = new LinkedHashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName : headerNames) {
            String header = response.getHeader(headerName);
            requestHeaderMap.put(headerName, header);
        }
        return requestHeaderMap;
    }

}
