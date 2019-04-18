package im.shimo.react.x5.webview;

import com.facebook.react.ReactPackage;

import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.ViewManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.ValueCallback;
import android.net.Uri;
import android.content.Intent;
import android.app.Activity;
import android.util.Log;
import android.os.Build;
import android.Manifest;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import android.util.Log;

import im.shimo.react.x5.webview.RNX5WebViewManager;
import im.shimo.react.x5.webview.X5WebViewModule;

public class RNX5WebViewPackage implements ReactPackage {

    private X5WebViewModule module;
    private RNX5WebViewManager manager;

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        module = new X5WebViewModule(reactContext);
        module.setPackage(this);
        modules.add(module);
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        List<ViewManager> modules = new ArrayList<>();
        Log.d("react-native-x5", "start...");
        manager = new RNX5WebViewManager(reactContext);
        manager.setPackage(this);
        modules.add(manager);
        return modules;
    }

    public RNX5WebViewManager getManager() {
        return manager;
    }

    public X5WebViewModule getModule() {
        return module;
    }
}
