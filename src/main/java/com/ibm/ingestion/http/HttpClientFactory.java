package com.ibm.ingestion.http;

//import org.apache.http.impl.client.CloseableHttpClient;
// import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientFactory {

    public static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

//    public static CloseableHttpClient getClientInstance() {
//
//        // TODO(millies): Update to serve up same instanace of httpclient for app
//        // while generating individual httpcontexts for each requesting thread.
//
//        //try {
//            // HACK(Millies): fix when CORE-API is fixed.
////            SSLContextBuilder builder = new SSLContextBuilder();
////            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
//
//            return HttpClientBuilder.create()
//                    //.setSSLContext(builder.build())
////                    .setSSLHostnameVerifier(new HostnameVerifier() {
////                        @Override
////                        public boolean verify(String hostname, SSLSession session) {
////                            // FIX ME WHEN CORE-API has a real certificate.
////                            return true;
////                        }
////                    })
//                    .build();
//
////        } catch (KeyManagementException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        } catch (NoSuchAlgorithmException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        } catch (KeyStoreException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        }
//
//        //return null;
//    }
}
