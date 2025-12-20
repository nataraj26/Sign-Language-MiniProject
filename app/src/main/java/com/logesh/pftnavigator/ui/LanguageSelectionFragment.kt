package com.logesh.pftnavigator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.logesh.pftnavigator.R

class LanguageSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_language_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Only ISL and ASL buttons exist now
        val buttonIsl: Button = view.findViewById(R.id.button_isl)
        val buttonAsl: Button = view.findViewById(R.id.button_asl)

        // ISL → MainFragment
        buttonIsl.setOnClickListener {
            findNavController().navigate(R.id.action_languageSelectionFragment_to_mainFragment)
        }

        // ASL → AslFragment
        buttonAsl.setOnClickListener {
            findNavController().navigate(R.id.action_languageSelectionFragment_to_aslFragment)
        }
    }
}
