package com.parrot.hellodrone

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.parrot.hellodrone.databinding.FragmentExitBinding
import com.parrot.hellodrone.databinding.FragmentLoginBinding


class ExitFragment : Fragment() {
    private lateinit var binding: FragmentExitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            FragmentExitBinding.inflate(inflater, container, false)
        binding.YesButton.setOnClickListener { view : View ->
            Navigation.findNavController(view as View)
                .navigate(R.id.action_exitFragment_to_loginFragment)
        }
        return binding.root
    }


}