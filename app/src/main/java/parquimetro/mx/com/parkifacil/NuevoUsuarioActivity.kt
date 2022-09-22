package parquimetro.mx.com.parkifacil

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_guardado.view.*
import kotlinx.android.synthetic.main.content_nuevo_usuario.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.tokenModel
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection


class NuevoUsuarioActivity : AppCompatActivity() {


    lateinit var checkboxAcepto:CheckBox
    lateinit var btnNuevo:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_usuario)

        checkboxAcepto = findViewById(R.id.checkboxAcepto)
        btnNuevo = findViewById(R.id.btnNewUser)

        checkboxAcepto.setOnCheckedChangeListener { _, isChecked ->
            // do whatever you need to do when the switch is toggled here
        }

    }


    fun btnNewUser(view: View){
        var email = edtCorreoElectronico.text.toString()
        var name = edtNombre.text.toString()
        var pass = edtPassword.text.toString()

        var value = isValidEmail(email)

        if((value)&&(!name.isNullOrEmpty())&&(!pass.isNullOrEmpty())){

            if (checkboxAcepto.isChecked==true){
                if ((pass.contains(" "))){
                    Toast.makeText(
                        this,
                        "La contraseña no debe contener espacios",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (email.contains(" ")){
                    Toast.makeText(this, "El correo no debe contener espacios.", Toast.LENGTH_LONG).show()
                } else {
                    UserNewTask(
                        edtNombre.text.toString(),
                        edtCorreoElectronico.text.toString(),
                        edtPassword.text.toString()
                    ).execute()
                }
            } else{
                Toast.makeText(
                    this,
                    "Acepte los terminos y condiciones y vuelva a intentarlo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }else{
            Toast.makeText(
                this,
                "Error... verifique sus datos y vuelva a intentarlo",
                Toast.LENGTH_LONG
            ).show()
        }
    }



    inner class UserNewTask(strName: String, str_Email: String, strPassword: String) : AsyncTask<String, String, String>() {
        var strNombreUser = strName
        var strEmail = str_Email
        var strPass = strPassword
        var inString =""
        var msg = ""

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Cuentas/CrearUsuarioMovil")

                val urlParameters = "{\"UserName\":\"$strNombreUser\"," +
                        "\"Email\":\"$strEmail\"," +
                        "\"created_by\":\"android\"," +
                        "\"Password\":\"$strPass\"," +
                        "\"Rol\":\"MOVIL\"}"

                println("parametros json" + urlParameters)

                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@NuevoUsuarioActivity));

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "POST"
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                DataOutputStream(urlConnect.outputStream).use { wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("jso new user $inString ")

                val gson = Gson()
                val userToken: tokenModel = gson.fromJson(
                    inString,
                    object : TypeToken<tokenModel>() {}.type
                )
                msg = userToken.token!!

                println("mensaje: " + msg)

            } catch (ex: Exception) {
                println("error AsyncTask new user $ex")
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
            if (msg.contains("El correo o usuario ya se encuentra registrado")){
                Toast.makeText(
                    applicationContext,
                    "El correo o usuario ya se encuentra registrado.",
                    Toast.LENGTH_LONG
                ).show()

            }else if(msg.contains("Username or password invalid")){
                Toast.makeText(
                    applicationContext,
                    "Usuario o contraseña invalida.",
                    Toast.LENGTH_LONG
                ).show()

            }else {
                alert()
            }
        }


    }

    private fun alert() {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)

        mDialogView.txtTitleDialog.text = "Registro"
        mDialogView.txtTitle.text = "El registro se realizó con éxito, recibirá un correo electrónico para validar su cuenta. "

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            var intent= Intent(baseContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
            mAlertDialog.dismiss()
        }

    }

    fun isValidEmail(target: CharSequence):Boolean {
        if (target == null)
            return false
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }


    fun txtTerminos(view: View){
        //var intent= Intent(baseContext, Terminos::class.java)
        //startActivity(intent)
        try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://admin.parkifacil.com/api/api/Cuentas/TerminosCondiciones"))
            startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this, "Ninguna aplicación puede manejar esta solicitud."
                        + "Para abrirla instale un navegador web.", Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}