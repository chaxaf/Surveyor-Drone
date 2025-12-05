package com.parrot.hellodrone

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.parrot.hellodrone.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentLoginBinding
    private lateinit var sharedViewModel: SharedViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding =
            FragmentLoginBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        viewModel.key = viewModel.profileRef.push().key.toString()
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.app_name)


        binding.Userimage.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
            imgIntent.setType("image/*")
            resultLauncher.launch(imgIntent)
        }

        viewModel.uploadSuccsess.observe(viewLifecycleOwner, Observer { success ->
            if (success == false){
                Toast.makeText(context,"Profile image upload to firebase was failed.",
                    Toast.LENGTH_SHORT).show()
            }
        })


        binding.SignUp.setOnClickListener { view : View ->
            if(!viewModel.isConnected() ){
                Toast.makeText(context,"No internet connexion", Toast.LENGTH_SHORT).show()
            }
            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.imageUri == null) {
                Toast.makeText(context,"Pick an image", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()
                viewModel.sendDataToFireBase(activity?.applicationContext)

                // only for debug purposes, switch to videostream
                Log.d("YourFragment", "about to switch navigation")
                Navigation.findNavController(view)
                   .navigate(R.id.action_loginFragment_to_videoStreamFragment)
            }
        }


        viewModel.profilePresent.observe(viewLifecycleOwner, Observer { success ->
            if (success == false){
                Toast.makeText(context,"Incorrect password/username", Toast.LENGTH_SHORT).show()
            }

            else if (success == true){
                val userProfile = UserDataClass(username = viewModel.username, image = viewModel.imageUri, userKey = viewModel.key)
                (activity as MainActivity).loginInfo = userProfile
    /**
    * alternative method using safe args but i couldn't set up safe args soo
                val directions = LoginFragmentDirections
                    .actionLoginProfileFragmentToNewRecordingFragment(userProfile)
                findNavController().navigate(directions)
    **/
                Log.d("YourFragment", "about to switch navigation")
                //sharedViewModel.droneConnection(activity as MainActivity)
                Navigation.findNavController(view as View)
                .navigate(R.id.action_loginFragment_to_videoStreamFragment)

                /**
                 * TODO : modify this with ariane's code
                (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
                **/
            }
        })

        binding.SignIn.setOnClickListener { view : View ->

            if(!viewModel.isConnected() ){
                Toast.makeText(context,"No internet connexion", Toast.LENGTH_SHORT).show()
            }

            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()
                viewModel.fetchProfile()

            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).setBottomNavigationVisibility(View.GONE)
    }


    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                viewModel.imageUri = imageUri
                binding.Userimage.setImageURI(imageUri)
            }
        }
}