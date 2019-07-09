package im.shimo.react.x5.webview;

import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.List;

public class RNX5WebViewPackage implements ReactPackage {

    private X5WebViewModule module;
    private RNX5WebViewManager manager;

    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        module = new X5WebViewModule(reactContext);
        module.setPackage(this);
        modules.add(module);
        return modules;
    }

    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
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
