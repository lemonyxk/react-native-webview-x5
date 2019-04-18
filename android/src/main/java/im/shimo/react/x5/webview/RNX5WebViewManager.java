package im.shimo.react.x5.webview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.JavascriptInterface;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.webview.events.TopLoadingErrorEvent;
import com.facebook.react.views.webview.events.TopLoadingFinishEvent;
import com.facebook.react.views.webview.events.TopLoadingStartEvent;
import com.facebook.react.views.webview.events.TopMessageEvent;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manages instances of {@link WebView}
 * <p>
 * Can accept following commands: - GO_BACK - GO_FORWARD - RELOAD
 * <p>
 * {@link WebView} instances could emit following direct events: -
 * topLoadingFinish - topLoadingStart - topLoadingError
 * <p>
 * Each event will carry the following properties: - target - view's react tag -
 * url - url set for the webview - loading - whether webview is in a loading
 * state - title - title of the current page - canGoBack - boolean, whether
 * there is anything on a history stack to go back - canGoForward - boolean,
 * whether it is possible to request GO_FORWARD command
 */
public class RNX5WebViewManager extends SimpleViewManager<WebView> {

    private static final String REACT_CLASS = "RNX5WebView";

    private static final String HTML_ENCODING = "UTF-8";
    private static final String HTML_MIME_TYPE = "text/html; charset=utf-8";
    private static final String BRIDGE_NAME = "__REACT_WEB_VIEW_BRIDGE";

    private static final String HTTP_METHOD_POST = "POST";

    private static final int COMMAND_GO_BACK = 1;
    private static final int COMMAND_GO_FORWARD = 2;
    private static final int COMMAND_RELOAD = 3;
    private static final int COMMAND_STOP_LOADING = 4;
    private static final int COMMAND_POST_MESSAGE = 5;
    private static final int INJECT_JAVASCRIPT = 6;

    // Use `webView.loadUrl("about:blank")` to reliably reset the view
    // state and release page resources (including any running JavaScript).
    private static final String BLANK_URL = "about:blank";

    private RNX5WebViewConfig mWebViewConfig;
    private @Nullable
    WebView.PictureListener mPictureListener;

    private RNX5WebViewPackage aPackage;

    private static class X5WebViewClient extends WebViewClient {

        private boolean mLastLoadFailed = false;

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);

            if (!mLastLoadFailed) {
                X5WeView reactWebView = (X5WeView) webView;
                reactWebView.callInjectedJavaScript();
                reactWebView.linkBridge();
                emitFinishEvent(webView, url);
            }

            if (webView.getX5WebViewExtension() != null) {
                Log.d("react-native-x5,", "getX5WebViewExtension is ok");
            } else {
                Log.d("react-native-x5", "getX5WebViewExtension is no");
            }

            if (webView.isHardwareAccelerated()) {
                Log.d("react-native-x5", "isHardwareAccelerated is ok");
            } else {
                Log.d("react-native-x5", "isHardwareAccelerated is no");
            }
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);
            mLastLoadFailed = false;
            dispatchEvent(webView, new TopLoadingStartEvent(webView.getId(), createWebViewEvent(webView, url)));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                return false;
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
                return true;
            }
        }

        @Override
        public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
            super.onReceivedError(webView, errorCode, description, failingUrl);
            mLastLoadFailed = true;

            // In case of an error JS side expect to get a finish event first, and then get
            // an error event
            // Android WebView does it in the opposite way, so we need to simulate that
            // behavior
            emitFinishEvent(webView, failingUrl);

            WritableMap eventData = createWebViewEvent(webView, failingUrl);
            eventData.putDouble("code", errorCode);
            eventData.putString("description", description);

            dispatchEvent(webView, new TopLoadingErrorEvent(webView.getId(), eventData));
        }

        @Override
        public void doUpdateVisitedHistory(WebView webView, String url, boolean isReload) {
            super.doUpdateVisitedHistory(webView, url, isReload);

            dispatchEvent(webView, new TopLoadingStartEvent(webView.getId(), createWebViewEvent(webView, url)));
        }

        private void emitFinishEvent(WebView webView, String url) {
            dispatchEvent(webView, new TopLoadingFinishEvent(webView.getId(), createWebViewEvent(webView, url)));
        }

        private WritableMap createWebViewEvent(WebView webView, String url) {
            WritableMap event = Arguments.createMap();
            event.putDouble("target", webView.getId());
            // Don't use webView.getUrl() here, the URL isn't updated to the new value yet
            // in callbacks
            // like onPageFinished
            event.putString("url", url);
            event.putBoolean("loading", !mLastLoadFailed && webView.getProgress() != 100);
            event.putString("title", webView.getTitle());
            event.putBoolean("canGoBack", webView.canGoBack());
            event.putBoolean("canGoForward", webView.canGoForward());
            return event;
        }
    }

    /**
     * Subclass of {@link WebView} that implements {@link LifecycleEventListener}
     * interface in order to call {@link WebView#destroy} on activty destroy event
     * and also to clear the client
     */
    private static class X5WeView extends WebView implements LifecycleEventListener {
        private @Nullable
        String injectedJS;
        private boolean messagingEnabled = false;

        private class ReactWebViewBridge {
            X5WeView mContext;

            ReactWebViewBridge(X5WeView c) {
                mContext = c;
            }

            @JavascriptInterface
            public void postMessage(String message) {
                mContext.onMessage(message);
            }
        }

        /**
         * WebView must be created with an context of the current activity
         * <p>
         * Activity Context is required for creation of dialogs internally by WebView
         * Reactive Native needed for access to ReactNative internal system
         * functionality
         */
        public X5WeView(ThemedReactContext reactContext) {
            super(reactContext);
            setMessagingEnabled(true);
        }

        @Override
        public void onHostResume() {
            // do nothing
        }

        @Override
        public void onHostPause() {
            // do nothing
        }

        @Override
        public void onHostDestroy() {
            cleanupCallbacksAndDestroy();
        }

        public void setInjectedJavaScript(@Nullable String js) {
            injectedJS = js;
        }

        public void setMessagingEnabled(boolean enabled) {
            if (messagingEnabled == enabled) {
                return;
            }

            messagingEnabled = enabled;
            if (enabled) {
                addJavascriptInterface(new ReactWebViewBridge(this), BRIDGE_NAME);
                linkBridge();
            } else {
                removeJavascriptInterface(BRIDGE_NAME);
            }
        }

        public void callInjectedJavaScript() {
            if (getSettings().getJavaScriptEnabled() && injectedJS != null && !TextUtils.isEmpty(injectedJS))
                loadUrl("javascript:(function() {\n" + injectedJS + ";\n})();");
        }

        public void linkBridge() {
            if (messagingEnabled) {
                if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // See isNative in log
                    String testPostMessageNative = "String(window.postMessage) === String(Object.hasOwnProperty).replace('hasOwnProperty', 'postMessage')";
                    evaluateJavascript(testPostMessageNative, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (value.equals("true")) {
                                FLog.w(
                                    ReactConstants.TAG,
                                    "Setting onMessage on a WebView overrides existing values of window.postMessage, but a previous value was defined"
                                );
                            }
                        }
                    });
                }

                loadUrl(
                    "javascript:(" + "window.originalPostMessage = window.postMessage,"
                        + "window.postMessage = function(data) {" + BRIDGE_NAME + ".postMessage(String(data));" + "}"
                        + ")"
                );
            }
        }

        public void onMessage(String message) {
            dispatchEvent(this, new TopMessageEvent(this.getId(), message));
        }

        private void cleanupCallbacksAndDestroy() {
            setWebViewClient(null);
            destroy();
        }
    }

    RNX5WebViewManager(ReactContext reactContext) {

        Log.d("react-native-x5", "init...");

        QbSdk.setTbsListener(new TbsListener() {
            @Override
            public void onDownloadFinish(int i) {
                Log.d("react-native-x5", "onDownloadFinish");
            }

            @Override
            public void onInstallFinish(int i) {
                Log.d("react-native-x5", "onInstallFinish");
            }

            @Override
            public void onDownloadProgress(int i) {
                Log.d("react-native-x5", "onDownloadProgress:" + i);
            }
        });

        QbSdk.initX5Environment(reactContext, new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                Log.d("react-native-x5", " onViewInitFinished is " + arg0);
            }

            @Override
            public void onCoreInitFinished() {
                Log.d("react-native-x5", " onCoreInitFinished ");
            }
        });

        mWebViewConfig = new RNX5WebViewConfig() {
            public void configWebView(WebView webView) {
            }
        };
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        X5WeView webView = new X5WeView(reactContext);

        final X5WebViewModule module = this.aPackage.getModule();

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissionsCallback callback) {
                callback.invoke(origin, true, false);
            }

            // For Android > 4.1.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                module.setUploadMessage(uploadMsg);
                module.openFileChooserView();
            }

            // For Android > 5.0
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                Log.d("customwebview", "onShowFileChooser");

                module.setmUploadCallbackAboveL(filePathCallback);
                if (module.grantFileChooserPermissions()) {
                    module.openFileChooserView();
                } else {
                    // Toast.makeText(module.getActivity().getApplicationContext(),
                    // "Cannot upload files as permission was denied. Please provide permission to
                    // access storage, in order to upload files.",
                    // Toast.LENGTH_LONG).show();
                    Log.d("customwebview", "Cannot upload files as permission was denied");
                }
                return true;
            }
        });

        reactContext.addLifecycleEventListener(webView);
        mWebViewConfig.configWebView(webView);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);

        // Fixes broken full-screen modals/galleries due to body height being 0.
        webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        return webView;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactProp(name = "javaScriptEnabled")
    public void setJavaScriptEnabled(WebView view, boolean enabled) {
        view.getSettings().setJavaScriptEnabled(enabled);
    }

    @ReactProp(name = "scalesPageToFit")
    public void setScalesPageToFit(WebView view, boolean enabled) {
        view.getSettings().setUseWideViewPort(!enabled);
    }

    @ReactProp(name = "domStorageEnabled")
    public void setDomStorageEnabled(WebView view, boolean enabled) {
        view.getSettings().setDomStorageEnabled(enabled);
    }

    @ReactProp(name = "userAgent")
    public void setUserAgent(WebView view, @Nullable String userAgent) {
        if (userAgent != null) {
            view.getSettings().setUserAgentString(userAgent);
        }
    }

    @ReactProp(name = "mediaPlaybackRequiresUserAction")
    public void setMediaPlaybackRequiresUserAction(WebView view, boolean requires) {
        view.getSettings().setMediaPlaybackRequiresUserGesture(requires);
    }

    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(WebView view, boolean allow) {
        view.getSettings().setAllowUniversalAccessFromFileURLs(allow);
    }

    @ReactProp(name = "injectedJavaScript")
    public void setInjectedJavaScript(WebView view, @Nullable String injectedJavaScript) {
        ((X5WeView) view).setInjectedJavaScript(injectedJavaScript);
    }

    // @ReactProp(name = "messagingEnabled")
    // public void setMessagingEnabled(WebView view, boolean enabled) {
    // ((X5WeView) view).setMessagingEnabled(enabled);
    // }

    @ReactProp(name = "source")
    public void setSource(WebView view, @Nullable ReadableMap source) {
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                if (source.hasKey("baseUrl")) {
                    view.loadDataWithBaseURL(source.getString("baseUrl"), html, HTML_MIME_TYPE, HTML_ENCODING, null);
                } else {
                    view.loadData(html, HTML_MIME_TYPE, HTML_ENCODING);
                }
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                String previousUrl = view.getUrl();
                if (previousUrl != null && previousUrl.equals(url)) {
                    return;
                }
                if (source.hasKey("method")) {
                    String method = source.getString("method");
                    if (method.equals(HTTP_METHOD_POST)) {
                        byte[] postData = null;
                        if (source.hasKey("body")) {
                            String body = source.getString("body");
                            try {
                                postData = body.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                postData = body.getBytes();
                            }
                        }
                        if (postData == null) {
                            postData = new byte[0];
                        }
                        view.postUrl(url, postData);
                        return;
                    }
                }
                HashMap<String, String> headerMap = new HashMap<>();
                if (source.hasKey("headers")) {
                    ReadableMap headers = source.getMap("headers");
                    ReadableMapKeySetIterator iter = headers.keySetIterator();
                    while (iter.hasNextKey()) {
                        String key = iter.nextKey();
                        if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
                            if (view.getSettings() != null) {
                                view.getSettings().setUserAgentString(headers.getString(key));
                            }
                        } else {
                            headerMap.put(key, headers.getString(key));
                        }
                    }
                }
                view.loadUrl(url, headerMap);
                return;
            }
        }

        view.loadUrl(BLANK_URL);
    }

    @ReactProp(name = "onContentSizeChange")
    public void setOnContentSizeChange(WebView view, boolean sendContentSizeChangeEvents) {
        if (sendContentSizeChangeEvents) {
            view.setPictureListener(getPictureListener());
        } else {
            view.setPictureListener(null);
        }
    }

    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
        // Do not register default touch emitter and let WebView implementation handle
        // touches
        view.setWebViewClient(new X5WebViewClient());
    }

    @Override
    public @Nullable
    Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("goBack", COMMAND_GO_BACK, "goForward", COMMAND_GO_FORWARD, "reload", COMMAND_RELOAD,
            "stopLoading", COMMAND_STOP_LOADING, "postMessage", COMMAND_POST_MESSAGE, "injectJavascript",
            INJECT_JAVASCRIPT);
    }

    @Override
    public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_GO_BACK:
                root.goBack();
                break;
            case COMMAND_GO_FORWARD:
                root.goForward();
                break;
            case COMMAND_RELOAD:
                root.reload();
                break;
            case COMMAND_STOP_LOADING:
                root.stopLoading();
                break;
            case COMMAND_POST_MESSAGE:
                try {
                    JSONObject eventInitDict = new JSONObject();
                    eventInitDict.put("data", args.getString(0));
                    root.loadUrl("javascript:(document.dispatchEvent(new MessageEvent('message', "
                        + eventInitDict.toString() + ")))");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                break;
            case INJECT_JAVASCRIPT:

                try {
                    JSONObject eventInitDict = new JSONObject();
                    eventInitDict.put("data", args.getString(0));
                    if (root.getSettings().getJavaScriptEnabled() && eventInitDict.toString() != null
                        && !TextUtils.isEmpty(eventInitDict.toString())) {
                        root.loadUrl("javascript:(function() {\n" + args.getString(0) + ";\n})();");
                    }
                    Log.d("react-native-x5", " get inject javascript code: " + args.getString(0));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    @Override
    public void onDropViewInstance(WebView webView) {
        super.onDropViewInstance(webView);
        ((ThemedReactContext) webView.getContext()).removeLifecycleEventListener((X5WeView) webView);
        ((X5WeView) webView).cleanupCallbacksAndDestroy();
    }

    private WebView.PictureListener getPictureListener() {
        if (mPictureListener == null) {
            mPictureListener = new WebView.PictureListener() {
                @Override
                public void onNewPicture(WebView webView, Picture picture) {
                    dispatchEvent(webView, new ContentSizeChangeEvent(webView.getId(), webView.getWidth(),
                        webView.getContentHeight()));
                }
            };
        }
        return mPictureListener;
    }

    private static void dispatchEvent(WebView webView, Event event) {
        ReactContext reactContext = (ReactContext) webView.getContext();
        EventDispatcher eventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
        eventDispatcher.dispatchEvent(event);
    }

    public void setPackage(RNX5WebViewPackage aPackage) {
        this.aPackage = aPackage;
    }

    public RNX5WebViewPackage getPackage() {
        return this.aPackage;
    }
}
