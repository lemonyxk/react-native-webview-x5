function moduleAvailable(name) {
    try {
        require.resolve(name);
        return true;
    } catch (e) {}
    return false;
}

const create = status => {
    if (status) return require("react-native-select-webview").WebView;
    return require("react-native").WebView;
};

export default create;
