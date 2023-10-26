package com.example.diary

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity(){
    var days = arrayOf<Button>()
    private val daysOfWeek = arrayOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    private val now = LocalDate.now().monthValue
    private val resourceArr = IntArray(42)

    private var dayOfWeek = 0
    private var year = LocalDate.now().year
    private var month = LocalDate.now().minusMonths(0).monthValue
    private var lastDay = LocalDate.now().lengthOfMonth()
    private var firstDay = LocalDate.of(year, now, 1).dayOfWeek.name
    private var newMonth = 0

    @SuppressLint("DiscouragedApi", "Range")
    fun makeCalendar(month: String, daysOfWeek: Int, lastDay: Int, vararg days: Button){
        val db = DBhelper(this, null)

        for(i in days.indices){
            days[i].text = ""
            days[i].background = null
            days[i].isEnabled = false
        }

        var index = 1
        for(i in daysOfWeek until lastDay+daysOfWeek){
            days[i].text = (index).toString()
            days[i].textScaleX = 1F
            days[i].isEnabled = true
            index += 1
        }

        val database = db.getDatabase()
        if(database?.moveToFirst() == true){
            database.moveToFirst()
            do {
                val imgArray: ByteArray = database.getBlob(database.getColumnIndex(DBhelper.IMG_COL))
                val day: String = database.getString(database.getColumnIndex(DBhelper.DATE_COL))
                val dayDate = day.split("/")
                val bmp = database.let { BitmapFactory.decodeByteArray(imgArray, 0, imgArray.size) }
                if(dayDate[0] == month){
                    val w: Int = bmp.width//get width
                    val h: Int = bmp.height//get height
                    val width = (150*w)/h
                    val b = Bitmap.createScaledBitmap(bmp, width, 100, false)
                    val finalImage = BitmapDrawable(resources, b)

                    days[(dayDate[1].toInt() + daysOfWeek - 1)].background = finalImage
                    days[(dayDate[1].toInt() + daysOfWeek - 1)].textScaleX = 0F
                }
            }while(database.moveToNext())
            database.close()
        }
    }

    @SuppressLint("DiscouragedApi", "Range")
    fun refreshCalendar(index: Int, month: String, vararg days: Button){
        val db = DBhelper(this, null)
        val date = days[index].text.toString()
        val byteArray = db.getData("$month/$date")

        if(byteArray?.moveToFirst() == true){
            byteArray.moveToFirst()
            val imgArray: ByteArray = byteArray.getBlob(byteArray.getColumnIndex(DBhelper.IMG_COL))
            val bmp = byteArray.let { BitmapFactory.decodeByteArray(imgArray, 0, imgArray.size) }
            val finalImage = BitmapDrawable(resources, bmp)
            days[index].background = finalImage
            days[index].textScaleX = 0F
            byteArray.close()
        }
    }

    private fun getDayOfWeek(daysOfWeek: Array<String>, firstDay: String): Int {
        var num = 1
        for(i in daysOfWeek.indices){
            if(firstDay == daysOfWeek[i]){
                num = i
            }
        }
        return num
    }

    @SuppressLint("DiscouragedApi", "SetTextI18n", "Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prev: Button = findViewById(R.id.prev)
        val next: Button = findViewById(R.id.next)
        val diaryMonth: TextView = findViewById(R.id.month)

        var index = 0
        for(i in 1..42){
            val day = this.resources.getIdentifier("day$i", "id", this.packageName)
            resourceArr[index] = day
            days = days.plus(findViewById<View>(day) as Button)
            index += 1
        }

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val newDiaryMonth: String? = bundle.getString("month")
            val newDayIndex: Int = bundle.getInt("index")

            val m = LocalDate.now().minusMonths((LocalDate.now().monthValue - newDiaryMonth!!.toInt()).toLong())
            firstDay = LocalDate.of(year, m.monthValue, 1).dayOfWeek.name
            dayOfWeek = getDayOfWeek(daysOfWeek, firstDay)
            lastDay = m.lengthOfMonth()
            newMonth = newDiaryMonth.toInt()

            if(m.monthValue.toString() != month.toString()){
                month = m.monthValue
                diaryMonth.text = m.month.toString() + " " + year.toString()
                makeCalendar(month.toString(), dayOfWeek, lastDay, *days)
            }else{
                refreshCalendar(newDayIndex, diaryMonth.text.toString(), *days)
            }
        }

        if(newMonth != 0){
            diaryMonth.text = LocalDate.now().minusMonths(LocalDate.now().monthValue - newMonth.toLong()).month.toString() + " " + year.toString()
        }else{
            diaryMonth.text = LocalDate.now().month.toString() + " " + year.toString()
        }

        dayOfWeek = getDayOfWeek(daysOfWeek, firstDay)
        makeCalendar(month.toString(), dayOfWeek, lastDay, *days)

        val sharedPref = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        for( i in 0..41){
            editor.putInt(i.toString(), resourceArr[i])
        }
        editor.apply()

        for (i in days.indices){
            days[i].setOnClickListener{
                val intent = Intent(this, Diary::class.java)
                intent.putExtra("month", month.toString())
                intent.putExtra("date", days[i].text.toString())
                intent.putExtra("index", i)

                startActivity(intent)
            }
        }

        var prevCounter = 0
        var yearCounter = 0
        prev.setOnClickListener {
            prevCounter += 1

//            check if #of prev clicks are == current month or multiple of 12 to change year
            if(prevCounter == now || ( prevCounter - now) % 12 == 0){
                yearCounter += 1
                year = LocalDate.now().minusYears(yearCounter.toLong()).year
            }

            if(newMonth == 0){
                val m = LocalDate.now().minusMonths(prevCounter.toLong())
                month = m.monthValue
                firstDay = LocalDate.of(year, month, 1).dayOfWeek.name
                lastDay = m.lengthOfMonth()
                diaryMonth.text = m.month.toString() + " " + year.toString()
            }else{
                val m = LocalDate.now().minusMonths((LocalDate.now().monthValue - newMonth).toLong() + prevCounter)
                month = m.monthValue
                firstDay = LocalDate.of(year, month, 1).dayOfWeek.name
                lastDay = m.lengthOfMonth()
                diaryMonth.text = m.month.toString() + " " + year.toString()
            }

            dayOfWeek = getDayOfWeek(daysOfWeek, firstDay)
            makeCalendar(month.toString(), dayOfWeek, lastDay, *days)
        }

        next.setOnClickListener {
            prevCounter -= 1

//            check if #of prev clicks are == current month or multiple of 12 to change year
            if((prevCounter.absoluteValue + now) == 13 || ((prevCounter.absoluteValue + now) > 12 && (prevCounter.absoluteValue + now) % 12 == 0)){
                yearCounter -= 1
                year = LocalDate.now().minusYears(yearCounter.toLong()).year
            }
            if(newMonth == 0){
                val m = LocalDate.now().minusMonths(prevCounter.toLong())
                month = m.monthValue
                firstDay = LocalDate.of(year, month, 1).dayOfWeek.name
                lastDay = m.lengthOfMonth()
                diaryMonth.text = m.month.toString() + " " + year.toString()
            }else{
                val m = LocalDate.now().minusMonths((LocalDate.now().monthValue - newMonth).toLong() + prevCounter)
                month = m.monthValue
                firstDay = LocalDate.of(year, month, 1).dayOfWeek.name
                lastDay = m.lengthOfMonth()
                diaryMonth.text = m.month.toString() + " " + year.toString()
            }

            dayOfWeek = getDayOfWeek(daysOfWeek, firstDay)
            makeCalendar(month.toString(), dayOfWeek, lastDay, *days)
        }
    }
}