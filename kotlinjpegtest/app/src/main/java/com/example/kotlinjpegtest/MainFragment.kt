package com.example.kotlinjpegtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinjpegtest.data.Marker
import com.example.kotlinjpegtest.databinding.ActivityMainBinding
import com.example.kotlinjpegtest.databinding.FragmentMainBinding
import com.example.kotlinjpegtest.databinding.ItemMarkerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    var sourcePhotoUri : Uri? = null
    var destPhotoUri : Uri? = null
    var resultBitMap: Bitmap? = null
    var sourceByteArray : ByteArray? = null
    var destByteArray : ByteArray? = null
    private lateinit var binding: FragmentMainBinding
    lateinit var mainActivity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 2. Context??? ??????????????? ??????????????? ??????
        mainActivity = context as MainActivity
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        //source ????????? ??????
        binding!!.imageView1.setOnClickListener {
            Log.d("?????????", "1 ??????")
            // ???????????? ??????
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            mainActivity.startActivityForResult(photoIntent, 0)
            // ?????? ??????
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        }

        //dest ????????? ??????
        binding!!.imageView2.setOnClickListener {
            Log.d("?????????", "2 ??????")
            // ???????????? ??????
            var photoIntent = Intent(Intent.ACTION_PICK)
            photoIntent.type = "image/*"
            mainActivity.startActivityForResult(photoIntent, 1)
            // ?????? ??????
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        }

        //add ?????? ??????
        binding!!.btnAdd.setOnClickListener{
            if(sourceByteArray != null && destByteArray != null){
                insertFrameToJpeg(sourceByteArray!!,destByteArray!!)
            }
        }

        // custom insert ?????? ??????
        binding!!.btnNew.setOnClickListener{
            val intent = Intent(getActivity(), CustomMainActivity::class.java)
            startActivity(intent)
        }
        return binding.root
    }


    fun insertFrameToJpeg(sourceByteArray : ByteArray, destByteArray: ByteArray){

        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        CoroutineScope(Dispatchers.Main).launch {
            // 1. source files??? main frame??? ?????????
            destFrameByteArray = extractFrame(destByteArray)
            if(destFrameByteArray == null){
                System.out.println("frame??? ?????? ??? ??????")
                return@launch
            }
            outputStream.write(sourceByteArray)
            outputStream.write(destFrameByteArray)
            // 2. ??????
            val resultBytes = outputStream.toByteArray()
            //

            var bundle = Bundle()
            bundle.putByteArray("image",resultBytes)
            var fragment = ResultFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment,fragment)
                .commit()
        }

    }
    //??????????????? ????????? ???
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        Log.d("??????", " ??????????????? onActivityResult ?????? ${requestCode}")
        // 1?????? image view ?????? (source image)
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                sourcePhotoUri = data?.data
                // ImageView??? image set
                binding.imageView1.setImageURI(sourcePhotoUri)
                val iStream: InputStream? = mainActivity.contentResolver.openInputStream(sourcePhotoUri!!)
                sourceByteArray = getBytes(iStream!!)
                Log.d("?????????", "sourceByteArray ${sourceByteArray}")
            }else{
                mainActivity.finish()
            }
            // 2?????? image view ?????? (dest image)
        }else if(requestCode ==1){
            if(resultCode == Activity.RESULT_OK){
                destPhotoUri = data?.data
                // ImageView??? image set
                binding.imageView2.setImageURI(destPhotoUri)
                val iStream: InputStream? = mainActivity.contentResolver.openInputStream(destPhotoUri!!)
                destByteArray = getBytes(iStream!!)
                Log.d("?????????", "destByteArray ${destByteArray}")
            }else{
                mainActivity.finish()
            }
        }
    }
        @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
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


    fun extractFrame(jpegBytes: ByteArray): ByteArray? {
        var n1: Int
        var n2: Int
        val resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var startCount = 0
        var endCount = 0
        //EOI ??????
        //outputStream.write((byte)Integer.parseInt("ff", 16));
        //outputStream.write((byte)Integer.parseInt("d9", 16));
        var startMax = 2
        val endMax = 1
        var isFindStartMarker = false // ?????? ????????? ???????????? ??????
        var isFindEndMarker = false // ?????? ????????? ???????????? ??????


        for (i in 0 until jpegBytes.size - 1) {

            n1 = Integer.valueOf(jpegBytes[i].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(jpegBytes[i+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }

            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                //println("?????? ?????? : ${i}: ${twoByteToNum}")
                //println("n1 : ${n1}, n2 : ${n2}")
                if (twoByteToNum == jpegConstant.SOF0_MARKER) {
                    println("SOF ?????? ?????? : ${i} : ${twoByteToNum}")
                    startCount++
                    if (startCount == startMax) {
                        startIndex = i
                        isFindStartMarker = true
                    }
                }
                if (isFindStartMarker) { // ????????? ???????????? start ????????? ?????? ???, end ?????? ??????
                    if (twoByteToNum == jpegConstant.EOI_MARKER) {
                        println("EOI ?????? ?????? : ${i}")
                        endCount++
                        if (endCount == endMax) {
                            endIndex = i
                            isFindEndMarker = true
                        }
                    }
                }
            }
        }
        if (!isFindStartMarker || !isFindEndMarker) {
            println("startIndex :${startIndex}")
            println("endIndex :${endIndex}")
            println("Error: ?????? ????????? ???????????? ??????")
            return null
        }
        // ??????
        resultByte = ByteArray(endIndex - startIndex + 2)
        // start ???????????? end ????????? ????????? ???????????? ???????????? resultBytes??? ??????
        System.arraycopy(jpegBytes, startIndex, resultByte, 0, endIndex - startIndex + 2)
        return resultByte
    }

}