package parquimetro.mx.com.parkifacil

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.confirm_guardado.view.*
import kotlinx.android.synthetic.main.confirm_guardado.view.btnAceptarDialog
import kotlinx.android.synthetic.main.confirm_guardado.view.txtTitle
import kotlinx.android.synthetic.main.confirm_guardado.view.txtTitleDialog
import kotlinx.android.synthetic.main.confirm_pago.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.SessionStorage
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class NuevaTarjetaActivity : AppCompatActivity() {
    lateinit var txtMes : EditText
    lateinit var txtAnio : EditText
    lateinit var txtNoTarjeta : EditText
    lateinit var txtTitular : EditText
    lateinit var btnNuevaTarjeta: Button
    var idTarjeta: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_tarjeta)

        txtMes = findViewById(R.id.txtMes)
        txtAnio = findViewById(R.id.txtAnio)
        txtNoTarjeta = findViewById(R.id.txtNoTarjeta)
        txtTitular = findViewById(R.id.txtTitular)
        btnNuevaTarjeta = findViewById(R.id.btnNuevaTarjeta)

        if (SessionStorage.status==1) {
            val strMes: String
            val strAnio: String
            val strNoTarjeta: String
            val strTitular: String
            val bundle = intent.extras

            if (bundle.isEmpty == false) {
                idTarjeta = bundle.getInt("id")
                strMes = "%02d".format(bundle.getInt("dcmMesVigencia")).toString()
                strAnio = bundle.getInt("dcManoVigencia").toString()
                strNoTarjeta = bundle.getString("strTarjeta")
                strTitular = bundle.getString("strTitular")

                btnNuevaTarjeta.text = "Actualizar tarjeta"
                txtMes.setText(strMes)
                txtAnio.setText(strAnio)
                txtNoTarjeta.setText(maskify(strNoTarjeta))
                txtTitular.setText(strTitular)
            }
        }

        txtNoTarjeta.addTextChangedListener(CreditCardNumberFormattingTextWatcher())

        btnNuevaTarjeta.setOnClickListener{
            btnGuardarTarjeta()
        }
    }

    fun maskify(str: String): String? {
        return str.replace("[0-9](?=.*.{4})".toRegex(), "*")
    }


    fun guardarTarjetaConfirmacion(){
        println(txtNoTarjeta.text.toString().length)
        if (
            txtMes.text.toString().isBlank() ||
            txtAnio.text.toString().isBlank() ||
            txtNoTarjeta.text.toString().isBlank() ||
            txtTitular.text.toString().isBlank()
        ) {
            alertDatosFaltantes()
        } else{
            if(txtMes.text.length < 2){
                alertDatosIncompletos(0)
            } else if(txtAnio.text.length < 2) {
                alertDatosIncompletos(1)
            } else if(txtMes.text.toString().toInt() > 12){
                alertDatosIncompletos(2)
            } else if(txtNoTarjeta.text.toString().length < 19){
                alertDatosIncompletos(3)
            } else {
                agregarTarjeta().execute()
            }
        }
    }

    fun actualizarTarjetaConfirmacion(){
        if (
            txtMes.text.toString().isBlank() ||
            txtAnio.text.toString().isBlank() ||
            txtNoTarjeta.text.toString().isBlank() ||
            txtTitular.text.toString().isBlank()
        ) {
            alertDatosFaltantes()
        } else{
            if(txtMes.text.length < 2 ){
                alertDatosIncompletos(0)
            } else if(txtAnio.text.length < 2) {
                alertDatosIncompletos(1)
            } else if(txtMes.text.toString().toInt() > 12){
                alertDatosIncompletos(2)
            } else if(txtNoTarjeta.text.toString().length < 19){
                alertDatosIncompletos(3)
            } else {
                modificarTarjeta().execute()
            }
        }
    }

    fun btnGuardarTarjeta(){
        if (SessionStorage.status==1){
            actualizarTarjetaConfirmacion()
        }else {
            guardarTarjetaConfirmacion()
        }

    }

    inner class CreditCardNumberFormattingTextWatcher : TextWatcher {
        private var current = ""
        private val nonDigits = Regex("[^\\d]")

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (s.toString() != current) {
                val userInput = s.toString().replace(nonDigits, "")
                if (userInput.length <= 16) {
                    current = userInput.chunked(4).joinToString(" ")
                    s.filters = arrayOfNulls<InputFilter>(0)
                }
                s.replace(0, s.length, current, 0, current.length)
            }
        }
    }

    inner class agregarTarjeta : AsyncTask<String, String, String>() {

        var inString = ""

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Tarjetas/mtdIngresarTarjeta")

                val urlParameters = "{" +
                        "\"dcManoVigencia\":\"${txtAnio.text}\"," +
                        "\"dcmMesVigencia\":\"${txtMes.text}\"," +
                        "\"strTarjeta\":\"${txtNoTarjeta.text}\"," +
                        "\"strTitular\":\"${txtTitular.text}\"," +
                        "\"intIdUsuarioId\":\"${SessionStorage.idUser}\"," +
                        "}"

                println("parametros ${urlParameters}")

                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "POST"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@NuevaTarjetaActivity));

                DataOutputStream(urlConnect.outputStream).use { wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("agregar tarjeta " + urlConnect.responseCode + " " + inString)

            } catch (ex: Exception) {
                println("error AsyncTask add tarjeta $ex ")
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
            println("onPostExecute $result")
            // Done
            if(inString.isNullOrEmpty()) {
                alertPostRequest(1)
            } else {
                if ((inString).contains("El registro que intenta gurarda o actualizar ya se encuentra en la BD, verifique.")) {
                    alertPostRequest(4)
                } else {
                    alertPostRequest(0)
                }
            }
        }
    }

    inner class modificarTarjeta : AsyncTask<String, String, String>() {

        var inString=""

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Tarjetas/mtdActualizaTarjeta?id=" + idTarjeta)

                val urlParameters = "{" +
                        "\"dcManoVigencia\":\"${txtAnio.text}\"," +
                        "\"dcmMesVigencia\":\"${txtMes.text}\"," +
                        "\"strTarjeta\":\"${txtNoTarjeta.text}\"," +
                        "\"strTitular\":\"${txtTitular.text}\"," +
                        "\"intIdUsuarioId\":\"${SessionStorage.idUser}\"," +
                        "}"

                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "PUT"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@NuevaTarjetaActivity));

                DataOutputStream(urlConnect.outputStream).use { wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("update vehiculo: " + urlConnect.responseCode + " " + inString)

            } catch (ex: Exception) {
                println("error AsyncTask updatetarjeta $ex ")
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
            println("onPostExecute $result")
            // Done

            if(inString.isNullOrEmpty()) {
                alertPostRequest(1)
            }else{
                alertPostRequest(3)
            }
        }


    }

    private fun alertPostRequest(value: Int) {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)
        if (value==0){
            mDialogView.txtTitleDialog.text = "Éxito al guardar la tarjeta"
            mDialogView.txtTitle.text = "Tarjeta agregada con éxito"
        }

        if (value==1){
            mDialogView.txtTitleDialog.text = "Error al guardar la tarjeta"
            mDialogView.txtTitle.text = "La tarjeta no se guardó correctamente "
        }

        if (value==3){
            mDialogView.txtTitleDialog.text = "Actualizar datos"
            mDialogView.txtTitle.text = "Los datos se actualizaron correctamente "
        }

        if (value == 4){
            mDialogView.txtTitleDialog.text = "Error al guardar la tarjeta"
            mDialogView.txtTitle.text = "La tarjeta ya se encuentra registrada"
        }

        val mBuilder = AlertDialog.Builder(this).setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            if(SessionStorage.status == 10) {
                startActivity(Intent(baseContext, AgregarSaldoActivity::class.java))
                finish()
            } else if(SessionStorage.status == 12) {
                startActivity(Intent(baseContext, MultaActivity::class.java))
                finish()
            } else {
                SessionStorage.status = 0
                var intent= Intent(baseContext, TarjetaActivity::class.java)
                startActivity(intent)
                finish()
                mAlertDialog.dismiss()
            }
        }

    }

    private fun alertDatosIncompletos(value: Int) {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)

        mDialogView.txtTitleDialog.text = "Error"
        mDialogView.txtTitle.text = "Ingrese todos los datos"

        if(value==0) {
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "Debe de ingresar dos dígitos en el mes"
        }

        if(value==1) {
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "Debe de ingresar dos dígitos en el año"
        }

        if(value==2) {
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "No puede ingresar un número de mes mayor a 12"
        }

        if(value==3) {
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "Debe de ingresar dieciséis dígitos en el número de tarjeta"
        }

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    private fun alertDatosFaltantes() {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)

        mDialogView.txtTitleDialog.text = "Error"
        mDialogView.txtTitle.text = "Ingrese todos los datos"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            SessionStorage.status = 0
            mAlertDialog.dismiss()
        }

    }


}