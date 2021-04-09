package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.components.Settings;
import com.davidsoft.http.WebApplication;
import com.davidsoft.serverprotect.libs.HttpPath;

import java.io.File;

public final class WebApplicationFactory {

    public static WebApplication fromSettings(Settings.WebApplication webApplicationSettings, HttpPath workingRootPath) {
        BaseWebApplication baseWebApplication;
        switch (webApplicationSettings.type) {
            case "app":
                baseWebApplication = new BaseWebApplication();
                break;
            case "file":
                baseWebApplication = new FileWebApplication();
                break;
            case "settings":
                baseWebApplication = new SettingsWebApplication();
                break;
            case "forward":
                baseWebApplication = new ForwardWebApplication(
                        webApplicationSettings.targetDomain,
                        webApplicationSettings.targetPort,
                        webApplicationSettings.targetSSL,
                        webApplicationSettings.forwardIp
                );
                break;
            default:
                return null;
        }
        baseWebApplication.initialize(
                webApplicationSettings.name,
                new File("apps" + File.separator + webApplicationSettings.name),
                workingRootPath,
                webApplicationSettings.whiteList,
                webApplicationSettings.allowDomains,
                webApplicationSettings.routers
        );
        return baseWebApplication;
    }
}
