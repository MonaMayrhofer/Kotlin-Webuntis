import java.io.File
import java.time.LocalDate
import java.time.LocalTime

import java.time.temporal.ChronoUnit.MINUTES
import java.time.format.DateTimeFormatter



fun getPassword(): String{
    var file = File("passwordsave")
    if(file.exists()){
        val password = file.inputStream().bufferedReader().use { it.readText() }
        return password
    }else{
        print("Now passwordsave file found, please enter password: ")
        var password = readLine()!!
        for(i in 1..20) println()
        file.createNewFile()
        file.appendText(password)
        return password
    }
}

fun printLessons(lessons: List<Lesson>){
}

fun main(args: Array<String>){
    val passwd = getPassword()

    println("Setting up webuntis")
    var f = Webuntis("if150113", passwd,"htbla linz leonding")
    println("Webuntis was set up!")

    print("Date [dd.MM.yyyy]: ")
    val dateString = readLine()!!
    var date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    var lastLessonEnded: LocalTime? = null
    val lessons = f.getLessons("3BHIF",date).sortedBy { it.startTime }
    lessons.forEach {
        if(lastLessonEnded != null){
            println("- ${MINUTES.between(lastLessonEnded, it.startTime)}")
        }
        lastLessonEnded = it.endTime
        print("[${it.startTime} - ${it.endTime}] ")
        when{
            it.isAdditional ->      print("!Additional:   ")
            it.isEvent ->           print("!Event:        ")
            it.isSubstitution ->    print("!Substitution: ")
            else ->                 print(" Normal:       ")
        }
        print("${it.subjects[0]?.name} (${it.subjects[0]?.longName})")
        println()
    }
}