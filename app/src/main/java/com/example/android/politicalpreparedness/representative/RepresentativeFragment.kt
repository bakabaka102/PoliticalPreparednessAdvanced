package com.example.android.politicalpreparedness.representative

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.basecontent.BaseFragment
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.representative.adapter.RepresentativeListAdapter
import com.example.android.politicalpreparedness.representative.adapter.RepresentativeListener
import com.example.android.politicalpreparedness.utils.isAccessFineLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

class RepresentativeFragment : BaseFragment<FragmentRepresentativeBinding>() {

    private val mViewModel: RepresentativeViewModel by viewModels()
    private lateinit var mContext: Context
    private lateinit var representativeAdapter: RepresentativeListAdapter
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionAccessLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocation()
            } else {
                Snackbar.make(
                    mFragmentBinding.root,
                    "No permission access location",
                    Snackbar.LENGTH_SHORT
                ).show()

            }
        }

    //TODO: Declare ViewModel
    //TODO: Establish bindings
    //TODO: Define and assign Representative adapter
    //TODO: Populate Representative adapter
    //TODO: Establish button listeners for field and location search

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun initData(data: Bundle?) {
        mFragmentBinding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel
            executePendingBindings()
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun initViews() {
        loadStates()
        /*mFragmentBinding.address =
            Address("Amphitheatre Parkway", "1600", "Mountain View", "California", "94043")*/
        loadRecyclerView()
    }

    private fun loadRecyclerView() {
        representativeAdapter = RepresentativeListAdapter(RepresentativeListener { })
        mFragmentBinding.representativesRecyclerView.adapter = representativeAdapter
        val linearLayoutManager = GridLayoutManager(requireActivity(), 1)
        mFragmentBinding.representativesRecyclerView.layoutManager = linearLayoutManager
    }

    private fun loadStates() {
        /*val states = resources.getStringArray(R.array.states)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, states)
        mFragmentBinding.state.adapter = adapter*/
        mFragmentBinding.state.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    mViewModel.fillState(requireContext().resources.getStringArray(R.array.states)[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
    }

    override fun initActions() {
        mFragmentBinding.buttonLocation.setOnClickListener {
            representativeAdapter.submitList(emptyList())
            checkLocationPermissions()
        }
    }

    override fun initObservers() {
        mViewModel.representatives.observe(viewLifecycleOwner) { representatives ->
            representativeAdapter.submitList(representatives)
        }
        mViewModel.errorMessage.observe(viewLifecycleOwner) {
            Snackbar.make(mFragmentBinding.root, it, Snackbar.LENGTH_SHORT).show()
        }
        mViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                mFragmentBinding.loadingView.visibility = View.VISIBLE
            } else {
                mFragmentBinding.loadingView.visibility = View.GONE
            }
        }
    }

    override fun layoutViewDataBinding(): Int = R.layout.fragment_representative

    private fun checkLocationPermissions(): Boolean {
        return if (mContext.isAccessFineLocation()) {
            getLocation()
            true
        } else {
            permissionAccessLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        //TODO: Get location from LocationServices
        //TODO: The geoCodeLocation method is a helper function to change the lat/long location to a human readable street address
        mFragmentBinding.loadingView.visibility = View.VISIBLE
        val cancelToken = object : CancellationToken() {
            override fun isCancellationRequested(): Boolean {
                return false
            }

            override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                return this
            }

        }
        mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelToken)
            .addOnSuccessListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    it?.let { location ->
                        mViewModel.searchForRepresentatives(geoCodeLocation(location))
                    }
                }
            }.addOnCompleteListener {
                mFragmentBinding.loadingView.visibility = View.GONE
            }.addOnFailureListener {
                mFragmentBinding.loadingView.visibility = View.GONE
            }
    }

    private fun geoCodeLocation(location: Location): Address? {
        if (Geocoder.isPresent().not()) return null
        val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
        return geocoder?.getFromLocation(location.latitude, location.longitude, 1)?.map { address ->
            Address(
                address.thoroughfare.orEmpty(),
                address.subThoroughfare.orEmpty(),
                address.locality.orEmpty(),
                address.adminArea.orEmpty(),
                address.postalCode.orEmpty()
            )
        }?.firstOrNull()
    }

    private fun hideKeyboard() {
        //val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val imm = activity?.getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

}