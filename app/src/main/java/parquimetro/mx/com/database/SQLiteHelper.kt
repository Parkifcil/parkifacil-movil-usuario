package parquimetro.mx.com.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import parquimetro.mx.com.models.MovimientoActivo
import java.lang.Exception

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object{
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "movimiento.db"
        private const val TBL_MOV = "tbl_movimiento"
        private const val ID = "id"
        private const val PLACA = "placa"
        private const val HORA_INICIO = "hora_inicio"
        private const val HORA_FIN = "hora_fin"
        private const val TIPO = "tipo"
        private const val CAJON = "cajon"
        private const val ID_USER = "id_user"
        private const val ID_NOTIFICACION = "id_notificacion"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTbMovimiento = ("CREATE TABLE " + TBL_MOV + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PLACA + " TEXT, " +
                HORA_INICIO + " TEXT, " +
                HORA_FIN + " TEXT, " +
                TIPO + " TEXT, " +
                CAJON + " TEXT, " +
                ID_USER + " TEXT, " +
                ID_NOTIFICACION + " INTEGER)")

        db?.execSQL(createTbMovimiento)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TBL_MOV")
        onCreate(db)
    }

    fun insertMovimiento(movimiento: MovimientoActivo): Long{
        val db = this.writableDatabase

        val contenValues = ContentValues()
        contenValues.put(PLACA, movimiento. strPlaca)
        contenValues.put(HORA_INICIO, movimiento.dtHoraInicio)
        contenValues.put(HORA_FIN, movimiento.dtmHoraFin)
        contenValues.put(TIPO, movimiento.tipo)
        contenValues.put(CAJON, movimiento.strNumeroCajon)
        contenValues.put(ID_USER, movimiento.idUser)
        contenValues.put(ID_NOTIFICACION, movimiento.intIdVehiculoId)

        val success = db.insert(TBL_MOV, null, contenValues)
        db.close()

        return success
    }

    fun getAllMovimientos(): ArrayList<MovimientoActivo>{
        val movList: ArrayList<MovimientoActivo> = ArrayList()
        val selectQuery = "SELECT * FROM $TBL_MOV ORDER BY $ID DESC"
        val db = this.readableDatabase

        val cursor: Cursor?

        try {
            var id:Int
            var intIdVehiculoId:Int
            var strPlaca:String
            var dtHoraInicio:String
            var dtmHoraFin:String
            var tipo: String
            var cajon: String
            var idUser: String

            cursor = db.rawQuery(selectQuery, null)

            if(cursor.moveToFirst()){
                do{
                    id = cursor.getInt(cursor.getColumnIndex("id"))
                    intIdVehiculoId = cursor.getInt(cursor.getColumnIndex("id_notificacion"))
                    strPlaca = cursor.getString(cursor.getColumnIndex("placa"))
                    dtHoraInicio = cursor.getString(cursor.getColumnIndex("hora_inicio"))
                    dtmHoraFin = cursor.getString(cursor.getColumnIndex("hora_fin"))
                    tipo = cursor.getString(cursor.getColumnIndex("tipo"))
                    cajon = cursor.getString(cursor.getColumnIndex("cajon"))
                    idUser = cursor.getString(cursor.getColumnIndex("id_user"))

                    val mov = MovimientoActivo(id, intIdVehiculoId, strPlaca, dtHoraInicio, dtmHoraFin, tipo, cajon, idUser)
                    movList.add(mov)
                } while (cursor.moveToNext())
            }
            db.close()
            return movList
        } catch (e: Exception){
            e.printStackTrace()
            db.close()
            return ArrayList()
        }
    }

    fun getById(id: String): ArrayList<MovimientoActivo> {
        val movList: ArrayList<MovimientoActivo> = ArrayList()
        val selectQuery = "SELECT * FROM $TBL_MOV  where $ID_USER = $id ORDER BY $ID DESC"
        val db = this.readableDatabase

        val cursor: Cursor?

        try {
            var id:Int
            var intIdVehiculoId:Int
            var strPlaca:String
            var dtHoraInicio:String
            var dtmHoraFin:String
            var tipo: String
            var cajon: String
            var idUser: String

            cursor = db.rawQuery(selectQuery, null)

            if(cursor.moveToFirst()){
                do{
                    id = cursor.getInt(cursor.getColumnIndex("id"))
                    intIdVehiculoId = cursor.getInt(cursor.getColumnIndex("id_notificacion"))
                    strPlaca = cursor.getString(cursor.getColumnIndex("placa"))
                    dtHoraInicio = cursor.getString(cursor.getColumnIndex("hora_inicio"))
                    dtmHoraFin = cursor.getString(cursor.getColumnIndex("hora_fin"))
                    tipo = cursor.getString(cursor.getColumnIndex("tipo"))
                    cajon = cursor.getString(cursor.getColumnIndex("cajon"))
                    idUser = cursor.getString(cursor.getColumnIndex("id_user"))

                    val mov = MovimientoActivo(id, intIdVehiculoId, strPlaca, dtHoraInicio, dtmHoraFin, tipo, cajon, idUser)
                    movList.add(mov)
                } while (cursor.moveToNext())
            }
            db.close()
            return movList
        } catch (e: Exception){
            e.printStackTrace()
            db.close()
            return ArrayList()
        }
    }

    fun deleteTable(){
        val db = this.writableDatabase
        println("DROP TABLE")
        db?.execSQL("DROP TABLE IF EXISTS "+TBL_MOV)
        db.close()
    }
}