package parquimetro.mx.com.parkifacil

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
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.feeeei.circleseekbar.CircleSeekBar
import kotlinx.android.synthetic.main.activity_parqueo_actual.*
import kotlinx.android.synthetic.main.confirm_pago.view.*
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.database.SQLiteHelper
import parquimetro.mx.com.models.*
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class ParqueoActualActivity : AppCompatActivity() {

    private val START_TIME_IN_MILLIS:Long = 600000
    lateinit var mMinuteSeekbar: CircleSeekBar
    lateinit var txtTime : TextView
    lateinit var newtimer : CountDownTimer
    lateinit var txtSaldo:TextView
    lateinit var txtComision:TextView
    lateinit var txtEstacionamiento:TextView
    lateinit var txtTotal: TextView
    lateinit var txtfecha:TextView
    lateinit var btnAddPago:Button
    lateinit var txtFechaFinaliza:TextView
    lateinit var fechaFinal: String
    lateinit var fechaInicial: String
    lateinit var sqLiteHelper: SQLiteHelper
    var minutosApi: Int = 0
    private var mTimeLeftInMillis = START_TIME_IN_MILLIS
    var fDate=""
    var value: Int? = null
    lateinit var timeFinal: String
    var dcmPorcentaje: Double? = null
    var costo: Double? = 0.0
    var costoTotal: Double? = null
    lateinit var strMM:String
    lateinit var builder: NotificationCompat.Builder
    var tarifa: Tarifa? = null
    var alertTime15Minutos: Date? = null
    var alertTime10Minutos: Date? = null
    var alertTime5Minutos: Date? = null
    lateinit var viewDialog: ViewDialog
    var saldoActualizado = 0.0
    var tarifaMinima: Double? = 0.0
    var tarifaMaxima: Double? = 0.0
    var tarifaIntervalo: Double? = 0.0
    var porcentaje: Double? = 0.0
    var intTiempoMaximo: Int? = 0
    var intTiempoMinimo: Int? = 0
    var intIntervaloMinutos: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parqueo_actual)

        getComision().execute()
        getTarifas().execute()

        createNotificationChannel()

        txtTime = findViewById(R.id.txtTime)
        mMinuteSeekbar = findViewById<CircleSeekBar>(R.id.seek_minute)
        txtComision = findViewById<TextView>(R.id.txtComision)
        txtSaldo = findViewById<TextView>(R.id.txtMonto)
        txtEstacionamiento = findViewById<TextView>(R.id.txtEst)
        txtTotal = findViewById<TextView>(R.id.txtTotal)
        txtfecha = findViewById<TextView>(R.id.txtFechActual)
        btnAddPago  = findViewById(R.id.btnAddPago)
        txtFechaFinaliza = findViewById(R.id.txtFinalizatime)

        btnAddPago.text = "Pagar $${0.00} MXN"
        txtEstacionamiento.text = "\$${0.00} MXN"

        txtComision.text = "\$${0.00} MXN"
        txtTotal.text = "\$${0.00} MXN"

        var millis = TimeUnit.MINUTES.toMillis(SessionStorage.tiempoPagado)
        mTimeLeftInMillis = millis

        println("millis ${SessionStorage.tiempoPagado}")

        var cDate = Date()
        fDate = SimpleDateFormat("yyyy-MM-dd").format(cDate)

        println("tiempoSession2 ${SessionStorage.tiempoPagado}")

        getSaldo().execute()
        getFechaInicial()

        btnAddPago.setOnClickListener {
            val dateFinal = SessionStorage.fechaFinDate
            val dateInicio = SessionStorage.fechaFinDate
            var tiempoExtension = dateInicio?.time?.let { it1 -> dateFinal?.time?.minus(it1) }
            var minutes = tiempoExtension?.let { it1 -> TimeUnit.MILLISECONDS.toMinutes(it1) }
            if (minutes != null) {
                if(minutes > 180){
                    modalSaveVenta(1, "")
                } else {
                    modalSaveVenta(0, "")
                }
            }
        }

        viewDialog = ViewDialog(this)

        sqLiteHelper = SQLiteHelper(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        newtimer.cancel()
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
                textTitle.text = "No se puede extender el tiempo de aparcado más de 180 minutos"

                btnAceptarDialog.text = "Aceptar"
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
                        viewDialog.showDialog()
                        extenderTiempoTask().execute()
                        mAlertDialog.dismiss()
                    }
                    if (i==1) {
                        mAlertDialog.dismiss()
                    }
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

    private fun changeText( minute:Int) {
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
        var dateFinal = SessionStorage.fechaFinDate
        var calendarFinal = dateFinal?.let { getTime(it) }

        val totalMinutos = strMM.toInt()

        val ONE_MINUTE_IN_MILLIS:Long = 60000//millisecs
        var ONE_SECOND_IN_MILLIS:Long? = null//millisecs
        val afterAddingTenMins = calendarFinal?.plus((totalMinutos * ONE_MINUTE_IN_MILLIS))?.let {
            Date(
                it
            )
        }

        ONE_SECOND_IN_MILLIS = 80
        fechaFinal = SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(afterAddingTenMins)

        txtFechaFinaliza.text = fechaFinal

        val afterAddingOneSecond = calendarFinal?.plus((totalMinutos * ONE_SECOND_IN_MILLIS))?.let {
            Date(
                it
            )
        }

        SessionStorage.fechaFinDate = afterAddingOneSecond

        if (calendarFinal != null) {
            alertTime15Minutos = Date(calendarFinal + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (15 * ONE_MINUTE_IN_MILLIS))
            alertTime10Minutos = Date(calendarFinal + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (10 * ONE_MINUTE_IN_MILLIS))
            alertTime5Minutos = Date(calendarFinal + (totalMinutos * ONE_MINUTE_IN_MILLIS) - (5 * ONE_MINUTE_IN_MILLIS))
        }
    }

    private fun getFechaInicial() {
        var dateInicial = SessionStorage.fechaInicioDate
        var calendarInicial = dateInicial?.let { getTime(it) }

        fechaInicial = SimpleDateFormat("dd/MM/YYYY hh:mm:ss").format(calendarInicial)

        txtfecha.text = fechaInicial
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

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@ParqueoActualActivity));

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
            mMinuteSeekbar.setOnSeekBarChangeListener(object:CircleSeekBar.OnSeekBarChangeListener {
                override fun onChanged(seekbar:CircleSeekBar, curValue:Int) {
                    var step = intIntervaloMinutos
                    var max = intTiempoMaximo
                    var min = intTiempoMinimo

                    if (max != null) {
                        mMinuteSeekbar.maxProcess = (max - min!!) / (step!!)
                    }

                    value = (curValue * step!!) + intTiempoMinimo!!

                    SessionStorage.tiempoPagado = minutosApi.toLong()

                    changeText(value!!)
                }
            })

            mMinuteSeekbar.curProcess = 0

            newtimer = object:CountDownTimer(mTimeLeftInMillis, 1000) {
                override fun onTick(millisUntilFinished:Long) {
                    mTimeLeftInMillis = millisUntilFinished
                    getfechaFinaliza()
                }
                override fun onFinish() {
                    newtimer.cancel()
                }
            }

            newtimer.start()

            costo = tarifaIntervalo

            println("COSTO ${costo}")

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
        val interval = (minute - intTiempoMinimo!!)/intIntervaloMinutos!!

        costo = tarifaMinima!! + tarifaIntervalo?.times(interval)!!

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

    inner class extenderTiempoTask : AsyncTask<String, String, String>() {
        var inString=""

        override fun onPreExecute() {

            println("preExecutee" )
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Movimientos/mtdMovExtenderTiempo?intIdMovimiento="+ SessionStorage.idMovimiento)
                //val mURL = URL("http://74.208.91.19:9000/api/Movimientos/mtdMovExtenderTiempo?intIdMovimiento="+ SessionStorage.idMovimiento)

                val urlParameters = "{" +
                        "\"int_tiempo\":$value,"+
                        "\"fltMonto\":$costo,"+
                        "\"fltTotalConComision\":$costoTotal,"+
                        "\"intIdUsuarioId\" : \"${SessionStorage.idUser}\"" +
                        "}"

                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                println("json_extender " + urlParameters)

                val urlConnect=mURL.openConnection() as HttpsURLConnection
                //val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@ParqueoActualActivity))

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "PUT"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                DataOutputStream(urlConnect.outputStream).use {
                    wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("EXTENDER"+ urlConnect.responseCode + " "+ inString )


            } catch (ex: Exception) {
                println("error AsyncTask extender tiempo $ex ")
            } finally {
                if (urlConnection != null) {
                    //  urlConnection.disconnect()
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
            viewDialog.hideDialog()

            var intent= Intent(baseContext, HistorialActivity::class.java)
            startActivity(intent)
            finish()

            val tipoMovimiento = "Extensión tiempo"

            var movimientoSql = MovimientoActivo(SessionStorage.idPlaca, SessionStorage.strPlaca,
                fechaInicial, fechaFinal, tipoMovimiento, SessionStorage.idUser, SessionStorage.strNumeroCajon)

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

            SessionStorage.fechaFin = fechaFinal

            newtimer.cancel()

        }
    }

    inner class desaparcarTask : AsyncTask<String, String, String>() {

        var inString=""

        override fun onPreExecute() {
            println("preExecutee" )
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                val mURL = URL("https://admin.parkifacil.com/api/api/Movimientos/mtdMovDesaparcar?intIdMovimiento="+SessionStorage.idMovimiento)

                val urlParameters = "{" +
                        "\"lastModifiedBy\":\"${SessionStorage.idUser}\""+
                        "}"
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                println("json_desaparcar" + urlParameters)
                println(postDataLength)

                val urlConnect=mURL.openConnection() as HttpsURLConnection

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@ParqueoActualActivity))

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "PUT"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                DataOutputStream(urlConnect.outputStream).use {
                        wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("DESAPARCAR"+ urlConnect.responseCode + " "+ inString )
            } catch (ex: Exception) {
                println("error AsyncTask desaparcar $ex ")
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
            var intent= Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
            finish()

            println("onPostExecute $result" )
            // Done
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

            urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@ParqueoActualActivity))

            urlConnect.requestMethod = "GET"
            urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)

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
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            format.maximumFractionDigits = 2
            format.currency = Currency.getInstance("MXN")

            txtSaldo.text = "${format.format(saldoActualizado)} MXN"
        }
    }

    inner class generarMultaTask : AsyncTask<String, String, String>() {


        var inString=""

        override fun onPreExecute() {

            println("preExecutee" )
            // Before doInBackground
        }

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null

            try {

                val mURL = URL("https://proyeasyp.rj.r.appspot.com/api/Multas/mtdIngresarMulta")

                var fechaInicio= SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

                println("fecha " + fechaInicio)

                SessionStorage.fechaPago = fechaInicio

                var monto = 2* minutosApi

                val urlParameters = "{" +
                        "        \"created_by\": \"jr\"," +
                        "        \"last_modified_by\":\"jr\"," +
                        "        \"flt_monto\": \"${monto}\"," +
                        "        \"str_motivo\": \"Motivo de la multa\"," +
                        "        \"str_id_agente_id\": \"${SessionStorage.idUser}\"," +
                        "        \"int_id_movimiento_id\": \"${SessionStorage.idMovimiento}\"," +
                        "        \"int_id_saldo_id\" : \"1\"," +
                        "        \"intidconcesion_id\" : 1," +
                        "        \"dtm_fecha\" : \"$fDate\"," +
                        "        \"int_id_vehiculo_id\": ${SessionStorage.idPlaca}" +
                        "}"


                println("parametros" + urlParameters)
                val postData = urlParameters.toByteArray(StandardCharsets.UTF_8) //.getBytes(StandardCharsets.UTF_8)
                val postDataLength = postData.size

                val urlConnect=mURL.openConnection() as HttpURLConnection

                urlConnect.doOutput = true
                urlConnect.instanceFollowRedirects = false
                urlConnect.requestMethod = "POST"
                urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)
                urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty("charset", "utf-8")
                urlConnect.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                urlConnect.useCaches = false

                DataOutputStream(urlConnect.outputStream).use {

                        wr-> wr.write(postData)
                    wr.flush()
                    wr.close()
                }

                inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                println("jso response multa"+ urlConnect.responseCode + " "+ inString )

                //  val gson = Gson()
                //  val response: ResponseModel = gson.fromJson(inString, object : TypeToken<ResponseModel>() {}.type)

                // sessionStorage.idMovimiento = response.idMovimiento!!.toInt()

            } catch (ex: Exception) {
                println("error AsyncTask multa $ex ")
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

            //if(inString.contains("200")) {
            // modalResponse(0)
            // }
            println("onPostExecute $result" )
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

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@ParqueoActualActivity))

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