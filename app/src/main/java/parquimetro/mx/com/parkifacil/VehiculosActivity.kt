package parquimetro.mx.com.parkifacil

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.confirm_pago.view.*
import kotlinx.android.synthetic.main.confirm_pago.view.btnAceptarDialog
import kotlinx.android.synthetic.main.confirm_pago.view.btnCancelDialog
import kotlinx.android.synthetic.main.confirm_pago.view.txtTitle
import kotlinx.android.synthetic.main.confirm_pago.view.txtTitleDialog
import kotlinx.android.synthetic.main.confirm_reintegro.view.*
import kotlinx.android.synthetic.main.unidades_row.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.*
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList


class VehiculosActivity : AppCompatActivity() {

    lateinit var mlistView: ListView
    lateinit var btnAddPlaca:Button
    lateinit var viewDialog: ViewDialog
    lateinit var now: Date
    var lstMovimientos = ArrayList<MovimientoActivo>()
    var lstSV = ArrayList<Unidad>()
    var lstPlacasOcupadas: ArrayList<MovimientoActivo> = ArrayList()
    var lstVehiculos: ArrayList<Any> = ArrayList()
    var itemPosition = 0
    var condition = 0
    var saldoActualizado = 0.0
    var saldoAnterior = 0.0
    var tarifa: Tarifa? = null
    var dcmPorcentaje: Double? = null
    var costo: Double? = 0.0
    var costoEstacionamiento: Double? = 0.0
    var estacionamientoComision: Double? = 0.0
    var diffInMin: Long = 0
    var comisionMonto: Double? = 0.0
    var dateTotal: Long = 0
    var dateAparcado: Long = 0
    var intIntervaloMinutos: Int? = null
    var intTiempoMaximo: Int? = null
    var intTiempoMinimo: Int? = null
    var dateInicio: Date? = null
    var dateFin: Date? = null
    var tarifaMinimaSaldo: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placas)

        getSaldo().execute()
        getComision().execute()
        getTarifaMinima().execute()

        mlistView = findViewById<ListView>(R.id.listUnidades) as ListView
        btnAddPlaca = findViewById(R.id.btnAddPlaca)

        viewDialog = ViewDialog(this)
        viewDialog.showDialog()

        placasDisponibles()

        btnAddPlaca.setOnClickListener {
            SessionStorage.status = 0
            this.startActivity(Intent(this, NewPlacaActivity::class.java))
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

    private fun alert(value: Int?, devolucion: Devolucion) {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_reintegro, null)

        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("MXN")

        if(value==0) {
            if(devolucion.minutosOcupados < intTiempoMinimo!!){
                val date = Calendar.getInstance()
                val totalMinutos = date.timeInMillis
                val ONE_MINUTE_IN_MILLIS:Long = 60000//millisecs
                var afterAddingTenMins = Date(totalMinutos + (30 * ONE_MINUTE_IN_MILLIS))

                val fechaFinal = SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(afterAddingTenMins)

                mDialogView.txtTitle.text = "Se realizará un reintegro de saldo de ${format.format(
                    devolucion.montoTotalDevolucion
                )} " +
                        "comisión incluida por ${devolucion.tiempoDevuelto} minutos que no se ocuparon."
                mDialogView.txtHoraInicio.text = "Inicio: ${SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(dateInicio!!)}"
                mDialogView.txtHoraFin.text = "Fin: ${fechaFinal}"
                mDialogView.btnAceptarDialog.text="Reintegrar saldo"
            } else {
                mDialogView.txtTitle.text = "Se realizará un reintegro de saldo de ${format.format(
                    devolucion.montoTotalDevolucion
                )} " +
                        "comisión incluida por ${devolucion.tiempoDevuelto} minutos que no se ocuparon."
                mDialogView.txtHoraInicio.text = "Inicio: ${SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(dateInicio!!)}"
                mDialogView.txtHoraFin.text = "Fin: ${SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(dateFin)}"
                mDialogView.btnAceptarDialog.text="Reintegrar saldo"
            }
        }

        if(value==1) {
            mDialogView.txtTitleDialog.text = "Reintegrar Saldo"
            mDialogView.txtTitle.text = "No se puede reintegrar saldo. Para reintegrar" +
                    " saldo el tiempo debe ser mayor a ${devolucion.intTiempoMinimo} minutos"
            mDialogView.txtHoraInicio.visibility = View.GONE
            mDialogView.txtHoraFin.visibility = View.GONE
            mDialogView.btnAceptarDialog.text="Aceptar"
        }

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            if (value==0){
                mAlertDialog.dismiss()
                alertConfirmacion(devolucion)
            }
            if (value==1){
                mAlertDialog.dismiss()
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    private fun alertConfirmacion(devolucion: Devolucion) {

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

        mDialogView.txtTitleDialog.text = "Reintegrar Saldo"
        mDialogView.txtTitle.text = "Reintegro exitoso"
        mDialogView.btnAceptarDialog.text="Aceptar"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            mAlertDialog.dismiss()
            reintegrarSaldo(devolucion).execute()
        }
    }

    private fun callAdpater() {
        var myAdapter = MyAdpater(this, lstVehiculos)
        mlistView.adapter = myAdapter
    }

    inner class MyAdpater : BaseAdapter {
        var listNotesAdpater = ArrayList<Any>()
        var context: Context? = null

        constructor(context: Context, listNotesAdpater: ArrayList<Any>) : super() {
            this.listNotesAdpater = listNotesAdpater
            this.context = context

            if (condition==1){
                listNotesAdpater.removeAt(itemPosition)
                condition==0
            }
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var myView = layoutInflater.inflate(R.layout.unidades_row, null)
            val imgEdt = myView.findViewById<ImageView>(R.id.imgEdt)
            val imgDelet = myView.findViewById<ImageView>(R.id.imgDelet)
            val btnExtenderTiempo = myView.findViewById<Button>(R.id.btnExtenderTiempo)
            val btnReintegrarSaldo = myView.findViewById<Button>(R.id.btnReintegrarSaldo)
            val layoutInicio = myView.findViewById<LinearLayout>(R.id.layoutInicio)
            val layoutFin = myView.findViewById<LinearLayout>(R.id.layoutFin)
            var myNote = listNotesAdpater[p0]

            if(myNote is Unidad){
                myView.txtDescripcion.text = "${myNote.strModelo} ${myNote.strColor}"
                myView.txtPlaca.text = myNote.strPlacas
                myView.txtDisponibilidad.setTextColor(Color.parseColor("#32a852"))
                myView.txtDisponibilidad.text = "disponible".toUpperCase()

                btnExtenderTiempo.visibility = View.GONE
                btnReintegrarSaldo.visibility = View.GONE
                layoutInicio.visibility = View.GONE
                layoutFin.visibility = View.GONE

                myView!!.setOnClickListener {}

                imgEdt.setOnClickListener {
                    SessionStorage.strPlaca = myNote.strPlacas!!
                    SessionStorage.idPlaca = myNote.id!!
                    SessionStorage.status = 1


                    var intent = Intent(baseContext, NewPlacaActivity::class.java)

                    intent.putExtra("strColor", myNote.strColor)
                    intent.putExtra("strModelo", myNote.strModelo)

                    startActivity(intent)
                    finish()
                }

                imgDelet.setOnClickListener {
                    try{
                        itemPosition = p0
                        condition = 1
                        modalDelete(myNote.id)
                    }catch (ex: Exception){

                        println("Error" + ex)
                    }
                }
            }
            if(myNote is MovimientoActivo){
                val txtInicio = myView.findViewById<TextView>(R.id.txtInicio)
                val txtFin = myView.findViewById<TextView>(R.id.txtFin)

                var now: Date

                myView.txtPlaca.text = myNote.strPlaca
                imgEdt.visibility = View.INVISIBLE
                imgDelet.visibility = View.INVISIBLE

                val oldFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                val oldDateInicio = oldFormat.parse(myNote.dtHoraInicio!!)
                val oldDateFin = oldFormat.parse(myNote.dtmHoraFin!!)
                val calendar = Calendar.getInstance()

                myView.txtDescripcion.text = "${myNote.strModelo} ${myNote.strColor}"
                myView.txtDisponibilidad.setTextColor(Color.parseColor("#d60000"))
                myView.txtDisponibilidad.text = "parqueado".toUpperCase()

                calendar.timeInMillis = oldDateInicio.time
                txtInicio.text = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)} " +
                        "${"%02d".format(calendar.get(Calendar.HOUR_OF_DAY))}:${"%02d".format(calendar.get(Calendar.MINUTE))}:${"%02d".format(calendar.get(Calendar.SECOND))}"

                calendar.timeInMillis = oldDateFin.time
                txtFin.text = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)} " +
                        "${"%02d".format(calendar.get(Calendar.HOUR_OF_DAY))}:${"%02d".format(calendar.get(Calendar.MINUTE))}:${"%02d".format(calendar.get(Calendar.SECOND))}"

                SessionStorage.strPlaca = myNote.strPlaca!!
                SessionStorage.idPlaca = myNote.intIdVehiculoId!!

                myView!!.setOnClickListener {}

                btnExtenderTiempo.setOnClickListener {
                    if(saldoActualizado == 0.0 || tarifaMinimaSaldo!! > saldoActualizado){
                        Toast.makeText(
                            applicationContext,
                            "No cuenta con saldo suficiente",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else{
                        try {
                            var dateInicioString: String
                            var dateFinString: String
                            var dateFormat: SimpleDateFormat
                            var dateInicio: Date
                            var dateFin:  Date
                            var diffInMillisec: Long

                            dateInicioString = myNote.dtHoraInicio!!
                            dateFinString = myNote.dtmHoraFin!!

                            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                            dateInicio = dateFormat.parse(dateInicioString)
                            dateFin = dateFormat.parse(dateFinString)
                            now = Date()

                            dateAparcado = TimeUnit.MILLISECONDS.toMinutes(dateFin.time - dateInicio.time)
                            dateTotal =  TimeUnit.MILLISECONDS.toMinutes(now.time - dateInicio.time)

                            diffInMillisec =  dateInicio.time - dateFin.time
                            diffInMin = TimeUnit.MILLISECONDS.toMinutes(diffInMillisec)

                            SessionStorage.idMovimiento = myNote.id!!
                            SessionStorage.fechaInicioDate = dateInicio
                            SessionStorage.fechaFinDate = dateFin
                            SessionStorage.strPlaca = myNote.strPlaca!!
                            SessionStorage.tiempoPagado = diffInMin
                            SessionStorage.strNumeroCajon = myNote.strNumeroCajon!!

                            val intent = Intent(context, ParqueoActualActivity::class.java)
                            startActivity(intent)
                            finish()
                        }catch (ex: Exception){
                            println("error convert fecha" + ex)
                        }
                    }
                }

                btnReintegrarSaldo.setOnClickListener {
                    try {
                        var dateInicioString = myNote.dtHoraInicio
                        var dateFinString = myNote.dtmHoraFin

                        var dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                        dateInicio = dateFormat.parse(dateInicioString)
                        dateFin = dateFormat.parse(dateFinString)
                        now = Date()

                        var dateFormatTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        var dateActual = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(
                            Date()
                        )

                        dateAparcado = dateFin?.time?.minus(
                            dateInicio?.time!!
                        )?.let { it1 -> TimeUnit.MILLISECONDS.toMinutes(it1) }!!

                        dateTotal =  TimeUnit.MILLISECONDS.toMinutes(now.time - dateInicio?.time!!)

                        var dateFinTime = myNote.dtmHoraFin!!.substring(0, 19).replace("T", " ")

                        var date1 = dateFormatTime.parse(dateActual)
                        var date2 = dateFormatTime.parse(dateFinTime)


                        var diffInMillisec =  date2.time -date1.time
                        diffInMin = TimeUnit.MILLISECONDS.toMinutes(diffInMillisec)

                        myNote.id?.let { it1 -> getTarifas(diffInMin.toInt(), it1).execute() }

                        val notificationManager =
                            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(myNote.intIdVehiculoId!!+1)
                        notificationManager.cancel(myNote.intIdVehiculoId!!+2)
                        notificationManager.cancel(myNote.intIdVehiculoId!!+3)

                    }catch (ex: Exception){
                        println("error convert fecha" + ex)
                    }

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

    }

    private fun placasDisponibles(){
        getMovimientosActuales().execute()
        getVehiculos().execute()
    }

    private fun modalDelete(idItem: Int?){
        var id = idItem

        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

        var textTitleDial = mDialogView.findViewById(R.id.txtTitleDialog) as TextView
        var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView
        var txtbtnAceptar = mDialogView.findViewById(R.id.btnAceptarDialog) as TextView

        textTitleDial.text = "Eliminar placa"
        textTitle.text = "¿Seguro de realizar esta acción?"
        txtbtnAceptar.text ="Eliminar"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialog.setOnClickListener {
            try {
                viewDialog.showDialog()
                deleteVehiculo(id).execute()
                mAlertDialog.dismiss()
            }catch (ex: Exception){
                println("error " + ex)
            }
        }

        mDialogView.btnCancelDialog.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    inner class getVehiculos : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null
            try {

                // println("token " + sessionStorage.tokenSession)
                val mURL = URL("https://admin.parkifacil.com/api/api/Vehiculos/mtdConsultarVehiculosXIdUsuario?id=" + SessionStorage.idUser)
                //val mURL = URL("http://74.208.91.19:9000/api/Vehiculos/mtdConsultarVehiculosXIdUsuario?id=" + SessionStorage.idUser)

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))
                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                publishProgress(inString)

                println("Recibiendo info de Vehiculos " + inString)

                val gson = Gson()
                val unidadesJson: Unidades = gson.fromJson(inString, Unidades::class.java)

                unidadesJson.data?.forEach{
                    lstSV.add(
                        Unidad(
                            id = it.id!!,
                            strColor = it.strColor!!,
                            strModelo = it.strModelo!!,
                            strPlacas = it.strPlacas!!
                        )
                    )
                }

                lstVehiculos.addAll(lstSV)

                for(vehiculo in lstSV){
                    for(movimiento in lstMovimientos){
                        if(vehiculo.strPlacas == movimiento.strPlaca){
                            vehiculo.estado = false
                            lstPlacasOcupadas.add(movimiento)
                        }
                    }
                }

                for(vehiculo in lstSV){
                    if(vehiculo.estado == false){
                        lstVehiculos.remove(vehiculo)
                    }
                }

                lstVehiculos.addAll(lstPlacasOcupadas)

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
            viewDialog.hideDialog()
        }
    }

    inner class deleteVehiculo(id: Int?) : AsyncTask<String, String, String>() {

        var id = id

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                println("token delete " + SessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Vehiculos/mtdBajaVehiculo?id=$id")

                val urlConnect = mURL.openConnection() as HttpsURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))

                urlConnect.requestMethod = "DELETE"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                publishProgress(inString)

                println("delete vehiculo " + inString)

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
            getVehiculos().execute()
            callAdpater()
            viewDialog.hideDialog()
        }
    }

    inner class getMovimientosActuales : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            //var urlConnection: HttpsURLConnection? = null
            var urlConnection: HttpURLConnection? = null

            lstMovimientos.clear()

            try {

                val mURL = URL(
                    "https://admin.parkifacil.com/api/api/Movimientos/mtdConsultarMovimientosActivosXIdUsuarioE?idUsuario=" + SessionStorage.idUser +
                            "&status=true&bolEspacio=false="
                )

                /*
                val mURL = URL(
                    "http://74.208.91.19:9000/api/Movimientos/mtdConsultarMovimientosActivosXIdUsuarioE?idUsuario=" + SessionStorage.idUser +
                            "&status=true&bolEspacio=false="
                )
                */

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))
                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

                println(mURL)

                println("MOVIMIENTOS ${inString}")

                val gson = Gson()

                val movimientosJson: MovimientosActivos = gson.fromJson(
                    inString,
                    MovimientosActivos::class.java
                )

                println(movimientosJson.data)

                movimientosJson.data?.forEach {
                    lstMovimientos.add(
                        MovimientoActivo(
                            id = it.id!!,
                            intIdVehiculoId = it.intIdVehiculoId!!,
                            strPlaca = it.strPlaca!!,
                            dtHoraInicio = it.dtHoraInicio!!,
                            dtmHoraFin = it.dtmHoraFin!!,
                            intTiempo = it.intTiempo!!,
                            strModelo = it.strModelo!!,
                            strColor  = it.strColor!!,
                            strNumeroCajon = it.strNumeroCajon!!
                        )
                    )
                }
            } catch (ex: Exception) {
                println("error AsyncTask get all user $ex ")
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }
            return "Done"
        }

        override fun onProgressUpdate(vararg values: String?) {
            try {

            } catch (ex: Exception) {

            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            callAdpater()
            println("onPostExecute $result")
            viewDialog.hideDialog()
        }
    }

    inner class getSaldo : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        private fun consultarSaldoXIdUsuario(){
            val mURL = URL("https://admin.parkifacil.com/api/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)
            //val mURL = URL("http://74.208.91.19:9000/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)

            val urlConnect = mURL.openConnection() as HttpsURLConnection
            //val urlConnect = mURL.openConnection() as HttpURLConnection

            urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity));
            urlConnect.requestMethod = "GET"
            urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)

            var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

            val gson = Gson()

            var jsonSaldo: dataSaldo = gson.fromJson(
                inString,
                object : TypeToken<dataSaldo>() {}.type
            )

            saldoActualizado = jsonSaldo.data.dblSaldoActual
            saldoAnterior = jsonSaldo.data.dblSaldoAnterior

        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null
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
            println("saldoActualizado ${saldoActualizado}")
            println("saldoAnterior ${saldoAnterior}")
        }
    }

    inner class getTarifas(minute: Int, id: Int) : AsyncTask<String, String, String>() {

        var minute: Int = minute
        var tarifaMinima: Double? = null
        var tarifaMaxima: Double? = null
        var tarifaIntervalo: Double? = null
        var id: Int = id
        var montoTotalDevolucion: Double? = 0.0
        var montoTotalEstacionado: Double? = 0.0

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                // println("token " + sessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Tarifas/mtdConsultarTarifasXIdConcesion?intIdConcesion=2")
                //val mURL = URL("http://74.208.91.19:9000/api/Tarifas/mtdConsultarTarifasXIdConcesion?intIdConcesion=2")

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))
                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                //Cannot access to ui
                publishProgress(inString)

                println("TARIFAS " + inString)

                val gson = Gson()

                var tarifaJson = gson.fromJson(inString, DataTarifa::class.java)

                tarifa = tarifaJson.data

                tarifaMinima = tarifa?.fltTarifaMin
                tarifaMaxima = tarifa?.fltTarifaMax
                tarifaIntervalo = tarifa?.fltTarifaIntervalo

                intIntervaloMinutos = tarifaJson.data?.intIntervaloMinutos
                intTiempoMaximo = tarifaJson.data?.intTiempoMaximo
                intTiempoMinimo = tarifaJson.data?.intTiempoMinimo
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

        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            println("onPostExecute $result")
            val interval: Int
            var tiempoDevuelto: Int = 0

            var minutosOcupados: Int = dateAparcado.toInt() - minute

            if(minutosOcupados < intTiempoMinimo!!){
                var tiempoDevolucion: Int = (dateAparcado - intTiempoMinimo!!).toInt()

                interval = tiempoDevolucion/intIntervaloMinutos!!

                costo = tarifaIntervalo?.times(interval)!!

                tiempoDevuelto = (tiempoDevolucion/ intIntervaloMinutos!!) * intIntervaloMinutos!!

                println("costo $costo")

                println("TIEMPO minutos sobrantes ${minute}")
                println("TIEMPO minutos ocupados ${minutosOcupados}")
                println("date aparcado ${dateAparcado}")
            } else {
                interval = minute/intIntervaloMinutos!!

                println("intervalo $interval")

                costo = tarifaIntervalo?.times(interval)!!

                tiempoDevuelto = ((minute/intIntervaloMinutos!!)) * intIntervaloMinutos!!

                println("TIEMPO minutos sobrantes ${minute}")
                println("TIEMPO minutos ocupados ${minutosOcupados}")
                println("date aparcado ${dateAparcado}")
            }

            val intervalEstacionado = (minutosOcupados - intTiempoMinimo!!)/intIntervaloMinutos!!

            if(minutosOcupados <= 30){
                costoEstacionamiento = tarifaMinima
            } else if(minutosOcupados > 30 && minute <=180){
                costoEstacionamiento = tarifaMinima!! + tarifaIntervalo?.times(intervalEstacionado)!!
            } else {
                costoEstacionamiento = tarifaMaxima
            }

            comisionMonto = dcmPorcentaje?.let { costo?.times(it) }?.div(100)
            montoTotalDevolucion = comisionMonto?.let { costo?.plus(it) }

            estacionamientoComision = dcmPorcentaje?.let { costoEstacionamiento?.times(it) }?.div(
                100
            )
            montoTotalEstacionado = estacionamientoComision?.let { costoEstacionamiento?.plus(it) }

            println("MONTO TOTAL DEV ${montoTotalDevolucion}")
            println("TIEMPO DEVUELTO ${tiempoDevuelto}")
            println("MONTO TOTAL ESTACIONAMIENTO $montoTotalEstacionado")
            println("ESTACIONAMIENTO $costoEstacionamiento")
            println("COMISION $estacionamientoComision")

            var devolucion = Devolucion(
                tiempoDevuelto,
                id,
                intTiempoMinimo!!,
                montoTotalDevolucion!!,
                montoTotalEstacionado!!,
                minutosOcupados
            )

            if (minute != null) {
                if(minute <= intTiempoMinimo!!) {
                    alert(1, devolucion)
                    println("MINUTOS $minute")
                } else {
                    alert(0, devolucion)
                    println("MINUTOS $minute")
                }
            }
        }
    }

    inner class getTarifaMinima() : AsyncTask<String, String, String>() {

        var tarifaMinima: Double? = null
        var tarifaMaxima: Double? = null
        var tarifaIntervalo: Double? = null

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                // println("token " + sessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Tarifas/mtdConsultarTarifasXIdConcesion?intIdConcesion=2")
                //val mURL = URL("http://74.208.91.19:9000/api/Tarifas/mtdConsultarTarifasXIdConcesion?intIdConcesion=2")

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))
                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                //Cannot access to ui
                publishProgress(inString)

                println("TARIFAS " + inString)

                val gson = Gson()

                var tarifaJson = gson.fromJson(inString, DataTarifa::class.java)

                tarifa = tarifaJson.data

                tarifaMinima = tarifa?.fltTarifaMin
                tarifaMaxima = tarifa?.fltTarifaMax
                tarifaIntervalo = tarifa?.fltTarifaIntervalo

                intIntervaloMinutos = tarifaJson.data?.intIntervaloMinutos
                intTiempoMaximo = tarifaJson.data?.intTiempoMaximo
                intTiempoMinimo = tarifaJson.data?.intTiempoMinimo
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

        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            println("onPostExecute $result")
            tarifaMinimaSaldo = this.tarifaMinima
        }
    }

    inner class reintegrarSaldo(devolucion: Devolucion) : AsyncTask<String, String, String>() {

        var intId = devolucion.id
        var minute = devolucion.tiempoDevuelto
        var montoTotalDevolucion = devolucion.montoTotalDevolucion
        var montoTotalEstacionado = devolucion.montoTotalEstacionado
        var intTiempoMinimo = devolucion.intTiempoMinimo
        var minutosOcupados = devolucion.minutosOcupados
        var minutosOcupadosReal: Int = 0


        override fun onPreExecute() {
            if(minutosOcupados < intTiempoMinimo) {
                minutosOcupadosReal = intTiempoMinimo
            } else {
                minutosOcupadosReal = minutosOcupados
            }
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Movimientos/mtdMovDesaparcar?intIdMovimiento=" + intId)

                val urlParameters = "{" +
                        "\"Id\":${intId}," +
                        "\"IntIdUsuarioId\":\"${SessionStorage.idUser}\"," +
                        "\"FltSaldoAnterior\":${saldoAnterior}," +
                        "\"FltSaldoActual\":${saldoActualizado}," +
                        "\"int_tiempo\":${minutosOcupadosReal}," +
                        "\"IntTiempoDevuelto\":${minute}," +
                        "\"FltMontoDevolucion\":${costo}," +
                        "\"FltMontoPorcDevolucion\":${comisionMonto}," +
                        "\"FltTotalDevConComision\":${montoTotalDevolucion}," +
                        "\"FltMonto\":${costoEstacionamiento}," +
                        "\"FltMontoPorcentaje\":${estacionamientoComision}," +
                        "\"FltTotalConComision\":${montoTotalEstacionado}," +
                        "\"FltMontoReal\":${montoTotalEstacionado}," +
                        "\"StrPlaca\":\"${SessionStorage.strPlaca}\"" +
                        "}"


                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                println("json_desaparcar" + urlParameters)
                println(mURL)

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity));
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


                DataOutputStream(urlConnect.outputStream).use { wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("DESAPARCAR" + urlConnect.responseCode + " " + inString)

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
            getMovimientosActuales().execute()
            println("onPostExecute $result")
            onRestart()
            // Done
        }
    }

    inner class getComision : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                // println("token " + sessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Comisiones/mtdConsultarComisionesXIdConcesion?intIdConcesion=2&strtipo=PARQUIMETRO")
                //val mURL = URL("http://74.208.91.19:9000/api/Comisiones/mtdConsultarComisionesXIdConcesion?intIdConcesion=2&strtipo=PARQUIMETRO")

                val urlConnect=mURL.openConnection() as HttpsURLConnection
                //val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@VehiculosActivity))
                urlConnect.requestMethod = "GET"
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                //Cannot access to ui
                publishProgress(inString)

                println("COMISION " + inString)

                val gson = Gson()

                var tarifaJson = gson.fromJson(inString, DataComisiones::class.java)

                dcmPorcentaje = tarifaJson.data?.dcmPorcentaje

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
        }
    }

}