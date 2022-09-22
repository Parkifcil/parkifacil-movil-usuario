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
import kotlinx.android.synthetic.main.confirm_guardado.view.*
import kotlinx.android.synthetic.main.content_new_placa.*
import parquimetro.mx.com.api.ApiConfig.Companion.ConvertStreamToString
import parquimetro.mx.com.models.SessionStorage
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class NewPlacaActivity : AppCompatActivity() {

    lateinit var edtPlaca:EditText
    lateinit var edtModelo:EditText
    lateinit var edtColor:EditText
    lateinit var btnNewPlaca :Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_placa)

        edtPlaca = findViewById(R.id.edtNoPlaca)
        edtModelo = findViewById(R.id.edtModelo)
        edtColor = findViewById(R.id.edtColor)
        btnNewPlaca = findViewById(R.id.btnNewPlaca)

        if (SessionStorage.status==1) {
            val strModelo: String
            val strColor: String
            val bundle = intent.extras
            if (bundle != null) {
                strModelo = bundle.getString("strModelo")
                strColor = bundle.getString("strColor")

                btnNewPlaca.text = "Actualizar placa"
                edtPlaca.setText(SessionStorage.strPlaca)
                edtColor.setText(strColor)
                edtModelo.setText(strModelo)
            }
        }
    }

    fun savePlacaConfirm(){
        if (edtPlaca.text.toString() == "") {
            alert2(1, "")
        } else if (edtModelo.text.toString() == "") {
            alert2(1, "")
        }else if (edtColor.text.toString() == ""){
            alert2(1, "")
        }else{
            savePlacaTask().execute()
        }
    }

    fun updatePlacaConfirm(){
        if (edtPlaca.text.toString() == "") {
            alert3(1, "")
        } else if (edtModelo.text.toString() == "") {
            //  savePlacaTask().execute()
            alert3(1, "")
        }else if (edtColor.text.toString() == ""){
            alert3(1, "")
        }else{
            updatePlacaTask().execute()
        }
    }

    fun btnSavePlaca(view: View){
        if (SessionStorage.status==1){
            updatePlacaConfirm()
        }else {
            savePlacaConfirm()
        }

    }


    inner class savePlacaTask : AsyncTask<String, String, String>() {
        var inString=""

        override fun onPreExecute() {}

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Vehiculos/mtdIngresarVehiculo")

                var placaNew = edtNoPlaca.text
                var colorNew = edtColor.text
                var modeloNew = edtModelo.text

                val urlParameters = "{" +
                        "\"CreatedBy\":\"Jr\"," +
                        "\"LastModifiedBy\":\"android\"," +
                        "\"BitStatus\":true," +
                        "\"StrColor\":\"$colorNew\","+
                        "\"StrModelo\":\"$modeloNew\","+
                        "\"StrPlacas\":\"$placaNew\","+
                        "\"IntIdUsuarioId\":\"${SessionStorage.idUser}\""+

                        "}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "POST"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@NewPlacaActivity));


                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ConvertStreamToString(urlConnect.inputStream)

                println("jso response:"+ urlConnect.responseCode + " inString: "+ inString )
                println("inString:"+inString)

                SessionStorage.placaConfirm = inString
                SessionStorage.dataPlaca = placaNew.toString()
                SessionStorage.dataColor = colorNew.toString()
                SessionStorage.dataModelo = modeloNew.toString()
            } catch (ex: Exception) {
                println("error AsyncTask newPlaca $ex ")
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }

            return " "
        }

        override fun onProgressUpdate(vararg values: String?) {
            try {
                //println("ProgressUpdate"+json)
            } catch (ex: Exception) {

            }
        }

        override fun onPostExecute(result: String?) {

            println("dataId: "+SessionStorage.dataId)

            if(inString.isNullOrEmpty()) {
                println("entro 1 ")
                alert(1,"")

            }else{

                if ((SessionStorage.placaConfirm).contains("El registro que intenta gurarda o actualizar ya se encuentra en la BD, verifique.")) {
                    alert(5, inString)
                }else{
                    alert(0,inString)
                }
            }
        }
    }


    inner class updatePlacaTask : AsyncTask<String, String, String>() {
        var inString=""

        override fun onPreExecute() {}

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Vehiculos/mtdActualizaVehiculo?id="+ SessionStorage.idPlaca)

                var placaNew = edtNoPlaca.text
                var colorNew = edtColor.text
                var modeloNew = edtModelo.text

                val urlParameters = "{" +
                        "\"CreatedBy\":\"Jr\"," +
                        "\"LastModifiedBy\":\"android\"," +
                        "\"BitStatus\":true," +
                        "\"StrColor\":\"$colorNew\","+
                        "\"StrModelo\":\"$modeloNew\","+
                        "\"StrPlacas\":\"$placaNew \","+
                        "\"IntIdUsuarioId\":\"${SessionStorage.idUser}\""+
                        "}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "PUT"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@NewPlacaActivity));

                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ConvertStreamToString(urlConnect.inputStream)
                println("update vehiculo: "+ urlConnect.responseCode + " "+ inString )
            } catch (ex: Exception) {
                println("error AsyncTask updatePlaca $ex ")
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

            if(inString.isNullOrEmpty()) {
                println("entro 0 ")
                alert(1,"")
            }else{
                alert(3,"")
            }
        }
    }




    private fun alert(value:Int,error:String){

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)

        if (value==0){
            mDialogView.txtTitle.text = "Placa agregada con éxito"
        }
        if (value==1){
            mDialogView.txtTitleDialog.text = "Error al guardar placa"
            mDialogView.txtTitle.text = "Error " + error
        }
        if (value==3){

            mDialogView.txtTitleDialog.text = "Actualizar datos"
            mDialogView.txtTitle.text = "Los datos se actualizarón correctamente "
        }
        if (value==4){

            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "ingrese todos los datos"
        }
        if (value == 5){
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "La placa ya se encuentra registrada"
        }

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            SessionStorage.status = 0
            var intent= Intent(baseContext, PlacasActivity::class.java)
            startActivity(intent)
            finish()

            mAlertDialog.dismiss()


        }

    }

    private fun alert2(value:Int,error:String){

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)
        if (value==1){
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "ingrese todos los datos"
        }

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            SessionStorage.status = 0
            mAlertDialog.dismiss()
        }
    }

    private fun alert3(value:Int,error:String){

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_guardado, null)
        if (value==1){
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "ingrese todos los datos"
        }
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()
        mDialogView.btnAceptarDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

}
