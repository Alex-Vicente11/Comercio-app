package com.oax.comercioapp.ui.auth


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.oax.comercioapp.databinding.FragmentRegisterBinding

class RegisterFragment: Fragment(){
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    companion object {
        private const val TAG = "RegisterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: RegisterFragment initialized")

        setupObservers()
        setupListeners()
        setupTextWatchers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroy: Cleaning up binding")
        _binding = null
    }
}