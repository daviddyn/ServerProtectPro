package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.components.Settings;
import com.davidsoft.net.http.WebApplication;
import com.davidsoft.url.URI;

import java.io.File;

public final class WebApplicationFactory {

    public static WebApplication fromSettings(Settings.WebApplication webApplicationSettings, URI requestRelativeURI) {
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
                requestRelativeURI,
                webApplicationSettings.whiteList,
                webApplicationSettings.allowDomains,
                webApplicationSettings.routers
        );
        return baseWebApplication;
    }
}
