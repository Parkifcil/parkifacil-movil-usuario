package parquimetro.mx.com.parkifacil

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_pago.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.tokenModel
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class RecuperaContrasenaActivity : AppCompatActivity() {

    lateinit var edtEmailRecupera:EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_recupera_contrasena)

        edtEmailRecupera = findViewById(R.id.edtEmailRecupera)
        edtEmailRecupera.setSelection(0)
    }

    fun btnRecuperaPassword(view: View){
        if ((!edtEmailRecupera.text.toString().isNullOrEmpty())){
            getPasswordTask(edtEmailRecupera.text.toString()).execute()
        }else{
            Toast.makeText(this,"Error verifique su información y vuelva a intenrarlo!",Toast.LENGTH_LONG).show()
        }
    }

    inner class getPasswordTask(strUser: String) : AsyncTask<String, String, String>() {

        var user = strUser
        var value = ""

        override fun onPreExecute() {
            println("preExecutee" )
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Cuentas/ForgotPassword")

                val urlParameters = "{\"email\":\"$user\"}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection
                //val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "POST"
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false
                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@RecuperaContrasenaActivity))

                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }


                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("jso $inString ")

                val gson = Gson()
                val response: tokenModel = gson.fromJson(inString, object : TypeToken<tokenModel>() {}.type)

                value = response.token!!
            } catch (ex: Exception) {
                println("error AsyncTask recupera pass $ex ")
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }

            return " "
        }

        override fun onProgressUpdate(vararg values: String?) {
            try {

            } catch (ex: Exception) {

            }
        }

        override fun onPostExecute(result: String?) {
            modalResponse(value)
        }


    }


    private fun modalResponse(value: String) {
        try {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

            var txtTitleDialog = mDialogView.findViewById(R.id.txtTitleDialog) as TextView
            var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView
            var btnAceptarDialog  = mDialogView.findViewById(R.id.btnAceptarDialog) as Button
            var btnCancelDialog  = mDialogView.findViewById(R.id.btnCancelDialog) as Button

            btnCancelDialog.visibility = View.INVISIBLE
            txtTitleDialog.text = "Restablecer contraseña"

            if (value.isNullOrEmpty()){
                textTitle.text = " $value Algo salió mal, ingresa correctamente tu usuario ó contraseña y vuelve a intentarlo"
            }else {
                textTitle.text = "El correo se envió exitosamente"
            }

            btnAceptarDialog.text = "Aceptar"

            val mBuilder = AlertDialog.Builder(this)
                    .setView(mDialogView)

            val mAlertDialog = mBuilder.show()

            mDialogView.btnAceptarDialog.setOnClickListener {

                if (value.isNullOrEmpty()){
                    edtEmailRecupera.setText("")
                }else {
                    var intent = Intent(baseContext, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                mAlertDialog.dismiss()
            }

            mDialogView.btnCancelDialog.setOnClickListener {
                mAlertDialog.dismiss()
            }

        }catch (ex:Exception){
            println("error dialog " + ex )
        }

    }

}
