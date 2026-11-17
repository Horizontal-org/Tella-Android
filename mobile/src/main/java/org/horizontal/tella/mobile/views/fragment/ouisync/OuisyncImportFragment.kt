package org.horizontal.tella.mobile.views.fragment.ouisync

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.hzontal.shared_ui.utils.ScreenSecurity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.equalitie.ouisync.session.OuisyncException
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.ouisync.OuisyncClient
import org.horizontal.tella.mobile.databinding.FragmentOuisyncImportBinding
import timber.log.Timber

class OuisyncImportFragment : BottomSheetDialogFragment() {
    private var binding: FragmentOuisyncImportBinding? = null
    private var importing = false

    override fun getTheme(): Int = org.hzontal.shared_ui.R.style.AppBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).also { dialog ->
            ScreenSecurity.applyToDialog(dialog, requireContext())
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.setOnShowListener {
                val sheet = (dialog as? BottomSheetDialog)
                    ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener
                BottomSheetBehavior.from(sheet).isDraggable = false
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOuisyncImportBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val form = binding ?: return
        form.cancelButton.setOnClickListener { dismiss() }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateControls(form)
        }
        form.link.addTextChangedListener(watcher)
        form.importButton.setOnClickListener { importRepository() }
        updateControls(form)
    }

    override fun onDestroyView() {
        binding = null
        importing = false
        super.onDestroyView()
    }

    private fun updateControls(form: FragmentOuisyncImportBinding) {
        form.importButton.isEnabled = !importing && form.link.text.isNotBlank()
        form.importButton.alpha = if (form.importButton.isEnabled) 1f else 0.5f
        form.cancelButton.isEnabled = !importing
        isCancelable = !importing
        form.link.isEnabled = !importing
        form.progress.isVisible = importing
    }

    private fun importRepository() {
        val form = binding ?: return
        if (importing) return
        val link = form.link.text.toString().trim()
        if (link.isEmpty()) return
        val context = requireContext().applicationContext
        form.link.error = null
        importing = true
        updateControls(form)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                OuisyncClient.importRepository(context, link)
                if (binding === form) {
                    parentFragmentManager.setFragmentResult(IMPORT_COMPLETE, Bundle())
                    dismiss()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("ouisync import failed: ${e.javaClass.simpleName}")
                if (binding === form) {
                    val invalidLink =
                        e is OuisyncException.InvalidData || e is OuisyncException.InvalidInput
                    form.link.error = getString(
                        if (invalidLink) R.string.ouisync_invalid_link else R.string.ouisync_failed
                    )
                }
            } finally {
                if (binding === form) {
                    importing = false
                    updateControls(form)
                }
            }
        }
    }

    companion object {
        const val IMPORT_COMPLETE = "ouisyncImportComplete"
    }
}
