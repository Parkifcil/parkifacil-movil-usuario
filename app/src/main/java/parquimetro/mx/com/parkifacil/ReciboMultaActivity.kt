package parquimetro.mx.com.parkifacil

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import parquimetro.mx.com.models.SessionStorage
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ReciboMultaActivity : AppCompatActivity() {
    lateinit var txtNoTarjeta: TextView
    lateinit var txtFecha: TextView
    lateinit var txtMulta: TextView
    lateinit var btnGenerarTicket: Button

    private val STORAGE_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibo_saldo)

        txtNoTarjeta = findViewById(R.id.txtNoTarjeta)
        txtFecha = findViewById(R.id.txtFecha)
        btnGenerarTicket = findViewById(R.id.btnGenerarTicket)

        val sdf = SimpleDateFormat("dd/M/yyyy")
        val currentDate = sdf.format(Date())

        txtNoTarjeta.text = SessionStorage.noTarjeta
        txtFecha.text = currentDate

        btnGenerarTicket.setOnClickListener {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                    val permission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, STORAGE_CODE)
                } else {
                    savePDF()
                }
            } else {
                savePDF()
            }

            if(SessionStorage.status == 1){
                SessionStorage.status = 0
                val intent = Intent(baseContext, TimeActivity::class.java)
                startActivity(intent)
                finish()
            } else{
                startActivity(Intent(baseContext, MainActivity::class.java))
                finish()
            }
        }
    }

    override fun onBackPressed(){
        if (isTaskRoot) {
            if(SessionStorage.status == 1){
                SessionStorage.status = 0
                startActivity(Intent(applicationContext, TimeActivity::class.java))
            } else{
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
        }
        finish()
    }

    private fun savePDF(){
        val sdf = SimpleDateFormat("dd/M/yyyy")
        val currentDate = sdf.format(Date())

        val mDoc = Document()
        mDoc.setPageSize(PageSize.A7)
        mDoc.setMargins(5F, 5F, 25F, 25F)
        val mFileName = "Recibo_EasyPago_Multa"

        val mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + mFileName + ".pdf"

        try {
            PdfWriter.getInstance(mDoc, FileOutputStream(mFilePath))
            mDoc.open()

            val ims: InputStream = getAssets().open("logo_easypago.png")
            var bmp = BitmapFactory.decodeStream(ims)
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            var stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            var image = Image.getInstance(stream.toByteArray())
            image.scaleAbsolute(60F, 60F);
            image.alignment = Element.ALIGN_CENTER
            mDoc.add(image)

            mDoc.add(Paragraph("\n"))

            val fontLabel = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.LIGHT_GRAY)
            val fontSession = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.BLACK)

            val pointColumnWidths = floatArrayOf(100f, 170f)
            val tableTarjeta = PdfPTable(pointColumnWidths)
            val tableFecha = PdfPTable(pointColumnWidths)
            val tableRecarga = PdfPTable(pointColumnWidths)

            val cellNoTarjeta = PdfPCell(Phrase("\nNúmero de tarjeta: ", fontLabel))
            val cellNoTarjetaSession = PdfPCell(Phrase("\n" + SessionStorage.noTarjeta, fontSession))

            val cellFecha = PdfPCell(Phrase("\nFecha", fontLabel))
            val cellFechaSession = PdfPCell(Phrase("\n" + currentDate, fontSession))

            val cellRecarga = PdfPCell(Phrase("\nMulta de: ", fontLabel))
            val cellRecargaSession = PdfPCell(Phrase("\n" + 0.00, fontSession))

            cellNoTarjeta.borderColor = BaseColor.WHITE
            cellNoTarjetaSession.borderColor = BaseColor.WHITE
            cellFecha.borderColor = BaseColor.WHITE
            cellFechaSession.borderColor = BaseColor.WHITE
            cellRecarga.borderColor = BaseColor.WHITE
            cellRecargaSession.borderColor = BaseColor.WHITE

            tableTarjeta.addCell(cellNoTarjeta)
            tableFecha.addCell(cellFecha)
            tableRecarga.addCell(cellRecarga)
            tableTarjeta.addCell(cellNoTarjetaSession)
            tableFecha.addCell(cellFechaSession)
            tableRecarga.addCell(cellRecargaSession)

            mDoc.addAuthor("EASYPAGO")

            mDoc.add(tableTarjeta)
            mDoc.add(tableFecha)
            mDoc.add(tableRecarga)

            val gracias = Paragraph("\n\n¡GRACIAS!", Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLUE))
            gracias.alignment = Element.ALIGN_CENTER

            mDoc.add(gracias)

            mDoc.close()
            Toast.makeText(this, "$mFileName.pdf \nse almacenó en \n$mFilePath", Toast.LENGTH_SHORT).show()
        } catch (e: Exception){
            Toast.makeText(this, "Hubo problemas para generar el recibo en PDF", Toast.LENGTH_SHORT).show()
            println(e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            STORAGE_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    savePDF()
                } else {
                    Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}