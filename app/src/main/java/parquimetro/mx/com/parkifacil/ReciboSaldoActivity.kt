package parquimetro.mx.com.parkifacil

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.itextpdf.text.*
import com.itextpdf.text.BaseColor
import com.itextpdf.text.PageSize.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import parquimetro.mx.com.models.SessionStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class ReciboSaldoActivity : AppCompatActivity() {
    lateinit var txtNoTarjeta: TextView
    lateinit var txtFecha: TextView
    lateinit var txtMonto: TextView
    lateinit var btnGenerarTicket: Button

    private val STORAGE_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recibo_saldo)

        txtNoTarjeta = findViewById(R.id.txtNoTarjeta)
        txtFecha = findViewById(R.id.txtFecha)
        txtMonto = findViewById(R.id.txtMonto)
        btnGenerarTicket = findViewById(R.id.btnGenerarTicket)

        val sdf = SimpleDateFormat("dd/M/yyyy")
        val currentDate = sdf.format(Date())

        txtNoTarjeta.text = SessionStorage.noTarjeta
        txtFecha.text = currentDate
        txtMonto.text = SessionStorage.recarga.toString()

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
                startActivity(Intent(baseContext, AgregarSaldoActivity::class.java))
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
        mDoc.setPageSize(A7)
        mDoc.setMargins(5F, 5F, 25F, 25F)
        val mFileName = "Recibo_EasyPago"

        val mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + mFileName + ".pdf"

        try {
            PdfWriter.getInstance(mDoc, FileOutputStream(mFilePath))
            mDoc.open()

            val ims: InputStream = getAssets().open("logo_parkifacil_sinfondo.png")
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
            val cellNoTarjetaSession = PdfPCell(
                Phrase(
                    "\n" + SessionStorage.noTarjeta,
                    fontSession
                )
            )

            val cellFecha = PdfPCell(Phrase("\nFecha", fontLabel))
            val cellFechaSession = PdfPCell(Phrase("\n" + currentDate, fontSession))

            val cellRecarga = PdfPCell(Phrase("\nRecarga de: ", fontLabel))
            val cellRecargaSession = PdfPCell(
                Phrase(
                    "\n" + SessionStorage.recarga.toString(),
                    fontSession
                )
            )

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

            mDoc.add(tableTarjeta)
            mDoc.add(tableFecha)
            mDoc.add(tableRecarga)

            val gracias = Paragraph(
                "\n\n¡GRACIAS!", Font(
                    Font.FontFamily.HELVETICA,
                    10f,
                    Font.NORMAL,
                    BaseColor.BLUE
                )
            )
            gracias.alignment = Element.ALIGN_CENTER

            mDoc.add(gracias)

            mDoc.addAuthor("EASYPAGO")
            mDoc.close()
            Toast.makeText(this, "$mFileName.pdf \nse almacenó en \n$mFilePath", Toast.LENGTH_SHORT).show()
            println("$mFileName.pdf \nse almacenó en \n$mFilePath")

            var file: File? = null
            file = File(mFilePath)
            val uri: Uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )

            val target = Intent(Intent.ACTION_VIEW)
            target.setDataAndType(uri, "application/pdf")

            target.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION

            val intent = Intent.createChooser(target, "Open File")

            try{
                this.startActivity(intent)
            } catch (e: Exception){
                Toast.makeText(
                    this,
                    "Para leer el recibo generado descargue un lector de PDF",
                    Toast.LENGTH_SHORT
                ).show()
                println("Error $e")
            }
        } catch (e: Exception){
            Toast.makeText(this, "Hubo problemas para generar el recibo en PDF", Toast.LENGTH_SHORT).show()
            println(e)
        }
    }

}
