package com.example.kotlinjpegtest

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinjpegtest.data.Marker
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import com.example.kotlinjpegtest.databinding.FragmentMainBinding
import com.example.kotlinjpegtest.databinding.FragmentResultBinding
import com.example.kotlinjpegtest.databinding.ItemMarkerBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResultFragment : Fragment() {
    var jpegConstant : JpegConstant = JpegConstant()
    var resultBitMap: Bitmap? = null
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap
    private lateinit var binding: FragmentResultBinding
    var markerDataList : ArrayList<Marker> =arrayListOf()
    lateinit var mainActivity: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // 2. Context??? ??????????????? ??????????????? ??????
        mainActivity = context as MainActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentResultBinding.inflate(inflater, container, false)

        val resultByteArray = this.arguments?.getByteArray("image")
        resultBitMap = byteArrayToBitmap(resultByteArray!!)
        getMarker(resultByteArray)
        binding.resultImageView.setImageBitmap(resultBitMap)

        //save ?????? ??????
        binding!!.btnSave.setOnClickListener{
            val bitmap = drawBitmap()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q ?????? ????????? ??????. (??????????????? 10, API 29 ????????? ??????)
                if (bitmap != null) {
                    saveImageOnAboveAndroidQ(bitmap)
                }
                Toast.makeText(mainActivity, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
            } else {
                // Q ?????? ????????? ??????. ????????? ????????? ????????????.
                val writePermission = ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if(writePermission == PackageManager.PERMISSION_GRANTED) {
                    if (bitmap != null) {
                        saveImageOnUnderAndroidQ(bitmap)
                    }
                    Toast.makeText(mainActivity, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(mainActivity, permissionStorage, requestExternalStorageCode)
                }
            }
        }
        val linearLayoutManager = LinearLayoutManager(mainActivity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding!!.recycleView.setLayoutManager(linearLayoutManager)
        binding!!.recycleView.adapter = MarkerAdapter()
        return binding.root
        return binding.root
    }
    inner class MarkerViewHolder(val binding: ItemMarkerBinding) : RecyclerView.ViewHolder(binding.root)
    @SuppressLint("SuspiciousIndentation")
    inner class MarkerAdapter : RecyclerView.Adapter<MarkerViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding: ItemMarkerBinding = ItemMarkerBinding.inflate(inflater, parent, false)
            return MarkerViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return markerDataList!!.size
        }

        override fun onBindViewHolder(holder: MarkerViewHolder, position: Int) {
            val item = markerDataList!![position]
            Log.d("blutooth scan","onBindViewHolder. (position = ${position}, item = ${item})")
            holder.binding.textView.text=  item.name
            holder.binding.textView2.text= item.index
        }

    }
    fun getMarker(byteArray: ByteArray){
        var n1: Int
        var n2: Int

        for (i in 0 until byteArray.size - 1) {

            n1 = Integer.valueOf(byteArray[i].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(byteArray[i+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }

            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                //println("?????? ?????? : ${i}: ${twoByteToNum}")
                //println("n1 : ${n1}, n2 : ${n2}")
                var curMarker = Marker()
                curMarker.index = i.toString()
                curMarker.name = jpegConstant.nameHashMap.get(twoByteToNum);
                markerDataList!!.add(curMarker)
            }
        }
    }
    // ????????? ????????? View??? Bitmap??? ?????? ??????.
    private fun drawBitmap(): Bitmap {
        val backgroundWidth = resultBitMap?.width!!.toInt()
        val backgroundHeight = resultBitMap?.height!!.toInt()

        val totalBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888) // ????????? ??????
        val canvas = Canvas(totalBitmap) // ???????????? ???????????? Mapping.

        val imageViewLeft = 0
        val imageViewTop = 0

        canvas.drawBitmap(resultBitMap!!, imageViewLeft.toFloat(),imageViewTop.toFloat(), null)

        return totalBitmap
    }
    // Byte??? Bitmap?????? ??????
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    //Android Q (Android 10, API 29 ??????????????? ??? ???????????? ????????? ???????????? ????????????.)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageOnAboveAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".jpg" // ???????????? ????????????.jpg
        /*
        * ContentValues() ?????? ??????.
        * ContentValues??? ContentResolver??? ????????? ??? ?????? ?????? ???????????? ???????????? ????????????.
        * */
        val contentValues = ContentValues()
        contentValues.apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave") // ?????? ??????
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName) // ??????????????? put?????????.
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.IS_PENDING, 1) // ?????? is_pending ???????????? ???????????????.
            // ?????? ????????? ??? ???????????? ???????????? ??????????????? ?????????, ?????? ???????????? ????????? ??? ??????.
        }

        // ???????????? ????????? uri??? ?????? ??????????????????.
        val uri = mainActivity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null) {
                val image = mainActivity.contentResolver.openFileDescriptor(uri, "w", null)
                // write ????????? file??? open??????.

                if(image != null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    //???????????? FileOutputStream??? ?????? compress??????.
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // ????????? ????????? ????????????.
                    mainActivity.contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun saveImageOnUnderAndroidQ(bitmap: Bitmap) {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)

        if(dir.exists().not()) {
            dir.mkdirs() // ?????? ???????????? ?????? ??????
        }

        try {
            val fileItem = File("$dir/$fileName")
            fileItem.createNewFile()
            //0KB ?????? ??????.

            val fos = FileOutputStream(fileItem) // ?????? ????????? ?????????

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            //?????? ????????? ????????? ????????? ????????? Bitmap ??????.

            fos.close() // ?????? ????????? ????????? ?????? close

            mainActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
            // ?????????????????? ??????????????? ?????? ????????? ?????? ?????? ??????. ????????? ???????????? ????????? ????????? Uri??? ????????????.
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}