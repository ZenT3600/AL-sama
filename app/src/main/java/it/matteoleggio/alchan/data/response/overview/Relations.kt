package it.matteoleggio.alchan.data.response.overview

import com.google.gson.annotations.SerializedName


data class Relations (

  @SerializedName("__typename" ) var _typename : String?          = null,
  @SerializedName("edges"      ) var edges     : ArrayList<Edges> = arrayListOf()

)