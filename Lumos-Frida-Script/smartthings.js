Java.perform(function x(){
    console.log("Inside java perform function");    

    try{
        var CertificatePinner = Java.use("okhttp3.CertificatePinner$Pin");
        CertificatePinner.a.overload("java.lang.String").implementation = function (x) {
            console.log("Pinner$Pin a(String) : " + x);
            return this.$init.overload("java.lang.String").call(this,x);
        }
    }catch (e) {console.log(e);}

    // Pinner check
    try{
        var CertificatePinner = Java.use("okhttp3.CertificatePinner");
        CertificatePinner.a.overload("java.lang.String", "java.util.List").implementation = function (x,y) {
            console.log("CertificateChainCleaner a(okhttp3.internal.Cleaner) : " + x + "###" + y.size());
            return;
            // return this.a.overload("okhttp3.internal.tls.CertificateChainCleaner").call(this,x,y);
        }
    }catch (e) {console.log(e);}

    //SmartThings baseURL set
    try
    {
        var Retrofit = Java.use("retrofit2.Retrofit$Builder");
        Retrofit.a.overload("java.lang.String").implementation = function(x) {
            console.log("BaseURL : " + x);
            return this.a.overload("java.lang.String").call(this,x);
        }
        var RetrofitRB = Java.use("retrofit2.RequestBuilder");
        RetrofitRB.$init.overload("java.lang.String", "okhttp3.HttpUrl", "java.lang.String", "okhttp3.Headers", "okhttp3.MediaType", "boolean", "boolean", "boolean")
        .implementation = function(a,b,c,d,e,f,g,h) {
            console.log("Retrofit Request builder : " + b + c);
            return this.$init.overload("java.lang.String", "okhttp3.HttpUrl", "java.lang.String", "okhttp3.Headers", "okhttp3.MediaType", "boolean", "boolean", "boolean").call(this,a,b,c,d,e,f,g,h);
        }
    } catch(e) {console.log(e)}
    
    try
    {
        // Retrofit builder
        var RealSmartKit = Java.use("smartkit.RealSmartKit");
        RealSmartKit.createRetrofit.overload("smartkit.TokenManager", "smartkit.Endpoint", "smartkit.RealSmartKit$RetrofitType").implementation = function(x,y,z) {
            // console.log("RealSmartKit createRetrofit : " + y.getBaseUrl());
            return this.createRetrofit.overload("smartkit.TokenManager", "smartkit.Endpoint", "smartkit.RealSmartKit$RetrofitType").call(this, x,y,z);
        }
        RealSmartKit.getDeviceEvents.overload("java.lang.String", "java.lang.String", "org.joda.time.DateTime", "boolean").implementation = function(a,b,c,d) {
            console.log("getDeviceEvents(4) : " + a);
            return this.getDeviceEvents.overload("java.lang.String", "java.lang.String", "org.joda.time.DateTime", "boolean").call(this, a,b,c,d);
        }

        RealSmartKit.getDeviceEvents.overload("java.lang.String", "org.joda.time.DateTime", "boolean").implementation = function(a,b,c) {
            console.log("getDeviceEvents(3) : " + a);
            return this.getDeviceEvents.overload("java.lang.String", "org.joda.time.DateTime", "boolean").call(this, a,b,c);
        }
    }
    catch(e) {
        console.log(e);
    }

    //okhttp3.OkHttpClient
    try{
        OkHttpClient = Java.use("okhttp3.OkHttpClient");
        OkHttpClient.$init.overload().ismplementation = function() {
            console.log("Call a(okhttp3.Request)");
            return OkHttpClient.$init.overload().call();
        }
    } catch (e) {console.log(e);}

    try
    {
        var my_class2 = Java.use("java.net.URL");
        my_class2.$init.overload("java.lang.String").implementation = function(x) {
            // console.log("URL init: " + x);
            return this.$init(this, x);
        }
    } catch (e) {console.log(e)}
});