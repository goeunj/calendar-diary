package com.example.diary

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.time.LocalDate

class Diary : AppCompatActivity(){
    private lateinit var diaryText: Editable
    private lateinit var b: Bitmap
    private lateinit var byteArray: ByteArray

    fun alert(alertPopup: PopupWindow, edit: Button, save: Button, backButton: Button, photo: ImageView){
        alertPopup.isTouchable = true
        alertPopup.showAtLocation(findViewById(R.id.cardView), Gravity.CENTER_VERTICAL, 0, 0)
//                    alertPopup.update(0, 0, (this.resources.displayMetrics.widthPixels/2), (this.resources.displayMetrics.heightPixels/4))
        edit.isClickable = false
        save.isClickable = false
        backButton.isClickable = false
        photo.isClickable = false
    }
    @SuppressLint("DiscouragedApi", "SetTextI18n", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diary)

        val db = DBhelper(this, null)
        var prevDiaryText = ""
        var prevImage: ByteArray? = null

        var editClickFlag = false
        var imageChosenFlag = false

        val bundle: Bundle? = intent.extras
        val diaryMonth: String? = bundle!!.getString("month")
        val diaryDate: String? = bundle.getString("date")
        val index = bundle.getInt("index")

        val date: TextView = findViewById(R.id.date)
        val edit: Button = findViewById(R.id.edit)
        val save: Button = findViewById(R.id.save)
        val diary: TextInputEditText = findViewById(R.id.diaryText)
        val photo: ImageView = findViewById(R.id.photo)
        val backButton: Button = findViewById(R.id.backButton)
        val delete: ImageButton = findViewById(R.id.delete)

        val msgView = layoutInflater.inflate(R.layout.msg, null, false)
        val agree: Button = msgView.findViewById(R.id.yes)
        val disagree: Button = msgView.findViewById(R.id.no)

        val deleteView = layoutInflater.inflate(R.layout.delete, null, false)
        val deleteButton = deleteView.findViewById<Button>(R.id.yes)
        val doNotDeleteButton = deleteView.findViewById<Button>(R.id.no)

        val intent = Intent(this.applicationContext, MainActivity::class.java)
        val alertPopup = PopupWindow(msgView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false)
        val warningPopup = PopupWindow(deleteView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false)

        val days: Button
        val mainView = layoutInflater.inflate(R.layout.activity_main, null, false)

        val sh = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val buttonArr = sh.getInt(index.toString(), 0)
        days = mainView.findViewById<View>(buttonArr) as Button

        val month = LocalDate.now().minusMonths(((LocalDate.now().monthValue - diaryMonth!!.toInt()).toLong())).month.toString()
        date.text = "$month $diaryDate"

        val database = db.getData("$diaryMonth/$diaryDate")
        val databaseBool: Boolean
        @SuppressLint("DiscouragedApi", "Range")
        if(database?.moveToFirst() == true){
            database.moveToFirst()
            databaseBool  = true
            imageChosenFlag = true
            val imgArray: ByteArray = database.getBlob(database.getColumnIndex(DBhelper.IMG_COL))
            val dataText: String = database.getString(database.getColumnIndex(DBhelper.DIARY_COL))
            val bmp = database.let { BitmapFactory.decodeByteArray(imgArray, 0, imgArray.size) }

            photo.setImageBitmap(bmp)
            byteArray = imgArray
            prevImage = imgArray

            diary.text = Editable.Factory.getInstance().newEditable(dataText)
            prevDiaryText = (Editable.Factory.getInstance().newEditable(dataText)).toString()
            diaryText = Editable.Factory.getInstance().newEditable(dataText)
            diary.setTextColor(Color.parseColor("#000000"))

            database.close()
        }else{
            databaseBool = false
        }

        val pickImageFromGalleryForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val image: Uri? = result.data?.data
                try {
                    imageChosenFlag = true
                    val bitmap = BitmapFactory.decodeStream(image?.let {
                        contentResolver.openInputStream(
                            it,
                        )
                    })
                    val w: Int = bitmap.width//get width
                    val h: Int = bitmap.height//get height
                    val height = 500
                    val width = (w * height) / h
                    b = Bitmap.createScaledBitmap(bitmap, width, height, false)
                    photo.setImageBitmap(b)

                }catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                val stream = ByteArrayOutputStream()
                b.compress(Bitmap.CompressFormat.PNG, 100, stream)
                byteArray = stream.toByteArray()
            }else{
                imageChosenFlag = false
                return@registerForActivityResult
            }
        }

        edit.setOnClickListener {
            diary.isEnabled = true
            editClickFlag = true
            save.isEnabled = true
            diary.setSelection(diary.length())
            diary.requestFocus()
            diaryText = diary.editableText
        }

        save.setOnClickListener {
            editClickFlag = false
            diary.text = diaryText
            diary.clearFocus()
            diary.isEnabled = false
            diary.setTextColor(Color.parseColor("#000000"))
            if(diary.text.toString() != "" && imageChosenFlag){
                if(!databaseBool){
    //                    add new data and go back to home
                    db.addDiary("$diaryMonth/$diaryDate", byteArray, diaryText.toString())
                    startActivity(intent)
                }else if(diaryText.toString() == prevDiaryText && byteArray.contentEquals(prevImage)) {
    //                    no update needed
                    Toast.makeText(this, "No changes to save.", Toast.LENGTH_LONG).show()
                }else{
    //                    update data
                    db.update("$diaryMonth/$diaryDate", byteArray, diaryText.toString())
                    startActivity(intent)
                }
            }else if(diary.text.toString() == "" && imageChosenFlag){
    //                diary text not given; diary text cannot be empty
                Toast.makeText(this, "Diary must have some text to save diary.", Toast.LENGTH_LONG).show()
            }else if(diary.text.toString() != "" && !imageChosenFlag){
    //                diary image not given; diary image cannot be empty
                Toast.makeText(this, "Picture must be chosen to save diary.", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this, "Nothing to save.", Toast.LENGTH_LONG).show()
            }
        }

        intent.putExtra("index", index)
        intent.putExtra("month", diaryMonth)
        intent.putExtra("date", diaryDate)

        backButton.setOnClickListener{
            if(diary.text.toString() != "" && imageChosenFlag){
                if(editClickFlag){
//                    if diary and image given, but edit button is clicked (i.e., save isn't clicked)
//                    if diary or image is not given
//                    show popup
                    alert(alertPopup, edit, save, backButton, photo)
                }else if (diaryText.toString() == prevDiaryText && byteArray.contentEquals(prevImage)){
                    startActivity(intent)
                }
            }else if(diary.text.toString() == "" && !imageChosenFlag){
//                no new data; go back to home
                startActivity(intent)
            }else if((diary.text.toString() != "" && !imageChosenFlag) || (diary.text.toString() == "" && imageChosenFlag)){
                alert(alertPopup, edit, save, backButton, photo)
            }
        }

        agree.setOnClickListener{
            edit.isClickable = true
            save.isClickable = true
            backButton.isClickable = true
            photo.isClickable = true
            startActivity(intent)
            alertPopup.dismiss()
        }

        disagree.setOnClickListener {
            alertPopup.dismiss()
            edit.isClickable = true
            save.isClickable = true
            backButton.isClickable = true
            photo.isClickable = true
        }

        delete.setOnClickListener {
            val toDelete = db.getData("$diaryMonth/$diaryDate")
            if(toDelete?.moveToFirst() == false){
                Toast.makeText(this, "No diary to delete", Toast.LENGTH_LONG).show()
            }else {
                warningPopup.isTouchable = true
                warningPopup.showAtLocation(
                    findViewById(R.id.cardView),
                    Gravity.CENTER_VERTICAL,
                    0,
                    0
                )
                edit.isClickable = false
                save.isClickable = false
                backButton.isClickable = false
                photo.isClickable = false
            }
        }

        deleteButton.setOnClickListener {
            db.removeRow("$diaryMonth/$diaryDate")
            days.textScaleX = 1F
            edit.isClickable = true
            save.isClickable = true
            backButton.isClickable = true
            photo.isClickable = true
            startActivity(intent)
        }

        doNotDeleteButton.setOnClickListener {
            warningPopup.dismiss()
            edit.isClickable = true
            save.isClickable = true
            backButton.isClickable = true
            photo.isClickable = true
        }

        photo.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*"
            )
            pickImageFromGalleryForResult.launch(pickIntent)
        }
    }
}

