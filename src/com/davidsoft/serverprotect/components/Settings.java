package com.davidsoft.serverprotect.components;

import com.davidsoft.collections.ReadOnlyMap;
import com.davidsoft.collections.ReadOnlySet;
import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.enties.*;
import com.davidsoft.serverprotect.libs.HttpPath;
import com.davidsoft.serverprotect.libs.IpIndex;
import com.davidsoft.serverprotect.libs.PathIndex;
import com.davidsoft.utils.JsonNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Settings {

    public static final class ApplyException extends Exception {
        private ApplyException(String fieldName, String message) {
            super(fieldName + ": " + message);
        }

        private ApplyException(String fieldName, String message, Throwable cause) {
            super(fieldName + ": " + message, cause);
        }
    }

    public static final class StaticSettings {
        public final int maxConnections;
        public final int maxServices;
        public final boolean keepConnections;
        public final int maxPathLength;
        public final int maxHeaderSize;

        private StaticSettings(int maxConnections, int maxServices, boolean keepConnections, int maxPathLength, int maxHeaderSize) {
            this.maxConnections = maxConnections;
            this.maxServices = maxServices;
            this.keepConnections = keepConnections;
            this.maxPathLength = maxPathLength;
            this.maxHeaderSize = maxHeaderSize;
        }

        private static StaticSettings fromServerNode(ServerNode serverNode) {
            return new StaticSettings(serverNode.maxConnections, serverNode.maxServices, serverNode.keepConnections, serverNode.maxPathLength, serverNode.maxHeaderSize);
        }
    }

    public static final class WebApplication {
        public final String name;
        public final String type;
        public final IpIndex<Void> whiteList;
        public final ReadOnlySet<String> allowDomains;
        public final ReadOnlyMap<Integer, String> routers;
        public final String targetDomain;
        public final int targetPort;
        public final boolean targetSSL;
        public final boolean forwardIp;

        private WebApplication(String name, String type, IpIndex<Void> whiteList, ReadOnlySet<String> allowDomains, ReadOnlyMap<Integer, String> routers, String targetDomain, int targetPort, boolean targetSSL, boolean forwardIp) {
            this.name = name;
            this.type = type;
            this.whiteList = whiteList;
            this.allowDomains = allowDomains;
            this.routers = routers;
            this.targetDomain = targetDomain;
            this.targetPort = targetPort;
            this.targetSSL = targetSSL;
            this.forwardIp = forwardIp;
        }

        private static WebApplication fromAppNode(AppNode appNode) throws ApplyException {
            if (Utils.isStringEmpty(appNode.name)) {
                throw new ApplyException("apps.name", "字段不能为空");
            }
            IpIndex<Void> whiteList = new IpIndex<>();
            if (appNode.whiteList != null) {
                for (String item : appNode.whiteList) {
                    whiteList.put(item, null);
                }
            }
            HashMap<Integer, String> routers = new HashMap<>();
            if (appNode.routers != null) {
                for (RouterNode node : appNode.routers) {
                    if (routers.containsKey(node.errorCode)) {
                        throw new ApplyException("apps.routers", "错误号重复定义");
                    }
                    routers.put(node.errorCode, node.location);
                }
            }
            switch (appNode.type) {
                case "app":
                case "file":
                case "settings":
                    break;
                case "forward":
                    if (Utils.isStringEmpty(appNode.target.domain)) {
                        throw new ApplyException("apps.target.domain", "字段不能为空");
                    }
                    break;
                default:
                    throw new ApplyException("apps.type", "无效app类型");
            }
            if (appNode.target == null) {
                return new WebApplication(
                        appNode.name,
                        appNode.type,
                        whiteList,
                        new ReadOnlySet<>(new HashSet<>(Arrays.asList(appNode.domains))),
                        new ReadOnlyMap<>(routers),
                        null, 0, false, false
                );
            }
            else {
                return new WebApplication(
                        appNode.name,
                        appNode.type,
                        whiteList,
                        new ReadOnlySet<>(new HashSet<>(Arrays.asList(appNode.domains))),
                        new ReadOnlyMap<>(routers),
                        appNode.target.domain, appNode.target.port, appNode.target.ssl, appNode.target.forwardIp
                );
            }
        }
    }

    public static final class ApplicationMapping {
        public final int port;
        public final boolean ssl;
        public final PathIndex<WebApplication> mappedApps;
        public final WebApplication defaultApp;

        private ApplicationMapping(int port, boolean ssl, PathIndex<WebApplication> mappedApps, WebApplication defaultApp) {
            this.port = port;
            this.ssl = ssl;
            this.mappedApps = mappedApps;
            this.defaultApp = defaultApp;
        }

        private static ApplicationMapping fromMappingNode(MappingNode mappingNode, HashMap<String, WebApplication> apps) throws ApplyException {
            PathIndex<WebApplication> mappedApps = new PathIndex<>();
            WebApplication app;
            boolean defaultMatch = Utils.isStringEmpty(mappingNode.defaultApp);
            if (mappingNode.apps != null) {
                for (MappingAppNode node : mappingNode.apps) {
                    if (node != null) {
                        if (Utils.isStringEmpty(node.root)) {
                            throw new ApplyException("mappings.apps.root", "字段不能为空");
                        }
                        if (Utils.isStringEmpty(node.name)) {
                            throw new ApplyException("mappings.apps.name", "字段不能为空");
                        }
                        if (!node.root.startsWith("/")) {
                            throw new ApplyException("mappings.apps.root", "必须以'/'开头");
                        }
                        app = apps.get(node.name);
                        if (app == null) {
                            throw new ApplyException("mappings.apps.name", "在apps.json中未找到此名称的app");
                        }
                        mappedApps.put(HttpPath.parse(node.root), apps.get(node.name));
                        if (!defaultMatch && mappingNode.defaultApp.equals(node.name)) {
                            defaultMatch = true;
                        }
                    }
                }
            }
            if (!defaultMatch) {
                throw new ApplyException("mappings.defaultApp", "在此端口号下包含的app中未找到此名称的app");
            }
            return new ApplicationMapping(
                    mappingNode.port,
                    mappingNode.ssl,
                    mappedApps,
                    Utils.isStringEmpty(mappingNode.defaultApp) ? null : apps.get(mappingNode.defaultApp)
            );
        }
    }

    public static final class ActionContent {
        public final String type;
        public final String mime;
        public final String content;

        private ActionContent(String type, String mime, String content) {
            this.type = type;
            this.mime = mime;
            this.content = content;
        }

        private static ActionContent fromActionContentNode(ActionContentNode actionContentNode) throws ApplyException {
            if (Utils.isStringEmpty(actionContentNode.type)) {
                throw new ApplyException("content.type", "字段不能为空");
            }
            switch (actionContentNode.type) {
                case "file":
                case "inline":
                    return new ActionContent(
                            actionContentNode.type,
                            actionContentNode.mime,
                            actionContentNode.content
                    );
                default:
                    throw new ApplyException("content.type", "无效内容类型");
            }
        }
    }

    public static final class Action {
        public final String actionType;
        public final ActionContent actionContent;

        private Action(String actionType, ActionContent actionContent) {
            this.actionType = actionType;
            this.actionContent = actionContent;
        }

        private static Action fromActionNode(ActionNode actionNode) throws ApplyException {
            if (actionNode == null) {
                return null;
            }
            if (Utils.isStringEmpty(actionNode.type)) {
                throw new ApplyException("action.type", "字段不能为空");
            }
            switch (actionNode.type) {
                case "shutdown":
                    return new Action(actionNode.type, null);
                case "response":
                    if (actionNode.content == null) {
                        throw new ApplyException("action.content", "当动作类型为'response'时，必须指定content字段");
                    }
                    return new Action(actionNode.type, ActionContent.fromActionContentNode(actionNode.content));
                default:
                    throw new ApplyException("content.type", "无效动作类型");
            }
        }
    }

    public static final class Precaution {
        public final String method;
        public final long blockLengthInMinute;
        public final Action action;
        public final Action xhrAction;

        private Precaution(String method, long blockLengthInMinute, Action action, Action xhrAction) {
            this.method = method;
            this.blockLengthInMinute = blockLengthInMinute;
            this.action = action;
            this.xhrAction = xhrAction;
        }

        private static Precaution fromPrecautionNode(PrecautionNode precautionNode) throws ApplyException {
            if (Utils.isStringEmpty(precautionNode.method)) {
                throw new ApplyException("method", "字段不能为空");
            }
            switch (precautionNode.method) {
                case "disabled":
                    return new Precaution(precautionNode.method, 0, null, null);
                case "block":
                    if (precautionNode.lengthInMinute < 1) {
                        throw new ApplyException("lengthInMinute", "封禁时间分钟数必须>=1");
                    }
                    return new Precaution(precautionNode.method, precautionNode.lengthInMinute, null, null);
                case "action":
                    if (Utils.isStringEmpty(precautionNode.action.type) && Utils.isStringEmpty(precautionNode.actionXhr.type)) {
                        throw new ApplyException("action / actionXhr", "当措施方法为'action'时，必须至少指定action字段或actionXhr字段之一");
                    }
                    Action action, actionXhr;
                    if (Utils.isStringEmpty(precautionNode.action.type)) {
                        action = Action.fromActionNode(precautionNode.actionXhr);
                        actionXhr = action;
                    }
                    else if (Utils.isStringEmpty(precautionNode.actionXhr.type)) {
                        action = Action.fromActionNode(precautionNode.action);
                        actionXhr = action;
                    }
                    else {
                        action = Action.fromActionNode(precautionNode.action);
                        actionXhr = Action.fromActionNode(precautionNode.actionXhr);
                    }
                    return new Precaution(
                            precautionNode.method,
                            0,
                            action,
                            actionXhr
                    );
                default:
                    throw new ApplyException("method", "无效措施方法");
            }
        }
    }

    public static final class Protections {
        public final long frequencyDetectIntervalInSecond;
        public final int frequencyDetectTimes;
        public final Precaution precautionForBlackList;
        public final Precaution precautionForIllegalAgent;
        public final Precaution precautionForIllegalData;
        public final Precaution precautionForIllegalForward;
        public final Precaution precautionForIllegalFrequency;
        public final Precaution precautionForIllegalMethod;
        public final Precaution precautionForIllegalRedirect;
        public final Precaution precautionForIllegalTrace;
        //TODO: 添加图字段

        private Protections(long frequencyDetectIntervalInSecond,
                            int frequencyDetectTimes,
                            Precaution precautionForBlackList,
                            Precaution precautionForIllegalAgent,
                            Precaution precautionForIllegalData,
                            Precaution precautionForIllegalForward,
                            Precaution precautionForIllegalFrequency,
                            Precaution precautionForIllegalMethod,
                            Precaution precautionForIllegalRedirect,
                            Precaution precautionForIllegalTrace) {
            this.frequencyDetectIntervalInSecond = frequencyDetectIntervalInSecond;
            this.frequencyDetectTimes = frequencyDetectTimes;
            this.precautionForBlackList = precautionForBlackList;
            this.precautionForIllegalAgent = precautionForIllegalAgent;
            this.precautionForIllegalData = precautionForIllegalData;
            this.precautionForIllegalForward = precautionForIllegalForward;
            this.precautionForIllegalFrequency = precautionForIllegalFrequency;
            this.precautionForIllegalMethod = precautionForIllegalMethod;
            this.precautionForIllegalRedirect = precautionForIllegalRedirect;
            this.precautionForIllegalTrace = precautionForIllegalTrace;
        }

        private static Protections fromProtectNode(ProtectNode protectNode) throws ApplyException {
            Protections protections = new Protections(
                    protectNode.config.frequencyDetectIntervalInSecond,
                    protectNode.config.frequencyDetectTimes,
                    Precaution.fromPrecautionNode(protectNode.blockAction),
                    Precaution.fromPrecautionNode(protectNode.illegalAgent),
                    Precaution.fromPrecautionNode(protectNode.illegalData),
                    Precaution.fromPrecautionNode(protectNode.illegalForward),
                    Precaution.fromPrecautionNode(protectNode.illegalFreq),
                    Precaution.fromPrecautionNode(protectNode.illegalMethod),
                    Precaution.fromPrecautionNode(protectNode.illegalRedirect),
                    Precaution.fromPrecautionNode(protectNode.illegalTrace)
            );
            return protections;
        }
    }

    public static final class RuntimeSettings {
        public final ReadOnlyMap<Integer, ApplicationMapping> appMappings;
        public final Protections protections;

        private RuntimeSettings(ReadOnlyMap<Integer, ApplicationMapping> appMappings, Protections protections) {
            this.appMappings = appMappings;
            this.protections = protections;
        }
    }

    public static final String LOG_CATEGORY = "设置管理器";
    private static StaticSettings staticSettings;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static RuntimeSettings runtimeSettings;

    private static final ReentrantReadWriteLock configFileLock = new ReentrantReadWriteLock(true);
    private static final File FILE_APPS = new File("configs" + File.separator + "apps.json");
    private static final File FILE_MAPPINGS = new File("configs" + File.separator + "mappings.json");
    private static final File FILE_PROTECT = new File("configs" + File.separator + "protect.json");
    private static final File FILE_SERVER = new File("configs" + File.separator + "server.properties");

    private static StaticSettings loadStaticSettings() {
        Properties properties = Utils.loadProperties(FILE_SERVER);
        if (properties == null) {
            Log.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法打开 " + FILE_SERVER.getAbsolutePath() + "，已使用默认配置。");
            return StaticSettings.fromServerNode(new ServerNode());
        }
        else {
            return StaticSettings.fromServerNode(new ServerNode(properties));
        }
    }

    public static final class ConfigFileInfo {
        public final JsonNode fileJson;
        public final String name;
        public final String absolutePath;

        public ConfigFileInfo(JsonNode fileJson, String name, String absolutePath) {
            this.fileJson = fileJson;
            this.name = name;
            this.absolutePath = absolutePath;
        }
    }

    //此函数可能会被其他线程调用
    public static ConfigFileInfo[] loadConfigJsons() {
        lock.readLock().lock();

        ConfigFileInfo[] infos = new ConfigFileInfo[3];
        infos[0] = new ConfigFileInfo(Utils.loadJson(FILE_APPS, null), FILE_APPS.getName(), FILE_APPS.getAbsolutePath());
        infos[1] = new ConfigFileInfo(Utils.loadJson(FILE_MAPPINGS, null), FILE_APPS.getName(), FILE_MAPPINGS.getAbsolutePath());
        infos[2] = new ConfigFileInfo(Utils.loadJson(FILE_PROTECT, null), FILE_APPS.getName(), FILE_PROTECT.getAbsolutePath());

        lock.readLock().unlock();
        return infos;
    }

    //此函数可能会被其他线程调用
    public static void saveConfigJsons(JsonNode appsJson, JsonNode mappingsJson, JsonNode protectJson) {
        configFileLock.writeLock().lock();
        if (!Utils.saveJson(FILE_APPS, appsJson, false, null)) {
            Program.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_APPS.getAbsolutePath() + "，新的设置可能无法被保存！");
        }
        if (!Utils.saveJson(FILE_MAPPINGS, mappingsJson, false, null)) {
            Program.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_MAPPINGS.getAbsolutePath() + " 新的设置可能无法被保存！");
        }
        if (!Utils.saveJson(FILE_PROTECT, protectJson, false, null)) {
            Program.logMain(Log.LOG_WARNING, LOG_CATEGORY, "无法写入 " + FILE_PROTECT.getAbsolutePath() + " 新的设置可能无法被保存！");
        }
        configFileLock.writeLock().unlock();
    }

    //此函数可能会被其他线程调用
    public static Object[] parseSettingNodesFromConfigJsons(ConfigFileInfo[] configFileInfos) throws IOException {
        if (configFileInfos[0].fileJson == null) {
            throw new IOException("无法打开 " + configFileInfos[0].absolutePath + "，请确保此文件可用。");
        }
        AppNode[] appNodes;
        try {
            appNodes = configFileInfos[0].fileJson.toObject(AppNode[].class);
        }
        catch (IllegalStateException | IllegalArgumentException e) {
            throw new IOException(configFileInfos[0].name + " 格式不正确，请重新配置。");
        }
        if (appNodes == null) {
            throw new IOException(configFileInfos[0].name + " 格式不正确，请重新配置。");
        }

        if (configFileInfos[1].fileJson == null) {
            throw new IOException("无法打开 " + configFileInfos[1].absolutePath + "，请确保此文件可用。");
        }
        MappingNode[] mappingNodes;
        try {
            mappingNodes = configFileInfos[1].fileJson.toObject(MappingNode[].class);
        }
        catch (IllegalStateException | IllegalArgumentException e) {
            throw new IOException(configFileInfos[1].name + " 格式不正确，请重新配置。");
        }
        if (mappingNodes == null) {
            throw new IOException(configFileInfos[1].name + " 格式不正确，请重新配置。");
        }

        if (configFileInfos[2].fileJson == null) {
            throw new IOException("无法打开 " + configFileInfos[2].absolutePath + "，请确保此文件可用。");
        }
        ProtectNode protectNode;
        try {
            protectNode = configFileInfos[2].fileJson.toObject(ProtectNode.class);
        }
        catch (IllegalStateException | IllegalArgumentException e) {
            throw new IOException(configFileInfos[2].name + " 格式不正确，请重新配置。");
        }
        if (protectNode == null) {
            throw new IOException(configFileInfos[2].name + " 格式不正确，请重新配置。");
        }

        return new Object[] {appNodes, mappingNodes, protectNode};
    }

    //此函数可能会被其他线程调用
    public static RuntimeSettings parseRuntimeSettingsFromSettingNodes(Object[] settingNodes) throws ApplyException {
        HashMap<Integer, ApplicationMapping> appMappings;
        Protections protections;
        HashMap<String, WebApplication> apps = new HashMap<>();
        for (AppNode node : (AppNode[])(settingNodes[0])) {
            if (node != null) {
                WebApplication application = WebApplication.fromAppNode(node);
                if (apps.containsKey(application.name)) {
                    throw new ApplyException("apps.name", "app名称存在重复，请重新配置。");
                }
                apps.put(application.name, application);
            }
        }
        appMappings = new HashMap<>();
        for (MappingNode node : (MappingNode[])(settingNodes[1])) {
            if (node != null) {
                ApplicationMapping mapping = ApplicationMapping.fromMappingNode(node, apps);
                if (appMappings.containsKey(mapping.port)) {
                    throw new ApplyException("mappings.port", "app映射存在重复定义的端口号，请重新配置。");
                }
                appMappings.put(mapping.port, mapping);
            }
        }
        protections = Protections.fromProtectNode((ProtectNode) settingNodes[2]);
        return new RuntimeSettings(new ReadOnlyMap<>(appMappings), protections);
    }

    static boolean initSettings() {
        staticSettings = loadStaticSettings();
        try {
            runtimeSettings = parseRuntimeSettingsFromSettingNodes(parseSettingNodesFromConfigJsons(loadConfigJsons()));
        }
        catch (IOException | ApplyException e) {
            Log.logMain(Log.LOG_ERROR, LOG_CATEGORY, e.getMessage());
            return false;
        }
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "配置文件加载成功！");
        return true;
    }

    static void applyNewRuntimeSettings(RuntimeSettings runtimeSettings) {
        lock.writeLock().lock();
        Settings.runtimeSettings = runtimeSettings;
        lock.writeLock().unlock();
        Log.logMain(Log.LOG_INFO, LOG_CATEGORY, "已应用新的配置。新的配置将从下一个连接起生效。");
    }

    //此函数可能会被其他线程调用
    public static StaticSettings getStaticSettings() {
        return staticSettings;
    }

    //此函数可能会被其他线程调用
    public static RuntimeSettings getRuntimeSettings() {
        lock.readLock().lock();
        RuntimeSettings ret = runtimeSettings;
        lock.readLock().unlock();
        return ret;
    }
}