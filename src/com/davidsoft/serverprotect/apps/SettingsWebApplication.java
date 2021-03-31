package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.components.BlackListManager;
import com.davidsoft.serverprotect.components.Log;
import com.davidsoft.serverprotect.components.Program;
import com.davidsoft.serverprotect.components.Settings;
import com.davidsoft.serverprotect.enties.*;
import com.davidsoft.serverprotect.http.*;
import com.davidsoft.serverprotect.libs.HttpPath;
import com.davidsoft.utils.JsonNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SettingsWebApplication extends FileWebApplication {

    private static final String LOG_CATEGORY = "设置APP";

    private static final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock(true);
    private static final File DIMENSION_FILE = new File("configs" + File.separator + "id_dimensions");

    private static final class HttpContentLjqJsonProvider extends HttpContentStringProvider {

        public HttpContentLjqJsonProvider(int responseCode, Object data) {
            super(
                    "{\"code\": " + responseCode + ", \"data\": " + JsonNode.valueOf(data) + "}",
                    StandardCharsets.UTF_8,
                    "application/json",
                    StandardCharsets.UTF_8
            );
        }
    }

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private DagNode pathsNode2DagNode(PathsNode pathsNode) {
        fileLock.readLock().lock();
        Scanner scanner;
        HashMap<Integer, String[]> dimensions = new HashMap<>();
        try {
            scanner = new Scanner(DIMENSION_FILE);
        } catch (FileNotFoundException e) {
            scanner = null;
            Program.logMain(Log.LOG_ERROR, LOG_CATEGORY, "无法读取 " + DIMENSION_FILE.getAbsolutePath() + "，无法加载路径节点的坐标信息！");
        }
        if (scanner != null) {
            while (scanner.hasNext()) {
                int id = scanner.nextInt();
                String x = scanner.next();
                String y = scanner.next();
                String color = scanner.next();
                dimensions.put(id, new String[]{x, y, color});
            }
            scanner.close();
        }
        fileLock.readLock().unlock();

        DagNode dagNode = new DagNode();
        dagNode.edges = new DagEdgeNode[pathsNode.topology.length];
        for (int i = 0; i < pathsNode.topology.length; i++) {
            dagNode.edges[i] = new DagEdgeNode();
            dagNode.edges[i].id = i + 1;
            dagNode.edges[i].src_node_id = pathsNode.topology[i][0];
            dagNode.edges[i].dst_node_id = pathsNode.topology[i][1];
            dagNode.edges[i].src_output_idx = 0;
            dagNode.edges[i].dst_input_idx = 0;
        }
        dagNode.nodes = new DagNodeNode[pathsNode.urls.length];
        for (int i = 0; i < pathsNode.urls.length; i++) {
            dagNode.nodes[i] = new DagNodeNode();
            dagNode.nodes[i].id = pathsNode.urls[i].id;
            dagNode.nodes[i].name = pathsNode.urls[i].url;
            String[] dimension = dimensions.get(pathsNode.urls[i].id);
            dagNode.nodes[i].iconStyle = new DagIconStyleNode();
            if (dimension == null) {
                dagNode.nodes[i].pos_x = "0";
                dagNode.nodes[i].pos_y = "0";
                dagNode.nodes[i].iconStyle.background = "#409EFF";
            }
            else {
                dagNode.nodes[i].pos_x = dimension[0];
                dagNode.nodes[i].pos_y = dimension[1];
                dagNode.nodes[i].iconStyle.background = dimension[2];
            }
            dagNode.nodes[i].form = new DagFormNode();
            dagNode.nodes[i].form.url = pathsNode.urls[i].url;
            dagNode.nodes[i].iconClassName = "el-icon-link";
            dagNode.nodes[i].in_ports = new int[] {0};
            dagNode.nodes[i].out_ports = new int[] {0};
        }
        return dagNode;
    }

    private PathsNode dagNode2PathsNode(DagNode dagNode) {
        PathsNode pathsNode = new PathsNode();
        pathsNode.urls = new PathsUrlNode[dagNode.nodes.length];
        for (int i = 0; i < pathsNode.urls.length; i++) {
            pathsNode.urls[i] = new PathsUrlNode();
            pathsNode.urls[i].id = dagNode.nodes[i].id;
            pathsNode.urls[i].url = dagNode.nodes[i].name;
        }
        pathsNode.topology = new int[dagNode.edges.length][];
        for (int i = 0; i < pathsNode.topology.length; i++) {
            pathsNode.topology[i] = new int[] {dagNode.edges[i].src_node_id, dagNode.edges[i].dst_node_id};
        }
        return pathsNode;
    }

    private void saveDagDimensions(DagNode dagNode) {
        //保存ID的坐标
        fileLock.writeLock().lock();
        PrintStream out;
        try {
            out = new PrintStream(DIMENSION_FILE);
        } catch (FileNotFoundException e) {
            fileLock.writeLock().unlock();
            e.printStackTrace();
            Program.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + DIMENSION_FILE.getAbsolutePath() + "，路径节点的坐标信息将无法得以保存！");
            return;
        }
        for (int i = 0; i < dagNode.nodes.length; i++) {
            out.println(dagNode.nodes[i].id + "\t" + dagNode.nodes[i].pos_x + "\t" + dagNode.nodes[i].pos_y + "\t" + (dagNode.nodes[i].iconStyle == null ? "#409EFF" : dagNode.nodes[i].iconStyle.background));
        }
        out.close();
        fileLock.writeLock().unlock();
    }

    private HttpResponseSender onGetConfig() {
        //1. 读取配置文件，并转成与json对应的node对象
        Object[] settingNodes;
        try {
            settingNodes = Settings.parseSettingNodesFromConfigJsons(Settings.loadConfigJsons());
        } catch (IOException e) {
            Program.logMain(Log.LOG_ERROR, Settings.LOG_CATEGORY, e.getMessage());
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, e.getMessage()));
        }
        //2. 将PathsNode转成DagNode
        DagNode dagNode = pathsNode2DagNode(((ProtectNode) settingNodes[2]).paths);
        //3. 构造Protections的Json
        LinkedHashMap<String, JsonNode> fields = new LinkedHashMap<>(10);
        fields.put("blockAction", JsonNode.valueOf(((ProtectNode) settingNodes[2]).blockAction));
        fields.put("config", JsonNode.valueOf(((ProtectNode) settingNodes[2]).config));
        fields.put("illegalAgent", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalAgent));
        fields.put("illegalData", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalData));
        fields.put("illegalForward", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalForward));
        fields.put("illegalFreq", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalFreq));
        fields.put("illegalMethod", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalMethod));
        fields.put("illegalRedirect", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalRedirect));
        fields.put("illegalTrace", JsonNode.valueOf(((ProtectNode) settingNodes[2]).illegalTrace));
        fields.put("paths", JsonNode.valueOf(dagNode));
        JsonNode protectJsonNode = new JsonNode(fields);
        //4. 构造黑名单的json
        BlackListManager.freeze();
        JsonNode[] blockJsonNodes = new JsonNode[BlackListManager.size()];
        int i = 0;
        for (Map.Entry<Integer, long[]> entry : BlackListManager.entries()) {
            fields = new LinkedHashMap<>(2);
            fields.put("ip", new JsonNode(Utils.decodeIp(entry.getKey()), false));
            fields.put("expires", new JsonNode(BlackListManager.simpleDateFormat.format(entry.getValue()[0]), false));
            blockJsonNodes[i++] = new JsonNode(fields);
        }
        BlackListManager.unfreeze();
        JsonNode blocksJsonNode = new JsonNode(blockJsonNodes);
        //5. 构造总json
        fields = new LinkedHashMap<>(4);
        fields.put("apps", JsonNode.valueOf(settingNodes[0]));
        fields.put("mappings", JsonNode.valueOf(settingNodes[1]));
        fields.put("blocks", blocksJsonNode);
        fields.put("protect", protectJsonNode);
        JsonNode ret = new JsonNode(fields);
        //6. 发送给前端
        return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(200, ret));
    }

    private HttpResponseSender receiveJson(HttpRequestReceiver requestReceiver, JsonNode[] out) {
        HttpResponseSender response = AppUtils.analyseRequestContent(requestReceiver, "application/json");
        if (response != null) {
            return response;
        }
        Charset charset = requestReceiver.getContentCharset();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        try {
            out[0] = new HttpContentJsonReceiver(charset).onReceive(requestReceiver.getContentInputStream(), requestReceiver.getContentLength());
        }
        catch (ContentTooLargeException e) {
            return new HttpResponseSender(new HttpResponseInfo(413), null);
        }
        catch (SyntaxException e) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json语法不正确"));
        }
        catch (UnacceptableException e) {
            return new HttpResponseSender(new HttpResponseInfo(500), null);
        }
        catch (IOException e) {
            out[0] = null;
            return null;
        }
        return null;
    }

    private HttpResponseSender onSetConfig(HttpRequestReceiver requestReceiver) {
        JsonNode[] requestJson = new JsonNode[1];
        HttpResponseSender response = receiveJson(requestReceiver, requestJson);
        if (response != null) {
            return response;
        }
        if (requestJson[0] == null) {
            return null;
        }
        JsonNode appsJsonNode = requestJson[0].getField("apps");
        JsonNode mappingsJsonNode = requestJson[0].getField("mappings");
        JsonNode protectJsonNode = requestJson[0].getField("protect");
        if (protectJsonNode == null) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        JsonNode dagJsonNode = protectJsonNode.getField("paths");
        if (dagJsonNode == null) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        if (dagJsonNode.isNull() || !dagJsonNode.isObject()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        DagNode dagNode;
        try {
            dagNode = dagJsonNode.toObject(DagNode.class);
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        if (dagNode == null) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        protectJsonNode.setField("paths", JsonNode.valueOf(dagNode2PathsNode(dagNode)));

        Settings.ConfigFileInfo[] configFileInfos = new Settings.ConfigFileInfo[] {
                new Settings.ConfigFileInfo(appsJsonNode, "", ""),
                new Settings.ConfigFileInfo(mappingsJsonNode, "", ""),
                new Settings.ConfigFileInfo(protectJsonNode, "", "")
        };
        Object[] parsed;
        try {
            parsed = Settings.parseSettingNodesFromConfigJsons(configFileInfos);
        } catch (IOException e) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        Settings.RuntimeSettings runtimeSettings;
        try {
            runtimeSettings = Settings.parseRuntimeSettingsFromSettingNodes(parsed);
        } catch (Settings.ApplyException e) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, e.getMessage()));
        }
        Settings.saveConfigJsons(appsJsonNode, mappingsJsonNode, protectJsonNode);
        saveDagDimensions(dagNode);
        Program.applyNewSettings(runtimeSettings);
        return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(200, "新的配置应用成功，将从下一个连接起生效！"));
    }

    private HttpResponseSender onAddBlackList(HttpRequestReceiver requestReceiver) {
        JsonNode[] requestJson = new JsonNode[1];
        HttpResponseSender response = receiveJson(requestReceiver, requestJson);
        if (response != null) {
            return response;
        }
        if (requestJson[0] == null) {
            return null;
        }
        if (requestJson[0].isNull() || !requestJson[0].isObject()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        JsonNode node = requestJson[0].getField("ip");
        if (node == null || node.isNull() || !node.isPlain()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        String ip = node.getValue();
        if (!Utils.checkIpWithRegex(ip)) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "无效ip字段值"));
        }
        node = requestJson[0].getField("expires");
        if (node == null || node.isNull() || !node.isPlain()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        String expiresString = node.getValue();
        long expires;
        try {
            expires = simpleDateFormat.parse(expiresString).getTime();
        } catch (ParseException e) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "无效expires字段值"));
        }
        long result = Program.addBlackListSync(ip, expires);
        AddBlackListResponseNode responseNode = new AddBlackListResponseNode();
        responseNode.ip = ip;
        if (result == -1) {
            responseNode.status = "added";
            responseNode.expires = simpleDateFormat.format(expires);
            responseNode.message = "已禁止 " + ip + " 的访问，且将在 " + responseNode.expires + " 解除。从下一个连接起开始生效。";
        }
        else if (result == 0) {
            responseNode.status = "updated";
            responseNode.expires = simpleDateFormat.format(expires);
            responseNode.message = ip + " 的解除时间已延长至 " + responseNode.expires + " 。从下一个连接起开始生效。";
        }
        else {
            responseNode.status = "none";
            responseNode.expires = simpleDateFormat.format(expires);
            responseNode.message = ip + " 已经被禁止访问，且将在 " + responseNode.expires + " ，因此添加没有生效。";
        }
        return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(200, responseNode));
    }

    private HttpResponseSender onRemoveBlackList(HttpRequestReceiver requestReceiver) {
        JsonNode[] requestJson = new JsonNode[1];
        HttpResponseSender response = receiveJson(requestReceiver, requestJson);
        if (response != null) {
            return response;
        }
        if (requestJson[0] == null) {
            return null;
        }
        if (requestJson[0].isNull() || !requestJson[0].isObject()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        JsonNode node = requestJson[0].getField("ip");
        if (node == null || node.isNull() || !node.isPlain()) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "Json格式不正确"));
        }
        String ip = node.getValue();
        if (!Utils.checkIpWithRegex(ip)) {
            return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(500, "无效ip字段值"));
        }
        Program.removeBlackList(ip);
        return new HttpResponseSender(new HttpResponseInfo(200), new HttpContentLjqJsonProvider(200, "已允许 " + ip + " 的访问。从下一个连接起生效。"));
    }

    protected HttpResponseSender onClientRequest(HttpRequestReceiver requestReceiver, String ip, HttpPath requestRelativePath) {
        switch (requestRelativePath.toString()) {
            case "/get":
                //TODO: 将下面注释的代码取消注释使其仅支持POST请求。
                /*
                if (!"POST".equals(requestReceiver.getRequestInfo().method)) {
                    return new HttpResponseSender(new HttpResponseInfo(405), null);
                }
                */
                return onGetConfig();
            case "/set":
                if (!"POST".equals(requestReceiver.getRequestInfo().method)) {
                    return new HttpResponseSender(new HttpResponseInfo(405), null);
                }
                return onSetConfig(requestReceiver);
            case "/addBlackList":
                return onAddBlackList(requestReceiver);
            case "/removeBlackList":
                return onRemoveBlackList(requestReceiver);
            default:
                return super.onClientRequest(requestReceiver, ip, requestRelativePath);
        }
    }
}
