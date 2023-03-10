package mx.ggl.asistenciaibiz

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import mx.ggl.asistenciaibiz.databinding.ActivityMainBinding
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapPrimitive
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

private const val PERMISSION_REQUEST = 10

abstract class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<List<String>>{

    //VARIABLES
    //binding
    private lateinit var binding: ActivityMainBinding

    //implementacion de ubicacion
    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var hasNetwork = false
    private var locationGps: Location? = null
    private var locationNetwork: Location? = null

    //implementacion de SOAP
    private val nameSpace = "http://ws.dataservice.ecm.technology.totvs.com/"
    private val methodName ="getDatasetResponse"

    private val soapaction = nameSpace+methodName

    private val url= "https://ibiz.fluig.com:1205/webdesk/ECMDatasetService"

    private val loaderIDconstant = 1


    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_main)
        disableView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission(permissions)) {
                enableView()
            } else {
                requestPermissions(permissions, PERMISSION_REQUEST)
            }
        } else {
            enableView()
        }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress: String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        binding.tvIP.text = "Direccion IP del dispositivo: $ipAddress"
        println("Direccion IP del dispositivo: $ipAddress")

        loaderManager.initLoader(loaderIDconstant,null,this)

    }

    private fun disableView() {
        binding.btnSalida.isEnabled = false
        binding.btnEntrada.isEnabled=false
    }

    private fun enableView() {
        getLocation()
        binding.btnSalida.isEnabled = true
        binding.btnEntrada.isEnabled=true
        Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGps || hasNetwork) {

            if (hasGps) {
                Log.d("CodeAndroidLocation", "hasGps")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3600000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location != null) {
                            locationGps = location
                            binding.tvResult.append("\nGPS ")
                            binding.tvResult.append("\nLatitude : " + locationGps!!.latitude)
                            binding.tvResult.append("\nLongitude : " + locationGps!!.longitude)
                            Log.d("CodeAndroidLocation", " GPS Latitude : " + locationGps!!.latitude)
                            Log.d("CodeAndroidLocation", " GPS Longitude : " + locationGps!!.longitude)
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String) {

                    }

                    override fun onProviderDisabled(provider: String) {

                    }

                })

                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null)
                    locationGps = localGpsLocation
            }
            if (hasNetwork) {
                Log.d("CodeAndroidLocation", "hasGps")
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0F, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location != null) {
                            locationNetwork = location
                            binding.tvResult.append("\nNetwork ")
                            binding.tvResult.append("\nLatitude : " + locationNetwork!!.latitude)
                            binding.tvResult.append("\nLongitude : " + locationNetwork!!.longitude)
                            Log.d("CodeAndroidLocation", " Network Latitude : " + locationNetwork!!.latitude)
                            Log.d("CodeAndroidLocation", " Network Longitude : " + locationNetwork!!.longitude)
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                    }

                    override fun onProviderEnabled(provider: String) {

                    }

                    override fun onProviderDisabled(provider: String) {

                    }

                })

                val localNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation
            }

            if(locationGps!= null && locationNetwork!= null){
                if(locationGps!!.accuracy > locationNetwork!!.accuracy){
                    binding.tvResult.append("\nNetwork ")
                    binding.tvResult.append("\nLatitude : " + locationNetwork!!.latitude)
                    binding.tvResult.append("\nLongitude : " + locationNetwork!!.longitude)
                    Log.d("CodeAndroidLocation", " Network Latitude : " + locationNetwork!!.latitude)
                    Log.d("CodeAndroidLocation", " Network Longitude : " + locationNetwork!!.longitude)
                }else{
                    binding.tvResult.append("\nGPS ")
                    binding.tvResult.append("\nLatitude : " + locationGps!!.latitude)
                    binding.tvResult.append("\nLongitude : " + locationGps!!.longitude)
                    Log.d("CodeAndroidLocation", " GPS Latitude : " + locationGps!!.latitude)
                    Log.d("CodeAndroidLocation", " GPS Longitude : " + locationGps!!.longitude)
                }
            }

        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(permissions[i])
                    if (requestAgain) {
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Active los permisos en ajustes", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (allSuccess)
                enableView()

        }
    }

    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<List<String>> {
        return object : AsyncTaskLoader<List<String>>(this){
            override fun onStartLoading() {
                super.onStartLoading()
                forceLoad()
            }
            override fun loadInBackground(): List<String>? {
                val request = SoapObject(nameSpace,methodName)
                request.addProperty("PROYECTO", "")

                val envelope=SoapSerializationEnvelope(SoapEnvelope.VER12)
                envelope.dotNet = true
                envelope.setOutputSoapObject(request)

                val httpTransport = HttpTransportSE(url)
                try {
                    httpTransport.call(soapaction, envelope)
                    return extractDataFromXmlResponse(envelope)
                }catch(e: Exception) {
                    e.printStackTrace()
                }
                return null
                }
            }
        }



    override fun onLoadFinished(p0: Loader<List<String>>, data: List<String>?) {
        TODO("Not yet implemented")
    }

    override fun onLoaderReset(p0: Loader<List<String>>) {
        TODO("Not yet implemented")
    }
    @Throws(Exception::class)
    private fun extractDataFromXmlResponse(envelope: SoapSerializationEnvelope): List<String>? {
        val listaProyectos =mutableListOf<String>()

        val docBuildFactory=DocumentBuilderFactory.newInstance()
        val docBuilder = docBuildFactory.newDocumentBuilder()
        val doc= docBuilder.parse(InputSource(StringReader(envelope.response.toString())))

        val nodeList=doc.getElementsByTagName("Table")
        for (i in 0..nodeList.length - 1) {
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE){
                val element = node as Element

                listaProyectos.add(element.getElementsByTagName("PROYECTO").item(0).textContent)
            }
        }
        return listaProyectos
    }
}



/*private lateinit var binding: ActivityMainBinding



override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    //setContentView(R.layout.activity_main)
    //inicializar binding
    binding=ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root) //vista principal
}*/

