package com.example.audiorecorder

import MainViewModel
import WebSocketListner
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import java.io.IOException
import java.net.UnknownHostException
import java.util.Arrays


class MainActivity : AppCompatActivity(){

    var permissionsGranted: Boolean = false
    var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    lateinit var mTextView : TextView
    lateinit var mTextView2 : TextView
    lateinit var mTextView3 : TextView
    //==FOR WEBSOCKET===///
    private lateinit var webSocketListener: WebSocketListner
    private lateinit var mainViewModel : MainViewModel
    private var okHttpClient_WebSocket = OkHttpClient()
    private var mWebSocket: WebSocket? = null

    //==========+ON CREATE+=====//
    override fun onDestroy() {
        super.onDestroy()
        mWebSocket?.close(1001,"Closed by android device -> onDestroy.")
        Log.e("myLOG", "mLOG: DISCONNECT WEBSOCKET onDestroy")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mTextView = findViewById(R.id.textView)
        mTextView2 = findViewById(R.id.textView2)
        mTextView3 = findViewById(R.id.textView3)
        //check permission
        permissionsGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
        Log.e("mLOG", "mLOG: Permission : $permissionsGranted")
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }

            val but = findViewById<Button>(R.id.button)
            but.setOnClickListener {
                startStreaming()
            }


        ///===WEBSOCKET==//
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        webSocketListener = WebSocketListner(mainViewModel)
        mainViewModel.socketStatus.observe(this, {
            if(it == true){
                Log.e("mLOG", "mLOG: WEBSOCKET CONNECTED onCreate")
                mTextView.text = "WEBSOCKET CONNECTED : \n ws://server-pg.playgroundx.site:8008"
            }
            else{
                Log.e("mLOG", "mLOG: WEBSOCKET DISCONNECTED onCreate")
                mTextView.text = "WEBSOCKET DISCONNECTED"
            }
        })
        mainViewModel.message.observe(this,{
            val txtS = "${ if (it.first) "You: " else "Server: "} ${it.second} \n"
            Log.e("mLOG", "mLOG: WEBSOCKET \n ${txtS}")
        })

        fun createRequest(): Request {
            //val webSocketURL = "wss://s12191.nyc1.piesocket.com/v3/1?api_key=nvWQD5p7KOqWpJcEQ8lrJdrO5TGfoK6PNseR27ma&notify_self=1"
            val webSocketURL = "ws://server-pg.playgroundx.site:8008"
            return Request.Builder()
                .url(webSocketURL).build()
        }
        //connect websocket server
        mWebSocket = okHttpClient_WebSocket.newWebSocket(createRequest(),webSocketListener)

    }

    private val status = true
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var recorder: AudioRecord? = null
    var minBufSize: Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun startStreaming() {
        //====TRY SEND MESSAGE TO WEBSOCKET===//
        mWebSocket?.send("HELLO WEBSOCKET SERVER")
        mainViewModel.setMessage(Pair(true,"HELLO WEBSOCKET SERVER"))
        val streamThread = Thread {
            try {

                val buffer = ByteArray(minBufSize)
                Log.d("VS", "Buffer created of size $minBufSize")
                println(buffer)

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@Thread
                }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 10
                )
                recorder!!.startRecording();

                 fun getShort(argB1: Byte, argB2: Byte): Short {
                    return (argB1.toInt() and 0xFF or (argB2.toInt() shl 8)).toShort()
                }
                 var cAmplitude: Int = 0
                while (status === true) {
                    //reading data from MIC into buffer

                    minBufSize = recorder!!.read(buffer, 0, buffer.size)

                    for (i in 0 until minBufSize / 2) {
                        val curSample = getShort(buffer[i * 2], buffer[i * 2 + 1])
                        if (curSample > cAmplitude) {
                            cAmplitude = curSample.toInt()
                        }
                    }

                    println(cAmplitude)
                    Log.e("VS", "mLOG cAmplitude- > ${cAmplitude}")
                    runOnUiThread {
                        mTextView.setText("Biên Độ Âm Thanh: \n ${cAmplitude}")
                    }

                    //push to websocket
                    val byteArray = buffer
                    val  byteBufferString = ByteString.of(*byteArray)
                    Log.e("VS", "mLOG byteBufferString ${byteBufferString}")
                    mWebSocket?.send(byteBufferString)
                    //mainViewModel.setMessage(Pair(true,byteBufferString.toString()))
                    runOnUiThread {

                        mTextView2.setText("ByteArrayBuffer: ${Arrays.toString(byteArray)}.")
                        mTextView3.setText("ByteString: $byteBufferString")
                    }
                    cAmplitude = 0
                    // Add a delay of 1 second
                    Thread.sleep(1000)
                }

            } catch (e: UnknownHostException) {
                Log.e("VS", "UnknownHostException")
            } catch (e: IOException) {
                Log.e("VS", "IOException")
                e.printStackTrace()
            }
        }
        streamThread.start()
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            permissionsGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!permissionsGranted) {
                Log.e("mLOG", "mLOG: Permission Denied")
            }
            else{
                Log.e("mLOG", "mLOG: Permission Granted")
            }
        }
    }


}
