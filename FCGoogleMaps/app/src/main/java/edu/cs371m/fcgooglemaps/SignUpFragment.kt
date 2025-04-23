package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.cs371m.fcgooglemaps.data.FirebaseRepository
import edu.cs371m.fcgooglemaps.databinding.FragmentSignUpBinding
import edu.cs371m.fcgooglemaps.model.UserProfile
import edu.cs371m.fcgooglemaps.model.Vehicle
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignUp.setOnClickListener {
            val email = binding.etUsername.text.toString()
            val pass  = binding.etPassword.text.toString()
            val name  = binding.etName.text.toString()
            lifecycleScope.launch {
                FirebaseRepository.signUp(email, pass)
                    .onSuccess { user ->
                        // Create initial profile
                        val profile = UserProfile(
                            uid = user.uid,
                            username = name,
                            bio = "",
                            profilePicUrl = "",
                            vehicle = Vehicle()
                        )
                        FirebaseRepository.updateUserProfile(profile)
                        findNavController().navigate(R.id.action_signUp_to_main_graph)
                    }
                    .onFailure {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
