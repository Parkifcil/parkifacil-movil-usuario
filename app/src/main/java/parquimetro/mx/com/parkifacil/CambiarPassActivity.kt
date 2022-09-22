package parquimetro.mx.com.parkifacil

import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_error.view.*
import kotlinx.android.synthetic.main.confirm_pago.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.SessionStorage
import parquimetro.mx.com.models.tokenModel
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class CambiarPassActivity : AppCompatActivity() {

    lateinit var  edtPasswordLayout: TextInputLayout
    lateinit var edtConfirmPasswordLayout: TextInputLayout

    var password=""
    var confirmPassword=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_pass)

        edtPasswordLayout = this.findViewById(R.id.input_layout_password)
        edtConfirmPasswordLayout = findViewById(R.id.input_layout_confirm_password)
    }

    fun btnNewPass(view:View){
        password = edtPasswordLayout.editText?.text.toString()
        confirmPassword = edtConfirmPasswordLayout.editText?.text.toString()

        if ((password.equals(confirmPassword))&&(!password.isNullOrEmpty())&&(!confirmPassword.isNullOrEmpty())){
            modalResponse()
        }else{
            alert("Contraseña invalida, verifique sus datos y vuela a intentarlo")
        }
    }

    inner class changePassTask : AsyncTask<String, String, String>() {
        var inString=""
        var valResponse = true
        var strResponse = ""

        override fun onPreExecute() {
            println("preExecutee" )
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Cuentas/CambiarPassword?id="+ SessionStorage.idUser)

                val urlParameters = "{"+
                        "\"Password\":\"$password\""+
                        "}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                println("json_cambiar pass" + urlParameters)

                val urlConnect=mURL.openConnection() as HttpsURLConnection
                //val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "PUT"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false
                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@CambiarPassActivity))

                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("response cambiar contra "+ urlConnect.responseCode + " "+ inString )
                val gson = Gson()
                val userToken: tokenModel = gson.fromJson(inString, object : TypeToken<tokenModel>() {}.type)

                try {
                    if (!userToken.token!!.isNullOrEmpty()) {
                        strResponse = userToken.token!!
                        valResponse = false
                    }
                }catch (ex:Exception){
                    print(ex)
                }
            } catch (ex: Exception) {
                println("error AsyncTask change pass $ex ")
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
            if (valResponse){
                modalResponseExit()
                val prefs = PreferenceManager.getDefaultSharedPreferences(this@CambiarPassActivity)
                prefs.edit().putString("password", password)
                prefs.edit().apply()
                modalResponseExit()
            }else{
                alert(strResponse)
            }
        }


    }

    private fun alert(data:String) {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_error, null)

        mDialogView.txtTitleDialogError.text = "Cambiar contraseña"
        mDialogView.txtTitleError.text = "$data"


        val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialogError.setOnClickListener {
            mAlertDialog.dismiss()
        }
    }

    private fun modalResponse() {
        try {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

            var txtTitleDialog = mDialogView.findViewById(R.id.txtTitleDialog) as TextView

            var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView

            var btnAceptarDialog  = mDialogView.findViewById(R.id.btnAceptarDialog) as Button
            var btnCancelDialog  = mDialogView.findViewById(R.id.btnCancelDialog) as Button

            txtTitleDialog.text = "Cambiar contraseña"

            textTitle.text = "¿Seguro que desea cambiar la contraseña?"
            btnAceptarDialog.text = "Aceptar"

            val mBuilder = AlertDialog.Builder(this)
                    .setView(mDialogView)

            val mAlertDialog = mBuilder.show()

            mDialogView.btnAceptarDialog.setOnClickListener {
                changePassTask().execute()
                mAlertDialog.dismiss()
            }

            mDialogView.btnCancelDialog.setOnClickListener {
                mAlertDialog.dismiss()
            }

        }catch (ex:Exception){
            println("error dialog " + ex )
        }

    }


    private fun modalResponseExit() {
        try {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

            var txtTitleDialog = mDialogView.findViewById(R.id.txtTitleDialog) as TextView
            var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView
            var btnAceptarDialog  = mDialogView.findViewById(R.id.btnAceptarDialog) as Button
            var btnCancelDialog  = mDialogView.findViewById(R.id.btnCancelDialog) as Button

            btnCancelDialog.visibility = View.INVISIBLE
            txtTitleDialog.text = "Cambiar contraseña"
            textTitle.text = "Cambio de contraseña exitoso"
            btnAceptarDialog.text = "Aceptar"

            val mBuilder = AlertDialog.Builder(this)
                    .setView(mDialogView)

            val mAlertDialog = mBuilder.show()

            mDialogView.btnAceptarDialog.setOnClickListener {
                var intent= Intent(baseContext, MainActivity::class.java)
                startActivity(intent)
                finish()
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
