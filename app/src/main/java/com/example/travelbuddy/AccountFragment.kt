package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.extensions.toJson
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * A fragment representing a list of Items.
 */
class AccountFragment : Fragment() {
    private var mColumnCount = 2
    var appwriteUserHelper: AppwriteUserHelper? = null
    var userId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            mColumnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        AppwriteClientManager.initialize(requireContext())
        Toast.makeText(this.context, "oncreate AccountFargment", Toast.LENGTH_SHORT).show()
        appwriteUserHelper = AppwriteUserHelper()
        userId = appwriteUserHelper!!.getUserId()

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        if (view is RecyclerView) {
            val context = view.context
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }

            // Launch a coroutine to fetch places
            lifecycleScope.launch {
                val places = fetchPlacesFromAppwrite()
                view.adapter = PlacesItemRecyclerViewAdapter(places)
            }
        }
        return view
    }



    private suspend fun fetchPlacesFromAppwrite(): List<Places> {
        val places = ArrayList<Places>()

        val client = AppwriteClientManager.getClient()
        val account = AppwriteClientManager.getAccount()
        val user = account.get()
        val userId = user.id

        Log.d("userid", userId.toString())

        val queries: List<String> = listOf(
               Query.equal("userId",userId)
        )
        val databases = AppwriteClientManager.getDatabase()


        try {
            val response = databases.listDocuments(
                    "641e5388852e4b190226",
                    "641e53904d90ec403156",
                    queries
            )

            Log.d("Appwrite response", response.toString())


            // Convert the response object to a JSON object
            val jsonResponse = JSONObject(response.toJson())
            val documents = jsonResponse.getJSONArray("documents")

            for (i in 0 until documents.length()) {
                val document = documents.getJSONObject(i)

                // Extract the fields from the data object
                val data = document.getJSONObject("data")
                val name = data.getString("name")
                val address = data.getString("address")
                val latitude = data.getDouble("latitude")
                val longitude = data.getDouble("longitude")

                // Create a Places object and add it to the list
                val place = Places(name, address, latitude, longitude)
                places.add(place)
                // Convert the place object to a String


                // Show a Toast message with the placeString
                Toast.makeText(this.context, "it is a toast", Toast.LENGTH_SHORT).show()
                Log.d("Appwrite fetch place", place.toString())

            }
        } catch (e: AppwriteException) {
            e.printStackTrace()
        }
        // Create new Places objects and add them to the list
//        val place1 = Places("Museum of Modern Art", "11 W 53rd St, New York, NY 10019", 40.7614, -73.9776)
//        val place2 = Places("Central Park", "New York, NY 10024", 40.7851, -73.9683)
//        places.add(place1)
//        places.add(place2)

        return places
    }



    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int): AccountFragment {
            val fragment = AccountFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }
}
