package parquimetro.mx.com.parkifacil


import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.feeeei.circleseekbar.CircleSeekBar
import kotlinx.android.synthetic.main.activity_time.*
import kotlinx.android.synthetic.main.confirm_pago.view.*
import kotlinx.android.synthetic.main.content_time.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.database.SQLiteHelper
import parquimetro.mx.com.models.*
import java.io.DataOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.*
import java.util.*
import javax.net.ssl.HttpsURLConnection


class TimeActivity : AppCompatActivity() {

    lateinit var mMinuteSeekbar: CircleSeekBar
    lateinit var txtTime:TextView
    lateinit var txtSaldo:TextView
    lateinit var txtComision:TextView
    lateinit var txtEstacionamiento:TextView
    lateinit var txtTotal: TextView
    lateinit var txtfecha:TextView
    lateinit var btnAddPago:Button
    lateinit var newtimer : CountDownTimer
    lateinit var strMM:String
    lateinit var txtFechaFinaliza:TextView
    lateinit var fechaFinal :String
    lateinit var timeFinal: String
    lateinit var sqLiteHelper: SQLiteHelper
    lateinit var viewDialog: ViewDialog
    var minutosApi: Int = 0
    var value: Int? = null
    var tarifa: Tarifa? = null
    var costoTotal: Double? = null
    var costo: Double? = 0.0
    var dcmPorcentaje: Double? = null
    var alertTime15Minutos: Date? = null
    var alertTime10Minutos: Date? = null
    var alertTime5Minutos: Date? = null
    var tarifaMinima: Double? = 0.0
    var tarifaMaxima: Double? = 0.0
    var tarifaIntervalo: Double? = 0.0
    var porcentaje: Double? = 0.0
    var intTiempoMaximo: Int? = 0
    var intTiempoMinimo: Int? = 0
    var intIntervaloMinutos: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getComision().execute()
        getTarifas().execute()

        setContentView(R.layout.activity_time)

        createNotificationChannel()

        mMinuteSeekbar = findViewById<CircleSeekBar>(R.id.seek_minute)
        txtTime = findViewById<TextView>(R.id.txtTime)
        txtComision = findViewById<TextView>(R.id.txtComision)
        txtSaldo = findViewById<TextView>(R.id.txtMonto)
        txtEstacionamiento = findViewById<TextView>(R.id.txtEst)
        txtTotal = findViewById<TextView>(R.id.txtTotal)
        txtfecha = findViewById<TextView>(R.id.txtFechActual)
        btnAddPago  = findViewById(R.id.btnAddPago)
        txtFechaFinaliza = findViewById(R.id.txtFinalizatime)
        viewDialog = ViewDialog(this)

        getSaldo().execute()

        btnAddPago.setOnClickListener {
            modalSaveVenta(0, "")
        }



        sqLiteHelper = SQLiteHelper(this)
    }

    private fun scheduleNotification(id: String, timeAlert: Date?, minutes: Int, placa: String){
        val intent = Intent(applicationContext, Notification::class.java)
        intent.putExtra(titleExtra, "Tiempo de aparcado")
        intent.putExtra(messageExtra, "Tu tiempo de aparcado para el vehículo con placa $placa termina en ${minutes} minutos")
        intent.putExtra(numNotification, id.toString())

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = timeAlert?.let { getTime(it) }
        if (time != null) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
            println("Notificacion aparecera en $timeAlert")
            println("Notificacion id $id")
        }
    }

    private fun getTime(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.setTime(date)
        return calendar.timeInMillis
    }

    private fun createNotificationChannel(){
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelID, name, importance)
            channel.description = desc
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun changeText(minute: Int) {

        val minuteStr = if (minute > 9) minute  else "0" + minute

        var hours = minute/60
        var minutes:String = (minute%60).toString()

        if (minutes == "0"){
            minutes = "00"
        }

        timeFinal = "${hours}:${minutes}"

        txtTime.text = " $timeFinal"

        strMM = minuteStr.toString()

        tarifaPorMinuto(minute)
        getfechaFinaliza()
    }

    private fun getfechaFinaliza() {
        val totalMinutos = strMM.toInt()
        val ONE_MINUTE_IN_MILLIS:Long = 60000//millisecs
        var date = Calendar.getInstance()
        var t = date.timeInMillis
        var afterAddingTenMins = Date(t + (totalMinutos * ONE_MINUTE_IN_MILLIS))

        alertTime15Minutos = Date(t + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (15 * ONE_MINUTE_IN_MILLIS))
        alertTime10Minutos = Date(t + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (10 * ONE_MINUTE_IN_MILLIS))
        alertTime5Minutos = Date(t + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (5 * ONE_MINUTE_IN_MILLIS))

        fechaFinal = SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(afterAddingTenMins)

        var fechaFinalDate: String = SessionStorage.fechaFinDate.toString()

        fechaFinalDate = fechaFinal

        txtFechaFinaliza.text = fechaFinal
    }

    private fun modalSaveVenta(i: Int, s: String) {
        try {

            val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_pago, null)

            var textTitle = mDialogView.findViewById(R.id.txtTitle) as TextView

            var btnAceptarDialog  = mDialogView.findViewById(R.id.btnAceptarDialog) as Button
            var btnCancelDialog  = mDialogView.findViewById(R.id.btnCancelDialog) as Button


            if (i==0) {
                textTitle.text = "¿Seguro de realizar pago?"
            }


            if (i==1) {
                btnCancelDialog.visibility= View.GONE

                textTitle.text = "¡Pago realizado con éxito!"
                btnAceptarDialog.text = "Aceptar"
                this.startActivity(Intent(this, HistorialActivity::class.java))
                finish()

                Toast.makeText(
                    applicationContext,
                    "El automóvil se ha parqueado con éxito",
                    Toast.LENGTH_SHORT
                ).show()
                newtimer.cancel()
            }

            if (i==2) {
                btnCancelDialog.visibility= View.GONE
                textTitle.text = "$s"

                btnAceptarDialog.text = "Aceptar"
            }


            val mBuilder = AlertDialog.Builder(this)
                    .setView(mDialogView)

            val mAlertDialog = mBuilder.show()

            mDialogView.btnAceptarDialog.setOnClickListener {

                try {
                    if (i==0) {
                        savePagoTask().execute()
                        viewDialog.showDialog()
                    }
                    if(i==2){
                        var intent= Intent(baseContext, PlacasActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    mAlertDialog.dismiss()
                }catch (ex: Exception){
                    println("error " + ex)
                }
            }

            mDialogView.btnCancelDialog.setOnClickListener {
                mAlertDialog.dismiss()
            }

        }catch (ex: Exception){
            println("error dialog add product" + ex)
        }

    }

    inner class savePagoTask : AsyncTask<String, String, String>() {

        var inString=""

        override fun onPreExecute() {

            println("preExecutee")
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Movimientos/mtdMovAparcar")
                //val mURL = URL("http://74.208.91.19:9000/api/Movimientos/mtdMovAparcar")

                var fechaInicio= SimpleDateFormat("dd/MM/YYYY hh:mm:ss", Locale.getDefault()).format(
                    Date()
                )

                SessionStorage.fechaPago = fechaInicio

                val urlParameters = "{" +
                        "\"CreatedBy\": \"jr\"," +
                        "\"StrPlaca\":\"${SessionStorage.strPlaca}\"," +
                        "\"BooleanAutoRecarga\": false," +
                        "\"FltMonto\": ${costo}," +
                        "\"FltTotalConComision\": ${costoTotal}," +
                        "\"StrSo\": \"ANDROID\"," +
                        "\"StrLatitud\": \"${SessionStorage.dcmLatitud}\" ," +
                        "\"StrLongitud\": \"${SessionStorage.dcmLongitud}\"," +
                        "\"IntIdVehiculoId\": ${SessionStorage.idPlaca}," +
                        "\"IntIdConcesionId\": 2," +
                        "\"IntIdZona\": ${SessionStorage.idZona}," +
                        "\"IntIdUsuarioId\" : \"${SessionStorage.idUser}\"," +
                        "\"int_tiempo\": $value," +
                        "\"StrNumeroCajon\": ${SessionStorage.strNumeroCajon}" +
                        "}"

                println("APARCAR " + urlParameters)

                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpsURLConnection
                //val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TimeActivity));
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

                DataOutputStream(urlConnect.outputStream).use { wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("jso response pago" + inString)

                val gson = Gson()

                val responseJson: DataResponse? = gson.fromJson(inString, DataResponse::class.java)

                SessionStorage.idMovimiento = responseJson?.data?.idMovimiento!!.toInt()
            } catch (ex: Exception) {
                println("error AsyncTask pago $ex ")
                viewDialog.hideDialog()
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
            if(inString.contains("La transaccion se completo correctamente.")) {
                val tipoMovimiento = "Aparcado"

                println("FECHA FINAL ${fechaFinal}")

                var movimientoSql = MovimientoActivo(SessionStorage.idPlaca, SessionStorage.strPlaca,
                    SessionStorage.fechaPago , fechaFinal, tipoMovimiento, SessionStorage.idUser, SessionStorage.strNumeroCajon)

                println("MOV SQL ${movimientoSql.intIdVehiculoId} ${movimientoSql.strPlaca} " +
                        "${movimientoSql.dtHoraInicio} ${movimientoSql.dtmHoraFin} ${movimientoSql.tipo} ${movimientoSql.idUser} ${movimientoSql.strNumeroCajon}")

                val status = sqLiteHelper.insertMovimiento(movimientoSql)

                if(status > -1) {
                    println("GUARDADO")
                    scheduleNotification(movimientoSql.intIdVehiculoId.toString()+"1", alertTime15Minutos, 15, SessionStorage.strPlaca)
                    scheduleNotification(movimientoSql.intIdVehiculoId.toString()+"2", alertTime10Minutos, 10, SessionStorage.strPlaca)
                    scheduleNotification(movimientoSql.intIdVehiculoId.toString()+"3", alertTime5Minutos, 5, SessionStorage.strPlaca)
                } else {
                    println("NO GUARDADO")
                }

                viewDialog.hideDialog()
                var intent= Intent(baseContext, HistorialActivity::class.java)
                startActivity(intent)
                finish()
            }else{
                modalSaveVenta(2, "No se ha podido completar la operación, vuelva a realizarla.")
            }
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

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TimeActivity));

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

    inner class getTarifas() : AsyncTask<String, String, String>() {

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

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TimeActivity));

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
                intTiempoMaximo = tarifa?.intTiempoMaximo
                intTiempoMinimo = tarifa?.intTiempoMinimo
                intIntervaloMinutos = tarifa?.intIntervaloMinutos
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
            mMinuteSeekbar.setOnSeekBarChangeListener(object : CircleSeekBar.OnSeekBarChangeListener {
                override fun onChanged(seekbar: CircleSeekBar, curValue: Int) {
                    var step = intIntervaloMinutos
                    var max = intTiempoMaximo
                    var min = intTiempoMinimo

                    if (max != null) {
                        mMinuteSeekbar.maxProcess = (max - min!!) / (step!!)
                    }

                    value = (curValue * step!!) + min!!

                    SessionStorage.tiempoPagado = minutosApi.toLong()

                    println("VALUE ${value!!}")

                    changeText(value!!)
                }
            })

            mMinuteSeekbar.curProcess = 0

            newtimer = object: CountDownTimer(1000000000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    var now = Date()
                    var nowFormatted2 = SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(now)

                    txtfecha.text = nowFormatted2

                    var fechaInicio:String = SessionStorage.fechaInicioDate.toString()

                    fechaInicio = nowFormatted2

                    getfechaFinaliza()
                }
                override fun onFinish() {
                    newtimer.cancel()
                }
            }

            newtimer.start()

            costo = tarifaMinima
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            format.maximumFractionDigits = 2
            format.currency = Currency.getInstance("MXN")

            porcentaje = dcmPorcentaje?.let { costo?.times(it) }?.div(100)
            costoTotal = porcentaje?.let { costo!!.plus(it) }

            btnAddPago.text = "Pagar ${costoTotal?.let { format.format(it.toDouble()) }} MXN"
            txtEstacionamiento.text = "${costo?.let { format.format(it.toDouble()) }} MXN"

            txtComision.text = "${porcentaje?.let { format.format(it.toDouble()) }} MXN"
            txtTotal.text = "${costoTotal?.let { format.format(it.toDouble()) }} MXN"
        }
    }

    private fun tarifaPorMinuto(minute: Int){
        val interval = (minute - intTiempoMinimo!!)/ intIntervaloMinutos!!

        if(minute > intTiempoMinimo!! && minute < intTiempoMaximo !!){
            costo = tarifaMinima!! + tarifaIntervalo?.times(interval)!!
        } else if(minute <= intTiempoMinimo!!){
            costo = tarifaMinima
        } else if(minute == intTiempoMaximo){
            costo = tarifaMaxima
        }

        println("costo ${costo}")

        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("MXN")

        porcentaje = dcmPorcentaje?.let { costo?.times(it) }?.div(100)
        costoTotal = porcentaje?.let { costo!!.plus(it) }

        btnAddPago.text = "Pagar ${costoTotal?.let { format.format(it.toDouble()) }} MXN"
        txtEstacionamiento.text = "${costo?.let { format.format(it.toDouble()) }} MXN"

        txtComision.text = "${porcentaje?.let { format.format(it.toDouble()) }} MXN"
        txtTotal.text = "${costoTotal?.let { format.format(it.toDouble()) }} MXN"
    }

    inner class getSaldo : AsyncTask<String, String, String>() {

        var saldoActualizado: Double = 0.00

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        private fun consultarSaldoXIdUsuario(){
            val mURL = URL("https://admin.parkifacil.com/api/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)
            //val mURL = URL("http://74.208.91.19:9000/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)

            val urlConnect = mURL.openConnection() as HttpsURLConnection
            //val urlConnect = mURL.openConnection() as HttpURLConnection

            urlConnect.requestMethod = "GET"
            urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)

            urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@TimeActivity))

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

}
