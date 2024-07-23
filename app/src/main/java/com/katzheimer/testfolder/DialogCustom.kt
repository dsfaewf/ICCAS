package com.katzheimer.testfolder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.katzheimer.testfolder.databinding.DialogCustomBinding

interface DialogCustomInterface {
    fun onClickYesButton(id: Int)
}

class ConfirmDialog(
    private val confirmDialogInterface: DialogCustomInterface,
    private val title: String,
    private val content: String?,
    private val buttonText: String,
    private val id: Int
) : DialogFragment() {

    private var _binding: DialogCustomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomBinding.inflate(inflater, container, false)
        val view = binding.root

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.dialogTitleTv.text = title
        binding.dialogDescTv.text = content ?: ""
        binding.dialogDescTv.visibility = if (content == null) View.GONE else View.VISIBLE
        binding.dialogYesBtn.text = buttonText

        if (id == -1) {
            binding.dialogNoBtn.visibility = View.GONE
        }

        binding.dialogNoBtn.setOnClickListener {
            dismiss()
        }

        binding.dialogYesBtn.setOnClickListener {
            confirmDialogInterface.onClickYesButton(id)
            dismiss()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
