import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * 演示用「运维诊断」服务：根据传入的 host 执行 ping，并将命令输出回显到页面。
 * 故意未做输入校验，用于演示：1) 前端可见注入结果 2) RASP 可钩子 Runtime.exec 检测/阻断。
 */
public class DiagnosticsServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/internal-links", new InternalLinksHandler());
        server.createContext("/api/smtp-test", new SmtpTestHandler());
        server.createContext("/api/memory-shell", new MemoryShellHandler());
        server.createContext("/api/xss-demo", new XssDemoHandler());
        server.createContext("/api/fetch-url", new SsrfDemoHandler());
        server.createContext("/api/deserialization-import", new DeserializationDemoHandler());
        server.createContext("/api/jni-load", new JniLoadHandler());
        server.createContext("/", new RootHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Diagnostics server on http://0.0.0.0:" + PORT);
    }

    /** 模拟 OA 接口信息泄露：返回本应仅管理员可见的内部链接，供「攻击路径」演示使用 */
    static class InternalLinksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            String json = "[{\"name\":\"发件服务器测试\",\"url\":\"#/diagnostic\",\"desc\":\"邮件/通知设置\"},"
                    + "{\"name\":\"日志下载\",\"url\":\"#/logs\",\"desc\":\"仅管理员\"},"
                    + "{\"name\":\"审批导入\",\"url\":\"#/attack/deserialization\",\"desc\":\"模板导入\"},"
                    + "{\"name\":\"插件中心\",\"url\":\"#/attack/jni-load\",\"desc\":\"本地加速库\"},"
                    + "{\"name\":\"站内搜索\",\"url\":\"#/attack/xss\",\"desc\":\"意见/搜索反馈\"},"
                    + "{\"name\":\"资源预览\",\"url\":\"#/attack/ssrf\",\"desc\":\"URL 预览\"}]";
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            send(ex, 200, json);
        }
    }

    /**
     * 反序列化 RCE 演示：后端直接对用户可控数据执行 ObjectInputStream.readObject()，
     * readObject 链路中触发 Runtime.exec 并将执行结果回传（与内存马演示逻辑一致）。
     */
    static class DeserializationDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String cmd = "id";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "cmd".equals(pair.substring(0, eq))) {
                        cmd = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            System.out.println("[deserialization-import] exec: " + cmd);
            Process proc = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            String execOut = readProcessOutput(proc);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 200, execOut);
        }
    }

    /**
     * JNI 加载演示：插件管理接口可控加载本地 .so/.dll 路径，触发 System.load。
     */
    static class JniLoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String lib = "/tmp/oa-plugins/liboa-safe.so";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "lib".equals(pair.substring(0, eq))) {
                        lib = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            StringBuilder out = new StringBuilder();
            out.append("[jni-loader] try load: ").append(lib).append("\n");
            try {
                // 故意对外暴露可控路径，若攻击者可上传恶意 so/dll，可在加载时执行 native 代码。
                System.load(lib);
                out.append("JNI 库加载成功。\n");
            } catch (Throwable t) {
                out.append("JNI 库加载失败: ").append(t.getClass().getSimpleName()).append(" - ").append(t.getMessage()).append("\n");
                out.append("风险说明: 只要加载到了攻击者控制的本地库，native 代码会在 JVM 进程权限下执行。\n");
            }
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 200, out.toString());
        }
    }


    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            Path index = Paths.get("index.html");
            byte[] bytes;
            if (Files.isRegularFile(index)) {
                bytes = Files.readAllBytes(index);
            } else {
                String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                        + "<title>Server Diagnostics</title></head><body><h1>Server Diagnostics</h1>"
                        + "<p>index.html not found.</p></body></html>";
                bytes = html.getBytes(StandardCharsets.UTF_8);
            }
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /** OA 发件服务器连接测试接口：将用户输入的 address 拼进 shell 执行，故意未校验，演示命令注入（RASP 可钩 exec） */
    static class SmtpTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String address = "172.16.21.97";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "address".equals(pair.substring(0, eq))) {
                        address = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            // 故意将用户输入直接拼进 shell 命令，供 RASP 检测 Runtime.exec 命令注入
            String cmd = "ping -c 2 " + address;
            Process proc = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            String out = readProcessOutput(proc);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 200, out);
        }
    }

    /** 模拟已被注入的「内存马」后门：传入 cmd 在服务端真实执行（Runtime.exec），供 RASP 真实验证与钩子检测 */
    static class MemoryShellHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String cmd = "id";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "cmd".equals(pair.substring(0, eq))) {
                        cmd = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            System.out.println("[memory-shell] exec: " + cmd);
            Process proc = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            String out = readProcessOutput(proc);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 200, out);
        }
    }

    /**
     * 反射型 XSS 演示：将用户输入的关键词未经转义直接拼入 HTML 响应，
     * 供 RASP 检测反射型跨站脚本（Reflected XSS）。
     */
    static class XssDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String q = "";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "q".equals(pair.substring(0, eq))) {
                        q = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            // 故意未做 HTML 转义，将用户输入直接拼入响应，形成反射型 XSS
            String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>"
                    + "<p>您搜索的关键词：" + q + "</p></body></html>";
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /**
     * SSRF 演示：根据用户传入的 URL 由服务端发起请求并返回结果，未做 URL 白名单校验，
     * 供 RASP 钩子检测服务端请求伪造（Server-Side Request Forgery）。
     */
    static class SsrfDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equals(ex.getRequestMethod())) {
                send(ex, 405, "Method Not Allowed");
                return;
            }
            URI uri = ex.getRequestURI();
            String query = uri.getRawQuery();
            String urlParam = "";
            if (query != null) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "url".equals(pair.substring(0, eq))) {
                        urlParam = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
                        break;
                    }
                }
            }
            if (urlParam == null || urlParam.isEmpty()) {
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                send(ex, 400, "缺少参数 url");
                return;
            }
            StringBuilder out = new StringBuilder();
            try {
                URL url = new URL(urlParam);
                // 故意未做 URL 白名单校验，服务端按用户指定 URL 发起请求，形成 SSRF（可请求内网、file 等）
                try (InputStream is = url.openStream();
                        BufferedReader r = new BufferedReader(
                                new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                }
            } catch (Exception e) {
                out.append("请求失败: ").append(e.getMessage());
            }
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 200, out.toString());
        }
    }

    private static String readProcessOutput(Process proc) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sb.append("\n(interrupted)");
        }
        return sb.length() > 0 ? sb.toString() : "(无输出)";
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
