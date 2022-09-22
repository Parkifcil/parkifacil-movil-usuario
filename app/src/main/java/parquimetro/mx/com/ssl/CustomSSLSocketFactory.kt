package parquimetro.mx.com.ssl

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

object CustomSSLSocketFactory {
    private var sslSocketFactory: SSLSocketFactory? = null
    @Throws(CertificateException::class, IOException::class, GeneralSecurityException::class)
    fun getSSLSocketFactory(context: Context): SSLSocketFactory? {
        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        val cf = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = BufferedInputStream(context.assets.open("parkifacil.crt"))
        val ca: Certificate
        try {
            ca = cf.generateCertificate(caInput)
            Log.d("SSL", "ca=" + (ca as X509Certificate).subjectDN)
        } finally {
            caInput.close()
        }

        // Create a KeyStore containing our trusted CAs
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", ca)

        // Create a TrustManager that trusts the CAs in our KeyStore
        val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
        val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
        tmf.init(keyStore)

        // Create an SSLContext that uses our TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)
        sslSocketFactory = sslContext.socketFactory
        return sslSocketFactory
    }
}