package com.sandeep.tracuzproject

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.sandeep.fancyalertdialog.FancyAlertDialog
import com.sandeep.fancyalertdialog.FancyAlertDialogListener
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

var devices = ArrayList<BluetoothDevice>()
var deviceMap = HashMap<String, BluetoothDevice>()
var mArrayAdapter: ArrayAdapter<String>? = null
val uuid = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
var message = ""

class MainActivity : AppCompatActivity() {


    private var textView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setTitle(R.string.bluetooth_name)
        mArrayAdapter = ArrayAdapter(this, R.layout.dialog_select)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
        this.textView = findViewById(R.id.txtView)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener{view ->
            if (BluetoothAdapter.getDefaultAdapter() == null){
                Snackbar.make(view, "Bluetooth is disabled", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.CYAN)
                    .setTextColor(Color.RED)
                    .setAction("Action", null).show()
                alertDialog("Bluetooth Disabled", "Turn on Bluetooth to send Message !")

            }else{
                deviceMap = HashMap()
                devices = ArrayList()
                mArrayAdapter!!.clear()

                val editText = findViewById<EditText>(R.id.editText)
                message = editText.text.toString()
                editText.text.clear()
                for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices){
                    deviceMap[device.address] = device
                    devices.add(device)
                    mArrayAdapter!!.add((if (device.name != null) device.name else "unknown") + "\n"+ device.address + "\nPared")
                }

                if (BluetoothAdapter.getDefaultAdapter().startDiscovery()){
                    val dialog = SelectDeviceDialog()
                    dialog.show(supportFragmentManager, "select_device")
                }
            }
        }
        try{
            BluetoothServerControl(this).start()
        }catch(ex: Exception){
          //  Toast.makeText(this@MainActivity, "Blutooth is off", Toast.LENGTH_SHORT).show()
            alertDialog("Bluetooth Error", "Bluetooth is off !")
        }



    }
    @SuppressLint("SetTextI18n")
    fun appendText(text: String){
        runOnUiThread {
            this.textView?.text = this.textView?.text.toString() + "\n" + text
        }
    }

    private val mReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action){
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val pairedDevice = deviceMap[device!!.address]
                if (pairedDevice == null){
                    var index = -1
                    for (i in devices.indices){
                        val temp = devices[i]
                        if (temp.address == device.address){
                            index = i
                            break
                        }
                    }
                    if (index > -1){
                        if (device.name != null){
                            mArrayAdapter?.insert(
                                (if (device.name != null) device.name else "Unknown") + "\n" + device.address,
                                index
                            )

                        }

                    }else{
                        devices.add(device)
                        mArrayAdapter?.add((if (device.name != null) device.name else "unknown") + "\n" + device.address)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menuz, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.map -> {
                val mapLayout = Intent(this@MainActivity, Map::class.java)
                startActivity(mapLayout)
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun alertDialog(name: String, message: String){
        FancyAlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(message)
            .setNegativeBtnText("close")
            .setAnimation(FancyAlertDialog.Animation.POP)
            .isCancellable(true)
            .setBackgroundColor(R.color.alertBackground)
            .setIcon(R.drawable.ic_warning_black_24dp)
            .setBackgroundColor(R.color.dialogBackground)
            .setNegativeBtnBackground(R.color.dialogBtnBackground)
            .OnNegativeClicked(object: FancyAlertDialogListener {
                override fun OnClick() {
                    Toast.makeText(this@MainActivity, "alert closed", Toast.LENGTH_SHORT).show()
                }
            })
            .OnPositiveClicked(object : FancyAlertDialogListener{
                override fun OnClick() {
                    Toast.makeText(this@MainActivity, "Set bluetooth on", Toast.LENGTH_SHORT).show()

                }
            })
            .build()
    }
}

class BluetoothServerControl(action: MainActivity): Thread(){
    private var cancelled: Boolean
    private val serverSocket: BluetoothServerSocket?
    private val activity = action

    init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null){
            this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            this.cancelled = false
        }else{
            this.serverSocket = null
            this.cancelled = true
        }
    }

    override fun run() {
        var socket: BluetoothSocket

        while (true){
            if (this.cancelled){
                break
            }
            try {
                socket = serverSocket!!.accept()
            }catch (ex: IOException){
                Log.e("err", "$stackTrace, $ex")
                break
            }
            if (!this.cancelled && socket != null){
                Log.i("server", "connecting")
                BluetoothServer(this.activity, socket).start()
            }
        }
    }
    fun cancel(){
        this.cancelled = true
        this.serverSocket!!.close()
    }
}
class BluetoothServer(private val activity: MainActivity, private val socket: BluetoothSocket): Thread(){
    private val inputStream = this.socket.inputStream
    private val outputStream = this.socket.outputStream

    override fun run() {
        try {
            Log.d("server", "input:${inputStream.available()}")
            val avail = inputStream.available()
            val bytes = ByteArray(avail)
            Log.i("server", "reading $avail")
            inputStream.read(bytes, 0, avail)
            val text = String(bytes)
            Log.d("server", "msg: $text")
            Log.i("server", "Message received $text")
            Log.i("server", text)
            activity.appendText(text)
        }catch (ex: Exception){
            Log.e("client", "cannot read data", ex)
        }finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}
class SelectDeviceDialog: DialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.activity)
        builder.setTitle("send message to")
        builder.setAdapter(mArrayAdapter){_, where: Int ->
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            BluetoothClient(devices[where]).start()
        }
        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }
}
class BluetoothClient(device: BluetoothDevice): Thread(){
    private val socket = device.createRfcommSocketToServiceRecord(uuid)

    override fun run() {
        Log.i("client", "connecting")

        this.socket.connect()


        Log.i("client", "sending")
        val outputStream = this.socket.outputStream
        val inputStream = this.socket.inputStream
        try {
            outputStream.write(message.toByteArray())
            outputStream.flush()
            Log.i("client", "sent : $message")
        }catch (ex: Exception){
            Log.e("client", "cannot send", ex)
        }finally {
            outputStream.close()
            inputStream.close()
            this.socket.close()
        }
    }

}