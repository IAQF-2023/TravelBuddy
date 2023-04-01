package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
    private var mColumnCount = 1
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        val logoutButton = view.findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            lifecycleScope.launch {
                logoutAndNavigateToMainActivity()
            }
        }

        // Find the RecyclerView by its ID
        recyclerView = view.findViewById<RecyclerView>(R.id.list)
        val context = recyclerView.context
        recyclerView.adapter = null
        recyclerView.recycledViewPool.clear()

        recyclerView.layoutManager = GridLayoutManager(context, mColumnCount)

        // Launch a coroutine to fetch places
        lifecycleScope.launch {
            val places = fetchPlacesFromAppwrite()
            recyclerView.adapter = PlacesItemRecyclerViewAdapter(places)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val places = fetchPlacesFromAppwrite()
            recyclerView.adapter = PlacesItemRecyclerViewAdapter(places)
        }
    }


private suspend fun logoutAndNavigateToMainActivity() {
        // Log out the user using the Appwrite SDK
        try {
            AppwriteClientManager.getAccount().deleteSessions()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: AppwriteException) {
            e.printStackTrace()
            Toast.makeText(context, "Error logging out", Toast.LENGTH_SHORT).show()
            return
        }

        // Navigate back to the MainActivity
        activity?.runOnUiThread {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
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
                Log.d("Appwrite fetch place", place.toString())

            }
        } catch (e: AppwriteException) {
            e.printStackTrace()
        }

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
