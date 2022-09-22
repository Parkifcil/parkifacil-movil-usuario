package parquimetro.mx.com.parkifacil

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.historial_row.view.*
import parquimetro.mx.com.database.SQLiteHelper
import parquimetro.mx.com.models.*
import java.sql.SQLException
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList


class HistorialActivity : AppCompatActivity() {

    var lstMovimientos: ArrayList<MovimientoActivo> = ArrayList()
    var image_details_search: ArrayList<MovimientoActivo> = ArrayList()
    var lstMovimientosSql: ArrayList<MovimientoActivo> = ArrayList()
    lateinit var mlistView: ListView
    lateinit var spnPlacas: Spinner
    lateinit var sqLiteHelper: SQLiteHelper
    var nameSpinner: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_movimientos)

        mlistView = findViewById<ListView>(R.id.listHistorial) as ListView
        spnPlacas = findViewById<Spinner>(R.id.spnPlacas)

        sqLiteHelper = SQLiteHelper(this)

        var lstPlacas = ArrayList<String>()

        try{
            lstMovimientosSql = sqLiteHelper.getAllMovimientos()

            for (item in lstMovimientosSql){
                if(item.idUser == SessionStorage.idUser){
                    item.strPlaca?.let { lstPlacas.add(it) }
                    lstMovimientos.add(item)
                }
            }
        } catch(ex: SQLException){
            print(ex)
        }

        callAdpater()
        adapterSpiner(lstPlacas)
    }

    fun adapterSpiner(lst: ArrayList<String>){
        var aSetPlacas: Set<String> = HashSet<String>(lst)
        var lstPlacas = ArrayList<String>()

        lstPlacas.clear()
        lstPlacas.add("Selecciona una placa")
        lstPlacas.add("Todas las placas")
        lstPlacas.addAll(aSetPlacas)

        try {
            val adapter: ArrayAdapter<String?> =
                object : ArrayAdapter<String?>(
                    this,
                    R.layout.spinner_item,
                    lstPlacas as List<String?>
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

            spnPlacas. adapter = adapter

            spnPlacas?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val item = parent!!.getItemAtPosition(position) as String

                    println("seleccionado $position  other $item")

                    nameSpinner = item

                    for(i in lstMovimientosSql){
                        if(nameSpinner == "Todas las placas"){
                            callAdpater()
                        } else {
                            if (nameSpinner == i.strPlaca){
                                searchPlaca(nameSpinner)
                            }
                        }
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

        }
        catch (ex: Exception){
            println("error fill adapter spinner " + ex)
            nameSpinner = ""
        }
    }

    private fun callAdpater() {
        var myAdapter = MyAdapter(this, lstMovimientos)
        mlistView.adapter = myAdapter
    }

    inner class MyAdapter: BaseAdapter {

        var listNotesAdpater = ArrayList<MovimientoActivo>()
        var context: Context? = null

        constructor(context: Context, listNotesAdpater: ArrayList<MovimientoActivo>) : super() {
            this.listNotesAdpater = listNotesAdpater
            this.context = context
        }

        override fun getView(p0: Int, convertView: View?, parent: ViewGroup?): View {

            var myView = layoutInflater.inflate(R.layout.historial_row, null)

            var myNote = listNotesAdpater[p0]

            try{

                myView.txtPlaca.text =" "+ myNote.strPlaca
                myView.txtCajon.text = "${myNote.strNumeroCajon}"
                myView.txtFechaInicio.text = "${myNote.dtHoraInicio.toString()}"
                myView.txtFechaFin.text = "${myNote.dtmHoraFin}"
                myView.txtTipoMovimiento.text = "${myNote.tipo}"
            }
            catch (e: ParseException) {
                println("error calcular" + e)
            }

            myView!!.setOnClickListener {

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

    private fun searchPlaca(strCodigo: String) {
        image_details_search = ArrayList()
        for (item in lstMovimientosSql)
        {
            if (item.strPlaca!!.toUpperCase().contains(strCodigo.toUpperCase()))
                image_details_search.add(item)
        }
        mlistView.adapter = MyAdapter(this, image_details_search)
    }

}
