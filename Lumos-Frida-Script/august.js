Java.perform(function x(){
    console.log("Inside java perform function");    
    
    // Pinner check
    try{
        var CertificatePinner = Java.use("okhttp3.CertificatePinner");
        CertificatePinner.check.overload("java.lang.String", "java.util.List").implementation = function (x,y) {
            console.log("Pinner.check(String, List) : " + x + "###" + y.size());
            return;
            // return this.a.overload("okhttp3.internal.tls.CertificateChainCleaner").call(this,x,y);
        }
    }catch (e) {console.log(e);}

    // Pinner check
    try{
        var CertificatePinner = Java.use("okhttp3.CertificatePinner");
        CertificatePinner.check.overload("java.lang.String", "[Ljava.security.cert.Certificate;").implementation = function (x,y) {
            console.log("Pinner.check(String,Certificate) : " + x);
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

    try
    {
        var SSLContext = Java.use("com.august.luna.network.http.AugustAPIRestAdapter$PubKeyManager");
        SSLContext.checkServerTrusted.overload('[Ljava.security.cert.X509Certificate;', 'java.lang.String').implementation = function(x,y) {
            console.log("checkClientTrusted called : " + y);
            return;
            // return this.checkServerTrusted.overload('[Ljava.security.cert.X509Certificate;', 'java.lang.String').call(this,x,y);
        }
    }catch (e) {console.log(e);}
});