package nl.topicuszorg.databroker.lrzawebservice.service

import okhttp3.OkHttpClient
import org.springframework.core.io.Resource
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object KeystoreService
{
    fun createMtlsHttpClient(keystoreFile: Resource, keystorePassword: String, keystoreType: String = "pkcs12"): OkHttpClient
    {
        val keystore = loadKeystore(keystoreFile, keystorePassword, keystoreType)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keystore, keystorePassword.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?) // Use default JVM trust store

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(keyManagerFactory.keyManagers, null, null)

        val trustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    private fun loadKeystore(keystoreFile: Resource, keystorePassword: String, keystoreType: String = "pkcs12"): KeyStore
    {
        val keystore = KeyStore.getInstance(keystoreType)
        keystore.load(keystoreFile.inputStream, keystorePassword.toCharArray())
        return keystore
    }
}