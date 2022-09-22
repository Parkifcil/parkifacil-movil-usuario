package parquimetro.mx.com.parkifacil

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TextInputLayout
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_error.view.*
import kotlinx.android.synthetic.main.content_login.*
import parquimetro.mx.com.api.ApiConfig.Companion.ConvertStreamToString
import parquimetro.mx.com.models.*
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection


class LoginActivity : AppCompatActivity() {

    lateinit var viewDialog: ViewDialog
    lateinit var edtUserdInputLayout: TextInputLayout
    lateinit var edtPassworddInputLayout: TextInputLayout
    var user: String = ""
    var password: String = ""

    private val STORAGE_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        isStoragePermissionGranted()

        viewDialog = ViewDialog(this)

        edtUserdInputLayout = this.findViewById(R.id.input_layout_user)
        edtPassworddInputLayout = this.findViewById(R.id.input_layout_password)

        if (NetworkHelper.isNetworkConnected(this)){
        }else{
            alertConexion()
        }
        checkLogin()
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                println("Permission is granted")
                true
            } else {
                println("Permission is revoked")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
                false
            }
        } else {
            println("Permission is granted")
            true
        }
    }

    private fun recordarUsuario(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val usuario = edtUserdInputLayout.editText?.text.toString()
        val password = edtPassworddInputLayout.editText?.text.toString()
        val checkBox = checkBox

        if (checkBox.isChecked){
            val editor = prefs.edit()
            editor.putString("usuario", usuario)
            editor.putString("password", password)
            editor.putString("active", "true")
            editor.putString("remember", "true")
            editor.apply()
        }else {
            val editor = prefs.edit()
            editor.remove("usuario")
            editor.remove("password")
            editor.putString("active", "true")
            editor.putString("remember", "false")
            editor.apply()
            println("active: true, remember:false")
            checkBox.isChecked = false
        }
    }

    fun checkLogin(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if(prefs.getString("remember", "")=="true"){
            user = prefs.getString("usuario", "")
            password = prefs.getString("password", "")
            viewDialog.showDialog()
            getUserTask().execute()
            startActivity(Intent(baseContext, MainActivity::class.java))
        } else{
            edtUserdInputLayout.editText?.setText("")
            edtPassworddInputLayout.editText?.setText("")
            checkBox.isChecked = false
        }
    }

    fun btnLoign(view: View){
        if(edtUser.text!!.isEmpty()){
            Toast.makeText(applicationContext, "Usuario o correo electrónico requerido", Toast.LENGTH_LONG).show()
        } else if(edtPassword.text!!.isEmpty()){
            Toast.makeText(applicationContext, "Contraseña requerida", Toast.LENGTH_LONG).show()
        } else {
            user = edtUser.text.toString()
            password = edtPassword.text.toString()
            recordarUsuario()
            viewDialog.showDialog()
            getUserTask().execute()
        }
    }

    fun txtLink(view: View){
        var intent= Intent(baseContext, RecuperaContrasenaActivity::class.java)
        startActivity(intent)
    }

    fun txtnewUser(view: View){
        var intent= Intent(baseContext, NuevoUsuarioActivity::class.java)
        startActivity(intent)
    }


    inner class getUserTask : AsyncTask<String, String, String>() {

        var dataResult=false

        override fun onPreExecute() {

            println("preExecutee")
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Cuentas/Login?strCliente=ANDROID")
                //val mURL = URL("http://74.208.91.19:9000/api/Cuentas/Login?strCliente=ANDROID")

                val urlParameters = "{\"UserName\":\"$user\",\"Password\":\"$password\"}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
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

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@LoginActivity))

                DataOutputStream(urlConnect.outputStream).use {
                        wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                var inString = ConvertStreamToString(urlConnect.inputStream)

                SessionStorage.authConfirm = inString

                println("jso $inString " + urlConnect.responseCode)

                val gson = Gson()

                val userToken: tokenModel = gson.fromJson(
                    inString,
                    object : TypeToken<tokenModel>() {}.type
                )

                try {
                    if (!userToken.id!!.isNullOrEmpty()){
                        dataResult =true

                        viewDialog.hideDialog()
                        SessionStorage.tokenSession = userToken.token!!
                        SessionStorage.idUser = userToken.id!!
                        SessionStorage.strNombreUsuario = userToken.strNombreUsuario!!
                        var intent = Intent(baseContext, MainActivity::class.java)
                        startActivity(intent)

                    }
                }catch (ex: Exception){
                    println("userToken:" + userToken)
                    viewDialog.hideDialog()
                    dataResult = false
                    println("error login:" + ex + " " + dataResult)
                    alertAutenticacionIncorrecta()
                }
            } catch (ex: Exception) {
                println("error AsyncTask login $ex ")
                viewDialog.hideDialog()
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }

            return " "
        }



        override fun onProgressUpdate(vararg values: String?) {

        }

        override fun onPostExecute(result: String?) {
            if (NetworkHelper.isNetworkConnected(this@LoginActivity)){
                try {
                    println(" result $dataResult")
                    if(!dataResult){
                        if (SessionStorage.authConfirm == "{\"token\":\"Correo electrónico no confirmado\"}"){
                            alertAutenticacionFaltante()
                        }else{
                            alertAutenticacionIncorrecta()
                        }
                    }
                } catch (ex: Exception) {
                    println(ex)
                }
            }else{
                alertConexion()
            }
        }
    }

    private fun alertAutenticacionIncorrecta(){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_error, null)

        mDialogView.txtTitleDialogError.text = "Autenticación incorrecta"
        mDialogView.txtTitleError.text = "Usuario ó contraseña incorrecta, Verifique sus datos y vuela a intentarlo"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
        val  mAlertDialog = mBuilder.show()
        mDialogView.btnAceptarDialogError.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    private fun alertConexion(){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_error, null)

        mDialogView.txtTitleDialogError.text = "Sin conexión"
        mDialogView.txtTitleError.text = "Compruebe su conexión a internet para ingresar"


        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialogError.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    private fun alertAutenticacionFaltante(){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_error, null)

        mDialogView.txtTitleDialogError.text = "Error"
        mDialogView.txtTitleError.text = "La cuenta no ha sido autenticada"


        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialogError.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

}
