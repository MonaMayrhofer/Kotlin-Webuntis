import khttp.get
import khttp.post
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

data class SessionSave(val sessionId: String, val schoolName: String)
data class SchoolClass(val classTeacher1: String?, val classTeacher2: String?, val displayName: String?, val name: String, val id: Int)
data class Lesson(val date: LocalDate, val studentCount: Int, val lessonsCode: String, val code: Int, val periodText: String?, val lessonId: Int,
                  val lessonText: String, val studentGroup: String?, val isSubstitution: Boolean, val isAdditional: Boolean, val isEvent: Boolean, val priority: Int,
                  val hasInfo: Boolean, val cellState: String, val lessonNumber: Int, val startTime: LocalTime, val roomCapacity: Int,
                  val id: Int, val endTime: LocalTime)

fun parseSchoolClass(json: JSONObject): SchoolClass? {
    val ct = if(!json.isNull("classteacher")) json.getJSONObject("classteacher").getString("longName") else null
    val ct2 = if(!json.isNull("classteacher2")) json.getJSONObject("classteacher2").getString("longName") else null
    val dpname = if(!json.isNull("displayname")) json.getString("displayname") else null
    val name = if(!json.isNull("name")) json.getString("name") else null
    val id = if(!json.isNull("id")) json.getInt("id") else null

    if(name == null || id == null) return null
    return SchoolClass(ct, ct2, dpname,name,id)
}

fun getClasses(sessionInfo: String, schoolName: String): HashMap<String, SchoolClass>{
    var headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Encoding" to "gzip, deflate, sdch, br",
            "Accept-Language" to "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "Cookie" to "page=; JSESSIONID=$sessionInfo; schoolname=\"$schoolName\""
    )
    val classes = get("https://mese.webuntis.com/WebUntis/api/public/timetable/weekly/pageconfig?type=1", headers = headers)
    var jsonClasses = classes.jsonObject.getJSONObject("data").getJSONArray("elements")
    val classesMap = HashMap<String, SchoolClass>()

    jsonClasses.forEach { jsonClass ->
        run {
            if (jsonClass is JSONObject) {
                val c = parseSchoolClass(jsonClass)
                if (c != null)
                    classesMap.put(c.name, c)
            }
        }
    }
    return classesMap
}

fun login(username: String, password: String, school: String = "htbla linz leonding") : SessionSave? {
    val payload = mapOf(
            "school" to school,
            "j_username" to username,
            "j_password" to password,
            "token" to ""
    )
    val headers = mapOf(
            "accept" to "application/json",
            "accept-encoding" to "gzip, deflate, br,gzip, deflate",
            "accept-language" to "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4",
            "cache-control" to "no-cache",
            "connection" to "keep-alive",
            "cookie" to "page=; JSESSIONID=; schoolname=\"_aHRibGEgbGlueiBsZW9uZGluZw\"",
            "host" to "mese.webuntis.com",
            "origin" to "https://mese.webuntis.com",
            "pragma" to "no-cache",
            "referer" to "https://mese.webuntis.com/WebUntis/index.do",
            "user-agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"
    )
    val response = post("https://mese.webuntis.com/WebUntis/j_spring_security_check", headers=headers, data=payload, allowRedirects = false)

    val jsessionId = response.headers["Set-Cookie"]?.substringAfter("JSESSIONID=")?.substringBefore(';')
    val schoolname = response.headers["Set-Cookie"]?.substringAfter("schoolname=\"")?.substringBefore("\";")
    println("Statuscode: ${response.statusCode}")
    println("SessionId:  $jsessionId")
    println("Schoolname: $schoolname")

    if(jsessionId == null || schoolname == null){
        println("Could not fetch sessionid or schoolname")
        return null
    }
    if(response.statusCode != 200) {
        val validationHeaders = mapOf(
                "Accept" to "application/json",
                "Cookie" to "page=; JSESSIONID=$jsessionId; schoolname=\"$schoolname\""
        )
        val validationResponse = get("https://mese.webuntis.com/WebUntis/", headers = validationHeaders, allowRedirects = false)
        println(validationResponse.text)
    }else{
        println("Probably successful login")
        return SessionSave(jsessionId, schoolname)
    }
    return null
}

fun getLessons(sessionInfo: String, schoolName: String, classId: Int): JSONArray{
    val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Encoding" to "gzip, deflate, sdch, br",
            "Accept-Language" to "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "Cookie" to "page=; JSESSIONID=$sessionInfo; schoolname=\"$schoolName\""
    )
    val classes = get("https://mese.webuntis.com/WebUntis/api/public/timetable/weekly/data?elementType=1&elementId=273&date=2017-11-21&formatId=2", headers = headers)
    return classes.jsonObject.getJSONObject("data").getJSONObject("result").getJSONObject("data").getJSONObject("elementPeriods").getJSONArray(classId.toString())
}

fun parseLesson(json: JSONObject): Lesson{
    try {
        val date = LocalDate.parse(json.getInt("date").toString(), DateTimeFormatter.ofPattern("yyyyMMdd"))
        val studentsCount = json.getInt("studentCount")
        val lessonsCode = json.getString("lessonCode")
        val code = json.getInt("code")
        val periodText = json.getString("periodText")
        val lessonId = json.getInt("lessonId")
        val lessonText = json.getString("lessonText")
        val studentGroup = if (json.has("studentGroup")) json.getString("studentGroup") else null

        val isObject = json.getJSONObject("is")
        val isSubstitution = if(isObject.has("substitution")) isObject.getBoolean("substitution") else false
        val isEvent = if(isObject.has("event")) isObject.getBoolean("event") else false
        val isAdditional = if(isObject.has("additional")) isObject.getBoolean("additional") else false

        val priority = json.getInt("priority")
        val hasInfo = json.getBoolean("hasInfo")
        val cellState = json.getString("cellState")
        val lessonNumber = json.getInt("lessonNumber")

        var startTimeString = json.getInt("startTime").toString()
        if(startTimeString.length == 3) startTimeString = "0"+startTimeString

        val startTime = LocalTime.parse(startTimeString, DateTimeFormatter.ofPattern("HHmm"))
        val roomCapacity = json.getInt("roomCapacity")
        val id = json.getInt("id")
        var endTimeString = json.getInt("endTime").toString()

        if(endTimeString.length == 3) endTimeString = "0"+endTimeString
        val endTime = LocalTime.parse(endTimeString, DateTimeFormatter.ofPattern("HHmm"))

        return Lesson(date, studentsCount, lessonsCode, code, periodText, lessonId, lessonText, studentGroup, isSubstitution, isAdditional,
                isEvent, priority, hasInfo, cellState, lessonNumber, startTime, roomCapacity, id, endTime)
    }catch (ex: JSONException){
        println(json)
        throw ex
    }

}

fun main(args: Array<String>){
    println("Requesting Classes")

    print("Password: ")
    val passwd = readLine()!!
    for(i in 1..20) println(".")
    val (sessionId, schoolname) = login("if150113",passwd) ?: throw IllegalStateException("Could not login")


    val destClass = "3BHIF"
    val destDate = "20.11.2017"
    val date = LocalDate.parse("20.11.2017",DateTimeFormatter.ofPattern("d.M.yyyy"))

    val classes = getClasses(sessionId, schoolname)
    val threeBeehive = classes[destClass] ?: throw IllegalStateException("Could not find 3BHIF class")

    val jsonLessons = getLessons(sessionId,schoolname,threeBeehive.id).filter { lesson -> if(lesson is JSONObject) lesson.getInt("date") == date.year*10000+date.month.value*100+date.dayOfMonth else false }
    val lessons = MutableList<Lesson>(jsonLessons.count(), {ind -> parseLesson(jsonLessons[ind] as JSONObject)})
    jsonLessons.forEach { lesson-> if(lesson is JSONObject) parseLesson(lesson) }

    lessons.forEach { less -> println(less) }

    println("Done")
}