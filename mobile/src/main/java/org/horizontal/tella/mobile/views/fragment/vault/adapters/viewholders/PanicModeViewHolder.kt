package org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class PanicModeViewHolder (val view : View) : BaseViewHolder<String>(view) {
    private lateinit var panicSeekBar : SeekBar

    override fun bind(item: String, vaultClickListener: VaultClickListener) {
        panicSeekBar = view.findViewById(R.id.panic_seek)

        panicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                   // blendPanicScreens(i)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress > 0) {
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress == 100) {
                   // vaultClickListener.onPanicModeSwipeListener(progress = seekBar.progress)
                    seekBar.progress = 0
                } else {

                }
            }
        })

    }

    companion object {
        fun from(parent: ViewGroup): PanicModeViewHolder {
            return PanicModeViewHolder(parent.inflate(R.layout.item_vault_panic_button))
        }
    }

    private fun blendPanicScreens(i: Int) {
        panicSeekBar.visibility = if (i == 100) View.INVISIBLE else View.VISIBLE
    }

}