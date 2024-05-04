package dev.arkbuilders.sample.storage

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import dev.arkbuilders.sample.R
import dev.arkbuilders.sample.databinding.DialogMapEntryBinding

class MapEntryDialog(private val isDelete: Boolean,
                     private val onDone: ((key: String, value: String?) -> Unit)? = null)
    : DialogFragment() {

    private lateinit var mBinding: DialogMapEntryBinding
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mBinding = DialogMapEntryBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext(), R.style.SampleDialog)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setContentView(mBinding.root)
        initView()
        return dialog
    }

    private fun initView() {
        if (isDelete) {
            mBinding.tvTitle.text = getString(R.string.delete_map_entry)
            mBinding.edtValue.visibility = View.GONE
            mBinding.edtValue.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onDone?.invoke(mBinding.edtKey.text.toString(), null)
                    dismiss()
                    return@setOnEditorActionListener true
                }
                false
            }
        } else {
            mBinding.tvTitle.text = getString(R.string.new_map_entry)
            mBinding.edtValue.visibility = View.VISIBLE
            mBinding.edtValue.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onDone?.invoke(mBinding.edtKey.text.toString(), mBinding.edtValue.text.toString())
                    return@setOnEditorActionListener true
                }
                false
            }
        }
        mBinding.btnDone.setOnClickListener {
            onDone?.invoke(mBinding.edtKey.text.toString(), mBinding.edtValue.text.toString())
            dismiss()
        }

        mBinding.ivClose.setOnClickListener {
            dismiss()
        }

    }

}