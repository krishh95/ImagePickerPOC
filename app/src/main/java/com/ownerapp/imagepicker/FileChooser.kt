package com.ownerapp.imagepicker

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.work.*
import androidx.work.WorkInfo.State.*
import com.google.gson.Gson
import com.ownerapp.imagepicker.constants.Constants
import com.ownerapp.imagepicker.workerManagers.CacheFileSaver
import kotlinx.coroutines.*
import java.io.File
import java.util.*


/***
 * intentKeys:
 * OUTPUT_TYPE  of type
 * enum class OutputType{
Base64,File
*return constant for success RESULT_OK for error ERROR_CONST
}
 */
class FileChooser : AppCompatActivity() {
    private var outputFileUri: Uri?=null
    private lateinit var mimeType: Constants.Extensions

    companion object {
        val TAG = FileChooser::class.java.name ?:"FileChooser"
        const val OUTPUT = "OutPut"
        const val ERROR_CONST=-888
        const val RESULT_OK=-1
        /***
         * status ==0 fetch Error only
         * else get data and other values
         */
        class FileChooserResponse(
            val status: Int = ERROR_CONST,
            val data: String,
            val error: String
        ) {
            override fun toString(): String {
                return Gson().toJson(this)
            }
        }
    }

    private val selection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "imageSelection: " + result.data)

            CoroutineScope(Dispatchers.IO).launch {

                  val intent= if(outputFileUri!=null&&outputFileUri.toString().isNotEmpty()){
                val resultData=CacheFileSaver.doWorkAsync(
                        applicationContext.contentResolver,
                        applicationContext.cacheDir,
                        outputFileUri.toString(),
                        Constants.JPEG()
                    ).await()
                      val intent = Intent()
                      intent.putExtra(OUTPUT, resultData.toString())
                      intent
                }
                else if(result.data?.data != null){

                val resultData=CacheFileSaver.doWorkAsync(
                        applicationContext.contentResolver,
                        applicationContext.cacheDir,
                        (result.data?.data as Uri).toString(),
                        mimeType
                    ).await()
                      val intent = Intent()
                      intent.putExtra(OUTPUT, resultData.toString())
                      intent
                }else{
                    null
                }

                if(intent != null)
                    this@FileChooser.setResult(RESULT_OK, intent)
                else
                    this@FileChooser.setResult(ERROR_CONST)

                this@FileChooser.finish()
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alert = AlertDialog.Builder(this)
            .setPositiveButton(
                "Gallery"
            ) { _, _ ->
                mimeType = Constants.JPEG()
                getContent(mimeType, selection)
            }.setNeutralButton(
                "camera"
            ) { _, _ ->
                captureImage(selection)
            }
            .setNegativeButton(
                "PDF"
            ) { _, _ ->
                mimeType = Constants.PDF()
                getContent(mimeType, selection)
            }
            .create()
        alert.setCancelable(false)
        alert.setOnDismissListener {
            it.dismiss()
        }
        alert.setOnCancelListener {
            it.dismiss()
        }
        alert.show()
    }

    private fun getContent(mime: Constants.Extensions, selection: ActivityResultLauncher<Intent>) {
        val intent = Intent()
        intent.type = mime.mimeType
        intent.action = Intent.ACTION_GET_CONTENT
        val txt = when (mime) {
            is Constants.PDF -> {
                "Select PDF"
            }
            else -> {
                "Select Picture"
            }
        }
        selection.launch(Intent.createChooser(intent, txt))
    }

    private fun captureImage( selection: ActivityResultLauncher<Intent>) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file = File(cacheDir, "${UUID.randomUUID().toString().replace("-","").substring(0,15)}.jpg")
        if(file.exists()){
            file.delete()
        }
        file.createNewFile()
        outputFileUri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.provider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        selection.launch(intent)
    }
}