package com.example.kotlinjpegtest

package com.example.camerax

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
import android.service.controls.ControlsProviderService.TAG
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.*
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap

    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isFocusSuccess : Boolean? = null
    private lateinit var factory: MeteringPointFactory
    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private lateinit var currentPhotoPath: String
    private var isPointArrayFull: Boolean = false
    private var isImageArrayFull: Boolean = false

    private var context : Context? = null
    // CameraController
    private lateinit var camera : Camera
    private var cameraController : CameraControl ?= null
    private lateinit var cameraInfo: CameraInfo

    data class pointData (var x : Float, var y : Float)

    private var pointArrayList: ArrayList<pointData> = arrayListOf<pointData>()
    private var imageArrayList : ArrayList<ByteArray> = arrayListOf<ByteArray>()
    private var destImageByteList : ArrayList<ByteArray> = arrayListOf<ByteArray>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this.applicationContext
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // ????????? ?????? ??????
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val displayHeight = resources.displayMetrics.heightPixels
        val displayWidth = resources.displayMetrics.widthPixels
        Log.v("Size Info", "hxw : ${displayHeight}x${displayWidth}")


        val params: ConstraintLayout.LayoutParams = viewBinding.viewFinder.layoutParams as ConstraintLayout.LayoutParams
        params.width = 1080
        params.height = 1440
        viewBinding.viewFinder.layoutParams = params

        viewBinding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }

        // ????????? ????????? ????????? ??????
        viewBinding.multiFocusButton.setOnClickListener{
            factory = viewBinding.viewFinder.meteringPointFactory

            // ????????? ????????? 3?????? arrayList??? ??????
            pointArrayList.clear()
            pointArrayList.add(pointData(289f, 800f))
            pointArrayList.add(pointData(640f, 800f))
            pointArrayList.add(pointData(760f, 800f))

            //arraylist ??? ?????? ???????????? Focus ?????? ????????????
            takeFocusPhoto(0)

        }

        viewBinding.objectDetectionButton.setOnClickListener {
            factory = viewBinding.viewFinder.meteringPointFactory

            // ????????? ????????? 3?????? arrayList??? ??????
            pointArrayList.clear()
            imageArrayList.clear()
//            pointArrayList.add(pointData(289f, 800f))
//            pointArrayList.add(pointData(640f, 800f))
//            pointArrayList.add(pointData(760f, 800f))

            dispatchTakePictureIntent()

            //arraylist ??? ?????? ???????????? Focus ?????? ????????????
            //takeFocusPhoto(0)
        }

        viewBinding.viewFinder.setOnTouchListener { v : View, event : MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {

                    Log.v("Size Info", "viewBinding.viewFinder.width : ${viewBinding.viewFinder.width}")
                    Log.v("Size Info", "viewBinding.viewFinder.height : ${viewBinding.viewFinder.height}")

                    // Get the MeteringPointFactory from PreviewView
                    val factory = viewBinding.viewFinder.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)
                    Log.v("Touch Point", "${event.x}, ${event.y}")
                    Toast.makeText(this,
                        "${event.x}, ${event.y}",
                        Toast.LENGTH_SHORT).show()
                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point)
                        .build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    var result = cameraController?.startFocusAndMetering(action)!!

                    v.performClick()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    /**
     * ML Kit Object Detection function. We'll add ML Kit code here in the codelab.
     */
    private fun runObjectDetection(bitmap: Bitmap) {

        // Step 1. create ML Kit's InputImage object
        val image = InputImage.fromBitmap(bitmap, 0)

        // Step 2. acquire detector object
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE) // ?????? ???????????? ??????( ??????????????? ?????? ?????????)
            .enableMultipleObjects() // ?????? ?????? (?????? ?????? ?????? ?????? ??????)
            .build()
        val objectDetector = ObjectDetection.getClient(options)

        // Setp 3. feed given image to detector and setup callback
        objectDetector.process(image)
            .addOnSuccessListener {
                // Task completed successfully
                debugPrint(it)
                pointArrayList.clear()
                imageArrayList.clear()
                Log.v("Test num", "pointArray size : ${pointArrayList.size}")
                Log.v("pointArray", "pointArray size : ${pointArrayList.size}")

                // Parse ML Kit's DetectedObject and create corresponding visualization data
                val detectedObjects = it.map { obj ->
                    Log.v("PointCheck", "obj.boundingBox.left : ${obj.boundingBox.left}")
                    Log.v("PointCheck", "obj.boundingBox.top : ${obj.boundingBox.top}")
                    Log.v("PointCheck", "obj.boundingBox.right : ${obj.boundingBox.right}")
                    Log.v("PointCheck", "obj.boundingBox.bottom : ${obj.boundingBox.bottom}")

                    var text = "Unknown"

                    // We will show the top confident detection result if it exist
                    if (obj.labels.isNotEmpty()) {
                        val firstLabel = obj.labels.first()
                        text = "${firstLabel.text}, ${firstLabel.confidence.times(100).toInt()}%"
                    }
//      BoxWithText(obj.boundingBox, text)
                    try {
//                        Log.v("PointCheck", "obj.boundingBox.left : ${obj.boundingBox.left}")
//                        Log.v("PointCheck", "obj.boundingBox.top : ${obj.boundingBox.top}")
//                        Log.v("PointCheck", "obj.boundingBox.right : ${obj.boundingBox.right}")
//                        Log.v("PointCheck", "obj.boundingBox.bottom : ${obj.boundingBox.bottom}")
                        var pointX: Float =
                            (obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left) / 2)).toFloat()
                        var pointY: Float =
                            (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top) / 2)).toFloat()
                        Log.v("PointCheck", "x,y : ${pointX}, ${pointY}")

//                        if ( pointX < 0 ) pointX = 0F
//                        if ( pointY < 0 ) pointY = 0F
//                        if(pointX>1000) pointX = 1000F
//                        if(pointY >1800) pointY = 1800F

                        pointArrayList.add(pointData(pointX, pointY))

                    } catch (e: IllegalAccessException) {
                        e.printStackTrace();
                    } catch (e: InvocationTargetException) {
                        e.targetException.printStackTrace(); //getTargetException
                    }

                }
                isPointArrayFull = true

            }
            .addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        Log.v("Test num", "takeFocusPhoto()")


        var v: Boolean? = null
        while (!isPointArrayFull) {
            if (isPointArrayFull == true) {
                Log.v("Test num", "!!!!!!!!!! : ${isPointArrayFull}")
                Log.d("?????????","0: "  + imageArrayList.size.toString())
                isPointArrayFull = false
                takeFocusPhoto(0)
                break
            }
        }
        // ???????????? ??? ??????????????? ???????????? while???
//            while(!isImageArrayFull){
//                if(isImageArrayFull == true){
//                    Log.d("?????????","1: "  + imageArrayList.size.toString())
//                    // SOF~EOI?????????
//                    insertFrameToJpeg()
//                }
//
//            }


    }

    fun insertFrameToJpeg(){
        var sourceByteArray : ByteArray? = null


        var destFrameByteArray : ByteArray? = null
        val outputStream = ByteArrayOutputStream()

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("?????????","insertFrameToJpeg ??????")
            // 1.????????? ????????? ?????? ????????? write
            sourceByteArray = imageArrayList.get(0)
            outputStream.write(sourceByteArray)

            // 2. ????????? ????????? file??? ??? ????????? ????????? file?????? frame ????????? ?????? ??????
            // ????????? write
            for(i:Int in 1..imageArrayList.size){
                var imageByte = imageArrayList.get(i)
                if(imageByte == null){
                    Log.e("user error", "destByte frame not found" )
                }
                extractFrame(imageByte)?.let {
                    destImageByteList.add(it)
                    outputStream.write(it)
                    //  var resultBitMap = byteArrayToBitmap(resultByteArray!!)

                }
            }

            //3. ????????? ????????? ??????
            val resultBytes = outputStream.toByteArray()
            val resultBitMap = byteArrayToBitmap(resultBytes!!)
            var bitmap = drawBitmap(resultBitMap)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q ?????? ????????? ??????. (??????????????? 10, API 29 ????????? ??????)
                if (bitmap != null) {
                    saveImageOnAboveAndroidQ(bitmap)
                    Log.d("?????????","insertFrameToJpeg ??????")
                }
                Toast.makeText(context, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
            } else {
                // Q ?????? ????????? ??????. ????????? ????????? ????????????.
                val writePermission = context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                if (writePermission == PackageManager.PERMISSION_GRANTED) {
                    if (bitmap != null) {
                        saveImageOnUnderAndroidQ(bitmap)
                    }
                    Toast.makeText(context, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        permissionStorage,
                        requestExternalStorageCode
                    )
                }
            }
            Log.d("?????????","insertFrameToJpeg ???")
        }
//            var bundle = Bundle()
//            bundle.putByteArray("image",resultBytes)
//            var fragment = ResultFragment()
//            fragment.arguments = bundle
//            mainActivity.supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment,fragment)
//                .commit()
        // }

    }

    // ????????? ????????? View??? Bitmap??? ?????? ??????.
    private fun drawBitmap(bitmap:Bitmap): Bitmap {
        val backgroundWidth = bitmap?.width!!.toInt()
        val backgroundHeight = bitmap?.height!!.toInt()

        val totalBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888) // ????????? ??????
        val canvas = Canvas(totalBitmap) // ???????????? ???????????? Mapping.

        val imageViewLeft = 0
        val imageViewTop = 0

        canvas.drawBitmap(bitmap!!, imageViewLeft.toFloat(),imageViewTop.toFloat(), null)

        return totalBitmap
    }
    // ??? ???????????? SOF~EOI ????????? ???????????? ???????????? ?????? ByteArray??? ?????? ??????
    fun extractFrame(jpegBytes: ByteArray): ByteArray? {
        var n1: Int
        var n2: Int
        val resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var startCount = 0
        var endCount = 0
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
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }



    private fun debugPrint(detectedObjects: List<DetectedObject>) {
        detectedObjects.forEachIndexed { index, detectedObject ->
            val box = detectedObject.boundingBox

            Log.d(TAG, "Detected object: $index")
            Log.d(TAG, " trackingId: ${detectedObject.trackingId}")
            Log.d(TAG, " boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            detectedObject.labels.forEach {
                Log.d(TAG, " categories: ${it.text}")
                Log.d(TAG, " confidence: ${it.confidence}")
            }

        }

    }


    /**
     *  convert image proxy to bitmap
     *  @param image
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Open a camera app to take photo.
     */
    private fun dispatchTakePictureIntent() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1440, false)
                runObjectDetection(resizedBitmap)
                super.onCaptureSuccess(image)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }

        })

    }


    // ????????? pointArray[i] ?????? ?????????, takePhoto????????? ?????????
    private fun takeFocusPhoto( index : Int ): Boolean{
        Log.v("Test num", "${pointArrayList.size}")
        Log.v("Test num", "takeFocusPhoto ${index}")
        Log.d("?????????", "takeFocusPhoto(${index}):  "  +pointArrayList.size.toString())
        if(index >= pointArrayList.size)
            return false

        Log.v("Test num", "${pointArrayList.get(index)}")
        Log.v("Test num", "takeFocusPhoto ${index}")

        var point = factory.createPoint(pointArrayList.get(index).x, pointArrayList.get(index).y)
        var action = FocusMeteringAction.Builder(point)
            .build()

        Log.v("Test num", "takeFocusPhoto ${index}")

        var result = cameraController?.startFocusAndMetering(action)
        result?.addListener({
            try {
                Log.v("Test num", "takeFocusPhoto ${result}")
                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
                isFocusSuccess = result.get().isFocusSuccessful
                Log.v("Test num", "takeFocusPhoto ${isFocusSuccess}")
            } catch (e:IllegalAccessException){
                Log.e("Error", "IllegalAccessException")
            } catch (e:InvocationTargetException){
                Log.e("Error", "InvocationTargetException")
            }

            if (isFocusSuccess == true) {
                isFocusSuccess = false
                // ????????? ?????? ?????? ?????? ??????
                takePhoto()
                takeFocusPhoto(index+1)

            }
        }, ContextCompat.getMainExecutor(this))
        return true
    }

    // viewFinder??? ?????? ????????? ????????? ???????????? ??????
    private fun takePhoto(){
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    val iStream: InputStream? = contentResolver.openInputStream(output.savedUri!!)
//                        var sourceByteArray = getBytes(iStream!!)
//                        imageArrayList.add(sourceByteArray)
//                        Log.d("?????????", "takePhoto: "  +imageArrayList.size.toString())
//                        // ?????? ???????????? ?????? ?????? ?????? ????????? ???
//                        if(imageArrayList.size == pointArrayList.size){
//                            Log.d("?????????", "?????? ?????? ??????: "  +imageArrayList.size.toString())
//                            isImageArrayFull = true
//                        }
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                }
            }
        )
    }
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    private fun captureVideo(){}

    private fun startCamera(){
        // 1. CameraProvider ??????
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 2. CameraProvier ?????? ?????? ?????? ??????
            // ??????????????? binding ??? ??? ?????? ProcessCameraProvider ?????? ?????????
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 3. ???????????? ???????????? use case??? ?????? ??????????????? binding

            // 3-1. Preview??? ?????? ??? Preview??? ????????? ????????? ???????????? ????????? ??????.
            // surfaceProvider??? ???????????? ?????? ????????? ???????????? ????????? ??????????????? ????????????.
            // setSurfaceProvider??? PreviewView??? SurfaceProvider??? ???????????????.
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // 3-2. ????????? ??????
            // CameraSelector??? ????????? ????????? ?????????.(??????, ?????? ?????????)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // binding ?????? binding ?????????
                cameraProvider.unbindAll()

                // 3-3. use case??? ???????????? ?????? ????????? binding
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                cameraController = camera!!.cameraControl

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = // Array<String>
            mutableListOf (
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
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
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if(uri != null) {
                val image = contentResolver.openFileDescriptor(uri, "w", null)
                // write ????????? file??? open??????.

                if(image != null) {
                    val fos = FileOutputStream(image.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    //???????????? FileOutputStream??? ?????? compress??????.
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // ????????? ????????? ????????????.
                    contentResolver.update(uri, contentValues, null, null)
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

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
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