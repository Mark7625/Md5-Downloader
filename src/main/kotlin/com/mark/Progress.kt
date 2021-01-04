package com.mark

abstract class Progress {

    var fileName : String = ""
    var totalDone : Int = 0
    var size : Int = 0
    var link : String = ""

    abstract fun update(progress: Int, message: String)

}