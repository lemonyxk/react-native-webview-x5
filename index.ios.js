function moduleAvailable(name) {
    try {
        require.resolve(name);
        return true;
    } catch (e) {}
    return false;
}

let WebView = null;

if (moduleAvailable("react-native-webview")) {
    WebView = require("react-native-webview").WebView;
} else {
    WebView = require("react-native").WebView;
}

export default WebView;
