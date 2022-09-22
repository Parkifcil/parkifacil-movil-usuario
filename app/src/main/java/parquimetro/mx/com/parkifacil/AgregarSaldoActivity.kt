package parquimetro.mx.com.parkifacil

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_pago.view.btnAceptarDialog
import kotlinx.android.synthetic.main.confirm_pago.view.btnCancelDialog
import kotlinx.android.synthetic.main.confirm_pago.view.txtTitle
import kotlinx.android.synthetic.main.confirm_pago.view.txtTitleDialog
import kotlinx.android.synthetic.main.tarjetas_radiobutton_row.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.*
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList


class AgregarSaldoActivity : AppCompatActivity() {

    lateinit var spnMonto: Spinner
    lateinit var txtSaldo: TextView
    lateinit var txtSubtotal: TextView
    lateinit var txtIVA: TextView
    lateinit var txtComision: TextView
    lateinit var txtTotal: TextView
    lateinit var mlistView: ListView
    lateinit var noHayTarjeta: TextView
    lateinit var txtAgregarTarjeta: TextView

    var lstTarjeta = ArrayList<Tarjeta>()
    var itemValue =""
    var total: Double? = null
    var monto: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_saldo)

        spnMonto = findViewById(R.id.spnMonto)
        txtSaldo = findViewById(R.id.txtSaldo)
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtIVA = findViewById(R.id.txtIVA)
        txtComision = findViewById(R.id.txtComision)
        txtTotal = findViewById(R.id.txtTotal)
        mlistView = findViewById<ListView>(R.id.listTarjetas) as NonScrollListView
        noHayTarjeta = findViewById(R.id.noHayTarjeta)
        txtAgregarTarjeta = findViewById(R.id.txtAgregarTarjeta)

        val lstSaldo = resources.getStringArray(R.array.lst_saldo)

        getSaldo().execute()
        getTarjetas().execute()

        if (spnMonto != null) {
            val adapter = ArrayAdapter(
                this,
                R.layout.spinner_item_saldo, lstSaldo
            )
            spnMonto.adapter = adapter

            spnMonto.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View, position: Int, id: Long
                ) {
                    itemValue = lstSaldo[position]
                    getComision().execute()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

        txtAgregarTarjeta.setOnClickListener {
            SessionStorage.status = 10
            startActivity(Intent(baseContext, NuevaTarjetaActivity::class.java))
            finish()
        }

    }

    override fun onRestart() {
        super.onRestart()
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun callAdpater() {
        var myAdapter = MyAdpater(this, lstTarjeta)
        mlistView.adapter = myAdapter
    }

    inner class MyAdpater : BaseAdapter {
        var listNotesAdpater = ArrayList<Tarjeta>()
        var context: Context? = null

        constructor(context: Context, listNotesAdpater: ArrayList<Tarjeta>) : super() {
            this.listNotesAdpater = listNotesAdpater
            this.context = context

        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var myView = layoutInflater.inflate(R.layout.tarjetas_radiobutton_row, null)
            var myNote = listNotesAdpater[p0]

            myView.noTarjeta.text = myNote.strTarjeta?.let { maskify(it) }

            myView.noTarjeta.setOnClickListener {
                SessionStorage.noTarjeta = myNote.strTarjeta.toString()
                alertCVV()
            }

            return myView
        }

        override fun getItem(p0: Int): Any {
            return listNotesAdpater[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return listNotesAdpater.size
        }

        fun maskify(str: String): String? {
            return str.replace("[0-9](?=.*.{4})".toRegex(), "*")
        }
    }

    inner class addSaldoTask : AsyncTask<String, String, String>() {

        var inString=""

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {

                var monto = itemValue.toInt()

                val mURL = URL("https://admin.parkifacil.com/api/api/Saldos/mtdRecargarSaldo?fltMonto=$monto")

                val urlParameters = "{" +
                        "\"CreatedBy\":\"Jr\"," +
                        "\"CreatedDate\":\"2020-02-04\"," +
                        "\"LastModifiedBy\":\"android\"," +
                        "\"LastModifiedDate\":\"2020-02-04\"," +
                        "\"IntIdUsuarioTrans\":\"${SessionStorage.idUser}\","+
                        "\"StrFormaPago\":\"Efectivo\","+
                        "\"StrTipoRecarga\":\"Recarga\","+
                        "\"IntIdConcesionId\":2"+
                        "}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@AgregarSaldoActivity));

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

                println("Agregar saldo ${urlParameters}")

                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

            } catch (ex: Exception) {
                println("error AsyncTask add saldo $ex ")
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

            if(inString.isNullOrEmpty()) {
                alert(2)
            } else{
                alert(1)
            }
        }
    }

    inner class getComision : AsyncTask<String, String, String>() {

        var dcmPorcentaje: Double? = null

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                // println("token " + sessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Parametros/mtdConsultarParametroComisionRecarga")

                val urlConnect = mURL.openConnection() as HttpsURLConnection

                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@AgregarSaldoActivity));

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

                println("COMISION ${inString}")


                val gson = Gson()

                var tarifaJson = gson.fromJson(inString, DataComisionRecarga::class.java)

                dcmPorcentaje = tarifaJson.data?.get(0)?.porcentajeComisionRecarga

            } catch (ex: Exception) {
                println("error AsyncTask espacios $ex ")
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

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            println("onPostExecute $result")
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            format.maximumFractionDigits = 2
            format.currency = Currency.getInstance("MXN")

            monto = itemValue.toDouble()
            var subTotal: Double? = monto?.times(0.84)
            var iva: Double? = monto?.times(0.16)
            var porcentaje: Double? = dcmPorcentaje?.let { monto?.times(it) }?.div(100)
            total = porcentaje?.let { monto?.plus(it) }

            txtSubtotal.text = "${format.format(subTotal)} MXN"
            txtIVA.text = "${format.format(iva)} MXN"
            txtTotal.text = "${format.format(total)} MXN"
            txtComision.text = "${format.format(porcentaje)} MXN"

            SessionStorage.recarga = monto

        }
    }


    private fun alert(value: Int) {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

        var btnCancelDialog  = mDialogView.findViewById(R.id.btnCancelDialog) as Button

        var formatter: NumberFormat = DecimalFormat("#,###")
        var myNumber = itemValue.toDouble()
        var formattedNumber = formatter.format(myNumber)


        if(value==0) {
            mDialogView.txtTitleDialog.text = "No ha agregado CVV"
            mDialogView.txtTitle.text = "Para agregar saldo requiere ingresar la CVV de su tarjeta"
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        if(value==1) {
            btnCancelDialog.visibility = View.INVISIBLE
            mDialogView.txtTitleDialog.text = "Agregar Saldo"
            mDialogView.txtTitle.text = "Su recarga se agrego con éxito"
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        if (value == 2){
            mDialogView.txtTitleDialog.text = "Agregar Saldo"
            mDialogView.txtTitle.text = "Error al agregar saldo"
            //mDialogView.btnAceptarDialog.visibility = View.INVISIBLE
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        if(value==3) {
            mDialogView.txtTitleDialog.text = "Error"
            mDialogView.txtTitle.text = "Debe de ingresar tres dígitos en el CVV de la tarjeta"
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
        //show dialog
        val  mAlertDialog = mBuilder.show()
        //login button click of custom layout
        mDialogView.btnAceptarDialog.setOnClickListener {
            if (value==0){
                mAlertDialog.dismiss()
                onRestart()
            }
            if (value==1){
                mAlertDialog.dismiss()
                startActivity(Intent(baseContext, ReciboSaldoActivity::class.java))
                finish()
            }
            if(value==2){
                startActivity(Intent(baseContext, MainActivity::class.java))
                finish()
            }
            if (value==3){
                mAlertDialog.dismiss()
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    private fun alertCVV() {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_cvv, null)
        var txtCVV  = mDialogView.findViewById(R.id.txtCVV) as EditText

        mDialogView.txtTitleDialog.text = "Agregar Saldo"
        mDialogView.txtTitle.text = "Ingrese CVV"
        mDialogView.btnAceptarDialog.text="Continuar"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
        //show dialog
        val  mAlertDialog = mBuilder.show()
        //login button click of custom layout
        mDialogView.btnAceptarDialog.setOnClickListener {
            mAlertDialog.dismiss()

            if(TextUtils.isEmpty(txtCVV.text)){
                mAlertDialog.dismiss()
                alert(0)
            } else if(txtCVV.text.length < 3) {
                alert(3)
            } else {
                addSaldoTask().execute()
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
            onRestart()
        }

    }

    inner class getSaldo : AsyncTask<String, String, String>() {

        var saldoActualizado: Double = 0.00

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        private fun consultarSaldoXIdUsuario(){
            val mURL = URL("https://admin.parkifacil.com/api/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)

            val urlConnect = mURL.openConnection() as HttpsURLConnection

            urlConnect.requestMethod = "GET"
            urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)

            urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@AgregarSaldoActivity));

            var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

            val gson = Gson()

            var jsonSaldo: dataSaldo = gson.fromJson(
                inString,
                object : TypeToken<dataSaldo>() {}.type
            )

            saldoActualizado = jsonSaldo.data.dblSaldoActual
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            try {
                consultarSaldoXIdUsuario()
            } catch (ex: Exception) {
                println("error AsyncTask saldo $ex ")
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

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            println("onPostExecute $result")
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            format.maximumFractionDigits = 2
            format.currency = Currency.getInstance("MXN")
            txtSaldo.text = "${format.format(saldoActualizado)} MXN"
        }
    }

    inner class getTarjetas : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            lstTarjeta.clear()

            try {

                val mURL = URL("https://admin.parkifacil.com/api/api/Tarjetas/mtdConsultarTarjetasXIdUsuario?idUsuario=" + SessionStorage.idUser)
                val urlConnect = mURL.openConnection() as HttpsURLConnection

                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@AgregarSaldoActivity));

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

                println("Recibiendo info de tarjetas " + inString)

                val gson = Gson()
                val tarjetasJson: DataTarjeta = gson.fromJson(inString, DataTarjeta::class.java)

                tarjetasJson.data?.forEach{
                    lstTarjeta.add(
                        Tarjeta(
                            id = it.id!!,
                            dcManoVigencia = it.dcManoVigencia!!,
                            dcmMesVigencia = it.dcmMesVigencia!!,
                            strTarjeta = it.strTarjeta!!,
                            strTitular = it.strTitular!!
                        )
                    )
                }

            } catch (ex: Exception) {
                println("error AsyncTask zonas $ex ")
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

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            println("onPostExecute $result")

            if (lstTarjeta.size == 0){
                noHayTarjeta.visibility = View.VISIBLE
            } else {
                noHayTarjeta.visibility = View.GONE
                callAdpater()
            }
        }
    }
}
