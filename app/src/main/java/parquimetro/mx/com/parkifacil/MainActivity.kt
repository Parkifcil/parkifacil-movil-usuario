package parquimetro.mx.com.parkifacil

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.confirm_error.view.*
import kotlinx.android.synthetic.main.confirm_pago.view.*
import kotlinx.android.synthetic.main.confirm_zona.view.*
import org.jetbrains.anko.withAlpha
import parquimetro.mx.com.api.ApiConfig
import parquimetro.mx.com.models.*
import parquimetro.mx.com.ssl.CustomSSLSocketFactory
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,GoogleMap.OnCameraMoveListener,GoogleMap.OnCameraIdleListener {

    lateinit var spnCiudad:Spinner
    lateinit var spnZona:Spinner
    lateinit var btnUbicacion :Button
    lateinit var navSaldo: TextView
    lateinit var mapFragment : SupportMapFragment
    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGps: Location? = null
    private var locationNetwork: Location? = null
    private var mMap: GoogleMap? = null
    val lstCiudadesIdNombre = ArrayList<String>()
    val lstCiudadesId = ArrayList<Ciudades>()
    var lstCiudadesLatitud = ArrayList<String>()
    var lstCiudadesLongitud = ArrayList<String>()
    val lstSZonasNombres = ArrayList<String>()
    val lstSZonas = ArrayList<Zonas>()
    var lstMovimientos = ArrayList<MovimientoActivo>()
    var lstDrawPoligono = ArrayList<DrawPoligono>()
    var lstPolygon = ArrayList<LatLng>()
    var latitud: Double= 0.0
    var longitud: Double = 0.0
    var idCiudad: Int = 0
    var idZona: Int = 0
    var siglasZona: String? = ""
    var nameSpinner: String = ""

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        lstMovimientos.clear()
        spnCiudad = findViewById(R.id.spnCiudad)
        spnZona = findViewById(R.id.spnZona)

        spnZona.setEnabled(false)

        btnUbicacion = findViewById<Button>(R.id.btnUbicacion)

        getCiudades().execute()
        lstCiudadesIdNombre.add("Ciudades")
        lstSZonasNombres.add("Zonas")
        spinnerCiudades()
        spinnerZonas()

        Handler(Looper.getMainLooper())

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        navSaldo = headerView.findViewById(R.id.txtSaldoNav) as TextView

        getSaldo(navSaldo).execute()

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        mapFragment = supportFragmentManager.findFragmentById(R.id.frg_map) as SupportMapFragment

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try{
                getLocation()
            }
            catch (ex: Exception){
                println("error permisos para acceder ubicacion $ex")
            }
        } else {
            Toast.makeText(applicationContext, "No tiene permisos de ubicación", Toast.LENGTH_LONG).show()
        }

        if(SessionStorage.dcmLatitud == 0.0 && SessionStorage.dcmLongitud == 0.0){
            finish()
            startActivity(getIntent())
            getLocation()
        }

        ZoomMap(
            SessionStorage.dcmLatitud,
            SessionStorage.dcmLongitud
        )

        btnUbicacion.setOnClickListener {
            if(idCiudad == 0 || idZona == 0){
                Toast.makeText(
                    applicationContext,
                    "Debe elegir una ciudad y una zona",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                alertConfirmarZona()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        getSaldo(navSaldo).execute()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            false
        } else super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun alertConfirmarZona() {
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_zona, null)

        var txtZona = mDialogView.findViewById(R.id.txtZona) as TextView
        var edtCajon = mDialogView.findViewById(R.id.edtCajon) as TextView

        txtZona.text = siglasZona.toString()

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)

        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptar.setOnClickListener {
            if(edtCajon.text.toString() == ""){
                alertDatoFaltante()
            } else {
                this.startActivity(Intent(this, PlacasActivity::class.java))
                SessionStorage.placaStatus = 0
                SessionStorage.strNumeroCajon = edtCajon.text.toString()
                mAlertDialog.dismiss()
            }
        }
    }

    private fun alertDatoFaltante(){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.confirm_error, null)

        mDialogView.txtTitleDialogError.text = "Falta un dato"
        mDialogView.txtTitleError.text = "Debe ingresar el número de cajón"

        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
        val  mAlertDialog = mBuilder.show()

        mDialogView.btnAceptarDialogError.setOnClickListener {
            mAlertDialog.dismiss()
        }

    }

    fun deletePrefs(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()

        editor.remove("usuario")
        editor.remove("password")
        editor.putString("active", "false")
        editor.putString("remember", "false")
        editor.apply()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.historial -> {
                this.startActivity(Intent(this, HistorialActivity::class.java))
            }

            R.id.cambiar_contraseña -> {
                this.startActivity(Intent(this, CambiarPassActivity::class.java))
            }

            R.id.soporte -> {
                //this.startActivity(Intent(this, SoporteActivity::class.java))
                try {
                    val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    startActivity(myIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        this, "Ninguna aplicación puede manejar esta solicitud."
                                + " Para abrirla instale un navegador web.", Toast.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }

            R.id.medios_pago -> {
                this.startActivity(Intent(this, AgregarSaldoActivity::class.java))
            }

            R.id.tarjeta -> {
                this.startActivity(Intent(this, TarjetaActivity::class.java))
            }

            R.id.logout -> {
                deletePrefs()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }

            /*
            R.id.pagar_multa -> {
                this.startActivity(Intent(this, MultaActivity::class.java))
            }*/

            R.id.vehiculos -> {
                this.startActivity(Intent(this, VehiculosActivity::class.java))
                SessionStorage.placaStatus = 1
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }



    private fun spinnerCiudades(){
        try {
            val adapter: ArrayAdapter<String?> =
                object : ArrayAdapter<String?>(
                    this,
                    R.layout.spinner_item,
                    lstCiudadesIdNombre as List<String?>
                ) {
                    override fun isEnabled(position: Int): Boolean {
                        return if (position == 0) {
                            false
                        } else {
                            true
                        }
                    }

                    override fun getDropDownView(
                        position: Int, convertView: View?,
                        parent: ViewGroup?
                    ): View? {
                        val view = super.getDropDownView(position, convertView, parent)
                        val tv = view as TextView
                        if (position == 0) {
                            tv.setTextColor(Color.GRAY)
                        } else {
                            tv.setTextColor(Color.BLACK)
                        }
                        return view
                    }
                }

            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

            spnCiudad.adapter = adapter

            spnCiudad?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item = parent!!.getItemAtPosition(position) as String

                    println("seleccionado $position  other $item")

                    nameSpinner = item

                    var ciudad = parent.getItemAtPosition(position)

                    for(i in lstCiudadesId){
                        if(ciudad == i.strCiudad){
                            if(i.id!! != 1) {
                                Toast.makeText(
                                    applicationContext,
                                    "Ciudad no disponible",
                                    Toast.LENGTH_LONG
                                ).show()
                                spnZona.setEnabled(false)
                                spinnerZonas()
                            } else {
                                idCiudad = i.id!!
                                ZoomMap(
                                    i.strLatitud!!.toDouble(),
                                    i.strLongitud!!.toDouble()
                                )
                                lstDrawPoligono.clear()
                                getPoligono(i.id!!).execute()
                                spnCiudad.visibility=View.VISIBLE
                                getZonas(idCiudad).execute()
                                spnZona.setEnabled(true)
                                spinnerZonas()
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        }
        catch (ex: Exception){
            println("error fill adapter spinner " + ex)
            nameSpinner = ""
        }
    }


    fun spinnerZonas(){
        try {
            val adapter: ArrayAdapter<String?> =
                object : ArrayAdapter<String?>(
                    this,
                    R.layout.spinner_item,
                    lstSZonasNombres as List<String?>
                ) {
                    override fun isEnabled(position: Int): Boolean {
                        return if (position == 0) {
                            false
                        } else {
                            true
                        }
                    }

                    override fun getDropDownView(
                        position: Int, convertView: View?,
                        parent: ViewGroup?
                    ): View? {
                        val view = super.getDropDownView(position, convertView, parent)
                        val tv = view as TextView
                        if (position == 0) {
                            // Set the hint text color gray
                            tv.setTextColor(Color.GRAY)
                        } else {
                            tv.setTextColor(Color.BLACK)
                        }
                        return view
                    }
                }

            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

            spnZona.adapter = adapter

            spnZona?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item = parent!!.getItemAtPosition(position) as String

                    nameSpinner = item

                    var zona = parent.getItemAtPosition(position)

                    for(i in lstSZonas){
                        if (zona == i.strDescripcion){
                            idZona = i.id!!
                            siglasZona = i.strSiglas
                            SessionStorage.idZona = i.id!!
                        }
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

        }
        catch (ex: Exception){
            println("error fill adapter spinner " + ex)
            //positionSpinner = 0
            nameSpinner = ""
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps || hasNetwork) {
            if (hasGps) {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                locationGps = location

                                latitud = locationGps!!.latitude
                                longitud = locationGps!!.longitude

                                SessionStorage.dcmLatitud = latitud
                                SessionStorage.dcmLongitud = longitud
                            }
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {

                        }

                        override fun onProviderEnabled(provider: String?) {

                        }

                        override fun onProviderDisabled(provider: String?) {

                        }

                    })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null)
                    locationGps = localGpsLocation
            }
            if (hasNetwork) {

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                locationNetwork = location


                                latitud = locationNetwork!!.latitude
                                longitud = locationNetwork!!.longitude

                                SessionStorage.dcmLatitud = latitud
                                SessionStorage.dcmLongitud = longitud
                            }
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {

                        }

                        override fun onProviderEnabled(provider: String?) {

                        }

                        override fun onProviderDisabled(provider: String?) {

                        }

                    })

                val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation
            }

            if(locationGps!= null && locationNetwork!= null){
                if(locationGps!!.accuracy > locationNetwork!!.accuracy){

                    latitud = locationNetwork!!.latitude
                    longitud = locationNetwork!!.longitude

                    SessionStorage.dcmLatitud = latitud
                    SessionStorage.dcmLongitud = longitud
                }else{
                    latitud = locationGps!!.latitude
                    longitud = locationGps!!.longitude

                    SessionStorage.dcmLatitud = latitud
                    SessionStorage.dcmLongitud = longitud
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    override fun onCameraMove() {
        mMap!!.clear()
        // display imageView
        // imgPinUp?.visibility = View.VISIBLE
    }

    override fun onCameraIdle() {
        val markerOptions = MarkerOptions().position(mMap!!.cameraPosition.target)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic))
        mMap!!.addMarker(markerOptions)

        var latLng = markerOptions.position

        println("get position" + latLng)
    }

    private fun ZoomMap(lat: Double?, lng: Double?) {
        mapFragment.getMapAsync { mMap ->
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

            val googlePlex = CameraPosition.builder()
                .target(LatLng(lat!!, lng!!))
                .zoom(15F)
                .bearing(0F)
                .tilt(40F)
                .build()
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 5000, null)
        }
    }

    inner class getCiudades : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null
            //var urlConnection: HttpURLConnection? = null

            try {
                lstCiudadesIdNombre.clear()
                lstCiudadesIdNombre.add("Ciudades")
                println("token " + SessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Ciudades/mtdConsultarCiudades")
                //val mURL = URL("http://74.208.91.19:9000/api/Ciudades/mtdConsultarCiudades")

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@MainActivity))

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                publishProgress(inString)

                println("Ciudades " + inString)

                val gson = Gson()

                val ciudadesJson: DataCiudades? = gson.fromJson(inString, DataCiudades::class.java)

                ciudadesJson!!.data?.forEach{
                    lstCiudadesIdNombre.add(it.strCiudad!!)
                    lstCiudadesLatitud.add(it.strLatitud.toString())
                    lstCiudadesLongitud.add(it.strLongitud.toString())
                    lstCiudadesId.add(
                        Ciudades(
                            id = it.id!!,
                            strLatitud = it.strLatitud.toString(),
                            strLongitud = it.strLongitud.toString(),
                            strCiudad = it.strCiudad!!
                        )
                    )
                }

            } catch (ex: Exception) {


                println("error AsyncTask ciudades $ex ")
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

    inner class getZonas(idCiudad: Int) : AsyncTask<String, String, String>() {

        val idCiudad:Int = idCiudad

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            lstSZonasNombres.clear()
            lstSZonasNombres.add("Zonas")

            //var urlConnection: HttpsURLConnection? = null
            var urlConnection: HttpURLConnection? = null

            try {

                val mURL = URL("https://admin.parkifacil.com/api/api/Zonas/mtdZonasXCiudadYConcesion?idCiudad=" + idCiudad + "&idConcesion=2")
                //val mURL = URL("http://74.208.91.19:9000/api/Zonas/mtdZonasXCiudadYConcesion?idCiudad="+idCiudad+"&idConcesion=2")

                val urlConnect = mURL.openConnection() as HttpsURLConnection
                //val urlConnect = mURL.openConnection() as HttpURLConnection

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@MainActivity))

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)

                println("Zonas " + inString)

                val gson = Gson()

                val zonasJson: DataZonasXCiudad? = gson.fromJson(
                    inString,
                    DataZonasXCiudad::class.java
                )

                //zonasJson?.data?.strDescripcion?.let { lstSZonasNombres.add(it) }

                zonasJson?.data?.forEach{
                    lstSZonasNombres.add(
                        it.strDescripcion!!.toString()
                    )
                    lstSZonas.add(
                        Zonas(it.id!!, it.strDescripcion!!, it.strSiglas)
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
            spinnerZonas()
        }
    }

    inner class getPoligono(id: Int?) : AsyncTask<String, String, String>() {

        val idCiudad  = id

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpsURLConnection? = null

            try {
                // println("token " + sessionStorage.tokenSession)

                val mURL = URL("https://admin.parkifacil.com/api/api/Concesiones/mtdConsultarPoligonoXIdCiudad?idCiudad=" + idCiudad)

                val urlConnect = mURL.openConnection() as HttpsURLConnection

                //urlConnect.connectTimeout=9000
                urlConnect.requestMethod = "GET"
                //urlConnect.setRequestProperty("Content-Type", "application/json")
                urlConnect.setRequestProperty(
                    "Authorization",
                    "Bearer " + SessionStorage.tokenSession
                )

                urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@MainActivity));

                var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
                publishProgress(inString)

                println("Poligono " + inString)

                var gson = Gson()

                val poligonoJson: DataPoligono = gson.fromJson(
                    inString,
                    DataPoligono::class.java
                )

                val featuresPoligono = poligonoJson.data?.poligono?.features?.get(0)?.geometry

                val geometryPoligono = featuresPoligono?.coordinates

                val arraySize = (geometryPoligono?.get(0)?.size)?.minus(1)

                for (num in 0..arraySize!!){
                    lstDrawPoligono.add(
                        DrawPoligono(
                            strLongitud = geometryPoligono.get(0).get(num).get(0).toDouble(),
                            strLatitud = geometryPoligono.get(0).get(num).get(1).toDouble()
                        )
                    )
                }
            } catch (ex: Exception) {
                println("error AsyncTask poligono $ex ")
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

            if(lstDrawPoligono.size>0){
                drawPoligonos()
            }
        }
    }

    private fun drawPoligonos() {
        try{
            mapFragment.getMapAsync(object : OnMapReadyCallback {
                @SuppressLint("MissingPermission")

                override fun onMapReady(mMap: GoogleMap) {

                    try {
                        mMap.clear()
                        lstPolygon.clear()

                        try {
                            lstDrawPoligono.forEach {
                                var latLng = LatLng(it.strLatitud!!, it.strLongitud!!)
                                lstPolygon.add(latLng)
                                println("polygon lat ${it.strLatitud}  lng ${it.strLongitud}")
                            }

                            lstPolygon.forEach {
                                println("polygon lat ${it.latitude}  lng ${it.longitude}")
                            }
                        } catch (ex: Exception) {
                            println("error $ex fill list")
                        }

                        mMap.addPolygon(PolygonOptions().apply {
                            try {
                                addAll(lstPolygon)
                                fillColor(Color.parseColor("#0e5a97").withAlpha(30))
                                strokeColor(Color.parseColor("#0e5a97"))
                                strokeWidth(5.toFloat())
                            } catch (ex: Exception) {
                                println("$ex error poligono")
                            }

                        }).run {
                            //tag = CustomTag("origen - poliforum")
                        }
                        // }
                    } catch (ex: Exception) {
                        println("contiene circulos try")
                    }

                }
            })
        }
        catch (ex: Exception){

            println("$ex error de en mapa")
        }


    }

    inner class getSaldo(navSaldo: TextView?) : AsyncTask<String, String, String>() {

        var saldoActualizado: Double = 0.00
        var navSaldo = navSaldo

        override fun onPreExecute() {
            println("preExecutee")
            // Before doInBackground
        }

        private fun consultarSaldoXIdUsuario(){
            val mURL = URL("https://admin.parkifacil.com/api/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)
            //val mURL = URL("http://74.208.91.19:9000/api/Saldos/mtdConsultarSaldoXIdUsuario?intIdUsuario=" + SessionStorage.idUser)

            val urlConnect = mURL.openConnection() as HttpsURLConnection
            //val urlConnect = mURL.openConnection() as HttpURLConnection

            //urlConnect.connectTimeout=9000
            urlConnect.requestMethod = "GET"
            //urlConnect.setRequestProperty("Content-Type", "application/json")
            urlConnect.setRequestProperty("Authorization", "Bearer " + SessionStorage.tokenSession)

            urlConnect.setSSLSocketFactory(CustomSSLSocketFactory.getSSLSocketFactory(this@MainActivity))

            var inString = ApiConfig.ConvertStreamToString(urlConnect.inputStream)
            //Cannot access to ui
            // publishProgress(inString)

            println("Saldo " + inString)

            val gson = Gson()

            var jsonSaldo: dataSaldo = gson.fromJson(
                inString,
                object : TypeToken<dataSaldo>() {}.type
            )

            println("Saldo nav " + jsonSaldo.data.dblSaldoActual)

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
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            format.maximumFractionDigits = 2
            format.currency = Currency.getInstance("MXN")
            navSaldo?.text = "${format.format(saldoActualizado)} MXN"

            println("onPostExecute $result")


        }
    }
}