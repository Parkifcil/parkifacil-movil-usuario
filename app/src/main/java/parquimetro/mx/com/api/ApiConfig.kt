package parquimetro.mx.com.api

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

class ApiConfig {

    companion object {


        fun ConvertStreamToString(inputStream: InputStream):String{

            val bufferReader= BufferedReader(InputStreamReader(inputStream) as Reader?)
            var line:String
            var AllString:String=""

            try {
                do{
                    line=bufferReader.readLine()
                    if(line!=null){
                        AllString+=line
                    }
                }while (line!=null)
                inputStream.close()
            }catch (ex:Exception){}



            return AllString
        }
    }


}