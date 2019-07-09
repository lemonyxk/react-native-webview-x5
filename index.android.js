"use strict";
import React, { cloneElement } from "react";
import { WebView, NativeModules, requireNativeComponent, UIManager, findNodeHandle } from "react-native";
class X5WebView extends WebView {
    static getX5CoreVersion = function(cb) {
        if (cb) {
            NativeModules.X5WebView.getX5CoreVersion(cb);
        }

        return new Promise(resolve => {
            NativeModules.X5WebView.getX5CoreVersion(resolve);
        });
    };

    static install = function() {
        NativeModules.X5WebView.install();
    };

    postMessage = data => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this.refs["webview"]),
            UIManager.getViewManagerConfig("RCTWebView").Commands.postMessage,
            [String(data)]
        );
    };

    injectJavascript = data => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this.refs["webview"]),
            UIManager.getViewManagerConfig("RCTWebView").Commands.injectJavascript,
            [String(data)]
        );
    };

    reload = () => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this.refs["webview"]),
            UIManager.getViewManagerConfig("RCTWebView").Commands.reload,
            null
        );
    };

    goForward = () => {
        UIManager.dispatchViewManagerCommand(findNodeHandle(this.refs["webview"]), UIManager.RCTWebView.Commands.goForward, null);
    };

    goBack = () => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this.refs["webview"]),
            UIManager.getViewManagerConfig("RCTWebView").Commands.goBack,
            null
        );
    };

    render() {
        const wrapper = super.render();
        const [webview, ...children] = wrapper.props.children;
        const X5webview = <RNX5WebView {...webview.props} ref="webview" />;

        return cloneElement(wrapper, wrapper.props, X5webview, ...children);
    }
}

const RNX5WebView = requireNativeComponent("RNX5WebView", X5WebView);

const create = () => {
    return X5WebView;
};

export default create;
