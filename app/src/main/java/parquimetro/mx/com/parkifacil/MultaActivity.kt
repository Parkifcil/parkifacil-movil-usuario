package parquimetro.mx.com.parkifacil

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.confirm_pago.view.*
import kotlinx.android.synthetic.main.tarjetas_radiobutton_row.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.DataTarjeta
import parquimetro.mx.com.models.SessionStorage
import parquimetro.mx.com.models.Tarjeta
import java.net.HttpURLConnection
import java.net.URL

class MultaActivity : AppCompatActivity() {

    lateinit var mlistView: ListView
    lateinit var txtMonto: TextView
    lateinit var noHayTarjeta: TextView
    lateinit var txtAgregarTarjeta: TextView

    var lstTarjeta = ArrayList<Tarjeta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multa)

        getTarjetas().execute()

        mlistView = findViewById<ListView>(R.id.listTarjetas) as NonScrollListView
        txtMonto = findViewById(R.id.txtMonto)
        noHayTarjeta = findViewById(R.id.noHayTarjeta)
        txtAgregarTarjeta = findViewById(R.id.txtAgregarTarjeta)

        txtMonto.text = "$0.00 mx"

        txtAgregarTarjeta.setOnClickListener {
            SessionStorage.status = 12
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
                alert(1)
            } else {
                startActivity(Intent(baseContext, ReciboMultaActivity::class.java))
                finish()
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
            onRestart()
        }

    }

    private fun alert(value: Int) {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

        if(value==0) {
            mDialogView.txtTitleDialog.text = "No ha agregado CVV"
            mDialogView.txtTitle.text = "Para agregar saldo requiere ingresar la CVV de su tarjeta"
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        if(value==1) {
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
            if (value==3){
                mAlertDialog.dismiss()
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

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

    inner class getTarjetas : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null

            lstTarjeta.clear()

            try {

                val mURL = URL("http://74.208.91.19:9000/api/Tarjetas/mtdConsultarTarjetasXIdUsuario?idUsuario=" + SessionStorage.idUser)
                val urlConnect = mURL.openConnection() as HttpURLConnection

                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

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
