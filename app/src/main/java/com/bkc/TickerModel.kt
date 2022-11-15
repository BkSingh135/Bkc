package com.bkc

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Ticker {
    @SerializedName("text")
    @Expose
    var text: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null
}
