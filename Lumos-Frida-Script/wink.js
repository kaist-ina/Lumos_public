Java.perform(function x(){
    console.log("Inside java perform function");    
    
    // Pinner check
    try{
        var CertificatePinner = Java.use("okhttp3.g");
        CertificatePinner.a.overload("java.lang.String", "java.util.List").implementation = function (x,y) {
            console.log("Pinner.check(String, List) : " + x + "###" + y.size());
            return;
            // return this.a.overload("okhttp3.internal.tls.CertificateChainCleaner").call(this,x,y);
        }
    }catch (e) {console.log(e);}

    try
    {
        var my_class2 = Java.use("java.net.URL");
        my_class2.$init.overload("java.lang.String").implementation = function(x) {
            // console.log("URL init: " + x);
            return this.$init(this, x);
        }
    } catch (e) {console.log(e)}

    // try
    // {
    //     var RetrofitBuilder = Java.use("retrofit2.Retrofit$Builder");
    //     RetrofitBuilder.baseUrl.overload("java.lang.String").implementation = function(x) {
    //         console.log("Retrofit BaseURL init: " + x);
    //         console.log("Relace : " + x.replace("https" , "http"));
    //         return this.baseUrl.overload("java.lang.String").call(this, x.replace("https" , "http"));
    //     }
    // } catch (e) {console.log(e)}
    try
    {
        var RetrofitBuilder = Java.use("com.quirky.android.wink.api.s");
        RetrofitBuilder.checkServerTrusted.overload("[Ljava.security.cert.X509Certificate;", "java.lang.String").implementation = function(x,y) {
            console.log("com.quirky.android.wink.api.s: checkServerTrusted");
            return;
        }
    } catch (e) {console.log(e)}

    try
    {
        var RetrofitBuilder = Java.use("io.fabric.sdk.android.services.network.f");
        RetrofitBuilder.checkServerTrusted.overload("[Ljava.security.cert.X509Certificate;", "java.lang.String").implementation = function(x,y) {
            console.log("io.fabric.sdk.android.services.network.f: checkServerTrusted");
            return;
        }
    } catch (e) {console.log(e)}

    try
    {
        var RetrofitBuilder = Java.use("com.electricimp.blinkup.PinningTrustManager");
        RetrofitBuilder.checkServerTrusted.overload("[Ljava.security.cert.X509Certificate;", "java.lang.String").implementation = function(x,y) {
            console.log("com.electricimp.blinkup.PinningTrustManager: checkServerTrusted");
            return;
        }
    } catch (e) {console.log(e)}
});