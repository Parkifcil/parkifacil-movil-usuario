package parquimetro.mx.com.parkifacil

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.confirm_pago.view.*
import kotlinx.android.synthetic.main.tarjetas_row.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.DataTarjeta
import parquimetro.mx.com.models.SessionStorage
import parquimetro.mx.com.models.Tarjeta
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class TarjetaActivity : AppCompatActivity() {

    lateinit var btnAgregar: Button
    lateinit var mlistView: ListView
    lateinit var viewDialog: ViewDialog

    var lstTarjeta = ArrayList<Tarjeta>()
    var itemPosition = 0
    var condition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarjeta)

        getTarjetas().execute()

        mlistView = findViewById<ListView>(R.id.listTarjetas) as ListView
        btnAgregar = findViewById<Button>(R.id.btnAgregarTarjeta)

        btnAgregar.setOnClickListener {
            SessionStorage.status = 0
            this.startActivity(Intent(this, NuevaTarjetaActivity::class.java))
            finish()
        }

        viewDialog = ViewDialog(this)

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

            if (condition==1){
                if (listNotesAdpater.size != 0) {
                    listNotesAdpater.removeAt(listNotesAdpater.size - 1)
                }
                condition==0
            }
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var myView = layoutInflater.inflate(R.layout.tarjetas_row, null)
            val editar = myView.findViewById<ImageView>(R.id.btnModificarTarjeta)
            val eliminar = myView.findViewById<ImageView>(R.id.btnEliminarTarjeta)
            var myNote = listNotesAdpater[p0]

            myView.txtTarjeta.text = myNote.strTarjeta?.let { maskify(it) }

            myView.setOnClickListener(null)

            editar.setOnClickListener {
                SessionStorage.status = 1

                var intent = Intent(baseContext, NuevaTarjetaActivity::class.java)

                intent.putExtra("id", myNote.id)
                intent.putExtra("strTarjeta", myNote.strTarjeta)
                intent.putExtra("strTitular", myNote.strTitular)
                intent.putExtra("dcmMesVigencia", myNote.dcmMesVigencia)
                intent.putExtra("dcManoVigencia", myNote.dcManoVigencia)

                startActivity(intent)
                finish()
            }

            eliminar.setOnClickListener {
                try{
                    itemPosition = p0
                    condition = 1
                    modalDelete(myNote.id)
                }catch (ex: Exception){
                    println("Error" + ex)
                }
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

    private fun modalDelete(idItem: Int?){
        var id = idItem

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

        var textTitleDial = mDialogView.findViewById(R.id.txtTitleDialog) as TextView
        var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView
        var txtbtnAceptar = mDialogView.findViewById(R.id.btnAceptarDialog) as TextView

        textTitleDial.text = "Eliminar tarjeta"
        textTitle.text = "¿Seguro de realizar esta acción?"
        txtbtnAceptar.text ="Eliminar"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            try {
                viewDialog.showDialog()
                deleteTarjeta(id).execute()
                mAlertDialog.dismiss()
            }catch (ex: Exception){
                println("error " + ex)
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
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

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TarjetaActivity));

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

                for(tarjeta in lstTarjeta){
                    println("no. tarjetas ${tarjeta.strTarjeta}")
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
            callAdpater()
        }
    }

    inner class deleteTarjeta(id: Int?) : AsyncTask<String, String, String>() {

        var id = id

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Tarjetas/mtdBajaTarjeta?id=$id")

                val urlConnect = mURL.openConnection() as HttpsURLConnection

                urlConnect.requestMethod = "DELETE"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TarjetaActivity));

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                publishProgress(inString)

                println("delete tarjeta " + inString)

            } catch (ex: Exception) {
                println("error AsyncTask delete vehiculo $ex ")
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

            onRestart()
            getTarjetas().execute()
            viewDialog.hideDialog()
        }
    }
}