package parquimetro.mx.com.models

import android.content.Context
import android.database.sqlite.SQLiteDatabase

import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.Nullable

open class DatabaseConexion(@Nullable context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NOMBRE, null, DATABASE_VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(
            "CREATE TABLE " + TABLE_MOVIMIENTOS + "(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "placa TEXT NOT NULL," +
                    "fechaInicio TEXT NOT NULL," +
                    "fechaFin TEXT NOT NULL," +
                    "tipoMovimiento TEXT NOT NULL," +
                    "idNotificacion INTEGER)"
        )
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_MOVIMIENTOS)
        onCreate(sqLiteDatabase)
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NOMBRE = "movimientos.db"
        const val TABLE_MOVIMIENTOS = "movimientos"
    }
}